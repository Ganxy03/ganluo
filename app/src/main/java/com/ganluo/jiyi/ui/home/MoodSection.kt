package com.ganluo.jiyi.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganluo.jiyi.data.remote.dto.MoodDto
import com.ganluo.jiyi.ui.component.Avatar
import com.ganluo.jiyi.ui.component.JiyiCard
import com.ganluo.jiyi.ui.component.SectionTitle
import com.ganluo.jiyi.ui.theme.JiyiTheme
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.ui.util.DateUtil

/** 8 种心情配置 */
data class MoodDef(val type: String, val label: String, val emoji: String)

val MOODS = listOf(
    MoodDef("happy", "开心", "😊"),
    MoodDef("sweet", "幸福", "🥰"),
    MoodDef("calm", "平静", "😌"),
    MoodDef("excited", "兴奋", "🤩"),
    MoodDef("tired", "疲惫", "😪"),
    MoodDef("sad", "难过", "😢"),
    MoodDef("angry", "生气", "😠"),
    MoodDef("anxious", "焦虑", "😰")
)
private val MOOD_EMOJI = MOODS.associate { it.type to it.emoji }

@Composable
fun MoodSection(
    meNickname: String?,
    meAvatar: String?,
    partnerNickname: String?,
    partnerAvatar: String?,
    myMoods: List<MoodDto>,
    partnerMoods: List<MoodDto>,
    showAdd: Boolean,
    onAdd: () -> Unit,
    onLongPressMine: (MoodDto) -> Unit
) {
    JiyiCard {
        SectionTitle(label = "心情日记", action = if (showAdd) "＋ 记录" else null, onAction = onAdd)

        if (partnerNickname != null) {
            PersonHeader(name = "TA", nickname = partnerNickname, avatar = partnerAvatar)
            MoodList(moods = partnerMoods, onLongPress = null)
        }

        PersonHeader(name = "我", nickname = meNickname, avatar = meAvatar)
        if (myMoods.isEmpty()) {
            Text(
                "这天还没有记录",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = Spacing.xs)
            )
        } else {
            MoodList(moods = myMoods, onLongPress = { onLongPressMine(it) })
            Text(
                "长按一条可以删除",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm)
            )
        }
    }
}

@Composable
private fun PersonHeader(name: String, nickname: String?, avatar: String?) {
    Row(
        modifier = Modifier.padding(top = Spacing.sm, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(url = avatar, name = nickname, size = 28.dp)
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MoodList(moods: List<MoodDto>, onLongPress: ((MoodDto) -> Unit)?) {
    Column {
        moods.forEach { m ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.xs),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(MOOD_EMOJI[m.moodType] ?: "🙂", fontSize = 16.sp)
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = m.moodLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!m.content.isNullOrBlank()) {
                        Text(
                            text = m.content,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text(
                    text = DateUtil.timeOf(m.createdAt).ifBlank { m.time ?: "" },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** 心情选择弹层:固定 4 列网格 + 文字输入 + 取消/记录;外面套 verticalScroll 防弹窗过窄时裁掉 */
@Composable
fun MoodPickerSheet(
    visible: Boolean,
    onCancel: () -> Unit,
    onConfirm: (type: String, content: String) -> Unit
) {
    if (!visible) return
    var selected by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                "今天心情怎么样",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                MOODS.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        row.forEach { m ->
                            val active = selected == m.type
                            MoodCell(
                                emoji = m.emoji,
                                label = m.label,
                                active = active,
                                emojiSize = 26.sp,
                                onClick = { selected = m.type },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 500) content = it },
                    placeholder = { Text("想说点什么…(可留空)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected, content) },
                enabled = selected.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("记录") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("取消") }
        }
    )
}

/** 心情选择单元:等宽,垂直排 emoji + 标签,选中态高亮 */
@Composable
private fun MoodCell(
    emoji: String,
    label: String,
    active: Boolean,
    emojiSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(
                if (active) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = emojiSize)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

// ===== Preview =====

@Preview(showBackground = true, name = "心情选择弹窗(8 个)")
@Composable
private fun PreviewMoodPicker() {
    JiyiTheme {
        MoodPickerSheet(visible = true, onCancel = {}, onConfirm = { _, _ -> })
    }
}

@Preview(showBackground = true, name = "心情日记(空)")
@Composable
private fun PreviewMoodSectionEmpty() {
    JiyiTheme {
        JiyiCard {
            SectionTitle(label = "心情日记", action = "＋ 记录", onAction = {})
            Text("这天还没有记录", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}
