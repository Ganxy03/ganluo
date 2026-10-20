package com.ganluo.jiyi.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ganluo.jiyi.data.remote.dto.MessageDto
import com.ganluo.jiyi.ui.component.Avatar
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.ui.util.DateUtil

/**
 * 消息预览卡(2.0 首页下段):
 *  - 当天最近 3 条(倒序,新的在上),点任一条 → 进完整聊天窗
 *  - 底部「查看全部 N 条 ›」
 *  - 未绑定时提示去「我的」绑定
 */
@Composable
fun MessagePreviewCard(
    date: String,
    bound: Boolean,
    partnerNickname: String?,
    partnerAvatar: String?,
    meNickname: String?,
    meAvatar: String?,
    messages: List<MessageDto>,
    onOpenChat: () -> Unit,
    onGoProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {

            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "留言",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = if (messages.isEmpty()) "" else "${messages.size} 条",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                if (messages.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "进入聊天",
                        tint = Primary,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenChat)
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            when {
                !bound -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onGoProfile)
                            .padding(vertical = Spacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "还没有绑定另一半",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "去「我的」页面输入 TA 的邀请码,就能看到彼此的留言啦",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                messages.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenChat)
                            .padding(vertical = Spacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (DateUtil.isToday(date)) "今天还没有留言" else "${DateUtil.friendlyDate(date)}还没有留言",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "点这里进入聊天,跟 TA 说点什么",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    Column {
                        // 最近 3 条,倒序(新在上)
                        messages.takeLast(3).reversed().forEach { m ->
                            PreviewRow(
                                msg = m,
                                partnerNickname = partnerNickname,
                                partnerAvatar = partnerAvatar,
                                meNickname = meNickname,
                                meAvatar = meAvatar,
                                onClick = onOpenChat
                            )
                        }
                    }
                    if (messages.size > 3) {
                        Spacer(Modifier.height(Spacing.xs))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .clickable(onClick = onOpenChat)
                                .padding(vertical = Spacing.sm),
                            horizontalArrangement = ArrangementCenter(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "查看全部 ${messages.size} 条",
                                style = MaterialTheme.typography.labelMedium,
                                color = Primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun ArrangementCenter() = androidx.compose.foundation.layout.Arrangement.Center

@Composable
private fun PreviewRow(
    msg: MessageDto,
    partnerNickname: String?,
    partnerAvatar: String?,
    meNickname: String?,
    meAvatar: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.Top
    ) {
        Avatar(
            url = if (msg.isMine) meAvatar else partnerAvatar,
            name = if (msg.isMine) meNickname else partnerNickname,
            size = 32.dp
        )
        Spacer(Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (msg.isMine) "我" else "TA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = DateUtil.timeOf(msg.time).ifBlank { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (!msg.isRead && !msg.isMine) {
                    Spacer(Modifier.width(Spacing.xs))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Primary)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            if (!msg.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = msg.imageUrl,
                    contentDescription = "图片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .widthIn(max = 120.dp)
                        .heightIn(max = 90.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            } else {
                Text(
                    text = msg.content.ifBlank { "[图片]" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
