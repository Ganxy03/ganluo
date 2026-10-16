package com.ganluo.jiyi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ganluo.jiyi.ui.theme.JiyiTheme
import com.ganluo.jiyi.ui.theme.Online
import com.ganluo.jiyi.ui.theme.PrimaryDeep
import com.ganluo.jiyi.ui.theme.PrimarySoft
import com.ganluo.jiyi.ui.theme.Surface
import com.ganluo.jiyi.ui.util.DateUtil

/**
 * 圆形头像:有 URL 用 Coil,无 URL 用昵称首字 + 粉色占位
 * @param online 显示右下角绿色在线点(可选,默认不显示)
 * @param ring 头像外圈 primaryContainer 色描边(>40dp 才有视觉意义)
 */
@Composable
fun Avatar(
    url: String?,
    name: String?,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    online: Boolean = false,
    ring: Boolean = true
) {
    Box(modifier = modifier.size(size)) {
        // 头像本体
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(PrimarySoft)
                .let { if (ring && size >= 40.dp) it.border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape) else it },
            contentAlignment = Alignment.Center
        ) {
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(size).clip(CircleShape)
                )
            } else {
                Text(
                    text = DateUtil.initialOf(name),
                    color = PrimaryDeep,
                    style = when {
                        size <= 32.dp -> MaterialTheme.typography.labelLarge
                        size <= 56.dp -> MaterialTheme.typography.titleLarge
                        else          -> MaterialTheme.typography.headlineSmall
                    }
                )
            }
        }
        // 在线点(右下角)
        if (online) {
            val dotSize = (size.value * 0.28f).dp
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(Online)
                    .border((size.value * 0.06f).dp, Surface, CircleShape)
            )
        }
    }
}

// ===== Preview =====

@Preview(showBackground = true, name = "头像 - 多尺寸 + 在线点")
@Composable
private fun PreviewAvatars() {
    JiyiTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(url = null, name = "A", size = 32.dp)
            Avatar(url = null, name = "Ganxy", size = 48.dp)
            Avatar(url = null, name = "小白", size = 48.dp, online = true)
            Avatar(url = null, name = "李四", size = 64.dp)
        }
    }
}
