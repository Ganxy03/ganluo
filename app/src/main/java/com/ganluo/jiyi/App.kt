package com.ganluo.jiyi

import android.app.Application
import android.util.Log
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer
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
 *
 * 启动时:
 *  1. 高德 SDK 隐私合规声明(**必须在任何定位/地图调用之前**)
 *  2. 版本检查
 *  3. 有 token → 尝试启动位置共享 Service
 *
 * ⚠️ 高德 SDK 8.x 起强制隐私合规前置:不调用 updatePrivacyShow/updatePrivacyAgree,
 *    定位会静默失败(只回调 errorCode,不返回坐标),地图也可能白屏。
 *    这是「定位一直不工作」的根因。
 */
@HiltAndroidApp
class App : Application() {

    @Inject lateinit var appUpdateChecker: AppUpdateChecker
    @Inject lateinit var tokenStore: TokenStore

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        // ===== 1) 高德隐私合规(必须最先执行) =====
        runCatching {
            // 定位 SDK
            AMapLocationClient.updatePrivacyShow(this, true, true)
            AMapLocationClient.updatePrivacyAgree(this, true)
            // 地图 SDK
            MapsInitializer.updatePrivacyShow(this, true, true)
            MapsInitializer.updatePrivacyAgree(this, true)
            Log.i("AMapPrivacy", "privacy compliance OK")
        }.onFailure { Log.e("AMapPrivacy", "privacy init failed", it) }

        appScope.launch {
            // ===== 2) 版本检查 =====
            runCatching { appUpdateChecker.checkOnLaunch() }
            // ===== 3) 已有 token → 尝试启动位置共享 =====
            //    注意:此时可能还没有定位权限,LocationShareService.start() 内部会
            //    自行判断:没权限就只是登记"待启动",不会抛 SecurityException,
            //    等权限授予后由 MainActivity 再调一次真正启动。
            runCatching {
                val t = tokenStore.tokenFlow.first()
                if (!t.isNullOrBlank()) LocationShareService.start(this@App)
            }
        }
    }
}