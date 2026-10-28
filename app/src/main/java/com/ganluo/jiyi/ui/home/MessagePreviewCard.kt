package com.ganluo.jiyi.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ganluo.jiyi.data.remote.dto.MessageDto
import com.ganluo.jiyi.ui.component.MessageBubble
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.ui.util.DateUtil

/**
 * 首页留言卡(1.0.25)
 *
 * 版式(撑满首页剩余高度):
 *   ┌─ 留言 12 条 ───────────────›  固定头部,点 › 进完整聊天窗
 *   │  ┌────────────┐               对方:靠左,浅灰气泡
 *   │  │ 那我先去买水果🍊│
 *   │  └────────────┘
 *   │                ┌──────────┐   我:靠右,主题色气泡
 *   │                │ 好呀,七点到 │
 *   │                └──────────┘
 *   │        …(消息区内部滚动,进入自动贴底)
 *   ├──────────────────────────────   hairline
 *   │ [📷] 给 TA 留言…          [➤]  固定输入条
 *   └──────────────────────────────
 *
 * 实现要点:
 *  - 气泡直接复用聊天页的 [MessageBubble],保证首页和聊天窗的左右/配色完全一致
 *  - Surface 的高度由外层 `Modifier.weight(1f)` 决定,所以内部 Column 必须 fillMaxSize,
 *    消息区才能用 weight(1f) 拿到剩余空间
 *  - 消息区用 LazyColumn 自己滚,整页不再滚动;头部/输入条在 weight 之外,永远不会被顶走
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
    /** 底部内联输入条;未绑定时传 null */
    composer: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // 首次进入 / 新留言 / 切日期 → 贴到底部(聊天习惯)
    LaunchedEffect(messages.size, date) {
        if (messages.isNotEmpty()) {
            runCatching { listState.animateScrollToItem(messages.lastIndex) }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            // ===== 固定头部 =====
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

            // ===== 消息区:吃掉剩余高度,内部滚动 =====
            when {
                !bound -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(onClick = onGoProfile)
                            .padding(Spacing.md),
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
                            "去「我的」输入 TA 的邀请码,就能互相留言啦",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                messages.isEmpty() -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (DateUtil.isToday(date)) "今天还没有留言,说点什么吧"
                        else "${DateUtil.friendlyDate(date)}没有留言",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(vertical = Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(items = messages, key = { it.id }) { m ->
                        // 复用聊天页气泡:我=靠右主题色,TA=靠左浅灰
                        MessageBubble(
                            msg = m,
                            onClick = onOpenChat,
                            onLongClick = {}
                        )
                    }
                }
            }

            // ===== 固定输入条 =====
            if (composer != null) {
                Spacer(Modifier.height(Spacing.xs))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                composer()
            }
        }
    }
}
