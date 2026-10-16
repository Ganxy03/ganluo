package com.ganluo.jiyi.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 全局间距 token。统一引用,避免代码里散落各种魔数。
 * 4dp 基准:小 / 中 / 大 / 巨 4 档即可覆盖 90% 场景。
 */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /** 屏幕外边距(左右) */
    val screenH = 20.dp
    /** 卡片内边距 */
    val cardPadding = 20.dp
    /** 区块间距(垂直) */
    val sectionGap = 14.dp
    /** 顶部留白(从 TopBar 算起) */
    val topGap = 12.dp
    /** 底部留白(给 BottomNav 留位置) */
    val bottomGap = 24.dp
}
