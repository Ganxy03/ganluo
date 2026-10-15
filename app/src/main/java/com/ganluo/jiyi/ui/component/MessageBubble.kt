package com.ganluo.jiyi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ganluo.jiyi.data.remote.dto.MessageDto
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.PrimarySoft
import com.ganluo.jiyi.ui.theme.TextLight
import com.ganluo.jiyi.ui.util.DateUtil

@Composable
fun MessageBubble(
    msg: MessageDto,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMine = msg.isMine
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (isMine) PrimarySoft else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            msg.replyTo?.let { quote ->
                Row(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text("↳ ", color = TextLight, style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = quote.content,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
            }
            Text(text = msg.content, style = MaterialTheme.typography.bodyLarge)
        }
        Row(
            modifier = Modifier.padding(top = 4.dp, end = 4.dp, start = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isMine && !msg.isRead) {
                Text(
                    text = "未读",
                    color = Primary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = msg.time ?: DateUtil.timeOf(msg.content),
                color = TextLight,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
