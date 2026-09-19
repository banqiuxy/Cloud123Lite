// Adapted from Kyant0/AndroidLiquidGlass, tag 1.0.0, commit eada9a8600142c1bd6a5c5fc143d06f599cdfaef.
// Copyright the upstream contributors. Apache-2.0; see third_party/backdrop-catalog/LICENSE and PROVENANCE.md.
package com.banqiu.thirdparty123pan.ui.components.upstream.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastFirstOrNull

suspend fun PointerInputScope.inspectDragGestures(
    consumeGestures: Boolean = false,
    ignoreConsumed: Boolean = false,
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        // The dock opts into a whole-panel Initial-pass observer so every tab can start
        // the author's interaction, without a child Material clickable stealing release.
        val pass = if (consumeGestures) PointerEventPass.Initial else PointerEventPass.Main
        val down = awaitFirstDown(false, pass)
        if (consumeGestures) down.consume()
        onDragStart(down)
        var ended = false
        try {
            onDrag(down, Offset.Zero)
            var id = down.id
            while (true) {
                val event = awaitPointerEvent(pass)
                val change = event.changes.fastFirstOrNull { it.id == id } ?: break
                if (event.changes.count { it.pressed } > 1) break
                if (change.isConsumed && !ignoreConsumed && !consumeGestures) break
                if (change.changedToUpIgnoreConsumed()) {
                    // Include final pointer displacement before resolving the target.
                    onDrag(change, change.position - change.previousPosition)
                    if (consumeGestures) change.consume()
                    onDragEnd(change)
                    ended = true
                    break
                }
                onDrag(change, change.positionChange())
                if (consumeGestures) change.consume()
            }
        } finally {
            if (!ended) onDragCancel()
        }
    }
}
