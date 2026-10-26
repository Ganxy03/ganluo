package com.ganluo.jiyi.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RequiresPermission
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.ganluo.jiyi.util.AmapBootstrap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 高德定位 SDK 包装(2.1):
 *  - requestOnce(): 单次定位,成功发 1 个 fix 后结束;失败抛 [AmapLocationException]
 *  - startContinuous(): 持续定位,30s 一次;单次错误不会中断流
 *
 * 坐标系:高德返回 GCJ-02,和 3D MapView 一致,无需转换。
 *
 * ⚠️ 前置条件:App.onCreate() 里必须已调用
 *    AMapLocationClient.updatePrivacyShow / updatePrivacyAgree,
 *    否则 SDK 只回调 errorCode 且不返回坐标。
 */
data class AmapLocationFix(
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val timestamp: Long
)

/** 高德定位错误(带 errorCode,便于 UI 给出人话提示) */
class AmapLocationException(
    val code: Int,
    val detail: String?
) : Exception("AMapLocation error $code ${detail.orEmpty()}")

@Singleton
class AmapLocationProvider @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val ctx: Context
) {

    /**
     * 单次定位(高精度)。
     * 成功:emit 一个 fix 然后 close()
     * 失败:close(AmapLocationException) → collect 处会抛异常
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun requestOnce(): Flow<AmapLocationFix> = callbackFlow {
        AmapBootstrap.ensure(ctx)
        val client = AMapLocationClient(ctx.applicationContext)
        val opt = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isOnceLocation = true
            isOnceLocationLatest = true
            isNeedAddress = false        // 不做逆地理编码(省配额 / 省流量)
            isWifiScan = false           // 不需要「附近设备」权限,精度略降但兼容性好
            isLocationCacheEnable = false
            httpTimeOut = 12_000L
        }
        client.setLocationOption(opt)

        val listener = AMapLocationListener { loc: AMapLocation? ->
            if (loc == null) {
                close(AmapLocationException(-1, "定位回调为空"))
                return@AMapLocationListener
            }
            if (loc.errorCode == 0) {
                trySend(
                    AmapLocationFix(
                        lat = loc.latitude,
                        lng = loc.longitude,
                        accuracy = loc.accuracy,
                        timestamp = loc.time
                    )
                )
                close()
            } else {
                close(AmapLocationException(loc.errorCode, loc.errorInfo))
            }
        }
        client.setLocationListener(listener)
        client.startLocation()

        awaitClose {
            runCatching { client.stopLocation() }
            runCatching { client.onDestroy() }
        }
    }

    /**
     * 持续定位。intervalMs 直接下发给 SDK,默认 30s。
     * 单次错误只回调 onError(不中断流),由调用方决定是否重启。
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startContinuous(
        intervalMs: Long = 30_000L,
        onError: (AmapLocationException) -> Unit = {}
    ): Flow<AmapLocationFix> = callbackFlow {
        AmapBootstrap.ensure(ctx)
        val client = AMapLocationClient(ctx.applicationContext)
        val opt = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isOnceLocation = false
            isNeedAddress = false
            isWifiScan = false
            isLocationCacheEnable = false
            httpTimeOut = 12_000L
            // 关键:告诉 SDK 每 intervalMs 才回调一次,默认 2s 会耗电 + 刷后端
            interval = intervalMs
        }
        client.setLocationOption(opt)

        val listener = AMapLocationListener { loc: AMapLocation? ->
            if (loc == null) return@AMapLocationListener
            if (loc.errorCode == 0) {
                trySend(
                    AmapLocationFix(
                        lat = loc.latitude,
                        lng = loc.longitude,
                        accuracy = loc.accuracy,
                        timestamp = loc.time
                    )
                )
            } else {
                onError(AmapLocationException(loc.errorCode, loc.errorInfo))
            }
        }
        client.setLocationListener(listener)
        client.startLocation()

        awaitClose {
            runCatching { client.stopLocation() }
            runCatching { client.onDestroy() }
        }
    }

    companion object {
        /** 把高德 errorCode 翻译成人话,给 UI 用 */
        fun describe(code: Int, detail: String?): String = when (code) {
            12 -> "缺少定位权限,请在系统设置里允许「位置信息」"
            13 -> "定位失败,请检查 GPS 是否开启"
            14 -> "定位超时,请到空旷处重试"
            15 -> "定位服务不可用"
            16 -> "网络异常,请检查网络"
            17 -> "定位失败,请稍后重试"
            18 -> "定位服务异常"
            19 -> "定位服务被禁用"
            20 -> "定位失败,请检查定位开关"
            else -> if (detail.isNullOrBlank()) "定位失败($code)" else "定位失败($code):$detail"
        }
    }
}