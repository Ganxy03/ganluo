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
import com.ganluo.jiyi.data.repository.LocationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 后台位置共享 前台服务(默认开启):
 *  - 启动: `LocationShareService.start(ctx)` — 登录成功 / App 启动后自动调
 *  - 智能省电:距离上次上传 >100m **OR** 时间 >5min 才上传
 *  - 没定位权限时,以「轻松模式」空跑,不闪退不崩溃,通知栏文字友好提示
 *  - 通知栏低调显示「运行中」,只点开才能看详情
 *  - 通知带「停止」按钮(用户主动终止入口)
 */
@AndroidEntryPoint
class LocationShareService : Service() {

    @Inject lateinit var locRepo: LocationRepository
    @Inject lateinit var amap: AmapLocationProvider
    @Inject lateinit var sharingState: LocationSharingState

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var shareJob: Job? = null

    private var lastUploadedLat: Double = 0.0
    private var lastUploadedLng: Double = 0.0
    private var lastUploadedAt: Long = 0L

    /** 当前 Service 是否真的有定位权限 */
    private var hasLocationPerm: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service onCreate")
        ensureChannel()
        startInForeground(buildNotification(NotificationMode.STARTING, "正在启动…"))
        sharingState.set(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service onStartCommand action=${intent?.action}")
        // 通知栏「停止」按钮点了之后
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (shareJob?.isActive == true) return START_STICKY

        hasLocationPerm = checkLocationPerm(this)
        shareJob = scope.launch {
            if (!hasLocationPerm) {
                Log.w(TAG, "no location perm, idle loop")
                updateNotification(NotificationMode.NO_PERM, "未授予定位权限,无法上传")
                return@launch
            }
            // 1) 启动先拿一次
            try {
                amap.requestOnce().collect { fix ->
                    Log.i(TAG, "requestOnce fix: $fix")
                    handleFix(fix)
                    return@collect
                }
            } catch (e: Exception) {
                Log.e(TAG, "requestOnce failed", e)
                updateNotification(NotificationMode.REAL_TIME, "正在重试定位…")
            }
            // 2) 然后 30s 持续定位
            try {
                amap.startContinuous(intervalMs = 30_000L).collectLatest { fix ->
                    Log.d(TAG, "continuous fix: $fix")
                    handleFix(fix)
                }
            } catch (e: Exception) {
                Log.e(TAG, "startContinuous failed", e)
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
        super.onDestroy()
    }

    private suspend fun handleFix(fix: AmapLocationFix) {
        val now = System.currentTimeMillis()
        val moved = if (lastUploadedAt == 0L) {
            true
        } else {
            val meters = haversineMeters(fix.lat, fix.lng, lastUploadedLat, lastUploadedLng)
            meters > MIN_DISTANCE_M
        }
        val timedOut = (now - lastUploadedAt) > MAX_INTERVAL_MS
        if (moved || timedOut) {
            runCatching { locRepo.upload(fix.lat, fix.lng, fix.accuracy) }
                .onSuccess {
                    Log.i(TAG, "uploaded ok: $fix")
                    lastUploadedLat = fix.lat
                    lastUploadedLng = fix.lng
                    lastUploadedAt = now
                    updateNotification(NotificationMode.REAL_TIME, "刚刚上传")
                }
                .onFailure { e ->
                    Log.e(TAG, "upload failed: $fix", e)
                    updateNotification(NotificationMode.REAL_TIME, "上传失败,稍后重试")
                }
        } else {
            val sinceMin = (now - lastUploadedAt) / 60_000
            updateNotification(NotificationMode.SAVING, "静止中 · 上次 ${sinceMin} 分钟前")
        }
    }

    private enum class NotificationMode { STARTING, REAL_TIME, SAVING, NO_PERM }

    private fun startInForeground(initial: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                initial,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIF_ID, initial)
        }
    }

    private fun updateNotification(mode: NotificationMode, subText: String) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.notify(NOTIF_ID, buildNotification(mode, subText))
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
            Intent(this, LocationShareService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_marker_me)
            .setContentTitle("吉壹 · 位置共享")
            .setContentText(when (mode) {
                NotificationMode.STARTING  -> "启动中…"
                NotificationMode.REAL_TIME -> "运行中"
                NotificationMode.SAVING    -> "运行中(省电)"
                NotificationMode.NO_PERM   -> "需要定位权限"
            })
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

    private fun checkLocationPerm(ctx: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

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

        /**
         * 智能 start:
         *  - 如果 sharingState 已为 true(Service 已在跑)→ 跳过
         *  - 否则启动前台服务
         */
        fun start(ctx: Context) {
            // sharingState 是单例,跨进程同步。这里快速读一下当前已知状态;
            // 但注意:Service 是另一个进程,getInstance 注入是 Application 进程的 DI 图。
            // 改成:通过全局静态计数(进程内),简单可靠 —— 不持久化,App 重启会重置
            if (sRefCount.get() > 0) {
                Log.i(TAG, "already running in this proc, skip start")
                return
            }
            sRefCount.incrementAndGet()
            Log.i(TAG, "start requested from ${ctx.javaClass.simpleName}")
            val intent = Intent(ctx, LocationShareService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun stop(ctx: Context) {
            Log.i(TAG, "stop requested from ${ctx.javaClass.simpleName}")
            sRefCount.set(0)
            ctx.stopService(Intent(ctx, LocationShareService::class.java))
        }

        /** 当前 Application 进程内 Service 是不是在跑(只判断本进程,另一进程不感知) */
        fun isRunningProcess(): Boolean = sRefCount.get() > 0

        // 仅本进程内存计数(简洁可用)
        private val sRefCount = java.util.concurrent.atomic.AtomicInteger(0)
    }
}
