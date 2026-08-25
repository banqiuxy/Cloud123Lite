package com.banqiu.thirdparty123pan.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 圆角规范（UI-UX spec）：
 * - 大卡片 24dp / 底部导航 28dp / FAB 20dp / 按钮 14dp / 小标签 8dp
 */
val CloudShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val ShapeSmall = RoundedCornerShape(8.dp)
val ShapeMedium = RoundedCornerShape(14.dp)
val ShapeLarge = RoundedCornerShape(24.dp)
val ShapeFab = RoundedCornerShape(20.dp)
val ShapeBottomBar = RoundedCornerShape(28.dp)
