package com.ganluo.jiyi.ui.map

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.ganluo.jiyi.BuildConfig
import com.ganluo.jiyi.R
import com.ganluo.jiyi.util.AmapBootstrap

/**
 * 高德 3D MapView 的 Compose 包装(1.0.22 加固版)
 *
 * 安全策略(逐层拦截,目标是「地图挂了也不能闪退」):
 *  1. 先用 [AmapNative] 预检 native 库能不能 dlopen,不行直接走降级 UI
 *  2. 隐私合规初始化逐条 try/catch
 *  3. MapView 构造 try/catch
 *  4. 生命周期回调 try/catch
 *
 * 注意:即使是 native 的 SIGSEGV,Java 也挡不住;预检是目前能做到的最强防线。
 * 真正要彻底消除风险,只能不用 native 地图(改成 WebView / 外部地图 App)。
 */
@Composable
fun AmapMapView(
    modifier: Modifier = Modifier,
    /** 降级 UI:参数是不可用原因(放在 onMapReady 之前,保证调用处尾随 lambda 仍绑定 onMapReady) */
    fallback: @Composable (String) -> Unit = { DefaultMapFallback(it) },
    onMapReady: (AMap) -> Unit = {}
) {
    val context = LocalContext.current

    if (!AmapNative.available) {
        fallback(AmapNative.reason)
        return
    }

    // 懒初始化高德隐私合规(首次进地图页才触碰 SDK,避免启动期 native 崩溃)
    remember { runCatching { AmapBootstrap.ensure(context) }; Unit }
    // MapView 创建可能因 native 库加载失败而抛错,兜底返回 null 由 UI 显示占位
    val mapView = remember { runCatching { createMapView(context) }.getOrNull() }
    if (mapView == null) {
        fallback("地图组件初始化失败")
        return
    }

    DisposableEffect(Unit) {
        runCatching {
            mapView.onCreate(null)
            mapView.onResume()
            onMapReady(mapView.map)
        }
        onDispose {
            runCatching { mapView.onPause() }
            runCatching { mapView.onDestroy() }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView }
    )
}

@Composable
private fun DefaultMapFallback(reason: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                "地图组件在当前设备不可用",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                reason.take(120),
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

private fun createMapView(context: Context): MapView {
    // AMap 11.x 的 AMapOptions 字段名换了不少,这里直接走默认构造;
    // UI 覆盖(共享开关等)由 Compose 层负责。
    return MapView(context)
}

/**
 * 在地图上放置 / 更新两个 marker(自己 / 伴侣)。
 *  marker 用 vector drawable 转 BitmapDescriptor。
 */
fun AMap.placeMarkers(me: MarkerTarget?, partner: MarkerTarget?) {
    // AMap 没有 "update" API,先 clear 再 add。简单实现够用。
    clear()
    me?.let { m ->
        addMarker(
            MarkerOptions()
                .position(LatLng(m.lat, m.lng))
                .title("我")
                .snippet(m.snippet)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_me))
                .anchor(0.5f, 1.0f)
        )
    }
    partner?.let { p ->
        addMarker(
            MarkerOptions()
                .position(LatLng(p.lat, p.lng))
                .title("TA")
                .snippet(p.snippet)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_partner))
                .anchor(0.5f, 1.0f)
        )
    }
}

/** 把相机居中到包含两个点的矩形;只有一个就只盯它 */
fun AMap.focusOnBoth(me: MarkerTarget?, partner: MarkerTarget?) {
    val points = listOfNotNull(me?.let { LatLng(it.lat, it.lng) },
        partner?.let { LatLng(it.lat, it.lng) })
    when (points.size) {
        0 -> return
        1 -> animateCamera(CameraUpdateFactory.newLatLngZoom(points[0], 15f))
        else -> {
            val bounds = LatLngBounds.builder().apply {
                points.forEach { include(it) }
            }.build()
            animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))
        }
    }
}

/** 标点数据 */
data class MarkerTarget(
    val lat: Double,
    val lng: Double,
    val snippet: String? = null
)

/** 兼容 AMap Marker 公开类型 */
@Suppress("unused")
private fun AMap.dummyKeepMarkerImport(): Marker? = null

@Suppress("unused")
private val buildConfigMarker: String = BuildConfig.API_BASE_URL
