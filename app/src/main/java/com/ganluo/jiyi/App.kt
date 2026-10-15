package com.ganluo.jiyi

import android.app.Application
import com.ganluo.jiyi.update.AppUpdateChecker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application 入口,挂 Hilt 容器。
 * 启动时触发版本检查(网络可达时)。
 */
@HiltAndroidApp
class App : Application() {

    @Inject lateinit var appUpdateChecker: AppUpdateChecker

    // Application 进程级 scope,SupervisorJob 保证子任务失败不互相牵连
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching { appUpdateChecker.checkOnLaunch() }
        }
    }
}
