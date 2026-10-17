package com.ganluo.jiyi.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganluo.jiyi.data.remote.dto.MoodDto
import com.ganluo.jiyi.ui.component.Avatar
import com.ganluo.jiyi.ui.theme.Spacing

/**
 * 心情 strip:首页顶部条状,展示「今天」两位的最新心情
 *
 *  - 整个可点击 → 打开 MoodSheet(看完整心情 + 打卡)
 *  - 显示头像 + emoji + 一行短评 + 时间
 *  - 都没记录时显示空状态
 */
@Composable
fun MoodStrip(
    meNickname: String?,
    meAvatar: String?,
    partnerNickname: String?,
    partnerAvatar: String?,
    myLatest: MoodDto?,
    partnerLatest: MoodDto?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    "心情日记",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(Spacing.sm))

            // 两个人的最新心情(并排,各占一半)
            Row(modifier = Modifier.fillMaxWidth()) {
                MoodStripItem(
                    modifier = Modifier.weight(1f),
                    name = "TA",
                    nickname = partnerNickname,
                    avatar = partnerAvatar,
                    mood = partnerLatest
                )
                Spacer(Modifier.width(Spacing.sm))
                MoodStripItem(
                    modifier = Modifier.weight(1f),
                    name = "我",
                    nickname = meNickname,
                    avatar = meAvatar,
                    mood = myLatest
                )
            }
        }
    }
}

@Composable
private fun MoodStripItem(
    modifier: Modifier = Modifier,
    name: String,
    nickname: String?,
    avatar: String?,
    mood: MoodDto?
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像(心情 emoji 浮在右上角)
        Box(modifier = Modifier.size(40.dp)) {
            Avatar(url = avatar, name = nickname, size = 40.dp)
            if (mood != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = MOOD_EMOJI[mood.moodType] ?: "🙂",
                        fontSize = 11.sp
                    )
                }
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
            if (mood == null) {
                Text(
                    text = "还没记录",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = mood.content?.ifBlank { mood.moodLabel } ?: mood.moodLabel,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
