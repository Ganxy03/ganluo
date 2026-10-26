package com.ganluo.jiyi.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.ganluo.jiyi.location.Gcj02.wgs2gcj
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** 一次定位结果。坐标已转换成 GCJ-02(可直接喂给高德地图) */
data class LocationFix(
    val lat: Double,
    val lng: Double,
    val accuracy: Float,
    val timestamp: Long,
    /** 数据来源:gps / network / fused / cached,排障用 */
    val source: String = "system"
)

/** 系统定位不可用(没开 GPS / 没权限 / 超时) */
class LocationUnavailableException(
    val reason: String
) : Exception(reason)

/**
 * 系统定位(LocalManager)封装 —— **1.0.22 起取代高德定位 SDK**。
 *
 * 为什么要换掉高德定位:
 *  1. **闪退根因**:高德定位 SDK 会加载 native 库(libAMapSDK_MAP / libapssdk),
 *     部分真机 dlopen 失败或 SDK 内部 NPE,会抛 UnsatisfiedLinkError / Error。
 *     Error 不是 Exception,catch(Exception) 抓不住 → 协程里直接掀翻进程 = 打开就闪退。
 *     1.0.20/1.0.21 两版闪退都发生在「启动即自动开启位置共享」这条链路上。
 *  2. **省配额**:高德定位/地图 API 每日额度有限,两个人高频上报很容易打满。
 *     系统定位走 GPS/基站,**完全不吃高德配额**。
 *  3. **不用 key、不用隐私合规初始化**,少一个启动期风险点。
 *
 * 代价:系统给的是 WGS-84,高德瓦片是 GCJ-02,所以统一在 [toFix] 里做一次转换。
 */
@Singleton
class SystemLocationProvider @Inject constructor(
    @ApplicationContext private val ctx: Context
) {

    private val lm: LocationManager? =
        runCatching { ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }.getOrNull()

    /** 系统定位总开关是否打开(下拉快捷开关里的「位置信息」) */
    fun isLocationEnabled(): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm?.isLocationEnabled == true
        } else {
            @Suppress("DEPRECATION")
            (lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                    lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true)
        }
    }.getOrDefault(false)

    /**
     * 取一个「最后的已知位置」。
     * 冷启动 / 飞行模式下秒返回,用来先占位显示,避免干等。
     */
    @SuppressLint("MissingPermission")
    fun lastKnown(): LocationFix? {
        val mgr = lm ?: return null
        return runCatching {
            candidateProviders()
                .mapNotNull { p -> runCatching { mgr.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull { it.time }
                ?.let { toFix(it, "cached") }
        }.getOrNull()
    }

    /**
     * 单次定位。
     *  - 30 秒内有缓存 → 直接返回缓存,不发请求(省电)
     *  - 否则申请更新,超时 [timeoutMs] 后若有缓存则降级返回缓存,否则抛 [LocationUnavailableException]
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun requestOnce(timeoutMs: Long = 20_000L): Flow<LocationFix> = callbackFlow {
        val mgr = lm ?: run { close(LocationUnavailableException("系统定位服务不可用")); return@callbackFlow }
        val cached = lastKnown()
        if (cached != null && System.currentTimeMillis() - cached.timestamp < 30_000L) {
            trySend(cached)
            close()
            return@callbackFlow
        }
        if (!isLocationEnabled()) {
            close(LocationUnavailableException("系统定位开关没打开,请在设置里允许「位置信息」"))
            return@callbackFlow
        }

        var finished = false
        val listener = LocationListener { loc ->
            if (loc != null && !finished) {
                finished = true
                trySend(toFix(loc, "live"))
                close()
            }
        }

        val providers = candidateProviders()
        if (providers.isEmpty()) {
            close(LocationUnavailableException("没有可用的定位 provider"))
            return@callbackFlow
        }

        val started = runCatching {
            providers.forEach { p -> mgr.requestLocationUpdates(p, 0L, 0f, listener, Looper.getMainLooper()) }
        }.isSuccess
        if (!started) {
            close(LocationUnavailableException("无法启动定位,请检查定位权限"))
            return@callbackFlow
        }

        // 超时兜底
        val timer = launch {
            delay(timeoutMs)
            if (!finished) {
                finished = true
                if (cached != null) trySend(cached) else close(
                    LocationUnavailableException("定位超时,请到空旷处或靠近窗户再试")
                )
                close()
            }
        }

        awaitClose {
            timer.cancel()
            runCatching { mgr.removeUpdates(listener) }
        }
    }

    /**
     * 持续定位。intervalMs 是最小回调间隔(系统可能更快回落,由调用方节流)。
     * 不会主动结束;单次失败通过 [onError] 通知,不中断流。
     */
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startContinuous(
        intervalMs: Long = 30_000L,
        onError: (String) -> Unit = {}
    ): Flow<LocationFix> = callbackFlow {
        val mgr = lm ?: run { close(LocationUnavailableException("系统定位服务不可用")); return@callbackFlow }
        if (!isLocationEnabled()) {
            close(LocationUnavailableException("系统定位开关没打开"))
            return@callbackFlow
        }

        // 先把已知位置吐出来,通知栏/地图能立刻有内容
        lastKnown()?.let { trySend(it) }

        val listener = LocationListener { loc ->
            if (loc != null) trySend(toFix(loc, "live"))
        }
        val providers = candidateProviders()
        if (providers.isEmpty()) {
            close(LocationUnavailableException("没有可用的定位 provider"))
            return@callbackFlow
        }
        runCatching {
            providers.forEach { p -> mgr.requestLocationUpdates(p, intervalMs, 0f, listener, Looper.getMainLooper()) }
        }.onFailure {
            Log.w(TAG, "requestLocationUpdates failed", it)
            onError(it.message ?: "定位启动失败")
            close(LocationUnavailableException(it.message ?: "定位启动失败"))
        }

        awaitClose { runCatching { mgr.removeUpdates(listener) } }
    }

    /** 按精度从高到低列出可用 provider(去重,兼容 API 31 的 fused) */
    private fun candidateProviders(): List<String> {
        val mgr = lm ?: return emptyList()
        val all = runCatching { mgr.allProviders }.getOrDefault(emptyList())
        val wanted = buildList {
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }
        return wanted.filter { p ->
            p in all && runCatching { mgr.isProviderEnabled(p) }.getOrDefault(false)
        }.distinct()
    }

    private fun toFix(loc: Location, source: String): LocationFix {
        val (lat, lng) = wgs2gcj(loc.latitude, loc.longitude)
        return LocationFix(
            lat = lat,
            lng = lng,
            accuracy = if (loc.hasAccuracy()) loc.accuracy else 0f,
            timestamp = if (loc.time > 0) loc.time else System.currentTimeMillis(),
            source = source
        )
    }

    companion object {
        private const val TAG = "SysLocation"
    }
}
