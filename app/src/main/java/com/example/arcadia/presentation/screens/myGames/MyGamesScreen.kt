package com.example.arcadia.presentation.screens.myGames

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.LaunchedEffect
import com.example.arcadia.presentation.components.LibraryEmptyState
import com.example.arcadia.presentation.components.ListGameCard
import com.example.arcadia.presentation.components.MediaLayout
import com.example.arcadia.presentation.components.QuickRateDialog
import com.example.arcadia.presentation.components.QuickSettingsDialog
import com.example.arcadia.presentation.components.TopNotification
import com.example.arcadia.presentation.components.game_rating.GameRatingSheet
import com.example.arcadia.presentation.screens.myGames.components.GameStatsCard
import com.example.arcadia.presentation.screens.myGames.components.MyGameCard
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.util.RequestState
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.items as lazyItems

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MyGamesScreen(
    onNavigateBack: () -> Unit = {},
    onGameClick: (Int) -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    showBackButton: Boolean = false
) {
    val viewModel: MyGamesViewModel = koinViewModel()
    val screenState = viewModel.screenState
    var showStats by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show undo snackbar
    LaunchedEffect(screenState.showUndoSnackbar) {
        if (screenState.showUndoSnackbar) {
            snackbarHostState.showSnackbar("")
        }
    }
    
    Scaffold(
        containerColor = Surface,
        snackbarHost = {
            if (screenState.showUndoSnackbar) {
                SnackbarHost(hostState = snackbarHostState) {
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        containerColor = Color(0xFF1E2A47),
                        contentColor = TextSecondary,
                        action = {
                            TextButton(onClick = { viewModel.undoDeletion() }) {
                                Text(
                                    text = "UNDO",
                                    color = ButtonPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissAction = {
                            IconButton(onClick = { viewModel.dismissUndoSnackbar() }) {
                                Text(
                                    text = "✕",
                                    color = TextSecondary,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    ) {
                        Text("${screenState.deletedGame?.name} removed")
                    }
                }
            }
        },
        topBar = {
            if (showBackButton) {
                TopAppBar(
                    title = {
                        Text(
                            text = "My Game List",
                            color = TextSecondary,
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
                    actions = {
                        IconButton(onClick = { /* TODO: More options */ }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = ButtonPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Surface,
                        titleContentColor = TextSecondary
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Transparent)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Filter Chips
                // Quick Settings and Stats Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.showQuickSettingsDialog() }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Quick Settings",
                            tint = ButtonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        val activeFilterCount = screenState.quickSettingsState.selectedGenres.size + 
                                               screenState.quickSettingsState.selectedStatuses.size
                        Text(
                            text = if (activeFilterCount > 0) "Quick Settings ($activeFilterCount)" else "Quick Settings",
                            color = ButtonPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    
                    TextButton(
                        onClick = { showStats = !showStats }
                    ) {
                        Text(
                            text = if (showStats) "Hide Stats" else "Show Stats",
                            color = ButtonPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Games Grid
                when (val state = screenState.games) {
                    is RequestState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(color = ButtonPrimary)
                        }
                    }
                    
                    is RequestState.Success -> {
                        if (state.data.isEmpty()) {
                            val hasActiveFilters = screenState.quickSettingsState.selectedGenres.isNotEmpty() || 
                                                  screenState.quickSettingsState.selectedStatuses.isNotEmpty()
                            if (hasActiveFilters) {
                                // Show filtered empty state
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "🔍",
                                        fontSize = 48.sp
                                    )
                                    Text(
                                        text = "No games match your filters",
                                        color = TextSecondary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                    Text(
                                        text = "Try adjusting or clearing your filters",
                                        color = TextSecondary.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    TextButton(
                                        onClick = { viewModel.showQuickSettingsDialog() },
                                        modifier = Modifier.padding(top = 16.dp)
                                    ) {
                                        Text(
                                            text = "Adjust Filters",
                                            color = ButtonPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                LibraryEmptyState(
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            // Switch between List and Grid view based on settings with animation
                            AnimatedContent(
                                targetState = screenState.quickSettingsState.mediaLayout,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(300)) + 
                                    scaleIn(initialScale = 0.95f, animationSpec = tween(300)) togetherWith
                                    fadeOut(animationSpec = tween(200)) +
                                    scaleOut(targetScale = 0.95f, animationSpec = tween(200))
                                },
                                label = "viewModeTransition"
                            ) { layout ->
                                if (layout == MediaLayout.LIST) {
                                    // List View - Reduced padding for more space
                                    LazyColumn(
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        // Stats Card as first item (with animation)
                                        item {
                                            AnimatedVisibility(
                                                visible = showStats,
                                                enter = expandVertically() + fadeIn(),
                                                exit = shrinkVertically() + fadeOut()
                                            ) {
                                                GameStatsCard(
                                                    games = state.data,
                                                    onSeeMoreClick = onNavigateToAnalytics
                                                )
                                            }
                                        }
                                        
                                        // Games List items
                                        lazyItems(
                                            items = state.data,
                                            key = { game -> game.id }
                                        ) { game ->
                                            ListGameCard(
                                                game = game,
                                                showDateAdded = screenState.quickSettingsState.showDateAdded,
                                                showReleaseDate = screenState.quickSettingsState.showReleaseDate,
                                                onClick = { onGameClick(game.rawgId) },
                                                onLongClick = { viewModel.selectGameToEdit(game) },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                } else {
                                    // Grid View
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        contentPadding = PaddingValues(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        // Stats Card as first item (with animation)
                                        item(
                                            span = { GridItemSpan(3) }
                                        ) {
                                            AnimatedVisibility(
                                                visible = showStats,
                                                enter = expandVertically() + fadeIn(),
                                                exit = shrinkVertically() + fadeOut()
                                            ) {
                                                GameStatsCard(
                                                    games = state.data,
                                                    onSeeMoreClick = onNavigateToAnalytics
                                                )
                                            }
                                        }
                                        
                                        // Games Grid items
                                        items(
                                            items = state.data,
                                            key = { game -> game.id }
                                        ) { game ->
                                            MyGameCard(
                                                game = game,
                                                onClick = { onGameClick(game.rawgId) },
                                                onLongClick = { viewModel.selectGameToEdit(game) },
                                                modifier = Modifier.animateItem()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    is RequestState.Error -> {
                        ErrorState(
                            message = state.message,
                            onRetry = { viewModel.retry() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    else -> {}
                }
            }
            
            // Notification overlay
            TopNotification(
                visible = screenState.showSuccessNotification,
                message = screenState.notificationMessage,
                isSuccess = true,
                onDismiss = { viewModel.dismissNotification() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        
        // Game Rating/Edit Sheet
        screenState.selectedGameToEdit?.let { gameToEdit ->
            GameRatingSheet(
                game = gameToEdit,
                isOpen = true,
                onDismiss = { viewModel.selectGameToEdit(null) },
                onSave = { updatedGame -> 
                    viewModel.updateGameEntry(updatedGame)
                }
            )
        }
        
        // Quick Rate Dialog
        if (screenState.showQuickRateDialog && screenState.gameToQuickRate != null) {
            QuickRateDialog(
                gameName = screenState.gameToQuickRate.name,
                currentRating = screenState.gameToQuickRate.rating,
                onDismiss = { viewModel.dismissQuickRateDialog() },
                onRatingSelected = { rating ->
                    viewModel.quickRateGame(rating)
                }
            )
        }
        
        // Quick Settings Dialog
        if (screenState.showQuickSettingsDialog) {
            QuickSettingsDialog(
                state = screenState.quickSettingsState,
                onStateChange = { newSettings ->
                    viewModel.updateQuickSettings(newSettings)
                },
                onDismiss = { viewModel.dismissQuickSettingsDialog() },
                onDone = { viewModel.applyQuickSettings() },
                availableGenres = viewModel.getAvailableGenres(),
                availableStatuses = viewModel.getAvailableStatuses(),
                totalGames = viewModel.getTotalGamesCount(),
                filteredGamesCount = viewModel.getFilteredGamesCount(),
                onClearGenres = { viewModel.clearGenreFilters() },
                onClearStatuses = { viewModel.clearStatusFilters() }
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠️",
            fontSize = 48.sp
        )
        Text(
            text = "Oops! Something went wrong",
            color = TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = message,
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        TextButton(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "Retry",
                color = ButtonPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
