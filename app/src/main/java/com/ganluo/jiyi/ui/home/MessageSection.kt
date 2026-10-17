package com.ganluo.jiyi.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
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
import com.ganluo.jiyi.ui.theme.Spacing

/**
 * 留言列表(方案3 简化版):
 *  - 不再是 JiyiCard 包裹(主页 MoodStrip + 输入栏已占视觉重心)
 *  - 只渲染消息气泡,reverseLayout 保持聊天 App 标准
 *  - 短按消息 = 进引用回复(onTapMessage)
 *  - 长按消息 = 弹删除菜单(onLongPressMessage)
 *  - 「未读跳转」改成顶部小条(条件显示)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageSection(
    bound: Boolean,
    messages: List<MessageDto>,
    unreadTotal: Int,
    latestUnreadDate: String?,
    currentDate: String,
    onJumpUnread: () -> Unit,
    onTapMessage: (MessageDto) -> Unit,
    onLongPressMessage: (MessageDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        when {
            !bound -> EmptyHint("绑定后就能看到彼此的留言")
            messages.isEmpty() -> EmptyHint("这一天还没有留言,说点什么吧")
            else -> {
                // 未读跳转提示(顶部,小条)
                if (latestUnreadDate != null && latestUnreadDate != currentDate) {
                    Text(
                        text = "还有 $unreadTotal 条未读在 $latestUnreadDate,点这查看",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onJumpUnread() }
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                    )
                }

                // 消息列表(从旧到新,自然排列,最新在底部)
                val listState = rememberLazyListState()
                LaunchedEffect(messages.size) {
                    if (messages.isNotEmpty()) {
                        // 滚到最后一条 = 最新消息
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages, key = { it.id }) { m ->
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
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
