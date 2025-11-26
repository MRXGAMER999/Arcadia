package com.example.arcadia.presentation.screens.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.presentation.screens.analytics.AnalyticsState
import com.example.arcadia.domain.usecase.GamingPersonality
import com.example.arcadia.ui.theme.TextSecondary

/**
 * Gaming DNA card displaying the user's gaming personality and genre distribution.
 */
@Composable
fun GamingDNACard(state: AnalyticsState) {
    // Determine background gradient based on personality
    val gradientColors = when (state.gamingPersonality) {
        GamingPersonality.Completionist -> listOf(Color(0xFF1A237E), Color(0xFF3949AB)) // Deep Blue
        GamingPersonality.Explorer -> listOf(Color(0xFF006064), Color(0xFF0097A7)) // Cyan/Teal
        GamingPersonality.Hardcore -> listOf(Color(0xFFB71C1C), Color(0xFFD32F2F)) // Red
        GamingPersonality.Casual -> listOf(Color(0xFF1B5E20), Color(0xFF388E3C)) // Green
        else -> listOf(Color(0xFF1E2A47), Color(0xFF2D3E5F)) // Default
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(gradientColors))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gaming DNA",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LVL ${(state.totalGames / 5 + 1).coerceAtMost(99)}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Personality Badge
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF62B4DA), Color(0xFF9F55FF))
                            ),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = state.gamingPersonality.title.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = state.gamingPersonality.description,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Genre Pie Chart
                if (state.topGenres.isNotEmpty()) {
                    PieChart(
                        data = state.topGenres,
                        colors = listOf(
                            Color(0xFF4ADE80),
                            Color(0xFF60A5FA),
                            Color(0xFFF472B6),
                            Color(0xFFFBB02E),
                            Color(0xFFA78BFA),
                            Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }
    }
}
