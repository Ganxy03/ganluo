package com.ganluo.jiyi.ui.profile

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.ganluo.jiyi.permissions.LocationPermissionHelper
import com.ganluo.jiyi.ui.component.CompactTopBar
import com.ganluo.jiyi.ui.map.LocationShareService
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.Spacing
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.ui.platform.LocalContext

/**
 * 位置共享 子页
 *  - 默认开启(产品要求):进入 App 登录后会自动启动 LocationShareService
 *  - 这里仅展示状态(运行中/未运行)+ 手动「停止共享」/「重新开启」
 *  - 权限缺失时引导去系统设置
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ProfileLocationSubPage(
    onBack: () -> Unit,
    vm: ProfileViewModel = hiltViewModel()
) {
    val sharing by vm.sharing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showBgDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }

    val fgPerms = rememberMultiplePermissionsState(
        LocationPermissionHelper.FOREGROUND_PERMISSIONS
    )

    fun startWithPermCheck() {
        if (!fgPerms.allPermissionsGranted) {
            fgPerms.launchMultiplePermissionRequest()
            return
        }
        if (!LocationPermissionHelper.checkBackground(context)) {
            showBgDialog = true
            return
        }
        LocationShareService.start(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CompactTopBar(title = "位置共享", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenH, vertical = Spacing.md)
        ) {
            // 状态卡
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(Spacing.xl)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (sharing) "位置共享运行中" else "位置共享未运行",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (sharing)
                                "TA 在「地图」页面能看到你的实时位置"
                            else
                                "开启后,TA 在地图页能看到你的位置",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    StatusDot(active = sharing)
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            // 说明
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(Spacing.lg)
            ) {
                Column {
                    Text(
                        "默认开启",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "登录 App 后会自动持续共享位置,无需手动打开。" +
                            "系统会在通知栏显示「吉壹 · 位置共享」运行中提示,可以随时停止。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        "智能省电",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "移动超过 100m 或距离上次上传 5 分钟,才会上传一次,既保证实时又省电。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))

            // 操作按钮
            if (sharing) {
                Button(
                    onClick = { showStopDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text("停止共享", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { startWithPermCheck() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text("开启共享", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showStopDialog) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("停止共享位置") },
            text = { Text("停止后,TA 在地图页将看不到你的实时位置。重新打开 APP 可恢复(默认开启)。") },
            confirmButton = {
                TextButton(onClick = {
                    showStopDialog = false
                    LocationShareService.stop(context)
                }) { Text("停止", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showStopDialog = false }) { Text("取消") } }
        )
    }

    if (showBgDialog) {
        AlertDialog(
            onDismissRequest = { showBgDialog = false },
            title = { Text("需要「始终允许」") },
            text = {
                Text(
                    "App 在后台时持续共享位置,需要把位置权限设为「始终允许」。" +
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            "请在系统设置 → 应用 → 吉壹 → 权限 → 位置,选择「始终允许」。"
                        else "请在系统设置授予位置权限。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBgDialog = false
                    LocationPermissionHelper.launchAppSettings(context)
                }) { Text("去设置") }
            },
            dismissButton = { TextButton(onClick = { showBgDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun StatusDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant)
    )
}
