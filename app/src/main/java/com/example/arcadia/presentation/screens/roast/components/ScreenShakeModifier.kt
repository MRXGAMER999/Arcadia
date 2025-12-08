package com.example.arcadia.presentation.screens.roast.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.random.Random

/**
 * Modifier for applying a screen shake effect.
 * 
 * Requirements: 7.2, 12.1
 */
fun Modifier.shake(
    enabled: Boolean,
    intensity: Float = 10f
): Modifier = composed {
    val translationX = remember { Animatable(0f) }
    val translationY = remember { Animatable(0f) }

    LaunchedEffect(enabled) {
        if (enabled) {
            repeat(10) {
                translationX.animateTo(
                    targetValue = Random.nextFloat() * intensity - (intensity / 2),
                    animationSpec = spring(stiffness = 1000f)
                )
                translationY.animateTo(
                    targetValue = Random.nextFloat() * intensity - (intensity / 2),
                    animationSpec = spring(stiffness = 1000f)
                )
            }
            // Reset
            translationX.animateTo(0f)
            translationY.animateTo(0f)
        }
    }

    this.graphicsLayer {
        this.translationX = translationX.value
        this.translationY = translationY.value
    }
}
