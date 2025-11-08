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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.util.DisplayResult


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
        Scaffold(
            containerColor = Surface,
            topBar = {

            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Surface)
            ) {
                // Search Field
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    placeholder = { Text("Search games...", color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ButtonPrimary,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
                        focusedContainerColor = Surface,
                        unfocusedContainerColor = Surface,
                        cursorColor = ButtonPrimary
                    ),
                    textStyle = TextStyle(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                                color = TextSecondary.copy(alpha = 0.6f),
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
                                color = ButtonPrimary
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
                                    color = TextSecondary.copy(alpha = 0.7f),
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
                                    color = TextSecondary.copy(alpha = 0.6f),
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            // Show results
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
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

                                    HorizontalDivider(
                                        color = Color.White.copy(alpha = 0.2f),
                                        thickness = 0.6.dp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }
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
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Game image
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
                            color = ButtonPrimary
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

        // Game info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = game.name,
                color = TextSecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = game.genres.take(2).joinToString(", "),
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = game.released?.take(4) ?: "TBA",
                color = TextSecondary.copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }

        // Toggle button with animation
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
            targetValue = if (isAdded) ButtonPrimary.copy(alpha = 0.3f) else Color.Transparent,
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
                tint = ButtonPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
