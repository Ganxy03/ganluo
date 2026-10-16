package com.ganluo.jiyi.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ganluo.jiyi.data.remote.dto.MessageDto
import com.ganluo.jiyi.ui.component.JiyiCard
import com.ganluo.jiyi.ui.component.MessageBubble
import com.ganluo.jiyi.ui.component.SectionTitle
import com.ganluo.jiyi.ui.theme.Spacing

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageSection(
    bound: Boolean,
    messages: List<MessageDto>,
    unreadTotal: Int,
    latestUnreadDate: String?,
    currentDate: String,
    onJumpUnread: () -> Unit,
    onCompose: () -> Unit,
    onTapMessage: (MessageDto) -> Unit,
    onLongPressMessage: (MessageDto) -> Unit
) {
    JiyiCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(label = "留言提醒", action = null, onAction = null)
            Spacer(Modifier.width(Spacing.sm))
            if (unreadTotal > 0) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadTotal > 99) "99+" else unreadTotal.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (!bound) {
            Text(
                "绑定后就能看到彼此的留言",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.lg)
            )
        } else {
            if (latestUnreadDate != null && latestUnreadDate != currentDate) {
                Text(
                    text = "还有 $unreadTotal 条未读在 $latestUnreadDate,点这里查看",
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { onJumpUnread() }
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                )
                Spacer(Modifier.height(Spacing.sm))
            }

            if (messages.isEmpty()) {
                Text(
                    "这一天还没有留言",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.lg)
                )
            } else {
                Column {
                    messages.forEach { m ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onTapMessage(m) },
                                    onLongClick = { onLongPressMessage(m) }
                                )
                        ) {
                            MessageBubble(msg = m, onClick = {}, onLongClick = {})
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.sm))
            Button(
                onClick = onCompose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) { Text("＋ 写留言") }
        }
    }
}

/** 写留言/回复弹层:支持纯文字、纯图片、文字 + 图片 */
@Composable
fun ComposeMessageSheet(
    visible: Boolean,
    replyTo: MessageDto?,
    composerUpload: ComposerUpload,
    onCancel: () -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onSend: (
        content: String,
        imageUrl: String?,
        imageWidth: Int?,
        imageHeight: Int?
    ) -> Unit
) {
    if (!visible) return
    var content by remember(visible) { mutableStateOf("") }
    val isUploading = composerUpload is ComposerUpload.Loading
    val ready = composerUpload as? ComposerUpload.Ready

    // 上传失败时弹提示
    LaunchedEffect(composerUpload) {
        (composerUpload as? ComposerUpload.Failed)?.let { /* 由父级 toast/snackbar 提示 */ }
    }

    AlertDialog(
        onDismissRequest = { if (!isUploading) onCancel() },
        title = {
            Text(
                if (replyTo != null) "回复留言" else "给 TA 留言",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column {
                // 回复引用
                replyTo?.let { r ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(Spacing.sm)
                    ) {
                        Text(
                            text = r.content.ifBlank { "[图片]" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(Spacing.sm))
                }

                // 已选图片预览
                when (val st = composerUpload) {
                    is ComposerUpload.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    is ComposerUpload.Ready -> {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = st.url,
                                contentDescription = "已选图片",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                            IconButton(
                                onClick = onClearImage,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(Spacing.xs)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "移除图片",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    is ComposerUpload.Failed -> {
                        Text(
                            text = st.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    ComposerUpload.Idle -> Unit
                }

                Spacer(Modifier.height(Spacing.sm))

                // 输入框(纯图片也可以不发文字)
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 500) content = it },
                    placeholder = {
                        Text(
                            if (ready != null) "想说点什么…(可留空)" else "想对 TA 说的话…"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                Spacer(Modifier.height(Spacing.sm))

                // 图片选择按钮(图标按钮)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable(enabled = !isUploading, onClick = onPickImage)
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            Text(
                                text = if (ready != null) "换一张" else "图片",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    if (ready != null) {
                        Text(
                            text = "${ready.width}×${ready.height}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSend(
                        content.trim(),
                        ready?.url,
                        ready?.width,
                        ready?.height
                    )
                },
                enabled = !isUploading && (content.isNotBlank() || ready != null),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("发送") }
        },
        dismissButton = {
            TextButton(
                onClick = { if (!isUploading) onCancel() },
                enabled = !isUploading
            ) { Text("取消") }
        }
    )
}
