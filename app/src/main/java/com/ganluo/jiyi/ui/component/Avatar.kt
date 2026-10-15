package com.ganluo.jiyi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ganluo.jiyi.ui.theme.PrimaryDeep
import com.ganluo.jiyi.ui.theme.PrimarySoft
import com.ganluo.jiyi.ui.util.DateUtil

/** 圆形头像:有 URL 用 Coil,无 URL 用昵称首字 + 粉色占位 */
@Composable
fun Avatar(
    url: String?,
    name: String?,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(PrimarySoft),
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
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
