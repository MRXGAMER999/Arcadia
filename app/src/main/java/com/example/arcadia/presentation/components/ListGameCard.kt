package com.example.arcadia.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import com.example.arcadia.R
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.getRatingGradient
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.draw.alpha
import com.example.arcadia.ui.theme.getRatingGradient
import com.example.arcadia.ui.theme.getRatingColor
import java.util.Locale

/**
 * ListGameCard - A horizontal game card component for list views
 * Displays game cover, title, metadata, status icon, and rating with gradient
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun ListGameCard(
    game: GameListEntry,
    modifier: Modifier = Modifier,
    showDateAdded: Boolean = true,
    showReleaseDate: Boolean = false,
    onClick: () -> Unit = {},
    onEditClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalPlatformContext.current
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = 6.dp, bottom = 12.dp) // Add padding to make room for the edit icon
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F1B41)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Game Cover Image
            Card(
                modifier = Modifier
                    .width(90.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                val imageSizePx = with(density) {
                    90.dp.roundToPx()
                }

                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(game.backgroundImage ?: "")
                        .size(imageSizePx, imageSizePx)
                        .scale(Scale.FILL)
                        .memoryCacheKey(game.backgroundImage)
                        .diskCacheKey(game.backgroundImage)
                        .crossfade(true)
                        .build(),
                    contentDescription = game.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0F1922)),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(
                                color = Color(0xFF62B4DA),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0F1922)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎮", fontSize = 24.sp)
                        }
                    }
                )
            }

            // Game Info Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Section: Rating only (Status moved to right side of card)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (game.rating != null && game.rating > 0f) {
                        // Rating with gradient text
                        BasicText(
                            text = String.format(Locale.US, "%.1f", game.rating),
                            style = MaterialTheme.typography.titleLarge.copy(
                                brush = getRatingGradient(game.rating),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        )

                        // Rating icon with gradient color
                        Icon(
                            painter = painterResource(id = getRatingIcon(game.rating)),
                            contentDescription = "Rating icon",
                            modifier = Modifier.size(20.dp),
                            tint = getRatingColor(game.rating)
                        )
                    } else {
                        Text(
                            text = "Not Rated",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary.copy(alpha = 0.4f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Middle Section: Game Title
                Text(
                    text = game.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = TextSecondary,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Section: Genres and Dates
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Genres
                    if (game.genres.isNotEmpty()) {
                        Text(
                            text = game.genres.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    // Date Display (conditional based on settings)
                    val dateText = formatDates(
                        releaseDate = if (showReleaseDate) game.releaseDate else null,
                        addedAt = if (showDateAdded) game.addedAt else null,
                        updatedAt = if (showDateAdded) game.updatedAt else null
                    )
                    if (dateText.isNotEmpty()) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }
            
            // Right side: Status Badge only (Edit button moved outside)
            // Status Badge with Icon and Text - aligned to top right
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(getStatusColor(game.status))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Status Text
                Text(
                    text = game.status.displayName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = Color.Black
                )

                // Status Icon
                Icon(
                    painter = painterResource(id = getStatusIcon(game.status)),
                    contentDescription = game.status.displayName,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
        
        // Edit icon button - positioned at bottom end, above the card
        if (onEditClick != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 0.dp, y = 12.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2A47))
            ) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit game",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * Get the icon resource for a rating
 */
private fun getRatingIcon(rating: Float): Int {
    return when {
        rating == 0f -> R.drawable.no_rating_ic
        rating <= 1f -> R.drawable.between0and2_ic
        rating <= 2f -> R.drawable.between0and2_ic
        rating <= 3f -> R.drawable.from2to4_ic
        rating <= 4f -> R.drawable.from2to4_ic
        rating <= 5f -> R.drawable.from4to6_ic
        rating <= 6f -> R.drawable.from4to6_ic
        rating <= 7f -> R.drawable.from6_5to7_5_ic
        rating <= 7.5f -> R.drawable.from6_5to7_5_ic
        rating <= 8f -> R.drawable.from7_5to8_5_ic
        rating <= 8.5f -> R.drawable.from7_5to8_5_ic
        rating <= 9f -> R.drawable.from8_5to9_5_ic
        rating <= 9.5f -> R.drawable.from8_5to9_5_ic
        else -> R.drawable.from9_5to10_ic
    }
}

/**
 * Get the icon resource for a game status
 */
private fun getStatusIcon(status: GameStatus): Int {
    return when (status) {
        GameStatus.FINISHED -> R.drawable.finished_ic
        GameStatus.PLAYING -> R.drawable.playing_ic
        GameStatus.DROPPED -> R.drawable.dropped_ic
        GameStatus.ON_HOLD -> R.drawable.on_hold_ic
        GameStatus.WANT -> R.drawable.want_ic
    }
}

/**
 * Get the color for a game status
 */
private fun getStatusColor(status: GameStatus): Color {
    return when (status) {
        GameStatus.FINISHED -> Color(0xFFFBB02E)
        GameStatus.PLAYING -> Color(0xFFD34ECE)
        GameStatus.DROPPED -> Color(0xFFBA5C3E)
        GameStatus.ON_HOLD -> Color(0xFF62B4DA)
        GameStatus.WANT -> Color(0xFF3F77CC)
    }
}

/**
 * Get rating description text
 */
private fun getRatingDescription(rating: Float): String {
    return when {
        rating == 0f -> ""
        rating <= 3f -> "Poor"
        rating <= 6f -> "Good"
        rating <= 8f -> "Great"
        rating <= 10f -> "Masterpiece"
        else -> ""
    }
}

/**
 * Format release date and date added/updated to library
 * Returns empty string if neither date should be shown
 */
private fun formatDates(
    releaseDate: String?, 
    addedAt: Long?,
    updatedAt: Long? = null
): String {
    val parts = mutableListOf<String>()

    // Format release date if provided
    if (releaseDate != null) {
        val releaseDateStr = if (releaseDate.isNotEmpty()) {
            try {
                // Parse "YYYY-MM-DD" format and convert to "MMM dd, yyyy"
                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.US)
                val date = inputFormat.parse(releaseDate)
                if (date != null) "Released: ${outputFormat.format(date)}" else releaseDate
            } catch (e: Exception) {
                releaseDate // Return as-is if parsing fails
            }
        } else {
            "TBA"
        }
        parts.add(releaseDateStr)
    }
    
    // Show "Updated" if game was modified after being added, otherwise show "Added"
    if (updatedAt != null && updatedAt > 0 && addedAt != null && updatedAt > addedAt) {
        val date = java.util.Date(updatedAt)
        val format = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.US)
        parts.add("Updated: ${format.format(date)}")
    } else if (addedAt != null && addedAt > 0) {
        val date = java.util.Date(addedAt)
        val format = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.US)
        parts.add("Added: ${format.format(date)}")
    }

    return parts.joinToString(" | ")
}

// Preview Composables
@Preview(showBackground = true, backgroundColor = 0xFF0A1929)
@Composable
private fun ListGameCardPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(16.dp)
    ) {
        ListGameCard(
            game = GameListEntry(
                id = "1",
                rawgId = 3328,
                name = "The Witcher 3: Wild Hunt",
                backgroundImage = "https://media.rawg.io/media/games/618/618c2031a07bbff6b4f611f10b6bcdbc.jpg",
                genres = listOf("Action", "RPG"),
                platforms = listOf("PC", "PlayStation", "Xbox"),
                status = GameStatus.FINISHED,
                rating = 9.5f,
                hoursPlayed = 50,
                aspects = listOf("Great Story", "Amazing Gameplay")
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A1929)
@Composable
private fun ListGameCardPlayingPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(16.dp)
    ) {
        ListGameCard(
            game = GameListEntry(
                id = "2",
                rawgId = 3498,
                name = "Grand Theft Auto V",
                genres = listOf("Action", "Adventure"),
                platforms = listOf("PC", "PlayStation"),
                status = GameStatus.PLAYING,
                rating = 7.5f,
                hoursPlayed = 20
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A1929)
@Composable
private fun ListGameCardNoRatingPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(16.dp)
    ) {
        ListGameCard(
            game = GameListEntry(
                id = "3",
                rawgId = 1234,
                name = "Cyberpunk 2077",
                genres = listOf("RPG", "Action"),
                platforms = listOf("PC"),
                status = GameStatus.WANT,
                rating = null
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A1929, name = "Dropped Game - Low Rating")
@Composable
private fun ListGameCardDroppedPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(16.dp)
    ) {
        ListGameCard(
            game = GameListEntry(
                id = "4",
                rawgId = 5679,
                name = "Fallout 76",
                genres = listOf("Action", "Multiplayer", "RPG"),
                platforms = listOf("PC", "PlayStation", "Xbox"),
                status = GameStatus.DROPPED,
                rating = 3.5f,
                hoursPlayed = 5
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A1929, name = "On Hold - Good Rating")
@Composable
private fun ListGameCardOnHoldPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(16.dp)
    ) {
        ListGameCard(
            game = GameListEntry(
                id = "5",
                rawgId = 4200,
                name = "Portal 2",
                genres = listOf("Puzzle", "Platformer"),
                platforms = listOf("PC", "Mac", "Linux"),
                status = GameStatus.ON_HOLD,
                rating = 8.5f,
                hoursPlayed = 15
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A1929, name = "Long Title Test")
@Composable
private fun ListGameCardLongTitlePreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(16.dp)
    ) {
        ListGameCard(
            game = GameListEntry(
                id = "6",
                rawgId = 2454,
                name = "The Legend of Heroes: Trails of Cold Steel IV - The End of Saga",
                genres = listOf("JRPG", "Turn-Based Combat", "Story-Rich"),
                platforms = listOf("PlayStation", "Nintendo Switch", "PC"),
                status = GameStatus.PLAYING,
                rating = 9.0f,
                hoursPlayed = 80
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A1929, name = "All Status List", heightDp = 900)
@Composable
private fun ListGameCardAllStatusesPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Finished - Masterpiece
        ListGameCard(
            game = GameListEntry(
                id = "1",
                name = "Elden Ring",
                genres = listOf("Action RPG", "Souls-like"),
                platforms = listOf("PC", "PlayStation", "Xbox"),
                status = GameStatus.FINISHED,
                rating = 9.8f
            )
        )

        // Playing - Great
        ListGameCard(
            game = GameListEntry(
                id = "2",
                name = "Baldur's Gate 3",
                genres = listOf("RPG", "Turn-Based"),
                platforms = listOf("PC", "PlayStation"),
                status = GameStatus.PLAYING,
                rating = 8.2f
            )
        )

        // Dropped - Poor
        ListGameCard(
            game = GameListEntry(
                id = "3",
                name = "Anthem",
                genres = listOf("Action", "Multiplayer"),
                platforms = listOf("PC", "Xbox"),
                status = GameStatus.DROPPED,
                rating = 2.5f
            )
        )

        // On Hold - Good
        ListGameCard(
            game = GameListEntry(
                id = "4",
                name = "Death Stranding",
                genres = listOf("Action", "Adventure"),
                platforms = listOf("PC"),
                status = GameStatus.ON_HOLD,
                rating = 6.5f
            )
        )

        // Want - Not Rated
        ListGameCard(
            game = GameListEntry(
                id = "5",
                name = "Hades II",
                genres = listOf("Roguelike", "Action"),
                platforms = listOf("PC"),
                status = GameStatus.WANT,
                rating = null
            )
        )
    }
}
