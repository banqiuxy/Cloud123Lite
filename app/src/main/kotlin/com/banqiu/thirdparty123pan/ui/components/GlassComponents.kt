package com.banqiu.thirdparty123pan.ui.components

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.banqiu.thirdparty123pan.ui.theme.CloudBlue
import com.banqiu.thirdparty123pan.ui.theme.ShapeBottomBar
import com.banqiu.thirdparty123pan.ui.theme.ShapeLarge
import com.banqiu.thirdparty123pan.ui.theme.ShapeMedium
import dev.chrisbanes.haze.HazeState

/**
 * 液态玻璃风格基础组件（UI-UX spec）
 * - 玻璃卡片：背景模糊 35-50dp + 半透明填充 + 1dp 高光边框
 * - 降级方案：玻璃关闭时使用纯半透明表面
 */

/**
 * 页面背景：渐变 + 装饰光斑（作为 haze 模糊源）
 */
@Composable
fun CloudBackground(
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    darkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    val start = MaterialTheme.colorScheme.background
    val end = if (darkTheme) Color(0xFF111318) else Color(0xFFDFE3F0)

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(start, end)))

    ) {
        // 装饰光斑：打破纯平背景
        Box(
            Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-30).dp, y = 60.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            CloudBlue.copy(alpha = if (darkTheme) 0.16f else 0.14f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        Box(
            Modifier
                .size(220.dp)
                .align(Alignment.BottomStart)
                .offset(x = 40.dp, y = 0.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = if (darkTheme) 0.10f else 0.12f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
    }
}

/**
 * 玻璃卡片：Backdrop 折射 + 半透明填充 + 细边框 + 可选点击
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState?,
    shape: Shape = ShapeLarge,
    glassEnabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val base = modifier.glassSurface(
        shape as? androidx.compose.foundation.shape.CornerBasedShape ?: RoundedCornerShape(24.dp),
        enabled = glassEnabled
    ).clip(shape)

    if (onClick != null) {
        androidx.compose.foundation.layout.Column(
            modifier = base
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                ),
            content = content
        )
    } else {
        androidx.compose.foundation.layout.Column(
            modifier = base,
            content = content
        )
    }
}

/**
 * 玻璃顶栏：半透明 + 模糊 + 底部圆角
 */
@Composable
fun GlassTopBar(
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    title: String = "",
    glassEnabled: Boolean = true,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
    Row(
        modifier = modifier.fillMaxWidth().statusBarsPadding().height(56.dp)
            .glassSurface(shape, enabled = glassEnabled).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigationIcon?.invoke()
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        actions()
    }
}

/**
 * 玻璃底部导航栏：28dp 圆角胶囊，浮动于内容之上
 */
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

/**
 * 主色按钮（14dp 圆角）
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    containerColor: Color = CloudBlue,
    contentColor: Color = Color.White
) {
    AppButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ShapeMedium,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.7f)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * 玻璃 FAB（20dp 圆角）
 */
@Composable
fun GlassFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppButton(
        onClick = onClick,
        modifier = modifier.sizeIn(minWidth = 96.dp, minHeight = 56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Text(contentDescription, style = MaterialTheme.typography.labelLarge)
    }
}
