package com.example.arcadia.presentation.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.clip
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
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(painter = painterResource(id = R.drawable.controller), contentDescription = null, tint = YellowAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "GAMING STATS", fontSize = 16.sp, fontFamily = BebasNeueFont, color = YellowAccent, letterSpacing = 1.5.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Stats Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MainStatItem(value = statsState.totalGames.toString(), label = "Games", color = ButtonPrimary)
                MainStatItem(value = "${statsState.hoursPlayed}h", label = "Hours", color = NeonBlue)
                MainStatItem(
                    value = if (statsState.avgRating > 0) String.format("%.1f", statsState.avgRating) else "-",
                    label = "Rating",
                    color = Color(0xFF4ADE80)
                )
                MainStatItem(value = "${statsState.completionRate.toInt()}%", label = "Comp.", color = NeonPurple)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Distribution Bar
            val total = statsState.finishedGames + statsState.playingGames + statsState.wantToPlayGames + statsState.droppedGames + statsState.onHoldGames
            if (total > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    if (statsState.finishedGames > 0) Box(modifier = Modifier.weight(statsState.finishedGames.toFloat()).fillMaxHeight().background(YellowAccent))
                    if (statsState.playingGames > 0) Box(modifier = Modifier.weight(statsState.playingGames.toFloat()).fillMaxHeight().background(NeonPink))
                    if (statsState.wantToPlayGames > 0) Box(modifier = Modifier.weight(statsState.wantToPlayGames.toFloat()).fillMaxHeight().background(Color(0xFF64B5F6)))
                    if (statsState.onHoldGames > 0) Box(modifier = Modifier.weight(statsState.onHoldGames.toFloat()).fillMaxHeight().background(Color(0xFFFFB74D)))
                    if (statsState.droppedGames > 0) Box(modifier = Modifier.weight(statsState.droppedGames.toFloat()).fillMaxHeight().background(Color(0xFFFF5555)))
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        LegendItem(count = statsState.finishedGames, label = "Finished", color = YellowAccent)
                        LegendItem(count = statsState.playingGames, label = "Playing", color = NeonPink)
                        LegendItem(count = statsState.wantToPlayGames, label = "Backlog", color = Color(0xFF64B5F6))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        LegendItem(count = statsState.onHoldGames, label = "On Hold", color = Color(0xFFFFB74D), modifier = Modifier.padding(end = 16.dp))
                        LegendItem(count = statsState.droppedGames, label = "Dropped", color = Color(0xFFFF5555))
                    }
                }
            }
        }
    }
}

@Composable
private fun MainStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value, 
            fontSize = 24.sp, 
            fontFamily = BebasNeueFont, 
            color = color,
            style = TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = color.copy(alpha = 0.5f),
                    blurRadius = 8f
                )
            )
        )
        Text(text = label, fontSize = 12.sp, color = TextSecondary.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LegendItem(count: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "$count $label", fontSize = 12.sp, color = TextSecondary.copy(alpha = 0.9f), fontWeight = FontWeight.Medium)
    }
}
