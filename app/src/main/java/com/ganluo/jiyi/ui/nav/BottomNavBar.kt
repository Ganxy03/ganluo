package com.ganluo.jiyi.ui.nav

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ganluo.jiyi.ui.theme.Primary
import com.ganluo.jiyi.ui.theme.Spacing

/**
 * 自定义底部导航条(取代右侧抽屉):
 *  - 2 个 tab:记录 / 我的
 *  - 高度 60dp + 底部 systemBars 留白(避开手势条 / 虚拟键)
 *  - 选中态:粉色图标 + 粉色胶囊背景 + 加粗 label
 *  - 未选态:中性色图标 + 默认 label 字号
 *  - 上方 1dp hairline 分隔线,跟页面贴合
 */
@Composable
fun BottomNavBar(
    current: String,
    onSelect: (route: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navPad = WindowInsets.navigationBars.asPaddingValues()
    val items = listOf(
        BottomTab(Route.HOME,     "记录", Icons.Filled.Home),
        BottomTab(Route.PROFILE,  "我的", Icons.Filled.Person)
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { tab ->
                BottomNavItem(
                    tab = tab,
                    selected = tab.route == current,
                    onClick = { onSelect(tab.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(navPad.calculateBottomPadding()))
    }
}

@Composable
private fun BottomNavItem(
    tab: BottomTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint by animateColorAsState(
        targetValue = if (selected) Primary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tint"
    )
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                  else Color.Transparent
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.xs),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                tab.label,
                color = tint,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)
