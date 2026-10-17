package com.ganluo.jiyi.ui.map

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
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
 * 后台位置共享前台服务(1.0.13 加智能省电):
 *  - startForeground(API 34+ 声明 type=location)
 *  - 通知带「停止」按钮
 *  - amap 持续 30s 定位
 *  - 智能过滤:距离上次上传 >100m **OR** 时间 >5min → 上传
 *    否则跳过(避免 GPS 漂移 + 静止时刷后端)
 *  - 通知栏动态显示「实时模式」/「省电模式」+ 距上次上传时长
 */
@AndroidEntryPoint
class LocationShareService : Service() {

    @Inject lateinit var locRepo: LocationRepository
    @Inject lateinit var amap: AmapLocationProvider
    @Inject lateinit var sharingState: LocationSharingState

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var shareJob: Job? = null

    // 上次成功上传的状态
    private var lastUploadedLat: Double = 0.0
    private var lastUploadedLng: Double = 0.0
    private var lastUploadedAt: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service onCreate")
        ensureChannel()
        startInForeground(buildNotification(Mode.REAL_TIME, "刚刚开始"))
        sharingState.set(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service onStartCommand action=${intent?.action}")
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (shareJob?.isActive == true) return START_STICKY
        shareJob = scope.launch {
            Log.i(TAG, "shareJob started")
            // 1) 启动时立即 requestOnce 一次
            try {
                amap.requestOnce().collect { fix ->
                    Log.i(TAG, "requestOnce fix: $fix")
                    handleFix(fix)
                    return@collect
                }
            } catch (e: Exception) {
                Log.e(TAG, "requestOnce failed", e)
            }
            Log.i(TAG, "switching to startContinuous 30s")
            // 2) 然后切到 30s 持续定位
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

    /**
     * 智能节流核心:
     *  - 距离上次上传 >100m → 移动了,上传
     *  - 时间 >5min → 即使没动,也心跳一次
     *  - 否则:跳过,只更新通知栏文本
     */
    private suspend fun handleFix(fix: AmapLocationFix) {
        val now = System.currentTimeMillis()
        val moved = if (lastUploadedAt == 0L) {
            true  // 首次 fix 必须上传
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
                    updateNotification(Mode.REAL_TIME, "刚刚上传")
                }
                .onFailure { e -> Log.e(TAG, "upload failed: $fix", e) }
        } else {
            // 跳过上传,但更新通知让用户知道 App 还在跑
            val sinceMin = (now - lastUploadedAt) / 60_000
            updateNotification(Mode.SAVING, "静止中 · 上次上传 ${sinceMin} 分钟前")
        }
    }

    private enum class Mode { REAL_TIME, SAVING }

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

    private fun updateNotification(mode: Mode, subText: String) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.notify(NOTIF_ID, buildNotification(mode, subText))
    }

    private fun buildNotification(mode: Mode, subText: String): Notification {
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
        // 1.0.14:通知改低调 — 标题只显示应用名,内容只显示模式
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_marker_me)
            .setContentTitle("吉壹")
            .setContentText(
                when (mode) {
                    Mode.REAL_TIME -> "运行中"
                    Mode.SAVING    -> "运行中(省电)"
                }
            )
            .setSubText(subText)  // 二级文本:上次上传时间,默认折叠不可见
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

        // 智能省电阈值
        private const val MIN_DISTANCE_M = 100.0
        private const val MAX_INTERVAL_MS = 5L * 60_000

        fun start(ctx: Context) {
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
            ctx.stopService(Intent(ctx, LocationShareService::class.java))
        }
    }
}
