package com.example.arcadia.presentation.screens.profile.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.R
import com.example.arcadia.presentation.screens.profile.ProfileStatsState
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.YellowAccent

private val CardBackground = Color(0xFF0A1F4D)
private val CardBorder = Color(0xFF1E3A8A)
private val NeonBlue = Color(0xFF00D4FF)
private val NeonPurple = Color(0xFFBD00FF)
private val NeonPink = Color(0xFFFF006E)
private val ButtonPrimary = Color(0xFF0EA5E9)
private val TextSecondary = Color(0xFFE0E0E0)

@Composable
fun GamingStatsCard(statsState: ProfileStatsState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(painter = painterResource(id = R.drawable.controller), contentDescription = null, tint = YellowAccent, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "GAMING STATS", fontSize = 18.sp, fontFamily = BebasNeueFont, color = YellowAccent, letterSpacing = 2.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(value = statsState.totalGames.toString(), label = "Games", color = ButtonPrimary, modifier = Modifier.weight(1f))
                StatItem(value = statsState.finishedGames.toString(), label = "Finished", color = YellowAccent, modifier = Modifier.weight(1f))
                StatItem(value = statsState.playingGames.toString(), label = "Playing", color = NeonPink, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(value = statsState.droppedGames.toString(), label = "Dropped", color = Color(0xFFFF5555), modifier = Modifier.weight(1f))
                StatItem(value = statsState.onHoldGames.toString(), label = "On Hold", color = Color(0xFFFFB74D), modifier = Modifier.weight(1f))
                StatItem(value = statsState.wantToPlayGames.toString(), label = "Want to Play", color = Color(0xFF64B5F6), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Divider
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(value = "${statsState.hoursPlayed}h", label = "Hours", color = NeonBlue, modifier = Modifier.weight(1f))
                StatItem(
                    value = if (statsState.avgRating > 0) String.format("%.1f", statsState.avgRating) else "-",
                    label = "Avg Rating",
                    color = Color(0xFF4ADE80),
                    modifier = Modifier.weight(1f)
                )
                StatItem(value = "${statsState.completionRate.toInt()}%", label = "Completion", color = NeonPurple, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = value, 
            fontSize = 32.sp, 
            fontFamily = BebasNeueFont, 
            color = color,
            style = TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = color.copy(alpha = 0.5f),
                    blurRadius = 10f
                )
            )
        )
        Text(text = label, fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.7f), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
    }
}
