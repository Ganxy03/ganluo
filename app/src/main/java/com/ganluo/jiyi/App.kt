package com.ganluo.jiyi

import android.app.Application
import com.ganluo.jiyi.data.local.TokenStore
import com.ganluo.jiyi.ui.map.LocationShareService
import com.ganluo.jiyi.update.AppUpdateChecker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application 入口,挂 Hilt 容器。
 * 启动时:
 *  - 触发版本检查(网络可达时)
 *  - 如果本地已经有 token → 默认开启位置共享 Service
 */
@HiltAndroidApp
class App : Application() {

    @Inject lateinit var appUpdateChecker: AppUpdateChecker
    @Inject lateinit var tokenStore: TokenStore

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            // 1) 版本检查
            runCatching { appUpdateChecker.checkOnLaunch() }
            // 2) 已有 token → 开启位置共享(默认开启的产品语义)
            runCatching {
                val t = tokenStore.tokenFlow.first()
                if (!t.isNullOrBlank()) LocationShareService.start(this@App)
            }
        }
    }
}
