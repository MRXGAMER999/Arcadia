package com.example.arcadia.presentation.screens.myGames.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.ui.theme.YellowAccent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun MyGameCard(
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
    
    Column(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        // Game Image with Rating Badge
        Box {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                val imageSizePx = with(density) { 
                    150.dp.roundToPx() 
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
                                .background(Color(0xFF1E2A47)),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(
                                color = Color(0xFF62B4DA)
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1E2A47)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎮", fontSize = 32.sp)
                        }
                    }
                )
            }
            
            // Rating Badge - positioned outside the card in top right corner
            if (game.rating != null && game.rating > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp) // Offset outside the card bounds
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E2A47))
                        .wrapContentSize()
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = YellowAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = String.format("%.1f", game.rating),
                            color = YellowAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 3.dp)
                        )
                    }
                }
            }
            
            // Edit icon button - Bottom left corner, positioned outside the card
            if (onEditClick != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = (-8).dp, y = 8.dp) // Offset outside the card bounds
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2A47))
                ){
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit game",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
        
        // Game Info
        Column(
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                text = game.name,
                color = Color(0xFFDCDCDC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Display appropriate date based on settings
            val dateText = formatDateDisplay(
                releaseDate = if (showReleaseDate) game.releaseDate else null,
                addedAt = if (showDateAdded) game.addedAt else null,
                updatedAt = if (showDateAdded) game.updatedAt else null
            )
            if (dateText.isNotEmpty()) {
                Text(
                    text = dateText,
                    color = Color(0xFF9CA3AF),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Format date display for grid view
 * Shows release date OR added/updated date based on settings
 */
private fun formatDateDisplay(
    releaseDate: String?,
    addedAt: Long?,
    updatedAt: Long?
): String {
    // Prefer release date if requested
    if (releaseDate != null && releaseDate.isNotEmpty()) {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val date = inputFormat.parse(releaseDate)
            if (date != null) outputFormat.format(date) else releaseDate
        } catch (e: Exception) {
            releaseDate
        }
    }
    
    // Show "Updated" if game was modified after being added
    if (updatedAt != null && updatedAt > 0 && addedAt != null && updatedAt > addedAt) {
        val date = Date(updatedAt)
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return "Updated: ${formatter.format(date)}"
    }
    
    // Show added date
    if (addedAt != null && addedAt > 0) {
        val date = Date(addedAt)
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(date)
    }
    
    return ""
}
