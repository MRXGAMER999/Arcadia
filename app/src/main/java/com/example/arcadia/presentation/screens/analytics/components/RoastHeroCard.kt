package com.example.arcadia.presentation.screens.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hero card that invites users to get their gaming profile roasted.
 * Displays a dark gradient from deep red to black with dynamic hours text.
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4
 */
@Composable
fun RoastHeroCard(
    hoursPlayed: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dark gradient from deep red to black (Requirement 1.2)
    val gradientColors = listOf(
        Color(0xFF8B0000),  // Deep red
        Color(0xFF4A0000),  // Dark red
        Color(0xFF1A0000),  // Very dark red
        Color(0xFF000000)   // Black
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),  // Requirement 1.4
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(gradientColors)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Fire emoji header
                Text(
                    text = "🔥",
                    fontSize = 48.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Main title
                Text(
                    text = "Get Roasted",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Dynamic subtext with actual hours played (Requirement 1.3)
                Text(
                    text = buildRoastSubtext(hoursPlayed),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Call to action hint
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap to discover your gaming sins",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "→",
                        color = Color(0xFFFF6B35),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Builds dynamic subtext that references the user's actual hours played.
 * Property 1: Hero Card Hours Display - the subtext SHALL contain the hours value.
 */
private fun buildRoastSubtext(hoursPlayed: Int): String {
    return when {
        hoursPlayed < 10 -> "You've spent $hoursPlayed hours gaming. Let's see what you've been up to..."
        hoursPlayed < 50 -> "With $hoursPlayed hours logged, we've got plenty to work with..."
        hoursPlayed < 100 -> "$hoursPlayed hours of gaming? Time to face the music."
        hoursPlayed < 500 -> "$hoursPlayed hours in your library. That's a lot of questionable decisions."
        else -> "$hoursPlayed hours of your life. Let's see if it was worth it."
    }
}
