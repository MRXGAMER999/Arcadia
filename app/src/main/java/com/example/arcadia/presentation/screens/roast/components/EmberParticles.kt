package com.example.arcadia.presentation.screens.roast.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.arcadia.presentation.screens.roast.RoastTheme
import kotlin.random.Random

/**
 * Background component displaying floating ember particles.
 * 
 * Requirements: 1.2, 12.2
 */
@Composable
fun EmberParticles(
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false
) {
    if (reduceMotion) return

    val particles = remember { List(20) { EmberParticle() } }
    
    val transition = rememberInfiniteTransition(label = "embers")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { particle ->
            // Calculate position based on time
            // y moves up (negative), x sways
            val yProgress = (particle.initialYProgress - time * particle.speed) % 1.0f
            val y = (if (yProgress < 0) yProgress + 1 else yProgress) * height
            
            val xSway = kotlin.math.sin(time * particle.swaySpeed * 2 * kotlin.math.PI + particle.initialX) * particle.swayAmount
            val x = (particle.initialX * width) + xSway

            val alpha = if (y < height * 0.1f) {
                y / (height * 0.1f) // Fade out at top
            } else if (y > height * 0.9f) {
                (height - y) / (height * 0.1f) // Fade in at bottom
            } else {
                1f
            } * particle.maxAlpha

            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = particle.size,
                center = Offset(x.toFloat(), y)
            )
        }
    }
}

private data class EmberParticle(
    val initialX: Float = Random.nextFloat(),
    val initialYProgress: Float = Random.nextFloat(),
    val speed: Float = Random.nextFloat() * 0.5f + 0.2f, // 0.2 to 0.7 screens per 10s
    val swaySpeed: Float = Random.nextFloat() * 2f + 1f,
    val swayAmount: Float = Random.nextFloat() * 50f + 10f,
    val size: Float = Random.nextFloat() * 4f + 2f,
    val maxAlpha: Float = Random.nextFloat() * 0.6f + 0.2f,
    val color: Color = if (Random.nextBoolean()) RoastTheme.fireOrange else RoastTheme.emberRed
)
