package com.example.arcadia.presentation.screens.detailsScreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.arcadia.domain.model.Game
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GameHeaderSection(game: Game) {
    val context = LocalPlatformContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        if (game.backgroundImage != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(game.backgroundImage)
                    .memoryCacheKey(game.backgroundImage)
                    .diskCacheKey(game.backgroundImage)
                    .crossfade(true)
                    .build(),
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
                        LoadingIndicator(
                            color = ButtonPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                },
                error = {
                    GameHeaderPlaceholder(gameName = game.name)
                }
            )
        } else {
            GameHeaderPlaceholder(gameName = game.name)
        }

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
private fun GameHeaderPlaceholder(gameName: String) {
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
            text = gameName,
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}
