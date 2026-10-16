package com.ganluo.jiyi.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 圆角规范:小 8 / 中 16 / 大 24 / 巨 28
 * 头像/FAB/Pill 用 50% 在调用处取 shape = CircleShape
 */
val JiyiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),     // Chip / Tag
    small      = RoundedCornerShape(12.dp),    // TextField / Snackbar
    medium     = RoundedCornerShape(20.dp),    // Card / Button
    large      = RoundedCornerShape(24.dp),    // BottomSheet / 大卡
    extraLarge = RoundedCornerShape(28.dp)     // Dialog / 弹层
)
