package com.ganluo.jiyi.ui.nav

import android.content.Context
import androidx.lifecycle.ViewModel
import com.ganluo.jiyi.update.ApkInstaller
import com.ganluo.jiyi.update.AppUpdateChecker
import com.ganluo.jiyi.update.Downloader
import com.ganluo.jiyi.update.InstallResult
import com.ganluo.jiyi.update.PendingUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject

/**
 * AppRoot 用的 VM:把 AppUpdateChecker.pending 透给 UI,
 * 并提供下载 / 安装回调(在 Compose 里直接调)。
 */
@HiltViewModel
class AppRootViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    val updateChecker: AppUpdateChecker,
    private val downloader: Downloader,
    private val installer: ApkInstaller
) : ViewModel() {

    val pending: StateFlow<PendingUpdate?> = updateChecker.pending

    /** 触发下载,返回 Flow 给 UI 收集状态。 */
    fun startDownload(url: String, code: Int) = downloader.download(url, code)

    /** 装 APK。返回 Verified 时直接 startActivity;Mismatch 不装。 */
    fun install(apk: File, expectedSha256: String?): InstallResult {
        val r = installer.prepareInstall(apk, expectedSha256)
        when (r) {
            is InstallResult.Verified -> appContext.startActivity(r.intent)
            is InstallResult.Skipped  -> appContext.startActivity(r.intent)
            is InstallResult.Mismatch -> { /* 拒装 */ }
        }
        return r
    }

    fun dismissUpdate() = updateChecker.clear()
}
