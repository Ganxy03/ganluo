package com.ganluo.jiyi.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganluo.jiyi.ui.theme.Primary
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

/**
 * 弹窗 UI。持有本地状态(进度、下载完成、失败原因、安装结果)。
 * 真正下载/安装逻辑由 caller 注入(onDownload / onInstall),便于测。
 */
@Composable
fun UpdateSheet(
    pending: PendingUpdate?,
    onDismiss: () -> Unit,
    onDownload: (url: String, code: Int) -> kotlinx.coroutines.flow.Flow<Downloader.State>,
    onInstall: (apk: File, expectedSha256: String?) -> InstallResult
) {
    if (pending == null) return
    var progress by remember { mutableStateOf(0L to 0L) }
    var status by remember { mutableStateOf("") }
    var downloaded by remember { mutableStateOf<File?>(null) }
    var installMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!pending.forceUpdate) onDismiss() },
        title = { Text("发现新版本 v${pending.latestName}", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                if (!pending.changelog.isNullOrBlank()) {
                    Text(pending.changelog!!, fontSize = 13.sp)
                } else {
                    Text("更新内容", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                if (status.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(status, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (installMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        installMsg!!,
                        fontSize = 12.sp,
                        color = if (installMsg!!.startsWith("✓")) Primary
                                else MaterialTheme.colorScheme.error
                    )
                }
                if (progress.second > 0 && downloaded == null) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = (progress.first.toFloat() / progress.second).coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth(),
                        color = Primary
                    )
                }
            }
        },
        confirmButton = {
            when {
                downloaded != null -> {
                    Button(
                        onClick = {
                            val result = onInstall(downloaded!!, pending.sha256)
                            installMsg = when (result) {
                                is InstallResult.Verified -> "✓ 校验通过,正在唤起安装…"
                                is InstallResult.Skipped -> "⚠ 后端未提供 sha256,跳过校验,直接装"
                                is InstallResult.Mismatch ->
                                    "✗ SHA256 不一致,已拒绝安装(expected ${result.expected.take(8)}… got ${result.actual.take(12)}…)"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)
                    ) { Text("立即安装") }
                }
                status == "下载中" || status == "准备中" -> {
                    Text("下载中…")
                }
                else -> {
                    Button(
                        onClick = {
                            status = "准备中"
                            installMsg = null
                            scope.launch {
                                val flow = onDownload(pending.downloadUrl, pending.latestCode)
                                flow.collectLatest { state ->
                                    when (state) {
                                        is Downloader.State.Progress -> {
                                            progress = state.sofar to state.total
                                            status = "下载中"
                                        }
                                        is Downloader.State.Done -> {
                                            downloaded = state.file
                                            status = "下载完成"
                                        }
                                        is Downloader.State.Failed -> {
                                            status = "下载失败:${state.cause.message ?: "未知"}"
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)
                    ) { Text("立即更新") }
                }
            }
        },
        dismissButton = if (!pending.forceUpdate) {
            { TextButton(onClick = onDismiss) { Text("稍后") } }
        } else null
    )
}
