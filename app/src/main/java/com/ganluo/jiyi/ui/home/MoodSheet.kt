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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganluo.jiyi.data.remote.dto.MoodDto
import com.ganluo.jiyi.ui.component.Avatar
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.ui.util.DateUtil

/**
 * 心情日记 ModalBottomSheet:
 *  - 顶栏: ‹ 日期 › 切换 + 关闭
 *  - 内容: 当天心情列表(TA + 我)
 *  - 底部固定: ＋ 记录心情 按钮
 *
 * 日期切换在这里完成(不再依赖首页 DateBar)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodSheet(
    visible: Boolean,
    date: String,
    isToday: Boolean,
    meNickname: String?,
    meAvatar: String?,
    partnerNickname: String?,
    partnerAvatar: String?,
    myMoods: List<MoodDto>,
    partnerMoods: List<MoodDto>,
    onDismiss: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onAdd: () -> Unit,
    onLongPressMine: (MoodDto) -> Unit
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.screenH)) {
            // 顶栏:日期切换
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onPrev),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.ChevronLeft,
                            contentDescription = "前一天",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = DateUtil.friendlyDate(date),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isToday, onClick = onNext),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "后一天",
                            tint = if (isToday)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 心情列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 480.dp)
            ) {
                if (partnerNickname != null) {
                    item("header_partner") {
                        MoodHeader(name = "TA", nickname = partnerNickname, avatar = partnerAvatar)
                    }
                    if (partnerMoods.isEmpty()) {
                        item("partner_empty") {
                            Text(
                                "TA 这天还没有记录",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = Spacing.xs)
                            )
                        }
                    } else {
                        items(partnerMoods, key = { "p_${it.id}" }) { m ->
                            MoodRow(mood = m, onLongPress = null)
                        }
                    }
                }

                item("header_me") {
                    MoodHeader(name = "我", nickname = meNickname, avatar = meAvatar)
                }
                if (myMoods.isEmpty()) {
                    item("me_empty") {
                        Text(
                            "这天还没有记录",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = Spacing.xs)
                        )
                    }
                } else {
                    items(myMoods, key = { "m_${it.id}" }) { m ->
                        MoodRow(mood = m, onLongPress = { onLongPressMine(m) })
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // 底部按钮
            Button(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Spacing.xs))
                Text("记录心情", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun MoodHeader(name: String, nickname: String?, avatar: String?) {
    Row(
        modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(url = avatar, name = nickname, size = 24.dp)
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
private fun MoodRow(mood: MoodDto, onLongPress: ((MoodDto) -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onLongPress != null) { onLongPress?.invoke(mood) }
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(MOOD_EMOJI[mood.moodType] ?: "🙂", fontSize = 18.sp)
        }
        Spacer(Modifier.width(Spacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mood.moodLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!mood.content.isNullOrBlank()) {
                Text(
                    text = mood.content,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Text(
            text = DateUtil.timeOf(mood.createdAt).ifBlank { mood.time ?: "" },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
