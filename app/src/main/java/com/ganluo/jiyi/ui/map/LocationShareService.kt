package com.ganluo.jiyi.ui.map

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ganluo.jiyi.MainActivity
import com.ganluo.jiyi.R
import com.ganluo.jiyi.data.local.LocationCache
import com.ganluo.jiyi.data.repository.LocationRepository
import com.ganluo.jiyi.location.LocationFix
import com.ganluo.jiyi.location.LocationUnavailableException
import com.ganluo.jiyi.location.SystemLocationProvider
import com.ganluo.jiyi.util.CrashLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 后台位置共享 前台服务(1.0.22)
 *
 * 1.0.22 关键改动 —— **不再使用高德定位 SDK**,改用系统 LocationManager:
 *  - 1.0.20 / 1.0.21 的「打开就闪退」根因是启动即自动拉起的本服务里调用了高德定位,
 *    高德会 dlopen 自己的 native 库并执行大量内部初始化,异常是 UnsatisfiedLinkError /
 *    NoClassDefFoundError 这类 **Error**(不是 Exception),`catch (e: Exception)` 抓不到,
 *    在协程里直接掀翻整个进程。
 *  - 现在:定位走系统 GPS/基站(无 native 依赖、无 key、无配额),坐标系在 provider 内
 *    统一 WGS-84 → GCJ-02 转换,地图显示不偏移。
 *  - 同时把这里所有乐观假设都加了兜底:`catch (Throwable)` + [CoroutineExceptionHandler] +
 *    onCreate 全程 try,保证**这个服务永远不能把 App 弄崩**。
 *
 * 其它行为保持不变:
 *  - 权限感知启动(没权限不 startForeground,避免 Android 14 SecurityException)
 *  - 定位失败 5s → 10s → 30s → 60s 退避重试
 *  - 位移 > 100m 或距上次上传 > 5min 才上报
 *  - 每次拿到定位先写本地缓存,地图页可秒显示
 */
@AndroidEntryPoint
class LocationShareService : Service() {

    @Inject lateinit var locRepo: LocationRepository
    @Inject lateinit var locator: SystemLocationProvider
    @Inject lateinit var sharingState: LocationSharingState
    @Inject lateinit var cache: LocationCache

    /** 兜底:协程里漏出来的任何异常都不许带走进程 */
    private val crashGuard = CoroutineExceptionHandler { _, t ->
        Log.e(TAG, "coroutine crashed (swallowed)", t)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + crashGuard)
    private var shareJob: Job? = null

    /** 是否成功进入前台(onCreate 里设置)。没进前台就必须立刻 stopSelf */
    @Volatile private var foregroundOk = false

    private var lastUploadedLat = 0.0
    private var lastUploadedLng = 0.0
    private var lastUploadedAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        CrashLogger.stage(this, "svc.onCreate")
        val ok = runCatching {
            Log.i(TAG, "Service onCreate")
            ensureChannel()
            startInForegroundSafely()
        }.onFailure {
            Log.e(TAG, "onCreate failed", it)
            CrashLogger.reportNonFatal(this, "svc.onCreate failed", it)
        }.getOrDefault(false)

        Log.i(TAG, "startForeground ok=$ok")
        foregroundOk = ok
        sharingState.set(ok)

        if (!ok) {
            // ⚠️ 关键自救:startForegroundService() 之后如果没能真正 startForeground(),
            //    系统会在 10 秒内 **直接杀掉整个进程**——没有任何 Java 堆栈,用户看到的就是闪退。
            //    (1.0.22 线上小米 Android 15 的闪退就是这条路径。)
            //    这里必须立刻 stopSelf(),把「后台共享不可用」降级成「只是没开共享」。
            CrashLogger.stage(this, "svc.fg.failed -> stopSelf")
            // 记一笔「这台机器不允许后台定位服务」,以后不再自动尝试,
            // 避免每次启动都赌一次系统杀进程。用户可在「我的→位置共享」手动重试。
            setBackgroundBlocked(this, true)
            sRunning.set(0)
            stopSelf()
        } else {
            setBackgroundBlocked(this, false)
            CrashLogger.stage(this, "svc.fg.ok")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CrashLogger.stage(this, "svc.onStartCommand")
        // 没能进前台就直接退出,绝不能让系统等 10 秒后来杀进程
        if (!foregroundOk) {
            Log.w(TAG, "not in foreground, stopSelf")
            sRunning.set(0)
            stopSelf()
            return START_NOT_STICKY
        }
        runCatching {
            Log.i(TAG, "onStartCommand action=${intent?.action}")
            if (intent?.action == ACTION_STOP) {
                stopSelf()
                return START_NOT_STICKY
            }
            if (shareJob?.isActive == true) return START_STICKY
            shareJob = scope.launch { locationLoop() }
        }.onFailure { Log.e(TAG, "onStartCommand failed", it) }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "Service onDestroy")
        runCatching { shareJob?.cancel() }
        shareJob = null
        runCatching { scope.cancel() }
        sharingState.set(false)
        sRunning.set(0)
        super.onDestroy()
    }

    // ================= 主循环 =================

    private suspend fun locationLoop() {
        // 阶段 1:等权限(用户可能还没点允许)
        if (!hasForegroundPerm()) {
            Log.w(TAG, "no location permission yet, waiting…")
            updateNotification(NotificationMode.NO_PERM, "等待定位权限")
            while (!hasForegroundPerm()) delay(5_000L)
            Log.i(TAG, "permission granted, starting location")
            startInForegroundSafely()
        }

        // 阶段 2:定位 + 上传(退避重试,任何 Throwable 都不退出)
        CrashLogger.stage(this, "svc.loop.enter")
        var backoff = 5_000L
        while (true) {
            try {
                updateNotification(NotificationMode.REAL_TIME, "持续定位中")
                locator.startContinuous(intervalMs = LOCATION_INTERVAL_MS) { err ->
                    Log.w(TAG, "continuous error: $err")
                    updateNotification(NotificationMode.ERROR, err)
                }.collectLatest { fix ->
                    handleFix(fix)
                }
                // 流正常结束(极少见)→ 退避后重来
                backoff = 5_000L
                delay(backoff)
            } catch (e: LocationUnavailableException) {
                Log.e(TAG, "locate failed: ${e.reason}", e)
                updateNotification(NotificationMode.ERROR, e.reason)
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF_MS)
            } catch (t: Throwable) {
                // 关键:抓 Throwable 而非 Exception。UnsatisfiedLinkError /
                // NoClassDefFoundError / OutOfMemory 等都是 Error,catch(Exception) 抓不到。
                Log.e(TAG, "location loop error (swallowed)", t)
                updateNotification(NotificationMode.ERROR, "定位异常,重试中…")
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
        }
    }

    // ================= 一次定位结果的处理 =================

    private suspend fun handleFix(fix: LocationFix) {
        if (lastUploadedAt == 0L) CrashLogger.stage(this, "svc.first.fix")
        // 1) 先落本地缓存(地图页 / 冷启动秒显示,零成本)
        runCatching { cache.saveMy(fix.lat, fix.lng, fix.accuracy, fix.timestamp) }

        // 2) 节流判断
        val now = System.currentTimeMillis()
        val moved = lastUploadedAt == 0L ||
                haversineMeters(fix.lat, fix.lng, lastUploadedLat, lastUploadedLng) > MIN_DISTANCE_M
        val timedOut = (now - lastUploadedAt) > MAX_INTERVAL_MS

        if (!moved && !timedOut) {
            val sinceMin = (now - lastUploadedAt) / 60_000
            updateNotification(NotificationMode.SAVING, "静止中 · 上次 ${sinceMin} 分钟前")
            return
        }

        runCatching { locRepo.upload(fix.lat, fix.lng, fix.accuracy) }
            .onSuccess {
                Log.i(TAG, "uploaded ok lat=${fix.lat} lng=${fix.lng}")
                lastUploadedLat = fix.lat
                lastUploadedLng = fix.lng
                lastUploadedAt = now
                updateNotification(NotificationMode.REAL_TIME, "刚刚更新")
            }
            .onFailure { e ->
                Log.e(TAG, "upload failed", e)
                updateNotification(NotificationMode.ERROR, "上传失败,稍后重试")
            }
    }

    // ================= 前台通知 =================

    private enum class NotificationMode { STARTING, REAL_TIME, SAVING, NO_PERM, ERROR }

    /**
     * 进入前台。返回是否成功 —— **失败必须由调用方 stopSelf()**,否则系统会杀进程。
     *
     * 分三级降级尝试,尽量把服务真正推进前台:
     *  1. Android 14+:带 FOREGROUND_SERVICE_TYPE_LOCATION(有定位权限时)
     *  2. Android 14+ 无权限:不带类型(不声明 location,不会被要求 location 权限)
     *  3. 老系统 / 上面失败:普通两参 startForeground
     */
    private fun startInForegroundSafely(): Boolean {
        val n = runCatching {
            buildNotification(
                if (hasForegroundPerm()) NotificationMode.REAL_TIME else NotificationMode.NO_PERM,
                if (hasForegroundPerm()) "已启动" else "等待定位权限"
            )
        }.getOrElse {
            Log.e(TAG, "buildNotification failed", it)
            CrashLogger.reportNonFatal(this, "buildNotification failed", it)
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (hasForegroundPerm()) {
                runCatching {
                    startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                }.onSuccess {
                    return true
                }.onFailure {
                    // 常见原因:厂商 ROM 限制 location 类型 FGS / 后台启动限制
                    Log.e(TAG, "startForeground(location) failed", it)
                    CrashLogger.reportNonFatal(this, "startForeground(location) threw", it)
                }
            }
            runCatching {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
            }.onSuccess {
                return true
            }.onFailure {
                Log.e(TAG, "startForeground(none) failed", it)
                CrashLogger.reportNonFatal(this, "startForeground(none) threw", it)
            }
        }

        return runCatching { startForeground(NOTIF_ID, n) }
            .onFailure {
                Log.e(TAG, "startForeground failed", it)
                CrashLogger.reportNonFatal(this, "startForeground threw", it)
            }
            .isSuccess
    }

    private fun updateNotification(mode: NotificationMode, subText: String) {
        runCatching {
            val nm = getSystemService(NotificationManager::class.java) ?: return
            nm.notify(NOTIF_ID, buildNotification(mode, subText))
        }
    }

    private fun buildNotification(mode: NotificationMode, subText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, LocationShareService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_marker_me)
            .setContentTitle("吉壹 · 位置共享")
            .setContentText(
                when (mode) {
                    NotificationMode.STARTING -> "启动中…"
                    NotificationMode.REAL_TIME -> "运行中"
                    NotificationMode.SAVING -> "运行中(省电)"
                    NotificationMode.NO_PERM -> "需要定位权限"
                    NotificationMode.ERROR -> "定位异常"
                }
            )
            .setSubText(subText)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSilent(true)
            .addAction(0, "停止", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(
            CHANNEL_ID, "应用运行",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "应用后台运行提示"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        mgr.createNotificationChannel(ch)
    }

    private fun hasForegroundPerm(): Boolean = hasLocationPermission(this)

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).let { it * it } +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLng / 2).let { it * it }
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    companion object {
        const val CHANNEL_ID = "loc_share"
        const val NOTIF_ID = 1001
        const val ACTION_STOP = "com.ganluo.jiyi.action.STOP_LOC_SHARE"
        private const val TAG = "LocShareService"

        private const val MIN_DISTANCE_M = 100.0
        private const val MAX_INTERVAL_MS = 5L * 60_000
        private const val LOCATION_INTERVAL_MS = 60_000L
        private const val MAX_BACKOFF_MS = 60_000L

        private val sRunning = java.util.concurrent.atomic.AtomicInteger(0)

        /** 是否有前台定位权限(粗/精任一) */
        fun hasLocationPermission(ctx: Context): Boolean {
            val fine = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            return fine || coarse
        }

        /**
         * 智能 start:
         *  - **没有定位权限 → 直接返回,不启动 FGS**(避免 Android 14 SecurityException 崩溃)
         *    权限授予后需要 UI 层再调一次
         *  - 已在跑 → 跳过
         *  - 任何异常都吞掉:启动失败最多是「没定位」,绝不能是「闪退」
         */
        fun start(ctx: Context, force: Boolean = false) {
            runCatching {
                if (!force && isBackgroundBlocked(ctx)) {
                    Log.w(TAG, "start skipped: foreground service known-blocked on this device")
                    return
                }
                if (!hasLocationPermission(ctx)) {
                    Log.w(TAG, "start skipped: no location permission yet")
                    return
                }
                if (sRunning.get() > 0) {
                    Log.i(TAG, "already running, skip")
                    return
                }
                sRunning.set(1)
                Log.i(TAG, "starting foreground service")
                val intent = Intent(ctx, LocationShareService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(intent)
                } else {
                    ctx.startService(intent)
                }
                CrashLogger.stage(ctx, "svc.start.requested")
            }.onFailure {
                sRunning.set(0)
                Log.e(TAG, "startForegroundService failed", it)
                CrashLogger.reportNonFatal(ctx, "startForegroundService threw", it)
            }
        }

        fun stop(ctx: Context) {
            runCatching {
                Log.i(TAG, "stop requested")
                sRunning.set(0)
                ctx.stopService(Intent(ctx, LocationShareService::class.java))
            }
        }

        fun isRunning(): Boolean = sRunning.get() > 0

        // ===== 厂商 ROM 兼容开关(1.0.23)=====
        // 有些机型(线上首例:小米 Android 15)拒绝 location 类型前台服务,
        // 失败后系统会直接杀进程。失败一次就记住,不再自动重试。
        private const val PREF = "loc_share"
        private const val KEY_BLOCKED = "fgs_blocked"

        private fun prefs(ctx: Context) =
            ctx.applicationContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)

        /** 本机是否已被判定「后台定位服务不可用」 */
        fun isBackgroundBlocked(ctx: Context): Boolean =
            runCatching { prefs(ctx).getBoolean(KEY_BLOCKED, false) }.getOrDefault(false)

        private fun setBackgroundBlocked(ctx: Context, blocked: Boolean) {
            runCatching { prefs(ctx).edit().putBoolean(KEY_BLOCKED, blocked).apply() }
        }
    }
}
