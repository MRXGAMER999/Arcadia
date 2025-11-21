package com.example.arcadia.presentation.screens.myGames.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent

@Composable
fun GameStatsCard(
    games: List<GameListEntry>,
    modifier: Modifier = Modifier
) {
    val totalGames = games.size
    val finishedGames = games.count { it.status == GameStatus.FINISHED }
    val playingGames = games.count { it.status == GameStatus.PLAYING }
    val wantGames = games.count { it.status == GameStatus.WANT }
    val ratedGames = games.count { it.rating != null && it.rating!! > 0 }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2A47).copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Your Gaming Stats",
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Total games
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Games",
                    color = TextSecondary.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Text(
                    text = totalGames.toString(),
                    color = ButtonPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status breakdown
            StatItem(
                icon = Icons.Default.Check,
                label = "Finished",
                count = finishedGames,
                total = totalGames,
                color = Color(0xFF4ADE80)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            StatItem(
                icon = Icons.Default.PlayArrow,
                label = "Playing",
                count = playingGames,
                total = totalGames,
                color = ButtonPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            StatItem(
                icon = Icons.Default.Favorite,
                label = "Want to Play",
                count = wantGames,
                total = totalGames,
                color = Color(0xFFF472B6)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            StatItem(
                icon = Icons.Default.Star,
                label = "Rated",
                count = ratedGames,
                total = totalGames,
                color = YellowAccent
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    var animatedProgress by remember { mutableFloatStateOf(0f) }
    val progress = if (total > 0) count.toFloat() / total.toFloat() else 0f
    
    LaunchedEffect(progress) {
        animatedProgress = progress
    }
    
    val animatedProgressValue by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    color = TextSecondary.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
            Text(
                text = "$count / $total",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        LinearProgressIndicator(
            progress = { animatedProgressValue },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFF2D3E5F),
        )
    }
}

