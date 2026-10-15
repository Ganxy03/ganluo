package com.ganluo.jiyi.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * 业务页面直接用:
 *   val state = rememberPermissionState(PermissionRegistry.SCAN_BLE)
 *   if (!state.allPermissionsGranted) PermissionRequester.OpenSystemSettings(state)
 *
 * 这里只暴露一个 helper composable,真正入口在用方自己 rememberMultiplePermissionsState。
 */
object PermissionRequester {
    /** 默认对话框文案,可由调用方覆盖 */
    fun rationaleFor(perms: Array<String>): String = when {
        perms.contentEquals(PermissionRegistry.SCAN_BLE) ->
            "需要蓝牙权限才能扫描和连接外设"
        perms.contentEquals(PermissionRegistry.READ_IMAGES) ->
            "需要相册权限才能选头像 / 三餐照片"
        perms.contentEquals(PermissionRegistry.CAMERA) ->
            "需要相机权限才能拍照"
        perms.contentEquals(PermissionRegistry.INSTALL_PACKAGES) ->
            "需要安装权限才能更新到新版本"
        else -> "需要相关权限"
    }
}
