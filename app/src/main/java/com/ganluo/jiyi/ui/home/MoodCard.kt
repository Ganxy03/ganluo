package com.ganluo.jiyi.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganluo.jiyi.data.remote.dto.MoodDto
import com.ganluo.jiyi.ui.component.Avatar
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.ui.util.DateUtil

/**
 * 心情卡(2.0 首页中段):
 *  - 折叠态:一行 —— 两人当天最新心情摘要 + 「记录心情」按钮 + 展开箭头
 *  - 展开态:当天两人心情历史(可长按删除自己的)
 */
@Composable
fun MoodCard(
    date: String,
    meNickname: String?,
    meAvatar: String?,
    partnerNickname: String?,
    partnerAvatar: String?,
    myMoods: List<MoodDto>,
    partnerMoods: List<MoodDto>,
    onAdd: () -> Unit,
    onLongPressMine: (MoodDto) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val total = myMoods.size + partnerMoods.size

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {

            // ===== 折叠态 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "记录心情",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (total == 0) "这天还没有记录" else "这天共 $total 条",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 两个小头像 + 最新 emoji
                MoodDot(partnerMoods.maxByOrNull { it.id }, 20.dp)
                Spacer(Modifier.width(Spacing.xs))
                MoodDot(myMoods.maxByOrNull { it.id }, 20.dp)
                Spacer(Modifier.width(Spacing.sm))

                // 记录按钮
                Button(
                    onClick = onAdd,
                    shape = CircleShape,
                    contentPadding = PaddingValues2(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("记录", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(Spacing.xs))
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // ===== 展开态 =====
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.xs)) {
                    Spacer(Modifier.height(Spacing.xs))
                    HorizontalLine()
                    Spacer(Modifier.height(Spacing.sm))

                    if (partnerMoods.isEmpty() && myMoods.isEmpty()) {
                        Text(
                            text = "${DateUtil.friendlyDate(date)}还没有记录心情,点右上角「记录」",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Spacing.md)
                        )
                    }

                    if (partnerMoods.isNotEmpty()) {
                        MoodGroupHeader("TA", partnerNickname, partnerAvatar, partnerMoods.size)
                        partnerMoods.forEach { m ->
                            MoodLine(mood = m, onLongPress = null)
                        }
                        Spacer(Modifier.height(Spacing.sm))
                    }
                    if (myMoods.isNotEmpty()) {
                        MoodGroupHeader("我", meNickname, meAvatar, myMoods.size)
                        myMoods.forEach { m ->
                            MoodLine(mood = m, onLongPress = { onLongPressMine(m) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaddingValues2() = androidx.compose.foundation.layout.PaddingValues(
    horizontal = Spacing.md, vertical = 0.dp
)

@Composable
private fun HorizontalLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
private fun MoodDot(mood: MoodDto?, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mood?.let { MOOD_EMOJI[it.moodType] ?: "🙂" } ?: "·",
            fontSize = (size.value * 0.5f).sp
        )
    }
}

@Composable
private fun MoodGroupHeader(label: String, nickname: String?, avatar: String?, count: Int) {
    Row(
        modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(url = avatar, name = nickname, size = 22.dp)
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun MoodLine(mood: MoodDto, onLongPress: ((MoodDto) -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongPress?.invoke(mood) }
            )
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(MOOD_EMOJI[mood.moodType] ?: "🙂", fontSize = 13.sp)
        }
        Spacer(Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mood.content?.takeIf { it.isNotBlank() } ?: mood.moodLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = DateUtil.timeOf(mood.createdAt).ifBlank { mood.time ?: "" },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
