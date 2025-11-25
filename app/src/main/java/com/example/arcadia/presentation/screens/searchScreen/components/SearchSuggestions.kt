package com.example.arcadia.presentation.screens.searchScreen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.arcadia.domain.model.Game
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent
import com.example.arcadia.util.RequestState


/**
 * Search suggestions screen shown when search is idle
 * Displays: Recent searches, Trending games, Personalized suggestions
 */
@Composable
fun SearchSuggestions(
    searchHistory: List<String>,
    trendingGames: RequestState<List<Game>>,
    personalizedSuggestions: List<String>,
    isAIMode: Boolean,
    onHistoryItemClick: (String) -> Unit,
    onHistoryItemRemove: (String) -> Unit,
    onClearHistory: () -> Unit,
    onTrendingGameClick: (Int) -> Unit,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        // Recent Searches Section
        AnimatedVisibility(
            visible = searchHistory.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            RecentSearchesSection(
                history = searchHistory,
                onItemClick = onHistoryItemClick,
                onItemRemove = onHistoryItemRemove,
                onClearAll = onClearHistory
            )
        }
        
        // Personalized Suggestions (based on user's library genres)
        AnimatedVisibility(
            visible = personalizedSuggestions.isNotEmpty() && isAIMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PersonalizedSuggestionsSection(
                suggestions = personalizedSuggestions,
                onSuggestionClick = onSuggestionClick
            )
        }
        
        // Trending Games Section
        TrendingGamesSection(
            trendingGames = trendingGames,
            onGameClick = onTrendingGameClick
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RecentSearchesSection(
    history: List<String>,
    onItemClick: (String) -> Unit,
    onItemRemove: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Recent Searches",
                    color = TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            TextButton(onClick = onClearAll) {
                Text(
                    text = "Clear",
                    color = ButtonPrimary,
                    fontSize = 14.sp
                )
            }
        }
        
        // History items
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            history.forEach { query ->
                HistoryItem(
                    query = query,
                    onClick = { onItemClick(query) },
                    onRemove = { onItemRemove(query) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HistoryItem(
    query: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = query,
                color = TextSecondary,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = TextSecondary.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun PersonalizedSuggestionsSection(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = YellowAccent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "For You",
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        // Suggestion chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(suggestions) { suggestion ->
                SuggestionChip(
                    text = suggestion,
                    onClick = { onSuggestionClick(suggestion) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = ButtonPrimary.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            color = ButtonPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrendingGamesSection(
    trendingGames: RequestState<List<Game>>,
    onGameClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.LocalFireDepartment,
                contentDescription = null,
                tint = Color(0xFFFF6B35),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Trending Now",
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        when (trendingGames) {
            is RequestState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(color = ButtonPrimary)
                }
            }
            is RequestState.Success -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trendingGames.data) { game ->
                        TrendingGameCard(
                            game = game,
                            onClick = { onGameClick(game.id) }
                        )
                    }
                }
            }
            is RequestState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Couldn't load trending games",
                        color = TextSecondary.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun TrendingGameCard(
    game: Game,
    onClick: () -> Unit
) {
    val context = LocalPlatformContext.current
    val density = LocalDensity.current
    
    Card(
        modifier = Modifier
            .width(130.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1B41)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Game cover image
            val imageSizePx = with(density) { 130.dp.roundToPx() }
            
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
                        Text("🎮", fontSize = 24.sp)
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
            
            // Gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
            
            // Game info at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = game.name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                
                if (game.rating > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "⭐",
                            fontSize = 10.sp
                        )
                        Text(
                            text = String.format("%.1f", game.rating),
                            color = YellowAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
