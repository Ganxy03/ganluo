package com.ganluo.jiyi.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.Spacing
import com.ganluo.jiyi.ui.util.DateUtil
import java.util.Calendar

/**
 * 日历卡(2.0 首页顶部):
 *  - 折叠态:一行 —— ‹ 日期 ›  + 展开箭头
 *  - 展开态:整月网格(6 行 x 7 列),占满但留边距
 *  - 有心情 / 有消息的日期打小圆点(心点=心情,蓝点=消息)
 *  - 未来日期禁用(不能选)
 */
@Composable
fun DatePickerCard(
    date: String,
    markedDates: Set<String>,
    moodDates: Set<String>,
    messageDates: Set<String>,
    onDateChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {

            // ===== 折叠态:一行 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "前一天",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onDateChange(DateUtil.shiftDate(date, -1)) }
                        .padding(3.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = DateUtil.friendlyDate(date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "后一天",
                    tint = if (DateUtil.isToday(date))
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !DateUtil.isToday(date)) {
                            onDateChange(DateUtil.shiftDate(date, 1))
                        }
                        .padding(3.dp)
                )
                Spacer(Modifier.width(Spacing.xs))
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // ===== 展开态:整月网格 =====
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                MonthGrid(
                    date = date,
                    moodDates = moodDates,
                    messageDates = messageDates,
                    onDateChange = { d ->
                        onDateChange(d)
                        expanded = false   // 选完自动收起,体验更顺
                    }
                )
            }
        }
    }
}

/** 整月网格:星期头 + 6 行日期 */
@Composable
private fun MonthGrid(
    date: String,
    moodDates: Set<String>,
    messageDates: Set<String>,
    onDateChange: (String) -> Unit
) {
    val (y, m, _) = date.split("-").map { it.toInt() }
    val cal = remember(y, m) { Calendar.getInstance().apply { set(y, m - 1, 1) } }
    val firstWeekday = cal.get(Calendar.DAY_OF_WEEK) - 1      // 0=周日
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val today = DateUtil.toDateStr()

    // 上个月补位(显示灰字,不可点)
    val prevMonthDays = remember(y, m, firstWeekday) {
        val prev = Calendar.getInstance()
        prev.add(Calendar.MONTH, -1)
        val lastDay = prev.getActualMaximum(Calendar.DAY_OF_MONTH)
        (0 until firstWeekday).map { lastDay - firstWeekday + 1 + it }
    }

    Column(modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.sm)) {
        // 月份头
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${y}年${m}月",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            // 图例
            LegendDot(Primary, "心情")
            Spacer(Modifier.width(Spacing.sm))
            LegendDot(Color(0xFF6BA8E5), "留言")
        }

        // 星期头
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { w ->
                Text(
                    text = w,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(Spacing.xs))

        // 6 行格子
        val cells: List<Int?> = buildList {
            prevMonthDays.forEach { add(-it) }   // 负数 = 上个月
            for (d in 1..daysInMonth) add(d)
            while (size % 7 != 0) add(0)          // 0 = 下个月占位
        }
        val todayStr = remember(cells, today) { DateUtil.toDateStr() }

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { d ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        when {
                            d == null || d == 0 -> Spacer(Modifier.size(38.dp))
                            d < 0 -> Text(
                                text = "${-d}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                modifier = Modifier.size(38.dp),
                                textAlign = TextAlign.Center
                            )
                            else -> {
                                val dStr = String.format("%04d-%02d-%02d", y, m, d)
                                val isFuture = dStr > todayStr
                                DayCell(
                                    day = d,
                                    dateStr = dStr,
                                    selected = dStr == date,
                                    isToday = dStr == todayStr,
                                    enabled = !isFuture,
                                    hasMood = dStr in moodDates,
                                    hasMessage = dStr in messageDates,
                                    onClick = { onDateChange(dStr) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DayCell(
    day: Int,
    dateStr: String,
    selected: Boolean,
    isToday: Boolean,
    enabled: Boolean,
    hasMood: Boolean,
    hasMessage: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    selected -> Primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$day",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            // 标记点(最多两个)
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.height(5.dp)
            ) {
                if (hasMood) Dot(if (selected) MaterialTheme.colorScheme.onPrimary else Primary)
                if (hasMessage) Dot(if (selected) MaterialTheme.colorScheme.onPrimary else Color(0xFF6BA8E5))
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(color)
    )
}
