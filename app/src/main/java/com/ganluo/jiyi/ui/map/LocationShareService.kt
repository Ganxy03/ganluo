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
import dagger.hilt.android.AndroidEntryPoint
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
 * 后台位置共享 前台服务(2.1):
 *
 *  - **权限感知启动**:没定位权限时不启动 FGS(否则 Android 14 直接 SecurityException),
 *    只登记 pending,等权限授予后由 MainActivity 再调一次 [start]
 *  - **等待授权**:Service 内如果发现没权限,不会退出,而是每 5s 检查一次,权限一到立刻开始
 *  - **失败重试**:定位出错按 5s → 10s → 30s → 60s 退避重试,不会一次失败就躺平
 *  - **智能节流**:位移 >100m 或 距上次上传 >5min 才上传
 *  - **本地缓存**:每次拿到定位先写 [LocationCache],地图页可以秒显示
 */
@AndroidEntryPoint
class LocationShareService : Service() {

    @Inject lateinit var locRepo: LocationRepository
    @Inject lateinit var amap: AmapLocationProvider
    @Inject lateinit var sharingState: LocationSharingState
    @Inject lateinit var cache: LocationCache

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var shareJob: Job? = null

    private var lastUploadedLat = 0.0
    private var lastUploadedLng = 0.0
    private var lastUploadedAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service onCreate")
        ensureChannel()
        // 只有拿到权限才允许以 location 类型进入前台(Android 14 强校验)
        val ok = startInForegroundSafely()
        Log.i(TAG, "startForeground ok=$ok")
        sharingState.set(ok)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand action=${intent?.action}")
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (shareJob?.isActive == true) return START_STICKY

        shareJob = scope.launch {
            // ===== 阶段 1:等服务获得定位权限 =====
            if (!hasForegroundPerm()) {
                Log.w(TAG, "no location permission yet, waiting…")
                updateNotification(NotificationMode.NO_PERM, "等待定位权限")
                while (!hasForegroundPerm()) {
                    delay(5_000L)
                }
                Log.i(TAG, "permission granted, starting location")
                startInForegroundSafely()
            }

            // ===== 阶段 2:定位 + 上传(带退避重试)=====
            var backoff = 5_000L
            while (true) {
                try {
                    // 2.1 先单次定位,快速拿到第一个点
                    var gotFix = false
                    amap.requestOnce().collect { fix ->
                        gotFix = true
                        handleFix(fix)
                    }
                    if (!gotFix) throw IllegalStateException("no fix")

                    // 2.2 切持续定位(30s 一次)
                    updateNotification(NotificationMode.REAL_TIME, "持续定位中")
                    amap.startContinuous(intervalMs = LOCATION_INTERVAL_MS) { e ->
                        Log.w(TAG, "continuous error ${e.code}: ${e.message}")
                    }.collectLatest { fix ->
                        handleFix(fix)
                    }
                    // 流正常结束(极少见)→ 退避后重来
                    backoff = 5_000L
                } catch (e: AmapLocationException) {
                    val msg = AmapLocationProvider.describe(e.code, e.message)
                    Log.e(TAG, "locate failed: $msg", e)
                    updateNotification(NotificationMode.ERROR, msg)
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF_MS)
                } catch (e: Exception) {
                    Log.e(TAG, "location loop error", e)
                    updateNotification(NotificationMode.ERROR, "定位异常,重试中…")
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF_MS)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "Service onDestroy")
        shareJob?.cancel()
        shareJob = null
        scope.cancel()
        sharingState.set(false)
        sRunning.set(0)
        super.onDestroy()
    }

    // ================= 核心:一次定位结果的处理 =================

    private suspend fun handleFix(fix: AmapLocationFix) {
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

    /** 返回是否成功进入前台 */
    private fun startInForegroundSafely(): Boolean {
        return try {
            val n = buildNotification(
                if (hasForegroundPerm()) NotificationMode.REAL_TIME else NotificationMode.NO_PERM,
                if (hasForegroundPerm()) "已启动" else "等待定位权限"
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // 只有真的有定位权限时才声明 location 类型
                if (hasForegroundPerm()) {
                    startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                } else {
                    startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
                }
            } else {
                startForeground(NOTIF_ID, n)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            false
        }
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
        private const val LOCATION_INTERVAL_MS = 30_000L
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
         */
        fun start(ctx: Context) {
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
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(intent)
                } else {
                    ctx.startService(intent)
                }
            }.onFailure {
                sRunning.set(0)
                Log.e(TAG, "startForegroundService failed", it)
            }
        }

        fun stop(ctx: Context) {
            Log.i(TAG, "stop requested")
            sRunning.set(0)
            ctx.stopService(Intent(ctx, LocationShareService::class.java))
        }

        fun isRunning(): Boolean = sRunning.get() > 0
    }
}