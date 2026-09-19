package com.banqiu.thirdparty123pan.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

internal val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }
internal val LocalGlassEnabled = staticCompositionLocalOf { true }

/** A sibling source cannot record any of the foreground controls that sample it. */
@Composable
fun GlassScene(enabled: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val backdrop = rememberLayerBackdrop()
    val active = enabled && Build.VERSION.SDK_INT >= 31
    Box(modifier) {
        Box(Modifier.matchParentSize()
            .then(if (active) Modifier.layerBackdrop(backdrop) else Modifier)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface))))
        CompositionLocalProvider(LocalGlassBackdrop provides if (active) backdrop else null,
            LocalGlassEnabled provides enabled) { content() }
    }
}

@Composable
internal fun Modifier.glassSurface(
    shape: CornerBasedShape = RoundedCornerShape(20.dp),
    tint: Color = MaterialTheme.colorScheme.surface,
    enabled: Boolean = true,
    compact: Boolean = false
): Modifier {
    val backdrop = LocalGlassBackdrop.current
    return if (backdrop != null) liquidGlass(backdrop, shape, enabled && LocalGlassEnabled.current, tint, compact)
    else background(tint, shape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
}

/** Dialogs/popups get their own coordinate space and never reuse the Activity's layer. */
@Composable
internal fun GlassWindow(content: @Composable () -> Unit) {
    GlassScene(LocalGlassEnabled.current) { content() }
}
