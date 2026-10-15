package com.ganluo.jiyi.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.ganluo.jiyi.data.remote.dto.MoodDto
import com.ganluo.jiyi.ui.component.Avatar
import com.ganluo.jiyi.ui.component.JiyiCard
import com.ganluo.jiyi.ui.component.SectionTitle
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.PrimaryDeep
import com.ganluo.jiyi.ui.theme.PrimarySoft
import com.ganluo.jiyi.ui.theme.TextLight
import com.ganluo.jiyi.ui.theme.TextSub
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
                color = TextLight,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        } else {
            MoodList(moods = myMoods, onLongPress = { onLongPressMine(it) })
            Text(
                "长按一条可以删除",
                color = TextLight,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun PersonHeader(name: String, nickname: String?, avatar: String?) {
    Row(
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(url = avatar, name = nickname, size = 28.dp)
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MoodList(moods: List<MoodDto>, onLongPress: ((MoodDto) -> Unit)?) {
    Column {
        moods.forEach { m ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(MOOD_EMOJI[m.moodType] ?: "🙂", fontSize = 16.sp)
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = m.moodLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!m.content.isNullOrBlank()) {
                        Text(
                            text = m.content,
                            color = TextSub,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text(
                    text = DateUtil.timeOf(m.createdAt).ifBlank { m.time ?: "" },
                    color = TextLight,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** 心情选择弹层:网格 + 文字输入 + 取消/记录;列数随弹窗宽度自适应 */
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
        title = { Text("今天心情怎么样", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    // 弹窗窄(<300dp)就用 3 列,避免 emoji + 文字 cell 挤变形
                    val cols = if (maxWidth < 300.dp) 3 else 4
                    val emojiSize = if (maxWidth < 300.dp) 22.sp else 26.sp
                    MOODS.chunked(cols).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { m ->
                                val active = selected == m.type
                                Column(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (active) PrimarySoft else Color(0xFFF8F4F5))
                                        .clickable { selected = m.type }
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(m.emoji, fontSize = emojiSize)
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        m.label,
                                        fontSize = 12.sp,
                                        color = if (active) PrimaryDeep else TextSub,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= 500) content = it },
                    placeholder = { Text("想说点什么…(可留空)") },
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected, content) },
                enabled = selected.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.White
                )
            ) { Text("记录") }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("取消") }
        }
    )
}
