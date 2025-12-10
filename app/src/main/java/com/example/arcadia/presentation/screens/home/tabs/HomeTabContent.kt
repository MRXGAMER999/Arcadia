package com.example.arcadia.presentation.screens.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.arcadia.data.remote.mapper.toGameListEntry
import com.example.arcadia.presentation.components.ScrollToTopFAB
import com.example.arcadia.presentation.components.UnsavedChangesSnackbar
import com.example.arcadia.presentation.components.game_rating.GameRatingSheet
import com.example.arcadia.presentation.screens.home.HomeViewModel
import com.example.arcadia.presentation.screens.home.components.GameListItem
import com.example.arcadia.presentation.screens.home.components.LargeGameCard
import com.example.arcadia.presentation.screens.home.components.SectionHeader
import com.example.arcadia.presentation.screens.home.components.SmallGameCard
import com.example.arcadia.presentation.screens.home.tabs.components.ErrorSection
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.util.RequestState

import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

/**
 * Home tab content displaying Popular Games, Upcoming Games, and Playlist Recommendations.
 * Extracted from HomeTabsNavigation for better separation of concerns.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeTabContent(
    viewModel: HomeViewModel,
    onGameClick: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    listState: LazyListState = rememberLazyListState()
) {
    val screenState = viewModel.screenState
    val addGameSheetState by viewModel.addGameSheetState.collectAsState()
    val unsavedAddGameState by viewModel.unsavedAddGameState.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    
    // Use offline-capable Paging 3 flow for recommendations
    val aiPagingItems = viewModel.aiRecommendationsPaged.collectAsLazyPagingItems()

    // Manage scroll state manually for Paging 3 using ViewModel state
    // This ensures persistence even if the View is destroyed/recreated
    
    // Track if we are currently restoring the scroll position
    // We initialize based on whether we have a saved position in the ViewModel
    var isRestoring by remember { mutableStateOf(viewModel.homeScrollIndex > 0) }
    
    // Cancel restoration if user manually scrolls
    LaunchedEffect(Unit) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collect { isRestoring = false }
    }
    
    // Restore scroll position when items are available
    LaunchedEffect(aiPagingItems.itemCount) {
        if (isRestoring && aiPagingItems.itemCount > viewModel.homeScrollIndex) {
            listState.scrollToItem(viewModel.homeScrollIndex, viewModel.homeScrollOffset)
            isRestoring = false
        }
    }
    
    // Save scroll position to ViewModel
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (!isRestoring) {
            viewModel.homeScrollIndex = listState.firstVisibleItemIndex
            viewModel.homeScrollOffset = listState.firstVisibleItemScrollOffset
        }
    }
    
    // For Home tab, consider paging refresh state for the pull-to-refresh indicator
    // Show refreshing when the list is empty and paging is loading (after cache clear)
    val isActuallyRefreshing = screenState.isRefreshing || 
        (aiPagingItems.loadState.refresh is LoadState.Loading && aiPagingItems.itemCount == 0)

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isActuallyRefreshing,
            onRefresh = { 
                viewModel.refreshHome()
                aiPagingItems.refresh() // Also refresh AI recommendations (cache cleared in refreshHome)
            },
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isActuallyRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Surface,
                    color = ButtonPrimary
                )
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Surface),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Popular Games Section (Carousel)
                item {
                    SectionHeader(
                        title = "Popular Games",
                        onSeeAllClick = { /* TODO */ }
                    )

                    when (val state = screenState.popularGames) {
                        is RequestState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator(color = ButtonPrimary)
                            }
                        }
                        is RequestState.Success -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(state.data, key = { it.id }) { game ->
                                    LargeGameCard(
                                        game = game,
                                        onClick = { onGameClick(game.id) }
                                    )
                                }
                            }
                        }
                        is RequestState.Error -> {
                            ErrorSection(
                                message = state.message,
                                onRetry = { viewModel.retry() }
                            )
                        }
                        else -> {}
                    }
                }

                // Upcoming Games Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title = "Upcoming",
                        onSeeAllClick = { /* TODO */ }
                    )

                    when (val state = screenState.upcomingGames) {
                        is RequestState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator(color = ButtonPrimary)
                            }
                        }
                        is RequestState.Success -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(state.data, key = { it.id }) { game ->
                                    SmallGameCard(
                                        game = game,
                                        onClick = { onGameClick(game.id) }
                                    )
                                }
                            }
                        }
                        is RequestState.Error -> {
                            ErrorSection(
                                message = state.message,
                                onRetry = { viewModel.retry() }
                            )
                        }
                        else -> {}
                    }
                }

                // Playlist Recommendation Section (as list items)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title = "Playlist Recommendation",
                        onSeeAllClick = { /* TODO */ }
                    )
                }
                
                // Recommended Games Section (Paging 3 with Offline Cache)
                val loadState = aiPagingItems.loadState.refresh
                
                // Check if we have cached data to show
                if (aiPagingItems.itemCount > 0) {
                    // Show cached data (limit to 3)
                    items(
                        count = minOf(aiPagingItems.itemCount, 3),
                        key = { index -> aiPagingItems[index]?.id ?: index }
                    ) { index ->
                        val game = aiPagingItems[index]
                        if (game != null) {
                            GameListItem(
                                game = game,
                                isInLibrary = viewModel.isGameInLibrary(game.id),
                                onClick = { onGameClick(game.id) },
                                onAddToLibrary = {
                                    viewModel.showStatusPicker(game)
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                } else {
                    // No data, handle loading/error states
                    when (loadState) {
                        is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LoadingIndicator(color = ButtonPrimary)
                                }
                            }
                        }
                        is LoadState.Error -> {
                            item {
                                ErrorSection(
                                    message = loadState.error.localizedMessage ?: "Failed to load recommendations",
                                    onRetry = { aiPagingItems.retry() }
                                )
                            }
                        }
                        else -> {} // Not loading, no error, no items (empty)
                    }
                }
            }
        }
        
        // Game Rating Sheet for adding games (Home Tab)
        if (addGameSheetState.isOpen && addGameSheetState.originalGame != null) {
            addGameSheetState.originalGame?.let { game ->
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
                            developers = game.developers,
                            publishers = game.publishers,
                            releaseDate = game.released
                        )
                        viewModel.addGameWithEntry(entryWithGameData)
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
        
        // Scroll to top FAB
        ScrollToTopFAB(
            listState = listState,
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        // Unsaved changes snackbar for add game flow
        UnsavedChangesSnackbar(
            visible = unsavedAddGameState.show,
            onReopen = { viewModel.reopenAddGameWithUnsavedChanges() },
            onSave = { viewModel.saveUnsavedAddGameChanges() },
            onDismiss = { viewModel.dismissUnsavedAddGameChanges() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
