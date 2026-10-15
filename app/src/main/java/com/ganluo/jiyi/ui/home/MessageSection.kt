package com.ganluo.jiyi.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganluo.jiyi.data.remote.dto.MessageDto
import com.ganluo.jiyi.ui.component.JiyiCard
import com.ganluo.jiyi.ui.component.MessageBubble
import com.ganluo.jiyi.ui.component.SectionTitle
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.PrimaryDeep
import com.ganluo.jiyi.ui.theme.PrimarySoft
import com.ganluo.jiyi.ui.theme.TextLight

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
            Spacer(Modifier.width(8.dp))
            if (unreadTotal > 0) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadTotal > 99) "99+" else unreadTotal.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (!bound) {
            Text(
                "绑定后就能看到彼此的留言",
                color = TextLight,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        } else {
            if (latestUnreadDate != null && latestUnreadDate != currentDate) {
                Text(
                    text = "还有 $unreadTotal 条未读在 $latestUnreadDate,点这里查看",
                    color = PrimaryDeep,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimarySoft)
                        .clickable { onJumpUnread() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            if (messages.isEmpty()) {
                Text(
                    "这一天还没有留言",
                    color = TextLight,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
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

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onCompose,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimarySoft,
                    contentColor = PrimaryDeep
                )
            ) { Text("＋ 写留言") }
        }
    }
}

/** 写留言/回复弹层 */
@Composable
fun ComposeMessageSheet(
    visible: Boolean,
    replyTo: MessageDto?,
    onCancel: () -> Unit,
    onSend: (content: String) -> Unit
) {
    if (!visible) return
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                if (replyTo != null) "回复留言" else "给 TA 留言",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column {
                replyTo?.let { r ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8F4F5))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = r.content,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 500) content = it },
                    placeholder = { Text(if (replyTo != null) "回复…" else "想对 TA 说的话…") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(content) },
                enabled = content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.White
                )
            ) { Text("发送") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("取消") }
        }
    )
}
