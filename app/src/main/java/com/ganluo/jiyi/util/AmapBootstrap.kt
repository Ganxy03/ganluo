package com.ganluo.jiyi.util

import android.content.Context
import android.util.Log
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 高德 SDK 隐私合规初始化:
 *  - 高德要求在任何定位/地图调用之前声明,否则定位静默失败
 *  - 但该初始化会触发 AMap native 库加载,放在 Application.onCreate 有真机崩溃风险
 *    (ABI 不匹配 / 16KB page size / SDK 版本冲突 -> 打开即闪退)
 *  - 所以改成**首次真正要用时才初始化**,逐条 try/catch
 */
object AmapBootstrap {
    private const val TAG = "AMapBootstrap"
    private val done = AtomicBoolean(false)

    fun ensure(ctx: Context) {
        if (!done.compareAndSet(false, true)) return
        val app = ctx.applicationContext
        runCatching {
            AMapLocationClient.updatePrivacyShow(app, true, true)
            AMapLocationClient.updatePrivacyAgree(app, true)
        }.onFailure { Log.e(TAG, "location privacy init failed", it) }
        runCatching {
            MapsInitializer.updatePrivacyShow(app, true, true)
            MapsInitializer.updatePrivacyAgree(app, true)
        }.onFailure { Log.e(TAG, "map privacy init failed", it) }
        Log.i(TAG, "amap privacy ensured")
    }
}