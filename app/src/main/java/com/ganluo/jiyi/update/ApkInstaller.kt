package com.ganluo.jiyi.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 装 APK 前先做 SHA256 校验(如果后端给了期望值),防中间人 / DNS 劫持。
 *
 * 三种返回结果:
 *   Verified(intent) - 校验通过,Intent 可以直接 startActivity
 *   Mismatch(expected, actual) - 哈希不一致,拒装
 *   Skipped(intent) - 后端没给 sha256(空),跳过校验(老版本过渡)
 */
@Singleton
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun canInstall(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.packageManager.canRequestPackageInstalls()
    } else true

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** 校验 + 构造 Intent。如果 expectedSha256 为空,只构造 Intent 不校验。 */
    fun prepareInstall(apk: File, expectedSha256: String?): InstallResult {
        val intent = buildIntent(apk)
        if (expectedSha256.isNullOrBlank()) {
            return InstallResult.Skipped(intent)
        }
        val actual = try {
            sha256Of(apk)
        } catch (e: Throwable) {
            return InstallResult.Mismatch(expectedSha256, "read-error:${e.message}", intent)
        }
        return if (actual.equals(expectedSha256, ignoreCase = true)) {
            InstallResult.Verified(intent)
        } else {
            InstallResult.Mismatch(expectedSha256, actual, intent)
        }
    }

    private fun buildIntent(apk: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, authority, apk)
        } else {
            Uri.fromFile(apk)
        }
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun sha256Of(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            var read: Int
            while (input.read(buf).also { read = it } > 0) {
                md.update(buf, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

sealed interface InstallResult {
    data class Verified(val intent: Intent) : InstallResult
    data class Skipped(val intent: Intent) : InstallResult
    data class Mismatch(
        val expected: String,
        val actual: String,
        val intent: Intent
    ) : InstallResult
}
