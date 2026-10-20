package com.ganluo.jiyi.ui.map

import android.Manifest
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amap.api.maps.AMap
import com.ganluo.jiyi.data.remote.dto.LocationDto
import com.ganluo.jiyi.ui.component.Avatar
import com.ganluo.jiyi.ui.component.CompactTopBar
import com.ganluo.jiyi.ui.theme.JiyiTheme
import com.ganluo.jiyi.ui.theme.Spacing
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * 地图页(1.0.14):
 *  - 顶部 TA 信息卡(头像 + 距离 + 上次更新时间)
 *  - 中部 高德 3D MapView(显示双方 marker)
 *  - 底部 留白(1.0.14 删了共享/定位我按钮 → 共享 toggle 在 Profile 页)
 *  - 进入页面:申请定位权限 → 30s 轮询 /both
 *  - 离开页面:停轮询
 *  - 顶栏 [🔄] 手动刷新
 *  - 顶栏 [☰] 抽屉(由 AppRoot 注入)
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    vm: MapViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val perms = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // 进入 → 申请权限 + 启动轮询
    LaunchedEffect(Unit) {
        if (!perms.allPermissionsGranted) perms.launchMultiplePermissionRequest()
        vm.startPolling()
    }
    // 离开 → 停轮询(共享保留:用户切走再回来,共享继续)
    DisposableEffect(Unit) {
        onDispose { vm.stopPolling() }
    }
    // 错误 → snackbar
    LaunchedEffect(state.lastError) {
        state.lastError?.let { snackbar.showSnackbar(it) }
    }

    var amap by remember { mutableStateOf<AMap?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CompactTopBar(
            title = "地图",
            actions = {
                IconButton(onClick = { vm.load() }) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "刷新",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        // 顶部 TA 信息卡
        PartnerLocationCard(
            nickname = state.partnerNickname ?: "TA",
            avatar = state.partnerAvatar,
            location = state.partner,
            myLocation = state.me
        )

        // 地图(weight 1,占满中间)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = Spacing.screenH, vertical = Spacing.sm)
                .clip(MaterialTheme.shapes.large)
        ) {
            AmapMapView(modifier = Modifier.fillMaxSize()) { map ->
                amap = map
            }

            // 初次 / 双方位置变化 → 重新放 marker + 居中
            LaunchedEffect(state.me?.lat, state.partner?.lat) {
                val m = amap ?: return@LaunchedEffect
                m.placeMarkers(
                    me = state.me?.toMarker(),
                    partner = state.partner?.toMarker()
                )
                m.focusOnBoth(
                    me = state.me?.toMarker(),
                    partner = state.partner?.toMarker()
                )
            }

            // 没数据时的占位提示
            if (state.me == null && state.partner == null && !state.loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            "还没有位置信息",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            "开启「共享我的位置」,就能看到彼此的位置",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // 底部留白(1.0.14 去掉共享/定位我按钮,开关已上移到 Profile)
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.md)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(snackbar)
    }
}

private fun LocationDto.toMarker(): MarkerTarget =
    MarkerTarget(lat = lat, lng = lng)

@Composable
private fun PartnerLocationCard(
    nickname: String,
    avatar: String?,
    location: LocationDto?,
    myLocation: LocationDto?
) {
    val distanceText = if (location != null && myLocation != null) {
        "距离 ${formatDistance(haversineMeters(myLocation.lat, myLocation.lng, location.lat, location.lng))}"
    } else if (location != null) {
        "TA 的位置"
    } else {
        "TA 还没共享"
    }
    val updatedText = when {
        location == null -> "未连接"
        else -> formatUpdatedAgo(location.updatedAt)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenH, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            url = avatar,
            name = nickname,
            size = 48.dp,
            online = location != null,
            ring = true
        )
        Spacer(Modifier.size(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nickname,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$distanceText · $updatedText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = kotlin.math.sin(dLat / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(lat1)) *
            kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLng / 2).let { it * it }
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return r * c
}

private fun formatDistance(meters: Double): String {
    return when {
        meters < 1000 -> "${meters.toInt()} m"
        meters < 100_000 -> String.format("%.1f km", meters / 1000.0)
        else -> String.format("%.0f km", meters / 1000.0)
    }
}

private fun formatUpdatedAgo(updatedAt: String?): String {
    if (updatedAt.isNullOrBlank()) return "未知"
    return try {
        // 兼容 "2026-10-17 14:04:55" / ISO 两种
        val s = updatedAt.replace('T', ' ').substringBefore('.')
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val t = sdf.parse(s) ?: return updatedAt
        val diffSec = (System.currentTimeMillis() - t.time) / 1000
        when {
            diffSec < 60 -> "刚刚"
            diffSec < 3600 -> "${diffSec / 60} 分钟前"
            diffSec < 86_400 -> "${diffSec / 3600} 小时前"
            else -> "${diffSec / 86_400} 天前"
        }
    } catch (_: Exception) {
        updatedAt
    }
}

// ===== Preview =====

@Preview(showBackground = true, name = "地图页")
@Composable
private fun PreviewMap() {
    JiyiTheme { MapScreen() }
}

