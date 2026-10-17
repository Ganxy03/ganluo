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

/**
 * 后台位置共享前台服务(1.0.12):
 *  - 启动后立即 startForeground(API 34+ 声明 type=location)
 *  - 通知带「停止」按钮 → 用户一键关闭
 *  - amap.startContinuous(30s) 持续定位,成功即上传 LocationRepository
 *  - 退出(VM 调 stopService / 系统杀 / 用户按停止)→ onDestroy 写回 state=false
 */
@AndroidEntryPoint
class LocationShareService : Service() {

    @Inject lateinit var locRepo: LocationRepository
    @Inject lateinit var amap: AmapLocationProvider
    @Inject lateinit var sharingState: LocationSharingState

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var shareJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        startInForeground()
        sharingState.set(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // 系统恢复(Service 进程被回收后 START_STICKY 会带 null intent 再调一次)
        if (shareJob?.isActive == true) return START_STICKY
        shareJob = scope.launch {
            amap.startContinuous(intervalMs = 30_000L).collectLatest { fix ->
                runCatching { locRepo.upload(fix.lat, fix.lng, fix.accuracy) }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        shareJob?.cancel()
        shareJob = null
        scope.cancel()
        sharingState.set(false)
        super.onDestroy()
    }

    private fun startInForeground() {
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun buildNotification(): Notification {
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
            .setContentText("正在共享位置给 TA")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "停止共享", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val ch = NotificationChannel(
            CHANNEL_ID, "位置共享",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "后台位置共享时显示"
            setShowBadge(false)
        }
        mgr.createNotificationChannel(ch)
    }

    companion object {
        const val CHANNEL_ID = "loc_share"
        const val NOTIF_ID = 1001
        const val ACTION_STOP = "com.ganluo.jiyi.action.STOP_LOC_SHARE"

        fun start(ctx: Context) {
            val intent = Intent(ctx, LocationShareService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, LocationShareService::class.java))
        }
    }
}
