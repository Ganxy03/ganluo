package com.ganluo.jiyi.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ganluo.jiyi.data.remote.dto.MessageDto
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.ui.util.DateUtil

/**
 * 微信式常驻底部输入栏
 *
 * 状态(全部受控):
 *  - text           : 输入框文字
 *  - showEmojiPanel : 表情素材库展开/收起
 *  - replyTo        : 引用回复(可关闭)
 *  - composerUpload : 图片上传(Idle / Loading / Ready / Failed)
 *
 * 上方条件显示:
 *  - 引用条(有 replyTo)
 *  - 图片预览(有 imageUrl)
 *  - 表情面板(emoji panel 展开时)
 *
 * 主行(永远显示):
 *  - [😊] 表情按钮(点击展开/收起 emoji 面板)
 *  - [📷] 图片按钮(系统 photo picker)
 *  - [输入框] BasicTextField,焦点状态管理
 *  - [→] 发送按钮(text 非空 或 已选图时 enabled)
 *
 * 输入栏自动 imePadding 适配键盘,不被遮挡。
 */
@Composable
fun MessageComposer(
    text: String,
    onTextChange: (String) -> Unit,
    replyTo: MessageDto?,
    onClearReply: () -> Unit,
    composerUpload: ComposerUpload,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onEmojiPick: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 表情面板展开状态(rememberSaveable,切 tab 回来还在)
    var showEmoji by rememberSaveable { mutableStateOf(false) }

    // 输入框是否聚焦(由 BasicTextField 内部管理,这里只读)
    val canSend = text.isNotBlank() || composerUpload is ComposerUpload.Ready
    val isUploading = composerUpload is ComposerUpload.Loading

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // ===== 引用条 / 图片预览(条件显示) =====
            if (replyTo != null) {
                ReplyBar(
                    authorName = if (replyTo.isMine) "你" else "TA",
                    content = replyTo.content.ifBlank { "[图片]" },
                    onClear = onClearReply
                )
            }
            (composerUpload as? ComposerUpload.Ready)?.let { st ->
                ImagePreviewBar(
                    url = st.url,
                    width = st.width,
                    height = st.height,
                    onClear = onClearImage
                )
            }
            (composerUpload as? ComposerUpload.Loading)?.let {
                UploadingBar()
            }
            (composerUpload as? ComposerUpload.Failed)?.let {
                FailedBar(message = it.message)
            }

            // ===== 主输入行 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 表情按钮
                IconButton(
                    onClick = { showEmoji = !showEmoji },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.EmojiEmotions,
                        contentDescription = "表情",
                        tint = if (showEmoji)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 输入框(占满中间,最多 5 行)
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= 500) onTextChange(it) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp, max = 120.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    textStyle = LocalTextStyle.current.merge(
                        TextStyle(color = MaterialTheme.colorScheme.onSurface)
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text(
                                text = if (replyTo != null) "回复…" else "写点什么…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(Modifier.width(Spacing.xs))

                // 图片按钮
                IconButton(
                    onClick = onPickImage,
                    enabled = !isUploading,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        contentDescription = "图片",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 发送按钮
                IconButton(
                    onClick = onSend,
                    enabled = canSend && !isUploading,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend && !isUploading)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "发送",
                        tint = if (canSend && !isUploading)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ===== 表情面板(条件显示) =====
            AnimatedVisibility(
                visible = showEmoji,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                EmojiPanel(onPick = { emoji ->
                    onEmojiPick(emoji)
                    // 单选插入:选完保留面板(像微信),用户可以连续选
                })
            }
        }
    }
}

/** 引用回复条:出现在输入栏上方 */
@Composable
private fun ReplyBar(authorName: String, content: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧粉色竖条
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(28.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "回复 $authorName",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = content,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onClear,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "取消引用",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** 图片预览条(已选图后) */
@Composable
private fun ImagePreviewBar(
    url: String,
    width: Int,
    height: Int,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
    ) {
        AsyncImage(
            model = url,
            contentDescription = "已选图片",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .heightIn(max = 120.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        IconButton(
            onClick = onClear,
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
        Text(
            text = "${width}×${height}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.sm)
        )
    }
}

/** 上传中状态条 */
@Composable
private fun UploadingBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            "图片上传中…",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/** 上传失败 */
@Composable
private fun FailedBar(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)
    )
}

/** 表情素材库:8 个心情 emoji,点哪个插入到输入框 */
@Composable
private fun EmojiPanel(onPick: (String) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MOODS.forEach { m ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onPick(m.emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(m.emoji, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}
