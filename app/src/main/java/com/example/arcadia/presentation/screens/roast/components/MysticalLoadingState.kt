package com.example.arcadia.presentation.screens.roast.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.arcadia.presentation.screens.roast.RoastTheme

/**
 * Mystical loading screen with crystal ball animation and rotating messages.
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5
 */
@Composable
fun MysticalLoadingState(
    message: String,
    reduceMotion: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Crystal Ball / Orb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            if (!reduceMotion) {
                val transition = rememberInfiniteTransition(label = "orb_pulse")
                val scale by transition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                
                // Outer Glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    RoastTheme.glowOrange,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Inner Orb
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                RoastTheme.fireOrange,
                                RoastTheme.emberRed
                            )
                        )
                    )
            )
            
            Text(
                text = "🔮",
                style = MaterialTheme.typography.displayMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Typewriter Message
        TypewriterText(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = RoastTheme.textPrimary,
            textAlign = TextAlign.Center,
            reduceMotion = reduceMotion
        )
    }
}
