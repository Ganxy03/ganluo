package com.ganluo.jiyi.ui.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

/**
 * 高德 3D MapView 的 Compose 包装:
 *  - 创建 / 销毁 MapView(配合生命周期)
 *  - 提供 [updateMarkers] 给外部刷新两个 marker(我 + TA)
 *  - 提供 [focusOn] 把相机对准某点
 */
@Composable
fun AmapMapView(
    modifier: Modifier = Modifier,
    onMapReady: (AMap) -> Unit = {}
) {
    val context = LocalContext.current
    val mapView = remember { createMapView(context) }

    DisposableEffect(Unit) {
        mapView.onCreate(null)
        mapView.onResume()
        onMapReady(mapView.map)
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView }
    )
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
