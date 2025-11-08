package com.example.arcadia.presentation.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.arcadia.domain.model.Game
import kotlinx.coroutines.delay
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
    var visible by remember { mutableStateOf(true) }

    // Trigger hide animation when game is added to library
    LaunchedEffect(isInLibrary) {
        if (isInLibrary) {
            delay(300) // Short delay to show the check icon briefly
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(animationSpec = tween(durationMillis = 300)) +
               shrinkVertically(animationSpec = tween(durationMillis = 300))
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Game Image
            Card(
                modifier = Modifier
                    .width(120.dp)
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(game.backgroundImage ?: "")
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
                            Text("🎮", fontSize = 48.sp)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Game Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp)
            ) {
                Text(
                    text = game.name,
                    color = Color(0xFFDCDCDC),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = game.genres.take(2).joinToString(", "),
                    color = Color(0xFFDCDCDC).copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Release date and rating
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Release",
                        tint = Color(0xFF62B4DA),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = game.released?.take(10) ?: "TBA",
                        color = Color(0xFFDCDCDC).copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    Text(
                        text = " • ",
                        color = Color(0xFFDCDCDC).copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )

                    Text(
                        text = "${game.playtime}h",
                        color = Color(0xFFDCDCDC).copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Animated Add/Check Button
                    val scale by animateFloatAsState(
                        targetValue = if (isInLibrary) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.5f,
                            stiffness = 300f
                        ),
                        label = "button_scale"
                    )

                    val rotation by animateFloatAsState(
                        targetValue = if (isInLibrary) 360f else 0f,
                        animationSpec = tween(durationMillis = 400),
                        label = "button_rotation"
                    )

                    val backgroundColor by animateColorAsState(
                        targetValue = if (isInLibrary) Color(0xFF4CAF50).copy(alpha = 0.3f) else Color.Transparent,
                        animationSpec = tween(durationMillis = 300),
                        label = "button_background"
                    )

                    IconButton(
                        onClick = onAddToLibrary,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = backgroundColor,
                                shape = CircleShape
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
                            tint = if (isInLibrary) Color(0xFF4CAF50) else Color(0xFF62B4DA),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}


