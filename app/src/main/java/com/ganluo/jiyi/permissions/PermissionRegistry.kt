package com.ganluo.jiyi.permissions

import android.Manifest
import android.os.Build

/**
 * 权限表:key → 需要的 Manifest.permission 数组。
 * 进具体功能前由 PermissionRequester.ensure(...) 取用。
 */
object PermissionRegistry {

    /** Android 12+ 用新权限;11 及以下用定位 */
    val SCAN_BLE: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /** 上传图片 / 选头像 */
    val READ_IMAGES: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /** 拍照 */
    val CAMERA: Array<String> = arrayOf(Manifest.permission.CAMERA)

    /** 安装 APK(更新流) */
    val INSTALL_PACKAGES: Array<String> = arrayOf(Manifest.permission.REQUEST_INSTALL_PACKAGES)
}
