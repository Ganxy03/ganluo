package com.ganluo.jiyi.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 右侧滑出抽屉(Material 3 ModalNavigationDrawer 不支持右侧,所以自建):
 *  - 展开时盖一层半透明遮罩(点遮罩关闭)
 *  - 抽屉本体 280dp 宽,贴右边缘,从右往左滑入
 *  - 系统返回键也能关闭
 */
@Composable
fun RightSideDrawer(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    // 系统返回键关闭
    BackHandler(enabled = visible) { onDismiss() }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 遮罩:点空白关闭
            val noRipple = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = noRipple,
                        indication = null,
                        onClick = onDismiss
                    )
            )

            // 抽屉本体
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(280.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
                tonalElevation = 2.dp
            ) {
                Column(content = content)
            }
        }
    }
}
