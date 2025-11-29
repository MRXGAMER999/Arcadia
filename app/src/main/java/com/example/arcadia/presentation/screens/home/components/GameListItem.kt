package com.example.arcadia.presentation.screens.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import com.example.arcadia.domain.model.Game

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameListItem(
    game: Game,
    modifier: Modifier = Modifier,
    isInLibrary: Boolean = false,
    onClick: () -> Unit = {},
    onAddToLibrary: () -> Unit = {}
) {
    val context = LocalPlatformContext.current
    val density = LocalDensity.current
    val imageSizePx = with(density) { 88.dp.roundToPx() }
    
    // Expandable state - collapsed by default
    var isExpanded by remember { mutableStateOf(false) }
    
    // Determine the AI badge type based on game metadata
    val badgeType = remember(game.aiConfidence, game.aiTier, game.aiReason, game.metacritic, game.rating) {
        if (game.aiConfidence != null || game.aiTier != null) {
            determineBadgeType(
                confidence = game.aiConfidence,
                tier = game.aiTier,
                reason = game.aiReason,
                metacritic = game.metacritic,
                rating = game.rating
            )
        } else null
    }
    
    // Check if this is an AI recommendation
    val isAIRecommendation = badgeType != null
    val hasExpandableContent = isAIRecommendation && game.aiReason != null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isAIRecommendation) Modifier.height(IntrinsicSize.Min) else Modifier.height(140.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1B41)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .animateContentSize(animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f))
                .padding(12.dp)
        ) {
            // Main Row: Image + Info + Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isAIRecommendation) 100.dp else 116.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Game Image
                Card(
                    modifier = Modifier
                        .width(90.dp)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
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
                                        .background(Color(0xFF252B3B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingIndicator(color = Color(0xFF62B4DA))
                                }
                            },
                            error = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF252B3B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.StarOutline,
                                        contentDescription = null,
                                        tint = Color(0xFF62B4DA).copy(alpha = 0.5f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                // Game Info Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top: Title
                    Text(
                        text = game.name,
                        color = Color(0xFFF0F0F0),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    
                    // Middle: Genres
                    if (game.genres.isNotEmpty()) {
                        Text(
                            text = game.genres.take(2).joinToString(" • "),
                            color = Color(0xFF8E99A8).copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Bottom: Metadata row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Year
                        Text(
                            text = game.released?.take(4) ?: "TBA",
                            color = Color(0xFF8E99A8),
                            fontSize = 12.sp
                        )
                        
                        // Rating
                        if (game.rating >= 3.5) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = String.format("%.1f", game.rating),
                                    color = Color(0xFFFFD700),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Metacritic
                        if (game.metacritic != null && game.metacritic > 0) {
                            val metacriticColor = when {
                                game.metacritic >= 75 -> Color(0xFF66BB6A)
                                game.metacritic >= 50 -> Color(0xFFFFCA28)
                                else -> Color(0xFFEF5350)
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = metacriticColor.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${game.metacritic}",
                                    color = metacriticColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                // Add Button - Aligned to bottom
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    val scale by animateFloatAsState(
                        targetValue = if (isInLibrary) 1.05f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                        label = "button_scale"
                    )
                    val rotation by animateFloatAsState(
                        targetValue = if (isInLibrary) 360f else 0f,
                        animationSpec = tween(durationMillis = 400),
                        label = "button_rotation"
                    )
                    val buttonColor by animateColorAsState(
                        targetValue = if (isInLibrary) Color(0xFF66BB6A) else Color(0xFF62B4DA),
                        animationSpec = tween(durationMillis = 300),
                        label = "button_color"
                    )
                    
                    IconButton(
                        onClick = onAddToLibrary,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = buttonColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                rotationZ = rotation
                            },
                        enabled = !isInLibrary
                    ) {
                        Icon(
                            imageVector = if (isInLibrary) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = if (isInLibrary) "In library" else "Add to library",
                            tint = buttonColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // AI Section - Always visible for AI recommendations
            if (isAIRecommendation) {
                Spacer(modifier = Modifier.height(10.dp))
                
                // Badges Row - Always visible
                if (game.aiBadges.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        game.aiBadges.take(3).forEach { badge ->
                            CompactAIBadge(text = badge)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Reasoning Preview + Expand Button
                if (game.aiReason != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF252B3B))
                            .clickable { isExpanded = !isExpanded }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // AI Icon
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF62B4DA),
                            modifier = Modifier.size(14.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Reasoning text - 1 line when collapsed, full when expanded
                        Text(
                            text = game.aiReason,
                            color = Color(0xFFB0BEC5),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Expand indicator
                        val expandRotation by animateFloatAsState(
                            targetValue = if (isExpanded) 180f else 0f,
                            animationSpec = tween(durationMillis = 200),
                            label = "expand_rotation"
                        )
                        
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = Color(0xFF62B4DA).copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(18.dp)
                                .graphicsLayer { rotationZ = expandRotation }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact AI badge with professional gradient styling.
 * These are meaningful, varied badges created by the AI based on the recommendation context.
 */
@Composable
private fun CompactAIBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    // Assign colors based on badge content for visual variety
    val (primaryColor, secondaryColor) = remember(text) {
        when {
            text.contains("Metacritic", ignoreCase = true) || text.contains("Acclaimed", ignoreCase = true) -> Color(0xFFFFD700) to Color(0xFFFF8F00)
            text.contains("Devotee", ignoreCase = true) || text.contains("Fan", ignoreCase = true) || text.contains("Love", ignoreCase = true) -> Color(0xFFFF6B9D) to Color(0xFFE91E63)
            text.contains("Hidden", ignoreCase = true) || text.contains("Gem", ignoreCase = true) || text.contains("Underrated", ignoreCase = true) -> Color(0xFFCE93D8) to Color(0xFFAB47BC)
            text.contains("Classic", ignoreCase = true) || text.contains("Legendary", ignoreCase = true) || text.contains("Timeless", ignoreCase = true) -> Color(0xFFFFB74D) to Color(0xFFFFA726)
            text.contains("Indie", ignoreCase = true) -> Color(0xFF81C784) to Color(0xFF66BB6A)
            text.contains("Story", ignoreCase = true) || text.contains("Narrative", ignoreCase = true) -> Color(0xFF64B5F6) to Color(0xFF42A5F5)
            text.contains("Challenge", ignoreCase = true) || text.contains("Hard", ignoreCase = true) || text.contains("Difficult", ignoreCase = true) -> Color(0xFFEF5350) to Color(0xFFE53935)
            text.contains("Cozy", ignoreCase = true) || text.contains("Relaxing", ignoreCase = true) || text.contains("Chill", ignoreCase = true) -> Color(0xFF80DEEA) to Color(0xFF4DD0E1)
            text.contains("Co-op", ignoreCase = true) || text.contains("Multiplayer", ignoreCase = true) || text.contains("Social", ignoreCase = true) -> Color(0xFFBA68C8) to Color(0xFFAB47BC)
            text.contains("Open World", ignoreCase = true) || text.contains("Explore", ignoreCase = true) || text.contains("Adventure", ignoreCase = true) -> Color(0xFF4DB6AC) to Color(0xFF26A69A)
            text.contains("Director", ignoreCase = true) || text.contains("Studio", ignoreCase = true) || text.contains("Developer", ignoreCase = true) -> Color(0xFFFFAB91) to Color(0xFFFF8A65)
            text.contains("Award", ignoreCase = true) || text.contains("GOTY", ignoreCase = true) || text.contains("Winner", ignoreCase = true) -> Color(0xFFFFE082) to Color(0xFFFFD54F)
            text.contains("Sequel", ignoreCase = true) || text.contains("Series", ignoreCase = true) || text.contains("Franchise", ignoreCase = true) -> Color(0xFF9FA8DA) to Color(0xFF7986CB)
            text.contains("RPG", ignoreCase = true) || text.contains("Action", ignoreCase = true) -> Color(0xFFB39DDB) to Color(0xFF9575CD)
            text.contains("Strategy", ignoreCase = true) || text.contains("Tactical", ignoreCase = true) -> Color(0xFF90CAF9) to Color(0xFF64B5F6)
            text.contains("Horror", ignoreCase = true) || text.contains("Scary", ignoreCase = true) -> Color(0xFFEF9A9A) to Color(0xFFE57373)
            else -> Color(0xFF81D4FA) to Color(0xFF4FC3F7)
        }
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.2f),
                        secondaryColor.copy(alpha = 0.1f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.4f),
                        secondaryColor.copy(alpha = 0.25f)
                    )
                ),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = primaryColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
