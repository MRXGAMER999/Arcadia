package com.example.arcadia.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Swipe-to-delete wrapper with smooth custom animations and haptic feedback.
 * Uses custom gesture handling for better control and undo support.
 */
@Composable
fun SwipeToDeleteItem(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    
    // Screen width for calculating thresholds
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val deleteThreshold = screenWidthPx * 0.4f // 40% of screen width to trigger delete
    
    // Animated offset for the swipe
    val offsetX = remember { Animatable(0f) }
    
    // Track if we've triggered haptic feedback for this swipe
    var hasTriggeredHaptic by remember { mutableFloatStateOf(0f) }
    
    // Calculate progress for animations (0 to 1)
    val swipeProgress = (offsetX.value.absoluteValue / deleteThreshold).coerceIn(0f, 1f)
    
    // Icon scale animation based on progress
    val iconScale by animateFloatAsState(
        targetValue = when {
            swipeProgress > 0.8f -> 1.3f
            swipeProgress > 0.5f -> 1.1f
            swipeProgress > 0.2f -> 1f
            else -> 0.8f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )
    
    // Background alpha based on swipe progress
    val backgroundAlpha by animateFloatAsState(
        targetValue = (swipeProgress * 1.2f).coerceIn(0f, 1f),
        animationSpec = tween(100),
        label = "backgroundAlpha"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Background (delete indicator)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF8B0000).copy(alpha = backgroundAlpha * 0.9f),
                            Color(0xFFDC143C).copy(alpha = backgroundAlpha)
                        )
                    )
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            // Show delete icon and text when swiping
            if (swipeProgress > 0.1f) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = swipeProgress
                        }
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = if (swipeProgress > 0.8f) "Release to delete" else "Delete",
                        color = Color.White.copy(alpha = swipeProgress),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.scale(iconScale)
                    )
                }
            }
        }
        
        // Foreground content (swipeable)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            hasTriggeredHaptic = 0f
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value < -deleteThreshold) {
                                    // Swipe past threshold - trigger delete with slide-out animation
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    
                                    // Animate off screen
                                    offsetX.animateTo(
                                        targetValue = -screenWidthPx,
                                        animationSpec = tween(200)
                                    )
                                    
                                    // Trigger delete callback
                                    onDelete()
                                    
                                    // Reset position for when item might be restored (undo)
                                    offsetX.snapTo(0f)
                                } else {
                                    // Didn't reach threshold - spring back with bounce
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            coroutineScope.launch {
                                // Only allow swiping left (negative direction) - ignore right swipes
                                if (offsetX.value == 0f && dragAmount > 0f) {
                                    // At rest position and trying to swipe right - ignore
                                    return@launch
                                }
                                val newValue = (offsetX.value + dragAmount).coerceAtMost(0f)
                                // Add resistance when swiping too far
                                val resistance = if (newValue < -deleteThreshold * 1.5f) 0.3f else 1f
                                val adjustedDrag = if (dragAmount > 0f) dragAmount else dragAmount * resistance
                                offsetX.snapTo((offsetX.value + adjustedDrag).coerceAtMost(0f))
                                
                                // Haptic feedback at threshold
                                val currentProgress = offsetX.value.absoluteValue / deleteThreshold
                                if (currentProgress >= 1f && hasTriggeredHaptic < 1f) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    hasTriggeredHaptic = 1f
                                }
                            }
                        }
                    )
                }
                .graphicsLayer {
                    // Subtle rotation during swipe for a more dynamic feel
                    rotationZ = (offsetX.value / screenWidthPx) * -2f
                }
        ) {
            content()
        }
    }
}
