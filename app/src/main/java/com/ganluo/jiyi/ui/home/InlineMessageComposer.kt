package com.ganluo.jiyi.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.Spacing

/**
 * 首页内联留言输入条(1.0.24 新增)。
 *
 * 排版考虑:
 *  - 放在「留言卡」最底部,和数据之间用一条 hairline 隔开,视觉上属于同一张卡,
 *    但不会和上方消息列表混在一起
 *  - 高度压在 40dp 左右(比聊天页的常驻输入栏更窄),不抢首页的版面
 *  - 只保留三件东西:[📷] [输入框] [发送],引用回复/表情面板仍留在完整聊天窗里
 *  - 选图后不弹大预览,只显示一条 40dp 的小缩略图 + ✕,避免首页被撑高
 *  - 输入框支持键盘「发送」键直接发出(IME action = Send)
 */
@Composable
fun InlineMessageComposer(
    text: String,
    onTextChange: (String) -> Unit,
    upload: ComposerUpload,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val ready = upload as? ComposerUpload.Ready
    val canSend = text.isNotBlank() || ready != null

    fun submit() {
        if (!canSend) return
        onSend()
        keyboard?.hide()
    }

    Column(modifier = modifier.fillMaxWidth()) {

        // ===== 上传状态(只有非 Idle 才占位置)=====
        when (upload) {
            is ComposerUpload.Loading -> Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Primary
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    "图片上传中…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is ComposerUpload.Ready -> Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    AsyncImage(
                        model = upload.url,
                        contentDescription = "已选择图片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "移除图片",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                            .align(Alignment.TopEnd)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onClearImage
                            )
                    )
                }
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    "已附带 1 张图片",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is ComposerUpload.Failed -> Text(
                upload.message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Spacing.xs)
            )

            ComposerUpload.Idle -> Unit
        }

        // ===== 主输入行 =====
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            IconButton(
                onClick = onPickImage,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = "发送图片",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= 500) onTextChange(it) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp, max = 96.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = Spacing.md, vertical = 9.dp),
                textStyle = LocalTextStyle.current.merge(
                    TextStyle(color = MaterialTheme.colorScheme.onSurface)
                ),
                cursorBrush = SolidColor(Primary),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text(
                            "给 TA 留言…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    inner()
                }
            )

            FilledIconButton(
                onClick = { submit() },
                enabled = canSend,
                modifier = Modifier.size(38.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Primary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
