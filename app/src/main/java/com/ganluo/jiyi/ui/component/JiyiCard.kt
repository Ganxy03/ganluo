package com.ganluo.jiyi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ganluo.jiyi.ui.theme.Card
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.PrimaryDeep

/** 通用卡片:沿用原小程序 .card 样式,白底 24rpx 圆角 + 粉色阴影 */
@Composable
fun JiyiCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Card,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) { content() }
    }
}

/** 区块标题:左侧 4dp 粉色竖条 + 文字,右侧 action 槽 */
@Composable
fun SectionTitle(
    label: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .background(Primary, shape = RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (action != null && onAction != null) {
            Spacer(Modifier.fillMaxWidth().weight(1f))
            Text(
                text = action,
                color = PrimaryDeep,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(start = 8.dp)
            )
        }
    }
}

/** 复用入口,直接指向 theme/Color.kt 的命名色 */
object JiyiColors {
    val Bg = Color(0xFFFDF6F7)
    val Card = Color(0xFFFFFFFF)
}
