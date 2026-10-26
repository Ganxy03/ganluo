package com.ganluo.jiyi

import android.app.Application
import com.ganluo.jiyi.update.AppUpdateChecker
import com.ganluo.jiyi.util.CrashLogger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application 入口,挂 Hilt 容器。
 *
 * 注意(1.0.21 起):
 *  - **不在这里碰高德 SDK**。高德隐私合规初始化会触发 native 库加载,
 *    某些真机 ABI/页大小不匹配会直接 native 崩溃(表现为打开就闪退)。
 *    改由 [com.ganluo.jiyi.util.AmapBootstrap] 在首次真正使用定位/地图时懒初始化。
 *  - **不在这里启动位置共享 Service**。冷启动时 App 还没进前台,
 *    startForegroundService 在 Android 12+ 可能抛 ForegroundServiceStartNotAllowedException。
 *    改由 AppRoot 在界面就绪且权限齐备后启动。
 */
@HiltAndroidApp
class App : Application() {

    @Inject lateinit var appUpdateChecker: AppUpdateChecker

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        // 崩溃日志(写 filesDir/crash-last.txt + logcat CRASHLOG)
        runCatching { CrashLogger.install(this) }

        appScope.launch {
            runCatching { appUpdateChecker.checkOnLaunch() }
        }
    }
}