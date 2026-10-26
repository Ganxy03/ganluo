package com.ganluo.jiyi.ui.profile

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amap.api.maps.AMap
import com.ganluo.jiyi.ui.component.CompactTopBar
import com.ganluo.jiyi.ui.map.AmapMapView
import com.ganluo.jiyi.ui.map.MapViewModel
import com.ganluo.jiyi.ui.map.MarkerTarget
import com.ganluo.jiyi.ui.map.focusOnBoth
import com.ganluo.jiyi.ui.map.placeMarkers
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.Spacing

/**
 * 位置地图子页(2.1 重做):
 *
 *  - 进页面:先渲染本地缓存 → 自动定位自己 → 拉 TA 位置
 *  - **自己也能看到自己的定位**(本地 GPS 拿点立即上图,不等上传)
 *  - 顶部状态条:定位状态 / 双方距离 / 各自更新时间
 *  - 右下角悬浮按钮:定位我自己
 */
@Composable
fun ProfileMapSubPage(
    onBack: () -> Unit,
    vm: MapViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            com.ganluo.jiyi.ui.map.LocationShareService.start(context)
            vm.locateMe(upload = true)
        }
    }

    LaunchedEffect(Unit) {
        vm.startPolling()
        val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            vm.autoLocateOnEnter()
        } else {
            permLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
    DisposableEffect(Unit) { onDispose { vm.stopPolling() } }
    LaunchedEffect(state.lastError) {
        state.lastError?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    var amap by remember { mutableStateOf<AMap?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CompactTopBar(
            title = "位置地图",
            subtitle = if (state.located) "已定位" else "定位中…",
            onBack = onBack,
            actions = {
                IconButton(onClick = { vm.load() }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "刷新",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        // ===== 状态条 =====
        MapStatusBar(
            me = state.me,
            partner = state.partner,
            partnerNickname = state.partnerNickname,
            locating = state.locating
        )

        // ===== 地图 =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .padding(horizontal = Spacing.screenH, vertical = Spacing.xs)
                .clip(MaterialTheme.shapes.large)
        ) {
            AmapMapView(modifier = Modifier.fillMaxSize()) { map -> amap = map }

            LaunchedEffect(state.me?.lat, state.partner?.lat) {
                val m = amap ?: return@LaunchedEffect
                m.placeMarkers(
                    me = state.me?.toMarker("我"),
                    partner = state.partner?.toMarker(state.partnerNickname ?: "TA")
                )
                m.focusOnBoth(
                    me = state.me?.toMarker(null),
                    partner = state.partner?.toMarker(null)
                )
            }

            // 空态
            if (state.me == null && state.partner == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (state.locating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 2.dp,
                                color = Primary
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                "正在获取你的位置…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                "还没有位置信息",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "点右下角按钮定位我",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 定位我(FAB)
            ExtendedFloatingActionButton(
                onClick = {
                    val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasPerm) vm.locateMe(upload = true)
                    else permLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.md),
                containerColor = Primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    if (state.locating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                },
                text = { Text(if (state.locating) "定位中" else "定位我", fontWeight = FontWeight.SemiBold) }
            )
        }

        Spacer(Modifier.height(Spacing.sm))
    }

    Box(modifier = Modifier.fillMaxSize()) { SnackbarHost(snackbar) }
}

/** 顶部状态条:我 / TA 的距离与更新时间 */
@Composable
private fun MapStatusBar(
    me: com.ganluo.jiyi.data.remote.dto.LocationDto?,
    partner: com.ganluo.jiyi.data.remote.dto.LocationDto?,
    partnerNickname: String?,
    locating: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenH, vertical = Spacing.xs),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(
                label = "我",
                dotColor = Primary,
                value = when {
                    locating -> "定位中…"
                    me != null -> "已定位"
                    else -> "未定位"
                }
            )
            val dist = if (me != null && partner != null) {
                formatDistance(haversine(me.lat, me.lng, partner.lat, partner.lng))
            } else "—"
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("相距", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(dist, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            StatItem(
                label = partnerNickname ?: "TA",
                dotColor = if (partner != null) Primary else MaterialTheme.colorScheme.outlineVariant,
                value = if (partner != null) agoOf(partner.updatedAt) else "未共享"
            )
        }
    }
}

@Composable
private fun StatItem(label: String, dotColor: androidx.compose.ui.graphics.Color, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(Spacing.xs))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun com.ganluo.jiyi.data.remote.dto.LocationDto.toMarker(title: String?): MarkerTarget =
    MarkerTarget(lat = lat, lng = lng, snippet = title)

private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = kotlin.math.sin(dLat / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLng / 2).let { it * it }
    return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
}

private fun formatDistance(m: Double): String = when {
    m < 1000 -> "${m.toInt()} m"
    m < 100_000 -> String.format("%.1f km", m / 1000)
    else -> String.format("%.0f km", m / 1000)
}

private fun agoOf(ts: String?): String {
    if (ts.isNullOrBlank()) return "未知"
    return runCatching {
        val s = ts.replace('T', ' ').substringBefore('.')
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val t = sdf.parse(s) ?: return ts
        when (val d = (System.currentTimeMillis() - t.time) / 1000) {
            in 0..60 -> "刚刚"
            in 61..3600 -> "${d / 60} 分钟前"
            in 3601..86400 -> "${d / 3600} 小时前"
            else -> "${d / 86400} 天前"
        }
    }.getOrDefault(ts)
}