package com.example.arcadia.presentation.screens.detailsScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.domain.model.Game
import com.example.arcadia.presentation.components.PlatformIcons
import com.example.arcadia.ui.theme.ButtonPrimary

import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun GameDescriptionSection(game: Game) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "About the Game",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        game.description?.let { description ->
            if (description.isNotBlank()) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = description,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (description.length > 200) { // Arbitrary length check
                        Text(
                            text = if (isExpanded) "Read Less" else "Read More",
                            color = ButtonPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { isExpanded = !isExpanded }
                        )
                    }
                }
            }
        }

        if (game.genres.isNotEmpty()) {
            Text(
                text = "Genres: ${game.genres.joinToString(", ")}",
                color = ButtonPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (game.platforms.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "Platforms",
                    color = ButtonPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                PlatformIcons(
                    platforms = game.platforms,
                    iconSize = 24.dp,
                    showBackground = false
                )
            }
        }

        if (game.tags.isNotEmpty()) {
            Text(
                text = "Tags: ${game.tags.joinToString(", ")}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Release Date:",
                    color = ButtonPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = game.released ?: "TBA",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }

            game.metacritic?.let { score ->
                Column {
                    Text(
                        text = "Metacritic:",
                        color = ButtonPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = score.toString(),
                        color = when {
                            score >= 75 -> Color(0xFF6DC849)
                            score >= 50 -> Color(0xFFFFD700)
                            else -> Color(0xFFFF6B6B)
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
