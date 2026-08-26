package com.usharik.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.zIndex
import com.usharik.app.utils.HapticFeedback

/**
 * Lightweight drag-and-drop controller mirroring the original View drag-and-drop.
 * Source items carry a string tag (pool item = word index; cell = "number_caseNum"),
 * drop targets register their bounds by key, and [onDrop] is invoked with the tag of
 * the dragged item and the key of the target under the pointer when the gesture ends.
 */
class DragAndDropState {
    var dragging by mutableStateOf(false)
        private set
    var draggedTag by mutableStateOf("")
        private set
    var dragText by mutableStateOf("")
        private set
    var pointer by mutableStateOf(Offset.Zero)
        private set
    var itemSize by mutableStateOf(IntSize.Zero)
        private set

    var onDrop: ((droppedTag: String, targetKey: Any?) -> Unit)? = null

    private val targets = mutableMapOf<Any, Rect>()

    fun registerTarget(key: Any, bounds: Rect) { targets[key] = bounds }
    fun unregisterTarget(key: Any) { targets.remove(key) }

    fun start(tag: String, text: String, at: Offset, size: IntSize) {
        draggedTag = tag; dragText = text; pointer = at; itemSize = size; dragging = true
    }

    fun move(delta: Offset) { pointer += delta }

    fun end() {
        if (!dragging) return
        val target = targets.entries.firstOrNull { it.value.contains(pointer) }?.key
        val tag = draggedTag
        dragging = false
        onDrop?.invoke(tag, target)
    }

    fun cancel() { dragging = false }
}

@Composable
fun rememberDragAndDropState(): DragAndDropState = remember { DragAndDropState() }

/**
 * Marks a composable as a draggable source carrying [tag]/[text]. The drag begins as soon as
 * the pointer moves past touch-slop, mirroring the original startDragAndDrop on ACTION_DOWN.
 */
fun Modifier.dragSource(
    state: DragAndDropState,
    tag: String,
    text: String,
    enabled: Boolean = true,
    onPickUp: () -> Unit = {},
): Modifier = composed {
    var itemRoot by remember { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current
    this
        .onGloballyPositioned { itemRoot = it.positionInRoot() }
        .pointerInput(tag, enabled) {
            if (!enabled) return@pointerInput
            detectDragGestures(
                onDragStart = { local ->
                    HapticFeedback.light(context)
                    onPickUp()
                    state.start(tag, text, itemRoot + local, size)
                },
                onDrag = { change, amount -> change.consume(); state.move(amount) },
                onDragEnd = { state.end() },
                onDragCancel = { state.cancel() },
            )
        }
}

/** Registers this composable's bounds as a drop target identified by [key]. */
fun Modifier.dropTarget(state: DragAndDropState, key: Any): Modifier = composed {
    DisposableEffect(key) { onDispose { state.unregisterTarget(key) } }
    this.onGloballyPositioned { state.registerTarget(key, it.boundsInRoot()) }
}

/**
 * Full-screen overlay that renders the floating drag shadow under the pointer while a drag is
 * active. Mirrors CustomDragShadowBuilder (a slightly enlarged, semi-transparent copy of the
 * dragged view). [chip] renders the shadow content for the current [DragAndDropState.dragText].
 */
@Composable
fun DragOverlay(state: DragAndDropState, chip: @Composable (String) -> Unit) {
    if (!state.dragging) return
    // The pointer is tracked in root coordinates, but this overlay may sit below the
    // toolbar/status bar; convert to overlay-local space so the chip stays under the finger.
    var overlayRoot by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(1f)
            .onGloballyPositioned { overlayRoot = it.positionInRoot() },
    ) {
        val halfW = state.itemSize.width / 2f
        val halfH = state.itemSize.height / 2f
        // Pops from 1x up to the drag scale right as the pointer picks the chip up, giving a
        // springy "lift-off" feel rather than snapping straight to the enlarged size.
        val scale by animateFloatAsState(
            targetValue = 1.35f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "dragChipScale",
        )
        // Translation, scale and alpha live in a single layer: the chip is composited at its
        // measured size and transformed as a whole, so scaling never clips its rounded border.
        Box(
            Modifier.graphicsLayer {
                translationX = state.pointer.x - overlayRoot.x - halfW
                translationY = state.pointer.y - overlayRoot.y - halfH
                scaleX = scale
                scaleY = scale
                alpha = 0.85f
            },
        ) { chip(state.dragText) }
    }
}
