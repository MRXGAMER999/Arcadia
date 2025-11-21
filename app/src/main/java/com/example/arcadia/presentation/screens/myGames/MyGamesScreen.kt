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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.LaunchedEffect
import com.example.arcadia.domain.repository.SortOrder
import com.example.arcadia.presentation.components.LibraryEmptyState
import com.example.arcadia.presentation.components.QuickRateDialog
import com.example.arcadia.presentation.components.TopNotification
import com.example.arcadia.presentation.components.game_rating.GameRatingSheet
import com.example.arcadia.presentation.screens.myGames.components.GameStatsCard
import com.example.arcadia.presentation.screens.myGames.components.MyGameCard
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.util.RequestState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MyGamesScreen(
    onNavigateBack: () -> Unit = {},
    onGameClick: (Int) -> Unit = {},
    showBackButton: Boolean = false
) {
    val viewModel: MyGamesViewModel = koinViewModel()
    val screenState = viewModel.screenState
    var showSortMenu by remember { mutableStateOf(false) }
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
                .background(Surface)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // "All" chip
                    item {
                        FilterChip(
                            selected = screenState.selectedGenre == null,
                            onClick = { viewModel.selectGenre(null) },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ButtonPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E2A47),
                                labelColor = TextSecondary
                            )
                        )
                    }
                    
                    // Genre chips
                    items(viewModel.availableGenres) { genre ->
                        FilterChip(
                            selected = screenState.selectedGenre == genre,
                            onClick = { viewModel.selectGenre(genre) },
                            label = { Text(genre) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ButtonPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E2A47),
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
                
                // Sort Controls and Stats Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showSortMenu = true }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = ButtonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Sort By",
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
                    
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Newest First",
                                    color = if (screenState.sortOrder == SortOrder.NEWEST_FIRST) ButtonPrimary else TextSecondary
                                ) 
                            },
                            onClick = {
                                viewModel.setSortOrder(SortOrder.NEWEST_FIRST)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    "Oldest First",
                                    color = if (screenState.sortOrder == SortOrder.OLDEST_FIRST) ButtonPrimary else TextSecondary
                                ) 
                            },
                            onClick = {
                                viewModel.setSortOrder(SortOrder.OLDEST_FIRST)
                                showSortMenu = false
                            }
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
                            LibraryEmptyState(
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Stats Card as first item (with animation)
                                item(
                                    span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }
                                ) {
                                    AnimatedVisibility(
                                        visible = showStats,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        GameStatsCard(games = state.data)
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
