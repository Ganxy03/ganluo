package com.ganluo.jiyi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ganluo.jiyi.ui.theme.Card
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.TextLight
import com.ganluo.jiyi.ui.util.DateUtil

@Composable
fun DateBar(
    date: String,
    isToday: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // ‹
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Card)
                .clickable(onClick = onPrev),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "前一天", tint = Primary)
        }

        // 中间日期(可点弹日历,这里用 onPick 占位,完整日期选择器用 Material3 DatePicker)
        Row(
            modifier = Modifier
                .clickable(onClick = onPick)
                .padding(horizontal = 24.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = DateUtil.friendlyDate(date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLight
                    )
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = TextLight,
                        modifier = Modifier.size(12.dp).padding(start = 4.dp)
                    )
                }
            }
        }

        // ›
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Card)
                .clickable(enabled = !isToday, onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "后一天",
                tint = if (isToday) TextLight else Primary
            )
        }
    }
}
