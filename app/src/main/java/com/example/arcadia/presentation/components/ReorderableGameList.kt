package com.example.arcadia.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.arcadia.domain.model.GameListEntry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A wrapper that enables drag-and-drop reordering for list items.
 * Shows elevation and scale effect while dragging.
 * Supports auto-scroll and live reordering preview.
 */
@Composable
fun DraggableGameItem(
    game: GameListEntry,
    index: Int,
    isReorderMode: Boolean,
    isDragging: Boolean,
    dragOffset: Float,
    currentIndex: Int, // The current visual index (may differ from original during drag)
    onDragStart: (Int) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onIndexChange: (Int) -> Unit, // Called when item should move to new index
    modifier: Modifier = Modifier,
    listState: LazyListState? = null,
    containerHeight: Float = 0f,
    itemHeight: Float = 156f,
    content: @Composable () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val canDrag = isReorderMode && game.rating != null
    val coroutineScope = rememberCoroutineScope()
    
    var itemPositionY by remember { mutableFloatStateOf(0f) }
    var autoScrollJob by remember { mutableStateOf<Job?>(null) }
    
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dragElevation"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dragScale"
    )
    
    // Calculate target index based on drag offset and trigger live reorder
    LaunchedEffect(isDragging, dragOffset) {
        if (isDragging && itemHeight > 0) {
            val offsetInItems = (dragOffset / itemHeight).roundToInt()
            val newIndex = (currentIndex + offsetInItems).coerceAtLeast(0)
            if (newIndex != currentIndex) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onIndexChange(newIndex)
            }
        }
    }
    
    // Auto-scroll when dragging near edges
    LaunchedEffect(isDragging) {
        if (isDragging && listState != null && containerHeight > 0f) {
            autoScrollJob = coroutineScope.launch {
                while (isActive) {
                    val currentY = itemPositionY + dragOffset
                    val edgeThreshold = containerHeight * 0.2f
                    
                    val scrollAmount = when {
                        currentY < edgeThreshold -> {
                            val proximity = ((edgeThreshold - currentY) / edgeThreshold).coerceIn(0f, 1f)
                            -(5f + proximity * 15f)
                        }
                        currentY > containerHeight - edgeThreshold -> {
                            val proximity = ((currentY - (containerHeight - edgeThreshold)) / edgeThreshold).coerceIn(0f, 1f)
                            5f + proximity * 15f
                        }
                        else -> 0f
                    }
                    
                    if (scrollAmount != 0f) {
                        listState.dispatchRawDelta(-scrollAmount)
                    }
                    delay(16)
                }
            }
        } else {
            autoScrollJob?.cancel()
        }
    }
    
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                itemPositionY = coordinates.positionInRoot().y
            }
            .then(
                if (isDragging) {
                    Modifier
                        .offset { IntOffset(0, dragOffset.roundToInt()) }
                        .zIndex(10f)
                        .shadow(elevation, RoundedCornerShape(12.dp))
                        .scale(scale)
                } else {
                    Modifier.zIndex(0f)
                }
            )
            .then(
                if (canDrag) {
                    Modifier.pointerInput(index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDragStart(index)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            },
                            onDragEnd = {
                                autoScrollJob?.cancel()
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDragEnd()
                            },
                            onDragCancel = {
                                autoScrollJob?.cancel()
                                onDragEnd()
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

/**
 * A wrapper for grid items that enables drag-and-drop reordering.
 * Supports auto-scroll and live reordering preview.
 */
@Composable
fun DraggableGridItem(
    game: GameListEntry,
    index: Int,
    isReorderMode: Boolean,
    isDragging: Boolean,
    dragOffsetX: Float,
    dragOffsetY: Float,
    currentIndex: Int,
    onDragStart: (Int) -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState? = null,
    containerHeight: Float = 0f,
    itemHeight: Float = 200f,
    itemWidth: Float = 120f,
    columns: Int = 3,
    content: @Composable () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val canDrag = isReorderMode && game.rating != null
    val coroutineScope = rememberCoroutineScope()
    
    var itemPositionY by remember { mutableFloatStateOf(0f) }
    var autoScrollJob by remember { mutableStateOf<Job?>(null) }
    
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 16.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dragElevation"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dragScale"
    )
    
    // Calculate target index based on drag offset and trigger live reorder
    LaunchedEffect(isDragging, dragOffsetX, dragOffsetY) {
        if (isDragging && itemHeight > 0 && itemWidth > 0) {
            val rowOffset = (dragOffsetY / itemHeight).roundToInt()
            val colOffset = (dragOffsetX / itemWidth).roundToInt()
            val newIndex = (currentIndex + rowOffset * columns + colOffset).coerceAtLeast(0)
            if (newIndex != currentIndex) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onIndexChange(newIndex)
            }
        }
    }
    
    // Auto-scroll when dragging near edges
    LaunchedEffect(isDragging) {
        if (isDragging && gridState != null && containerHeight > 0f) {
            autoScrollJob = coroutineScope.launch {
                while (isActive) {
                    val currentY = itemPositionY + dragOffsetY
                    val edgeThreshold = containerHeight * 0.2f
                    
                    val scrollAmount = when {
                        currentY < edgeThreshold -> {
                            val proximity = ((edgeThreshold - currentY) / edgeThreshold).coerceIn(0f, 1f)
                            -(5f + proximity * 15f)
                        }
                        currentY > containerHeight - edgeThreshold -> {
                            val proximity = ((currentY - (containerHeight - edgeThreshold)) / edgeThreshold).coerceIn(0f, 1f)
                            5f + proximity * 15f
                        }
                        else -> 0f
                    }
                    
                    if (scrollAmount != 0f) {
                        gridState.dispatchRawDelta(-scrollAmount)
                    }
                    delay(16)
                }
            }
        } else {
            autoScrollJob?.cancel()
        }
    }
    
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                itemPositionY = coordinates.positionInRoot().y
            }
            .then(
                if (isDragging) {
                    Modifier
                        .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
                        .zIndex(10f)
                        .shadow(elevation, RoundedCornerShape(12.dp))
                        .scale(scale)
                } else {
                    Modifier.zIndex(0f)
                }
            )
            .then(
                if (canDrag) {
                    Modifier.pointerInput(index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDragStart(index)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            },
                            onDragEnd = {
                                autoScrollJob?.cancel()
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDragEnd()
                            },
                            onDragCancel = {
                                autoScrollJob?.cancel()
                                onDragEnd()
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

/**
 * State holder for drag-and-drop reordering with live preview.
 */
class ReorderState {
    var isDragging by mutableStateOf(false)
        private set
    var draggedItemIndex by mutableIntStateOf(-1)
        private set
    var currentTargetIndex by mutableIntStateOf(-1)
        private set
    var dragOffset by mutableFloatStateOf(0f)
        private set
    var dragOffsetX by mutableFloatStateOf(0f)
        private set
    var dragOffsetY by mutableFloatStateOf(0f)
        private set
    
    private var originalIndex = -1
    var itemHeight = 0f
        private set
    var itemWidth = 0f
        private set
    var columnsCount = 1
        private set
    
    fun startDrag(index: Int, itemHeight: Float = 100f, itemWidth: Float = 100f, columns: Int = 1) {
        isDragging = true
        draggedItemIndex = index
        originalIndex = index
        currentTargetIndex = index
        dragOffset = 0f
        dragOffsetX = 0f
        dragOffsetY = 0f
        this.itemHeight = itemHeight
        this.itemWidth = itemWidth
        this.columnsCount = columns
    }
    
    fun updateDrag(offsetY: Float) {
        dragOffset += offsetY
    }
    
    fun updateGridDrag(offsetX: Float, offsetY: Float) {
        dragOffsetX += offsetX
        dragOffsetY += offsetY
    }
    
    fun updateTargetIndex(newIndex: Int) {
        if (newIndex != currentTargetIndex && newIndex >= 0) {
            currentTargetIndex = newIndex
        }
    }
    
    fun endDrag(): Pair<Int, Int>? {
        if (!isDragging || originalIndex < 0) {
            reset()
            return null
        }
        
        val fromIndex = originalIndex
        val toIndex = currentTargetIndex
        
        reset()
        
        return if (fromIndex != toIndex && toIndex >= 0) {
            fromIndex to toIndex
        } else {
            null
        }
    }
    
    private fun reset() {
        isDragging = false
        draggedItemIndex = -1
        originalIndex = -1
        currentTargetIndex = -1
        dragOffset = 0f
        dragOffsetX = 0f
        dragOffsetY = 0f
    }
}

@Composable
fun rememberReorderState(): ReorderState {
    return remember { ReorderState() }
}
