package io.github.benji377.timety.ui.components.habit

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt


/**
 * Renders [items] in a plain [Column]; while [isReorderEnabled] is true, each item can be
 * dragged (immediately, no long-press) to swap it past its neighbors, and [onOrderChanged] is
 * called with the new order once the drag ends. While disabled, this is a pure passthrough with
 * no gesture handling attached at all, so callers get zero behavior change when reorder mode
 * is off.
 *
 * Item heights aren't uniform (notes, progress bars, etc. vary tile height), so each item's
 * height is tracked live via [onGloballyPositioned] rather than assumed.
 */
@Composable
fun <T> DraggableReorderColumn(
    items: List<T>,
    key: (T) -> String,
    isReorderEnabled: Boolean,
    onOrderChanged: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, isBeingDragged: Boolean) -> Unit,
) {
    if (!isReorderEnabled) {
        Column(modifier) {
            items.forEach { item -> itemContent(item, false) }
        }
        return
    }

    var localOrder by remember { mutableStateOf(items) }
    LaunchedEffect(items) { localOrder = items }

    val itemHeights = remember { mutableStateMapOf<String, Int>() }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    Column(modifier) {
        localOrder.forEach { item ->
            val itemKey = key(item)
            key(itemKey) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { itemHeights[itemKey] = it.size.height }
                        .zIndex(if (itemKey == draggingKey) 1f else 0f)
                        .offset {
                            IntOffset(0, if (itemKey == draggingKey) dragOffsetPx.roundToInt() else 0)
                        }
                        .pointerInput(itemKey) {
                            detectDragGestures(
                                onDragStart = {
                                    draggingKey = itemKey
                                    dragOffsetPx = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetPx += dragAmount.y

                                    var idx = localOrder.indexOfFirst { key(it) == itemKey }
                                    if (idx == -1) return@detectDragGestures

                                    while (dragOffsetPx > 0 && idx < localOrder.lastIndex) {
                                        val belowHeight = itemHeights[key(localOrder[idx + 1])] ?: break
                                        if (dragOffsetPx <= belowHeight / 2f) break
                                        localOrder = localOrder.toMutableList().apply {
                                            add(idx, removeAt(idx + 1))
                                        }
                                        dragOffsetPx -= belowHeight
                                        idx++
                                    }
                                    while (dragOffsetPx < 0 && idx > 0) {
                                        val aboveHeight = itemHeights[key(localOrder[idx - 1])] ?: break
                                        if (dragOffsetPx >= -aboveHeight / 2f) break
                                        localOrder = localOrder.toMutableList().apply {
                                            add(idx, removeAt(idx - 1))
                                        }
                                        dragOffsetPx += aboveHeight
                                        idx--
                                    }
                                },
                                onDragEnd = {
                                    onOrderChanged(localOrder)
                                    draggingKey = null
                                    dragOffsetPx = 0f
                                },
                                onDragCancel = {
                                    localOrder = items
                                    draggingKey = null
                                    dragOffsetPx = 0f
                                },
                            )
                        },
                ) {
                    itemContent(item, itemKey == draggingKey)
                }
            }
        }
    }
}
