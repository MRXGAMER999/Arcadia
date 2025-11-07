package com.example.arcadia.presentation.screens.searchScreen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.example.arcadia.domain.model.Game
import com.example.arcadia.presentation.components.TopNotification
import com.example.arcadia.util.DisplayResult
import com.example.arcadia.util.RequestState

// 🎨 Colors
val Background = Color(0xFF00123B)
val FieldTxt = Color(0xFFDCDCDC)
val FieldBg = Color(0xFF00123B)
val ButtonBlue = Color(0xFF62B4DA)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: SearchViewModel = org.koin.androidx.compose.koinViewModel()
) {
    val state = viewModel.screenState
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(16.dp)
        ) {

            // 🔙 Back + Search Field Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { onBackClick?.invoke() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = FieldTxt
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    placeholder = { Text("Search games...", color = FieldTxt.copy(alpha = 0.5f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(50.dp), // Rounded pill shape
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ButtonBlue,
                        unfocusedBorderColor = FieldTxt.copy(alpha = 0.4f),
                        focusedContainerColor = FieldBg,
                        unfocusedContainerColor = FieldBg,
                        cursorColor = ButtonBlue
                    ),
                    textStyle = TextStyle(color = FieldTxt)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display search results based on state
            state.results.DisplayResult(
                modifier = Modifier.fillMaxSize(),
                onIdle = {
                    // Show empty state when no search has been performed
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Type to search for games",
                            color = FieldTxt.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        )
                    }
                },
                onLoading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator(
                            color = ButtonBlue
                        )
                    }
                },
                onError = { errorMessage ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Error",
                                color = Color(0xFFE57373),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = errorMessage,
                                color = FieldTxt.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                onSuccess = { games ->
                    if (games.isEmpty()) {
                        // No results found
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No games found",
                                color = FieldTxt.copy(alpha = 0.6f),
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        // Show results
                        LazyColumn {
                            items(games) { game ->
                                SearchResultCard(
                                    game = game,
                                    isAdded = viewModel.isGameInLibrary(game.id),
                                    onToggle = {
                                        viewModel.toggleGameInLibrary(
                                            game = game,
                                            onSuccess = { 
                                                notificationMessage = "${game.name} added to My Games"
                                                isSuccess = true
                                                showNotification = true
                                            },
                                            onError = { error ->
                                                notificationMessage = error
                                                isSuccess = false
                                                showNotification = true
                                            }
                                        )
                                    }
                                )

                                Divider(
                                    color = Color.White.copy(alpha = 0.2f),
                                    thickness = 0.6.dp,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                        }
                    }
                }
            )
        }

        // Top notification banner
        TopNotification(
            visible = showNotification,
            message = notificationMessage,
            isSuccess = isSuccess,
            onDismiss = { showNotification = false },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun SearchResultCard(
    game: Game,
    isAdded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 🎨 Game image
        Card(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(10.dp)
        ) {
            SubcomposeAsyncImage(
                model = game.backgroundImage ?: "",
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
                            color = ButtonBlue
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
                        Text("🎮", fontSize = 24.sp)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 📝 Game info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = game.name,
                color = FieldTxt,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = game.genres.take(2).joinToString(", "),
                color = FieldTxt.copy(alpha = 0.6f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = game.released?.take(4) ?: "TBA",
                color = FieldTxt.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }

        // ➕ / ✅ Toggle button with animation
        val scale by animateFloatAsState(
            targetValue = if (isAdded) 1.1f else 1f,
            animationSpec = spring(
                dampingRatio = 0.5f,
                stiffness = 300f
            ),
            label = "scale"
        )

        val rotation by animateFloatAsState(
            targetValue = if (isAdded) 360f else 0f,
            animationSpec = tween(durationMillis = 400),
            label = "rotation"
        )

        val backgroundColor by animateColorAsState(
            targetValue = if (isAdded) ButtonBlue.copy(alpha = 0.3f) else Color.Transparent,
            animationSpec = tween(durationMillis = 300),
            label = "backgroundColor"
        )

        IconButton(
            onClick = { onToggle() },
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
                }
        ) {
            Icon(
                imageVector = if (isAdded) Icons.Default.Check else Icons.Default.Add,
                contentDescription = "Add or Remove",
                tint = ButtonBlue,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
