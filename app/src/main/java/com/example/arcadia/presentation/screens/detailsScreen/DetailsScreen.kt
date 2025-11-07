package com.example.arcadia.presentation.screens.detailsScreen

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.example.arcadia.domain.model.Game
import com.example.arcadia.presentation.componenets.PrimaryButton
import com.example.arcadia.presentation.componenets.VideoPlayerWithLoading
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.util.RequestState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    gameId: Int,
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: DetailsScreenViewModel = koinViewModel()

    LaunchedEffect(gameId) {
        viewModel.loadGameDetails(gameId)
    }

    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Game Details",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ButtonPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Surface
    ) { paddingValues ->
        when (val state = uiState.gameState) {
            is RequestState.Loading -> LoadingState(modifier = Modifier.padding(paddingValues))
            is RequestState.Success -> GameDetailsContent(
                game = state.data,
                isInLibrary = uiState.isInLibrary,
                addToLibraryInProgress = uiState.addToLibraryInProgress,
                onAddToLibrary = { viewModel.addToLibrary() },
                modifier = Modifier.padding(paddingValues)
            )
            is RequestState.Error -> ErrorState(message = state.message, onRetry = { viewModel.retry() }, modifier = Modifier.padding(paddingValues))
            else -> {}
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = ButtonPrimary)
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "⚠️", fontSize = 48.sp)
        Text(
            text = "Failed to load game details",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = message,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
            Text(text = "Retry", color = ButtonPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GameDetailsContent(
    game: Game,
    isInLibrary: Boolean,
    addToLibraryInProgress: Boolean,
    onAddToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        // Original background header with game image
        GameHeaderSection(game = game)

        GameStatsSection(game = game)

        // Media carousel with trailer and screenshots
        Spacer(modifier = Modifier.height(16.dp))
        MediaCarouselSection(game = game)

        // Primary action - Add to Library only
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = if (isInLibrary) "Game is already in your library" else "Add to Library",
                enabled = !isInLibrary && !addToLibraryInProgress,
                onClick = onAddToLibrary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        GameDescriptionSection(game = game)
        Spacer(modifier = Modifier.height(24.dp))
        UserRatingSection()
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MediaCarouselSection(game: Game) {
    val mediaItems = buildList {
        // Add trailer as first item if available
        game.trailerUrl?.let { add(MediaItem.Video(it)) }
        // Add screenshots
        game.screenshots.forEach { add(MediaItem.Screenshot(it)) }
    }

    if (mediaItems.isNotEmpty()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = if (game.trailerUrl != null) "Trailer & Screenshots" else "Screenshots",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(mediaItems.size) { index ->
                val item = mediaItems[index]
                Box(
                    modifier = Modifier
                        .width(if (index == 0 && item is MediaItem.Video) 350.dp else 300.dp)
                        .height(200.dp)
                        .padding(start = if (index == 0) 16.dp else 0.dp, end = if (index == mediaItems.size - 1) 16.dp else 0.dp)
                ) {
                    when (item) {
                        is MediaItem.Video -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(Color(0xFF1E2A47))
                            ) {
                                VideoPlayerWithLoading(
                                    videoUrl = item.url,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        is MediaItem.Screenshot -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(Color(0xFF1E2A47)),
                                contentAlignment = Alignment.Center
                            ) {
                                SubcomposeAsyncImage(
                                    model = item.url,
                                    contentDescription = "Game screenshot",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.medium),
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = ButtonPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    error = {
                                        Text("🎮", fontSize = 32.sp)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sealed class to represent media items
sealed class MediaItem {
    data class Video(val url: String) : MediaItem()
    data class Screenshot(val url: String) : MediaItem()
}

@Composable
fun GameHeaderSection(game: Game) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Load game background image from API or use placeholder
        if (game.backgroundImage != null) {
            SubcomposeAsyncImage(
                model = game.backgroundImage,
                contentDescription = "Game background",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E2A47)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ButtonPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF1E2A47),
                                        Color(0xFF2D3E5F)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = game.name,
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E2A47),
                                Color(0xFF2D3E5F)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = game.name,
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Surface
                        ),
                        startY = 400f,
                        endY = 600f
                    )
                )
        )

        // Game Title at Bottom
        Text(
            text = game.name,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )
    }
}

@Composable
fun GameStatsSection(game: Game) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Rating
        StatItem(
            value = game.rating.toString(),
            label = "Rating",
            icon = Icons.Filled.Star,
            color = Color(0xFFFFD700)
        )

        // Rating Top
        StatItem(
            value = game.ratingTop.toString(),
            label = "Max Rating",
            icon = null,
            color = Color(0xFF62B4DA)
        )

        // Reviews
        StatItem(
            value = formatCount(game.ratingsCount),
            label = "Reviews",
            icon = null,
            color = Color(0xFF62B4DA)
        )

        // Playtime
        StatItem(
            value = "${game.playtime}h",
            label = "Playtime",
            icon = null,
            color = Color(0xFF62B4DA)
        )
    }
}

// Helper function to format large numbers
private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

@Composable
fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = value,
                color = color,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun UserRatingSection() {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(Color(0xFF1E2A47), MaterialTheme.shapes.medium)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎮",
            fontSize = 48.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Your Rating & Status",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Feature Coming Soon",
            color = ButtonPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Rate games, track your progress, and share your reviews",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GameDescriptionSection(game: Game) {
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

        // Description
        game.description?.let { description ->
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }

        // Genres
        if (game.genres.isNotEmpty()) {
            Text(
                text = "Genres: ${game.genres.joinToString(", ")}",
                color = ButtonPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Platforms
        if (game.platforms.isNotEmpty()) {
            Text(
                text = "Platforms: ${game.platforms.joinToString(", ")}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Tags
        if (game.tags.isNotEmpty()) {
            Text(
                text = "Tags: ${game.tags.joinToString(", ")}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Additional info
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


