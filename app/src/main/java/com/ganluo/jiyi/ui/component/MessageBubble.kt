package com.ganluo.jiyi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ganluo.jiyi.data.remote.dto.MessageDto
import com.ganluo.jiyi.ui.theme.JiyiTheme
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.ui.util.DateUtil

/**
 * 留言气泡:我发的 = primaryContainer / TA 发的 = surfaceContainer
 * 三种内容组合:
 *   - 纯文本
 *   - 纯图片(宽 220dp,按原图比例)
 *   - 图片 + 文字
 */
@Composable
fun MessageBubble(
    msg: MessageDto,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMine = msg.isMine
    val hasImage = !msg.imageUrl.isNullOrBlank()
    val hasText = msg.content.isNotBlank()
    val bubbleColor =
        if (isMine) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh
    val onBubbleColor =
        if (isMine) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        // 气泡主体(纯图片时不加底色,直接 AsyncImage 撑开)
        if (hasImage && !hasText) {
            AsyncImage(
                model = msg.imageUrl,
                contentDescription = "图片",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .widthIn(max = 220.dp)
                    .clip(MaterialTheme.shapes.large)
            )
        } else {
            Column(
                modifier = Modifier
                    .background(
                        color = bubbleColor,
                        shape = MaterialTheme.shapes.large
                    )
                    .widthIn(max = 280.dp)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            ) {
                if (hasImage) {
                    AsyncImage(
                        model = msg.imageUrl,
                        contentDescription = "图片",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                    )
                    if (hasText) Spacer(Modifier.height(Spacing.sm))
                }
                msg.replyTo?.let { quote ->
                    Row(
                        modifier = Modifier
                            .padding(bottom = Spacing.sm)
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(Spacing.sm)
                    ) {
                        Text(
                            text = "↳ ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = quote.content,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
                if (hasText) {
                    Text(
                        text = msg.content,
                        color = onBubbleColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // 时间 + 未读标签
        Row(
            modifier = Modifier.padding(
                top = Spacing.xs,
                start = Spacing.xs,
                end = Spacing.xs
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isMine && !msg.isRead) {
                Text(
                    text = "未读",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                text = msg.time ?: DateUtil.timeOf(msg.content),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ===== Preview =====

@Preview(showBackground = true, name = "纯文本 - 我发")
@Composable
private fun PreviewMine() {
    JiyiTheme {
        MessageBubble(
            msg = MessageDto(id = 1, content = "今天想吃火锅", time = "12:34", isMine = true, isRead = true),
            onClick = {}, onLongClick = {}
        )
    }
}

@Preview(showBackground = true, name = "纯文本 - TA 发 + 未读")
@Composable
private fun PreviewTheirs() {
    JiyiTheme {
        MessageBubble(
            msg = MessageDto(id = 2, content = "好啊!", time = "12:35", isMine = false, isRead = false),
            onClick = {}, onLongClick = {}
        )
    }
}

@Preview(showBackground = true, name = "纯图片")
@Composable
private fun PreviewImageOnly() {
    JiyiTheme {
        MessageBubble(
            msg = MessageDto(
                id = 3, content = "",
                imageUrl = "https://picsum.photos/400/300",
                time = "13:00", isMine = true, isRead = true
            ),
            onClick = {}, onLongClick = {}
        )
    }
}

@Preview(showBackground = true, name = "图片 + 文字")
@Composable
private fun PreviewImageAndText() {
    JiyiTheme {
        MessageBubble(
            msg = MessageDto(
                id = 4, content = "今天的早餐!",
                imageUrl = "https://picsum.photos/400/300",
                time = "08:00", isMine = false, isRead = true
            ),
            onClick = {}, onLongClick = {}
        )
    }
}
