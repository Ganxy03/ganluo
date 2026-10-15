package com.ganluo.jiyi.update

import android.content.Context
import com.ganluo.jiyi.BuildConfig
import com.ganluo.jiyi.data.repository.UpdateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App 启动时拉一次版本,新版本就写 state 让 UI 弹 UpdateSheet。
 * 实际下载/安装交给 Downloader / ApkInstaller。
 */
@Singleton
class AppUpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: UpdateRepository
) {
    private val _pending = MutableStateFlow<PendingUpdate?>(null)
    val pending = _pending.asStateFlow()

    /** 手动触发检查(给「检查更新」按钮用),返回明确结果。 */
    suspend fun tryCheck(): UpdateCheckResult = try {
        val v = repo.latest()
        if (v.latestCode > BuildConfig.VERSION_CODE) {
            val p = PendingUpdate(
                latestCode   = v.latestCode,
                latestName   = v.latestName,
                downloadUrl  = v.downloadUrl,
                sha256       = v.sha256?.takeIf { it.isNotBlank() },
                changelog    = v.changelog,
                forceUpdate  = v.forceUpdate
            )
            _pending.value = p
            UpdateCheckResult.HasUpdate(p.latestName)
        } else {
            // 没新版本,把残留的 pending 清掉(用户手动点过的话该消)
            _pending.value = null
            UpdateCheckResult.UpToDate
        }
    } catch (e: Throwable) {
        UpdateCheckResult.Failed(e.message ?: "网络错误")
    }

    /** 启动自动检查:有新版本就写 state,失败静默(不影响主流程)。 */
    suspend fun checkOnLaunch() {
        tryCheck()  // 结果丢弃,启动时不打扰
    }

    fun clear() { _pending.value = null }
}

sealed interface UpdateCheckResult {
    data class HasUpdate(val latestName: String) : UpdateCheckResult
    object UpToDate : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

data class PendingUpdate(
    val latestCode: Int,
    val latestName: String,
    val downloadUrl: String,
    val sha256: String?,           // 后端返回的 APK SHA256(hex,小写);空/null = 跳过校验
    val changelog: String?,
    val forceUpdate: Boolean
)
