package com.ganluo.jiyi.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ganluo.jiyi.ui.theme.Spacing

/**
 * 自定义紧凑顶栏,替掉 M3 的 CenterAlignedTopAppBar(默认 64dp,小屏太占地方)。
 * 高度 48dp,surfaceContainer 底色,左标题右 action。
 * 内部走 statusBarsPadding 让状态栏透出。
 *
 * @param title       标题(左对齐,留出 actions 空间)
 * @param actions     右侧操作区(IconButton 等)
 * @param onMenuClick 非空时在 actions 最左侧显示 ☰ 汉堡按钮(用于右滑抽屉)
 */
@Composable
fun CompactTopBar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    onMenuClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (onMenuClick != null) {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "菜单",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            actions()
        }
    }
}
