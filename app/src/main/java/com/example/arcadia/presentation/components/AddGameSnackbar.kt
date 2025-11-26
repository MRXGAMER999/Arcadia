package com.example.arcadia.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary

private const val UNDO_TIMEOUT_MS = 5000L

/**
 * Professional snackbar shown when a game is added to the library.
 * Matches the design of other snackbars in the app (MyGames remove snackbar).
 */
@Composable
fun AddGameSnackbar(
    visible: Boolean,
    gameName: String,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    // Countdown timer animation
    val timeRemaining = remember { Animatable(1f) }
    
    LaunchedEffect(visible) {
        if (visible) {
            timeRemaining.snapTo(1f)
            timeRemaining.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = UNDO_TIMEOUT_MS.toInt(),
                    easing = LinearEasing
                )
            )
            // Auto-dismiss when timer ends
            if (timeRemaining.value <= 0f) {
                onDismiss()
            }
        }
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(150)
        ) + fadeOut(animationSpec = tween(150)),
        modifier = modifier
    ) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            containerColor = Color(0xFF1E2A47),
            contentColor = TextSecondary,
            action = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular progress indicator showing time remaining
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { timeRemaining.value },
                            modifier = Modifier.size(24.dp),
                            color = ButtonPrimary,
                            strokeWidth = 2.dp,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "${(timeRemaining.value * (UNDO_TIMEOUT_MS / 1000)).toInt()}",
                            color = ButtonPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Undo button
                    TextButton(onClick = { 
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onUndo() 
                    }) {
                        Text(
                            text = "UNDO",
                            color = ButtonPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissAction = {
                IconButton(onClick = { 
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDismiss() 
                }) {
                    Text(
                        text = "✕",
                        color = TextSecondary,
                        fontSize = 18.sp
                    )
                }
            }
        ) {
            Text(
                text = "$gameName added",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
