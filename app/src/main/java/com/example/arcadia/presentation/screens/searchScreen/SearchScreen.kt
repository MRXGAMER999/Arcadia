package com.example.arcadia.presentation.screens.searchScreen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.data.remote.mapper.toGameListEntry
import com.example.arcadia.presentation.components.AddGameSnackbar
import com.example.arcadia.presentation.components.game_rating.GameRatingSheet
import com.example.arcadia.presentation.components.TopNotification
import com.example.arcadia.presentation.screens.searchScreen.components.SearchField
import com.example.arcadia.presentation.screens.searchScreen.components.SearchResultCard
import com.example.arcadia.presentation.screens.searchScreen.components.SearchSuggestions
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent
import com.example.arcadia.util.DisplayResult
import com.example.arcadia.util.RequestState


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    onBackClick: (() -> Unit)? = null,
    onGameClick: (Int) -> Unit = {},
    viewModel: SearchViewModel = org.koin.androidx.compose.koinViewModel()
) {
    val state = viewModel.screenState
    val gamesInLibrary by viewModel.gamesInLibrary.collectAsState()
    val snackbarState by viewModel.snackbarState.collectAsState()
    val addGameSheetState by viewModel.addGameSheetState.collectAsState()
    val unsavedAddGameState by viewModel.unsavedAddGameState.collectAsState()
    
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = Surface,
            topBar = {}
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Surface)
            ) {
                // Search Field
                SearchField(
                    query = state.query,
                    onQueryChange = { viewModel.updateQuery(it) },
                    placeholder = if (state.isAIMode) 
                        "Describe what you're looking for..." 
                        else "Search games..."
                )

                // AI Mode Toggle - Cool Segmented Button
                AISearchToggle(
                    isAIMode = state.isAIMode,
                    onToggle = { viewModel.toggleAIMode() }
                )

                // AI Status & Progress (only in AI mode with active search)
                AnimatedVisibility(
                    visible = state.aiStatus != null,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it }
                ) {
                    AISearchStatus(
                        status = state.aiStatus ?: "",
                        progress = state.searchProgress
                    )
                }

                // AI Error Message (shown gracefully, not as error state)
                AnimatedVisibility(
                    visible = state.aiError != null && state.isAIMode,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    AIErrorCard(message = state.aiError ?: "")
                }

                // AI Reasoning Card (when results are shown)
                AnimatedVisibility(
                    visible = state.aiReasoning != null && state.results is RequestState.Success,
                    enter = fadeIn() + slideInVertically { -it / 2 },
                    exit = fadeOut()
                ) {
                    AIReasoningCard(reasoning = state.aiReasoning ?: "")
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Results
                state.results.DisplayResult(
                    modifier = Modifier.fillMaxSize(),
                    onIdle = {
                        if (state.aiError == null) {
                            // Show suggestions when idle (history, trending, personalized)
                            val hasContent = state.searchHistory.isNotEmpty() || 
                                           state.trendingGames is RequestState.Success ||
                                           state.personalizedSuggestions.isNotEmpty()
                            
                            if (hasContent) {
                                SearchSuggestions(
                                    searchHistory = state.searchHistory,
                                    trendingGames = state.trendingGames,
                                    personalizedSuggestions = state.personalizedSuggestions,
                                    isAIMode = state.isAIMode,
                                    onHistoryItemClick = { query -> viewModel.selectHistoryItem(query) },
                                    onHistoryItemRemove = { query -> viewModel.removeFromHistory(query) },
                                    onClearHistory = { viewModel.clearHistory() },
                                    onTrendingGameClick = onGameClick,
                                    onSuggestionClick = { suggestion -> viewModel.selectHistoryItem(suggestion) }
                                )
                            } else {
                                EmptySearchState(isAIMode = state.isAIMode)
                            }
                        }
                    },
                    onLoading = {
                        LoadingSearchState(isAIMode = state.isAIMode)
                    },
                    onError = { errorMessage ->
                        if (!state.isAIMode) {
                            ErrorSearchState(errorMessage)
                        }
                    },
                    onSuccess = { games ->
                        if (games.isEmpty()) {
                            NoResultsState(isAIMode = state.isAIMode)
                        } else {
                            SearchResultsList(
                                games = games,
                                gamesInLibrary = gamesInLibrary,
                                viewModel = viewModel,
                                onGameClick = onGameClick,
                                onNotification = { message, success ->
                                    notificationMessage = message
                                    isSuccess = success
                                    showNotification = true
                                },
                                isAIMode = state.isAIMode
                            )
                        }
                    }
                )
            }
        }

        TopNotification(
            visible = showNotification,
            message = notificationMessage,
            isSuccess = isSuccess,
            onDismiss = { showNotification = false },
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Snackbar with undo for game additions (positioned above bottom bar)
        AddGameSnackbar(
            visible = snackbarState.show,
            gameName = snackbarState.gameName,
            onUndo = { viewModel.undoAddGame() },
            onDismiss = { viewModel.dismissSnackbar() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
        
        // Unsaved changes snackbar for add game flow
        com.example.arcadia.presentation.components.UnsavedChangesSnackbar(
            visible = unsavedAddGameState.show,
            onReopen = { viewModel.reopenAddGameWithUnsavedChanges() },
            onSave = { viewModel.saveUnsavedAddGameChanges() },
            onDismiss = { viewModel.dismissUnsavedAddGameChanges() },
            modifier = Modifier.padding(bottom = 80.dp)
        )
    }
    
    // Game Rating Sheet for adding games
    if (addGameSheetState.isOpen && addGameSheetState.originalGame != null) {
        val game = addGameSheetState.originalGame!!
        // Use unsaved entry if reopening, otherwise create fresh entry
        val initialEntry = addGameSheetState.unsavedEntry ?: game.toGameListEntry()
        // Original entry is always from the fresh game (for change detection)
        val originalEntry = game.toGameListEntry()
        
        GameRatingSheet(
            game = initialEntry,
            isOpen = true,
            onDismiss = { viewModel.dismissStatusPicker() },
            onSave = { entry ->
                // Use addGameWithEntry to preserve all fields (rating, aspects, hoursPlayed, etc.)
                val entryWithGameData = entry.copy(
                    rawgId = game.id,
                    name = game.name,
                    backgroundImage = game.backgroundImage,
                    genres = game.genres,
                    platforms = game.platforms,
                    releaseDate = game.released
                )
                viewModel.addGameWithEntry(
                    entry = entryWithGameData,
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
            },
            onRemove = null,
            isInLibrary = false,
            onDismissWithUnsavedChanges = { unsavedEntry ->
                // Show snackbar with option to reopen or save
                viewModel.handleSheetDismissedWithUnsavedChanges(unsavedEntry, game)
            },
            originalEntry = originalEntry
        )
    }
}

/**
 * Cool animated segmented toggle for AI mode
 */
@Composable
private fun AISearchToggle(
    isAIMode: Boolean,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val width = maxWidth
        val indicatorWidth = (width - 8.dp) / 2
        
        val selectedOffset by animateDpAsState(
            targetValue = if (isAIMode) 4.dp else (width / 2),
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            label = "offset"
        )

        // Background container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF0A1628))
                .border(
                    width = 1.dp,
                    color = TextSecondary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            // Animated selection indicator
            Box(
                modifier = Modifier
                    .offset(x = selectedOffset)
                    .padding(vertical = 4.dp)
                    .width(indicatorWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        brush = if (isAIMode) {
                            Brush.linearGradient(
                                colors = listOf(
                                    ButtonPrimary.copy(alpha = 0.8f),
                                    YellowAccent.copy(alpha = 0.6f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    ButtonPrimary.copy(alpha = 0.3f),
                                    ButtonPrimary.copy(alpha = 0.2f)
                                )
                            )
                        }
                    )
                    .then(
                        if (isAIMode) {
                            Modifier.blur(radius = (glowAlpha * 8).dp)
                        } else Modifier
                    )
            )

            // Actual selection box (on top of glow)
            Box(
                modifier = Modifier
                    .offset(x = selectedOffset)
                    .padding(vertical = 4.dp)
                    .width(indicatorWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        brush = if (isAIMode) {
                            Brush.linearGradient(
                                colors = listOf(
                                    ButtonPrimary,
                                    Color(0xFF4A90D9)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    ButtonPrimary.copy(alpha = 0.15f),
                                    ButtonPrimary.copy(alpha = 0.1f)
                                )
                            )
                        }
                    )
            )

            // Toggle buttons
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI Search Option
                ToggleOption(
                    icon = { 
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isAIMode) Color.White else TextSecondary.copy(alpha = 0.6f)
                        )
                    },
                    text = "AI Search",
                    isSelected = isAIMode,
                    onClick = { if (!isAIMode) onToggle() },
                    modifier = Modifier.weight(1f)
                )

                // Text Search Option
                ToggleOption(
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = if (!isAIMode) Color.White else TextSecondary.copy(alpha = 0.6f)
                        )
                    },
                    text = "Text Search",
                    isSelected = !isAIMode,
                    onClick = { if (isAIMode) onToggle() },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ToggleOption(
    icon: @Composable () -> Unit,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextSecondary.copy(alpha = 0.6f),
        animationSpec = tween(200),
        label = "textColor"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.95f,
        animationSpec = tween(200),
        label = "scale"
    )

    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .scale(scale)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * AI Search status with animated progress
 */
@Composable
private fun AISearchStatus(
    status: String,
    progress: Pair<Int, Int>?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = ButtonPrimary.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Animated AI icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(ButtonPrimary, YellowAccent)
                            )
                        )
                        .graphicsLayer { alpha = pulseAlpha },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
                
                Column {
                    Text(
                        text = status,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    progress?.let { (current, total) ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Found $current of $total games",
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            progress?.let { (current, total) ->
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { current.toFloat() / total.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ButtonPrimary,
                    trackColor = ButtonPrimary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

/**
 * AI Error card (friendly, not alarming)
 */
@Composable
private fun AIErrorCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = YellowAccent.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(YellowAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = YellowAccent
                )
            }
            Text(
                text = message,
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * AI Reasoning card showing why games were suggested
 */
@Composable
private fun AIReasoningCard(reasoning: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0A1628)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(ButtonPrimary.copy(alpha = 0.3f), YellowAccent.copy(alpha = 0.2f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = YellowAccent
                )
            }
            Text(
                text = reasoning,
                color = TextSecondary.copy(alpha = 0.8f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun EmptySearchState(isAIMode: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        brush = if (isAIMode) {
                            Brush.linearGradient(
                                colors = listOf(
                                    ButtonPrimary.copy(alpha = 0.15f),
                                    YellowAccent.copy(alpha = 0.1f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    ButtonPrimary.copy(alpha = 0.1f),
                                    ButtonPrimary.copy(alpha = 0.05f)
                                )
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAIMode) Icons.Rounded.AutoAwesome else Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = if (isAIMode) ButtonPrimary else TextSecondary.copy(alpha = 0.5f)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isAIMode) 
                        "Describe what you're looking for" 
                        else "Type to search for games",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                if (isAIMode) {
                    Text(
                        text = "Try: \"games like Stardew Valley\" or \"best co-op games\"",
                        color = TextSecondary.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingSearchState(isAIMode: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isAIMode) {
                // Custom AI loading animation
                val infiniteTransition = rememberInfiniteTransition(label = "ai_loading")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )
                
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer { rotationZ = rotation }
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    ButtonPrimary,
                                    YellowAccent,
                                    ButtonPrimary.copy(alpha = 0.3f),
                                    ButtonPrimary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = ButtonPrimary
                        )
                    }
                }
            } else {
                LoadingIndicator(color = ButtonPrimary)
            }
        }
    }
}

@Composable
private fun ErrorSearchState(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE57373).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Color(0xFFE57373)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Something went wrong",
                    color = TextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = errorMessage,
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun NoResultsState(isAIMode: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(TextSecondary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SearchOff,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = TextSecondary.copy(alpha = 0.5f)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "No games found",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (isAIMode) {
                    Text(
                        text = "Try a different description",
                        color = TextSecondary.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    games: List<com.example.arcadia.domain.model.Game>,
    gamesInLibrary: Set<Int>,
    viewModel: SearchViewModel,
    onGameClick: (Int) -> Unit,
    onNotification: (String, Boolean) -> Unit,
    isAIMode: Boolean = false
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp) // Ensure no centering
    ) {
        items(games) { game ->
            SearchResultCard(
                game = game,
                isAdded = game.id in gamesInLibrary,
                onClick = { onGameClick(game.id) },
                onToggle = {
                    viewModel.toggleGameInLibrary(
                        game = game,
                        onSuccess = {
                            onNotification("${game.name} added to My Games", true)
                        },
                        onError = { error ->
                            onNotification(error, false)
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
        
        // Load More button for AI search
        if (isAIMode && games.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedButton(
                        onClick = { viewModel.loadMoreAIResults() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ButtonPrimary
                        ),
                        border = BorderStroke(1.dp, ButtonPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Load More AI Suggestions")
                    }
                }
            }
        }
    }
}
