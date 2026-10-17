package com.ganluo.jiyi.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * 定位权限工具(1.0.16):
 *  - checkForeground:ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION(运行时权限)
 *  - checkBackground:ACCESS_BACKGROUND_LOCATION(API 29+;Android 10+ 用户必须手动选"始终允许")
 *  - launchAppSettings:跳到系统「应用详情」让用户手动改权限
 */
object LocationPermissionHelper {

    fun checkForeground(ctx: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkBackground(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        return ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** 跳到系统「应用详情」,Android 11+ 才有专门的「权限 → 位置 → 始终允许」入口 */
    fun launchAppSettings(ctx: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", ctx.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        ctx.startActivity(intent)
    }

    /** 用户拒了"始终允许"时,Snackbar/对话框提示用 */
    fun describeBackgroundDenied(ctx: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "请在系统设置 → 应用 → 吉壹 → 权限 → 位置,选「始终允许」"
        } else {
            "请在系统设置授予定位权限"
        }
    }

    val FOREGROUND_PERMISSIONS = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
}
