package com.banqiu.thirdparty123pan.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kyant.backdrop.Backdrop
import com.banqiu.thirdparty123pan.ui.components.upstream.components.LiquidBottomTabs
import com.banqiu.thirdparty123pan.ui.components.upstream.components.LiquidBottomTab

/** App adapter only. Optics/drag physics live in the attributed upstream Catalog component. */
@Composable
fun GlassBottomBar(
    backdrop: Backdrop,
    items: List<BottomNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    glassEnabled: Boolean = true
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var interactionGeneration by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) interactionGeneration++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val container = modifier.fillMaxWidth().navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 14.dp)
    if (glassEnabled && Build.VERSION.SDK_INT >= 31) {
        key(interactionGeneration) {
            LiquidBottomTabs(selectedTabIndex = { selectedIndex }, onTabSelected = onSelect,
                backdrop = backdrop, tabsCount = items.size, modifier = container.selectableGroup()) {
                items.forEachIndexed { index, item ->
                    LiquidBottomTab(onClick = { onSelect(index) }, modifier = Modifier.semantics { selected = index == selectedIndex }) {
                        Icon(if (index == selectedIndex) item.selectedIcon else item.icon, null,
                            tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
                        Text(item.label, color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    } else {
        Row(container.height(64.dp).background(MaterialTheme.colorScheme.surface, CircleShape).selectableGroup(),
            verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { index, item ->
                Column(Modifier.weight(1f).height(56.dp).selectable(index == selectedIndex,
                    interactionSource = null, indication = null, role = Role.Tab, onClick = { onSelect(index) }),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    val color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(if (index == selectedIndex) item.selectedIcon else item.icon, null, tint = color)
                    Text(item.label, color = color, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
