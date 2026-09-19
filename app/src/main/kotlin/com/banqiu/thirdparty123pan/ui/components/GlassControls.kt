package com.banqiu.thirdparty123pan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.role
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** All actionable glass buttons use Kyant's LiquidButton and its optical press feedback. */
@Composable
private fun CatalogButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    tint: Color,
    foreground: Color,
    height: Dp = 48.dp,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp),
    content: @Composable RowScope.() -> Unit
) {
    val backdrop = LocalGlassBackdrop.current
    val glass = LocalGlassEnabled.current && backdrop != null && android.os.Build.VERSION.SDK_INT >= 31
    CompositionLocalProvider(LocalContentColor provides foreground) {
        if (glass) {
            com.banqiu.thirdparty123pan.ui.components.upstream.components.LiquidButton(
                onClick = onClick, backdrop = backdrop!!, modifier = modifier,
                enabled = enabled, isInteractive = enabled, height = height, contentPadding = padding,
                tint = if (enabled) tint else Color.Unspecified,
                surfaceColor = if (!enabled) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Unspecified,
                content = content
            )
        } else {
            Row(modifier.height(height).clip(com.kyant.capsule.ContinuousCapsule)
                .background(if (tint == Color.Unspecified) MaterialTheme.colorScheme.surface else tint)
                .clickable(enabled = enabled, interactionSource = null, indication = null,
                    role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
                .padding(padding), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.CenterHorizontally),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, content = content)
        }
    }
}

@Composable
fun AppButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: CornerBasedShape = RoundedCornerShape(14.dp), colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding, content: @Composable RowScope.() -> Unit) {
    CatalogButton(onClick, modifier, enabled, if (enabled) colors.containerColor else colors.disabledContainerColor,
        if (enabled) colors.contentColor else colors.disabledContentColor, padding = contentPadding, content = content)
}

@Composable
fun AppTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    CatalogButton(onClick, modifier, enabled, Color.Unspecified,
        if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f), content = content)
}

@Composable
fun AppIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable () -> Unit) {
    CatalogButton(onClick, modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), enabled, Color.Unspecified,
        if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        padding = PaddingValues(12.dp)) { content() }
}

@Composable
fun AppOutlinedTextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, readOnly: Boolean = false, label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null, leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null, isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false, minLines: Int = 1, maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    shape: CornerBasedShape = RoundedCornerShape(14.dp)) {
    OutlinedTextField(value = value, onValueChange = onValueChange,
        modifier = modifier.glassSurface(shape, if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface, enabled),
        enabled = enabled, readOnly = readOnly, label = label, placeholder = placeholder,
        leadingIcon = leadingIcon, trailingIcon = trailingIcon, isError = isError,
        visualTransformation = visualTransformation, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        singleLine = singleLine, minLines = minLines, maxLines = maxLines, shape = shape,
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent, errorContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent))
}

@Composable
fun AppFilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier) {
    // A newly recorded layer on selection changes cannot retain the old selected tint.
    key(selected) {
        CatalogButton(onClick, modifier.semantics {
            this.selected = selected
            role = androidx.compose.ui.semantics.Role.Tab
        }, true, if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
            if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            padding = PaddingValues(horizontal = 12.dp)) { label() }
    }
}

@Composable
fun AppAlertDialog(onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier, dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null, text: (@Composable () -> Unit)? = null,
    shape: CornerBasedShape = RoundedCornerShape(24.dp), containerColor: Color = MaterialTheme.colorScheme.surface) {
    Dialog(onDismissRequest) {
        GlassWindow {
            Column(modifier.widthIn(max = 560.dp).heightIn(max = 560.dp).clip(shape).glassSurface(shape, containerColor)
                .verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                title?.invoke()
                text?.invoke()
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End)) {
                    dismissButton?.invoke(); confirmButton()
                }
            }
        }
    }
}

@Composable
fun AppDropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit, modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenu(expanded, onDismissRequest, modifier, containerColor = Color.Transparent,
        shadowElevation = 0.dp, tonalElevation = 0.dp, border = null) {
        GlassWindow { Column(Modifier.glassSurface(RoundedCornerShape(16.dp), containerColor), content = content) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, containerColor = Color.Transparent, dragHandle = null) {
        GlassWindow {
            Column(Modifier.fillMaxWidth().glassSurface(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))) {
                BottomSheetDefaults.DragHandle(modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
                content()
            }
        }
    }
}

@Composable
fun AppLinearProgressIndicator(progress: () -> Float, modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary, trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest) {
    LinearProgressIndicator(progress, modifier.glassSurface(RoundedCornerShape(8.dp), trackColor, compact = true),
        color = color, trackColor = trackColor.copy(alpha = 0.2f))
}

@Composable
fun AppCircularProgressIndicator(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, strokeWidth: Dp = 4.dp) {
    CircularProgressIndicator(modifier.glassSurface(RoundedCornerShape(50), compact = true), color, strokeWidth)
}
