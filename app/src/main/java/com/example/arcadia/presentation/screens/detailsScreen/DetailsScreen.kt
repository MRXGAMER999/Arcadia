package com.example.arcadia.presentation.screens.detailsScreen

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.Dialog // ← ADD THIS LINE
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.R
import com.example.arcadia.domain.model.Game
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.util.RequestState
import org.koin.androidx.compose.koinViewModel

// List of Hollow Knight screenshots
private val hollowKnightScreenshots = listOf(
    R.drawable.hollow_knight_screen1,
    R.drawable.hollow_knight_screen2,
    R.drawable.hollow_knight_screen3,
    R.drawable.hollow_knight_screen4
)

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

    val gameState = viewModel.gameState

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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFF00123B)
    ) { paddingValues ->
        when (val state = gameState) {
            is RequestState.Loading -> LoadingState(modifier = Modifier.padding(paddingValues))
            is RequestState.Success -> GameDetailsContent(game = state.data, modifier = Modifier.padding(paddingValues))
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
fun GameDetailsContent(game: Game, modifier: Modifier = Modifier) {
    var selectedScreenshot by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        GameHeaderSection(game = game)
        GameStatsSection(game = game)
        Spacer(modifier = Modifier.height(24.dp))
        UserRatingSection()
        Spacer(modifier = Modifier.height(24.dp))
        GameDescriptionSection()
        Spacer(modifier = Modifier.height(24.dp))
        ScreenshotsSection(
            onScreenshotClick = { screenshotResId ->
                selectedScreenshot = screenshotResId
            }
        )
        Spacer(modifier = Modifier.height(32.dp))
    }

    // Show popup if a screenshot is selected
    if (selectedScreenshot != null) {
        ScreenshotPopup(
            screenshotResId = selectedScreenshot!!,
            onDismiss = { selectedScreenshot = null }
        )
    }
}

@Composable
fun GameHeaderSection(game: Game) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        // Load from local resources
        Image(
            painter = painterResource(R.drawable.hollow_knight_background),
            contentDescription = "Game background",
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00123B)
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

        // Popularity (mocked)
        StatItem(
            value = "1.9K",
            label = "Popularity",
            icon = null,
            color = Color(0xFF62B4DA)
        )

        // Reviews (mocked)
        StatItem(
            value = "276",
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
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Your rating",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rating number
            Text(
                text = "10",
                color = Color(0xFF62B4DA),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Status
            Box(
                modifier = Modifier
                    .background(Color(0xFF62B4DA), MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Finished",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats row (Finished, Playing, Want, Dropped)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            UserStatItem(count = "1.3K", label = "Finished")
            UserStatItem(count = "100", label = "Playing")
            UserStatItem(count = "171", label = "Want")
            UserStatItem(count = "139", label = "Dropped")
        }
    }
}

@Composable
fun UserStatItem(count: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

@Composable
fun GameDescriptionSection() {
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

        Text(
            text = "Embark on the craziest journey of your life in Hollow Knight, a genre-bending platform adventure. Explore twisting caverns, battle tainted creatures, and befriend strange bugs, all in a classic, hand-drawn 2D style.",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Additional info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Developers:",
                    color = Color(0xFF62B4DA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Team Cherry",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }

            Column {
                Text(
                    text = "Publishers:",
                    color = Color(0xFF62B4DA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Team Cherry",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ScreenshotsSection(
    onScreenshotClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Screenshots",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(hollowKnightScreenshots) { screenshotResId ->
                Box(
                    modifier = Modifier
                        .size(200.dp, 120.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { onScreenshotClick(screenshotResId) }
                        .background(Color(0xFF1E2A47)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(screenshotResId),
                        contentDescription = "Game screenshot",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotPopup(
    screenshotResId: Int,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 13f)
                .clip(MaterialTheme.shapes.large)
                .background(Color(0xFF1E2A47)),
            contentAlignment = Alignment.Center
        ) {
            // Close button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            // Screenshot image
            Image(
                painter = painterResource(screenshotResId),
                contentDescription = "Game screenshot",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

private fun createMockGame(id: Int): Game {
    return Game(
        id = id,
        slug = "hollow-knight",
        name = "Hollow Knight",
        released = "2017-02-24",
        backgroundImage = null,
        rating = 8.9,
        metacritic = 87,
        playtime = 25,
        platforms = listOf("PC", "Nintendo Switch", "PlayStation 4", "Xbox One", "macOS", "Linux"),
        genres = listOf("Action-Adventure", "Metroidvania", "Platformer"),
        tags = listOf("2D", "Dark Fantasy", "Singleplayer", "Atmospheric", "Difficult", "Great Soundtrack")
    )
}