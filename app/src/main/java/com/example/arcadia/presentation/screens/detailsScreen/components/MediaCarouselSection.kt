package com.example.arcadia.presentation.screens.detailsScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import com.example.arcadia.domain.model.Game
import com.example.arcadia.presentation.components.FullscreenImageViewer
import com.example.arcadia.presentation.components.VideoPlayerWithLoading
import com.example.arcadia.ui.theme.ButtonPrimary

sealed class MediaItem {
    data class Video(val url: String) : MediaItem()
    data class Screenshot(val url: String) : MediaItem()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaCarouselSection(game: Game) {
    val context = LocalPlatformContext.current
    val mediaItems = buildList {
        game.trailerUrl?.let { add(MediaItem.Video(it)) }
        game.screenshots.forEach { add(MediaItem.Screenshot(it)) }
    }
    
    var showFullscreenViewer by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }

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
                        .padding(
                            start = if (index == 0) 16.dp else 0.dp,
                            end = if (index == mediaItems.size - 1) 16.dp else 0.dp
                        )
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
                            ScreenshotItem(
                                url = item.url,
                                context = context,
                                onClick = {
                                    // Calculate the screenshot index (excluding video)
                                    val screenshotIndex = if (game.trailerUrl != null) index - 1 else index
                                    selectedImageIndex = screenshotIndex
                                    showFullscreenViewer = true
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Fullscreen Image Viewer
        if (showFullscreenViewer && game.screenshots.isNotEmpty()) {
            FullscreenImageViewer(
                images = game.screenshots,
                initialPage = selectedImageIndex,
                onDismiss = { showFullscreenViewer = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScreenshotItem(
    url: String,
    context: coil3.PlatformContext,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.medium)
            .background(Color(0xFF1E2A47))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .size(600, 400) // Use reasonable thumbnail size for better quality
                .scale(Scale.FIT)
                .memoryCacheKey("thumb_$url")
                .diskCacheKey("thumb_$url")
                .crossfade(true)
                .build(),
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
                    LoadingIndicator(
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
