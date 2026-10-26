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
 * 这里只做三件**绝对不会崩**的事(全部 runCatching):
 *  1. 装崩溃兜底 + 开新一轮会话面包屑
 *  2. 补报上次没发出去的崩溃 / 异常结束记录
 *  3. 检查版本更新
 *
 * 明确**不在这里**做的事(都是 1.0.20/1.0.21 闪退的历史教训):
 *  - 不初始化高德 SDK(会加载 native 库,某些机型直接崩)
 *  - 不启动前台服务(冷启动启动 FGS 在 Android 12+ 可能被系统拒绝)
 */
@HiltAndroidApp
class App : Application() {

    @Inject lateinit var appUpdateChecker: AppUpdateChecker

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // 1) 崩溃兜底(必须在最前面)
        runCatching { CrashLogger.install(this) }
        runCatching { CrashLogger.beginSession(this) }

        appScope.launch {
            // 2) 补报上次没发出去的诊断信息
            runCatching { CrashLogger.uploadPending(this@App) }
            // 3) 版本检查
            runCatching { appUpdateChecker.checkOnLaunch() }
        }
    }
}
