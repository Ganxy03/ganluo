package com.ganluo.jiyi.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amap.api.maps.AMap
import com.ganluo.jiyi.ui.component.CompactTopBar
import com.ganluo.jiyi.ui.map.AmapMapView
import com.ganluo.jiyi.ui.map.focusOnBoth
import com.ganluo.jiyi.ui.map.placeMarkers
import com.ganluo.jiyi.ui.map.MarkerTarget
import com.ganluo.jiyi.ui.map.MapViewModel
import com.ganluo.jiyi.ui.theme.Spacing

/**
 * 位置地图子页(2.0):
 *  - 进入申请定位权限 + 启动 30s 轮询
 *  - 离开停止轮询
 *  - 顶栏 [🔄] 手动刷新
 */
@Composable
fun ProfileMapSubPage(
    onBack: () -> Unit,
    vm: MapViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.startPolling() }
    DisposableEffect(Unit) {
        onDispose { vm.stopPolling() }
    }
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
            title = "位置地图",
            subtitle = "我和 TA 的最新位置",
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .padding(horizontal = Spacing.screenH, vertical = Spacing.sm)
                .clip(MaterialTheme.shapes.large)
        ) {
            AmapMapView(modifier = Modifier.fillMaxSize()) { map -> amap = map }
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
            if (state.me == null && state.partner == null && !state.loading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.size(Spacing.sm))
                        Text(
                            "还没有位置信息",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            SnackbarHost(snackbar)
        }
    }
}

private fun com.ganluo.jiyi.data.remote.dto.LocationDto.toMarker(): MarkerTarget =
    MarkerTarget(lat = lat, lng = lng)



