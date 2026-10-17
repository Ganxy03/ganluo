package com.ganluo.jiyi.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.RequiresPermission
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 高德定位 SDK 包装(单次定位 + 持续定位):
 *  - requestOnce(): Flow<AmapLocationFix> 只发一次结果就结束
 *  - startContinuous(): Flow<AmapLocationFix> 持续发,collect 完自动 stop
 *
 * 高德 Location SDK 默认 GCJ-02,跟 3D MapView 坐标系一致,无需转换。
 */
data class AmapLocationFix(
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val timestamp: Long
)

@Singleton
class AmapLocationProvider @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val ctx: Context
) {
    /** 单次定位(隐私合规版:不持续后台) */
    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun requestOnce(): Flow<AmapLocationFix> = callbackFlow {
        val client = AMapLocationClient(ctx.applicationContext)
        val opt = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            isOnceLocation = true
            isOnceLocationLatest = true
            isNeedAddress = false
            isWifiScan = false
        }
        client.setLocationOption(opt)
        val listener = AMapLocationListener { loc: AMapLocation? ->
            if (loc != null && loc.errorCode == 0) {
                trySend(
                    AmapLocationFix(
                        lat = loc.latitude,
                        lng = loc.longitude,
                        accuracy = loc.accuracy,
                        timestamp = loc.time
                    )
                )
            }
            close()
        }
        client.setLocationListener(listener)
        client.startLocation()
        awaitClose { client.stopLocation(); client.onDestroy() }
    }

    /** 持续定位(共享位置时用),调用方负责生命周期 */
    @SuppressLint("MissingPermission")
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startContinuous(intervalMs: Long = 30_000L): Flow<AmapLocationFix> = callbackFlow {
        val client = AMapLocationClient(ctx.applicationContext)
        val opt = AMapLocationClientOption().apply {
            locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
            // 高德 SDK 没有"间隔"字段,持续模式由 interval 控制 startLocation 频率
            isOnceLocation = false
            isNeedAddress = false
            isWifiScan = false
        }
        client.setLocationOption(opt)
        val listener = AMapLocationListener { loc: AMapLocation? ->
            if (loc != null && loc.errorCode == 0) {
                trySend(
                    AmapLocationFix(
                        lat = loc.latitude,
                        lng = loc.longitude,
                        accuracy = loc.accuracy,
                        timestamp = loc.time
                    )
                )
            }
        }
        client.setLocationListener(listener)
        client.startLocation()
        awaitClose {
            client.stopLocation()
            client.onDestroy()
        }
    }
}
