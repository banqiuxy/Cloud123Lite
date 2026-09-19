package com.banqiu.thirdparty123pan.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/** Real backdrop refraction on API 33+, blur on 31–32, opaque fallback on 29–30. */
@Composable
internal fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: CornerBasedShape,
    enabled: Boolean,
    tint: Color = MaterialTheme.colorScheme.surface,
    compact: Boolean = false,
    refractionBoost: Float = 1f
): Modifier {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return background(tint, shape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    }
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur((if (compact) 2.dp else 5.dp).toPx())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                lens((if (compact) 8.dp else 16.dp).toPx(),
                    (if (compact) 12.dp else 28.dp).toPx() * refractionBoost, depthEffect = true)
            }
        },
        shadow = null,
        onDrawSurface = { drawRect(tint.copy(alpha = if (compact) 0.36f else 0.52f)) }
    )
}

/** App adapter around the author's LiquidToggle, not a second custom thumb implementation. */
@Composable
fun LiquidGlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop,
    glassEnabled: Boolean,
    label: String,
    modifier: Modifier = Modifier
) {
    if (glassEnabled && Build.VERSION.SDK_INT >= 31) {
        Box(modifier.size(76.dp, 48.dp), contentAlignment = Alignment.Center) {
            com.banqiu.thirdparty123pan.ui.components.upstream.components.LiquidToggle(
                selected = { checked }, onSelect = onCheckedChange, backdrop = backdrop,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = label
                    this.role = Role.Switch
                    toggleableState = if (checked) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off
                    onClick { onCheckedChange(!checked); true }
                }
            )
        }
    } else {
        androidx.compose.material3.Switch(checked, onCheckedChange,
            modifier = modifier.semantics { contentDescription = label })
    }
}
