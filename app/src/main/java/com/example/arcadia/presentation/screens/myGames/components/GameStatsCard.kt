package com.example.arcadia.presentation.screens.myGames.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.presentation.components.getPlatformInfo
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameStatsCard(
    games: List<GameListEntry>,
    modifier: Modifier = Modifier,
    onSeeMoreClick: (() -> Unit)? = null
) {
    val totalGames = games.size
    val totalHours = games.sumOf { it.hoursPlayed }
    val ratedGamesCount = games.count { it.rating != null && it.rating!! > 0 }
    val avgRating = if (ratedGamesCount > 0) {
        games.mapNotNull { it.rating }.filter { it > 0 }.average()
    } else {
        0.0
    }

    val finishedGames = games.count { it.status == GameStatus.FINISHED }
    val playingGames = games.count { it.status == GameStatus.PLAYING }
    val wantGames = games.count { it.status == GameStatus.WANT }
    
    // Top Genres
    val topGenres = games.flatMap { it.genres }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(5)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2A47).copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(24.dp) // Increased corner radius for modern look
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gaming Profile",
                    color = TextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Only show Full Analytics button if callback is provided
                if (onSeeMoreClick != null) {
                    androidx.compose.material3.TextButton(onClick = onSeeMoreClick) {
                        Text(
                            text = "Full Analytics",
                            color = ButtonPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Hero Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HeroStatItem(
                    value = totalGames.toString(),
                    label = "Games",
                    icon = null // Just number
                )
                HeroStatItem(
                    value = totalHours.toString(),
                    label = "Hours",
                    icon = Icons.Default.AccessTime
                )
                HeroStatItem(
                    value = String.format(Locale.US, "%.1f", avgRating),
                    label = "Avg Rating",
                    icon = Icons.Default.Star,
                    iconTint = YellowAccent
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = TextSecondary.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(20.dp))

            // Status Breakdown (Compact)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    CompactStatItem(
                        icon = Icons.Default.Check,
                        label = "Finished",
                        count = finishedGames,
                        total = totalGames,
                        color = Color(0xFF4ADE80)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CompactStatItem(
                        icon = Icons.Default.Star,
                        label = "Rated",
                        count = ratedGamesCount,
                        total = totalGames,
                        color = YellowAccent
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    CompactStatItem(
                        icon = Icons.Default.PlayArrow,
                        label = "Playing",
                        count = playingGames,
                        total = totalGames,
                        color = ButtonPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CompactStatItem(
                        icon = Icons.Default.Favorite,
                        label = "Want to Play",
                        count = wantGames,
                        total = totalGames,
                        color = Color(0xFFF472B6)
                    )
                }
            }

            if (topGenres.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Top Genres",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    topGenres.forEach { (genre, count) ->
                        GenreChip(name = genre, count = count)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStatItem(
    value: String,
    label: String,
    icon: ImageVector? = null,
    iconTint: Color = ButtonPrimary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp).padding(end = 4.dp)
                )
            }
            Text(
                text = value,
                color = TextSecondary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = label,
            color = TextSecondary.copy(alpha = 0.6f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CompactStatItem(
    icon: ImageVector,
    label: String,
    count: Int,
    total: Int,
    color: Color
) {
    val progress = if (total > 0) count.toFloat() / total.toFloat() else 0f
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = TextSecondary.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            Text(
                text = count.toString(),
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Color(0xFF2D3E5F),
        )
    }
}

@Composable
private fun GenreChip(name: String, count: Int) {
    Surface(
        color = Color(0xFF2D3E5F),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .background(ButtonPrimary.copy(alpha = 0.2f), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    color = ButtonPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PlatformStatItem(platformName: String, count: Int) {
    val platformInfo = getPlatformInfo(platformName)
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF2D3E5F), CircleShape)
        ) {
            if (platformInfo != null) {
                Icon(
                    painter = painterResource(id = platformInfo.iconRes),
                    contentDescription = platformName,
                    tint = platformInfo.color,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                // Fallback for unknown platforms
                Text(
                    text = platformName.take(1).uppercase(),
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = count.toString(),
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

