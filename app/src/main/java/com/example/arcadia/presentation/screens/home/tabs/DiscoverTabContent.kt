package com.example.arcadia.presentation.screens.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.arcadia.data.remote.mapper.toGameListEntry
import com.example.arcadia.domain.model.DiscoverySortType
import com.example.arcadia.presentation.components.DiscoveryFilterDialog
import com.example.arcadia.presentation.components.UnsavedChangesSnackbar
import com.example.arcadia.presentation.components.game_rating.GameRatingSheet
import com.example.arcadia.presentation.screens.home.HomeViewModel
import com.example.arcadia.presentation.screens.home.components.GameListItem
import com.example.arcadia.presentation.screens.home.components.SectionHeader
import com.example.arcadia.presentation.screens.home.components.SmallGameCard
import com.example.arcadia.presentation.screens.home.tabs.components.DiscoverySectionHeader
import com.example.arcadia.presentation.screens.home.tabs.components.EmptyDiscoveryFilterResult
import com.example.arcadia.presentation.screens.home.tabs.components.ErrorSection
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.util.RequestState

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.arcadia.ui.theme.TextSecondary

/**
 * Discover tab content displaying New Releases and Recommended games with filters.
 * Extracted from HomeTabsNavigation for better separation of concerns.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DiscoverTabContent(
    viewModel: HomeViewModel,
    onGameClick: (Int) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val screenState = viewModel.screenState
    val discoveryFilterState = viewModel.discoveryFilterState
    val showFilterDialog = viewModel.showDiscoveryFilterDialog
    val addGameSheetState by viewModel.addGameSheetState.collectAsState()
    val unsavedAddGameState by viewModel.unsavedAddGameState.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = screenState.isRefreshing,
            onRefresh = { viewModel.refreshDiscover() },
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = screenState.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Surface,
                    color = ButtonPrimary
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Surface),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // New Releases Section
                item {
                    SectionHeader(
                        title = "New Releases",
                        onSeeAllClick = { /* TODO */ }
                    )

                    when {
                        // Show loading when refreshing
                        screenState.isRefreshing -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator(color = ButtonPrimary)
                            }
                        }
                        screenState.newReleases is RequestState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator(color = ButtonPrimary)
                            }
                        }
                        screenState.newReleases is RequestState.Success -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items((screenState.newReleases as RequestState.Success).data, key = { it.id }) { game ->
                                    SmallGameCard(
                                        game = game,
                                        onClick = { onGameClick(game.id) }
                                    )
                                }
                            }
                        }
                        screenState.newReleases is RequestState.Error -> {
                            ErrorSection(
                                message = (screenState.newReleases as RequestState.Error).message,
                                onRetry = { viewModel.retry() }
                            )
                        }
                    }
                }

                // Recommended Games as list items with discovery filter
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    DiscoverySectionHeader(
                        title = "Recommended For You",
                        filterActive = viewModel.isDiscoveryFilterActive(),
                        activeFilterCount = discoveryFilterState.activeFilterCount,
                        isAiRecommendation = discoveryFilterState.sortType == DiscoverySortType.AI_RECOMMENDATION,
                        onFilterClick = { viewModel.showDiscoveryFilterDialog() },
                        onClearFilter = { viewModel.clearDiscoveryFilters() }
                    )
                }

                when {
                    // Show loading when refreshing
                    screenState.isRefreshing -> {
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
                    screenState.recommendedGames is RequestState.Loading -> {
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
                    screenState.recommendedGames is RequestState.Success -> {
                        val data = (screenState.recommendedGames as RequestState.Success).data
                        if (data.isEmpty() && viewModel.isDiscoveryFilterActive()) {
                            item {
                                EmptyDiscoveryFilterResult(
                                    onClearFilter = { viewModel.clearDiscoveryFilters() }
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = data,
                                key = { _, game -> game.id }
                            ) { index, game ->
                                // Load more when reaching 40% of the list
                                if (index >= (data.size * 0.4).toInt()) {
                                    LaunchedEffect(Unit) {
                                        if (viewModel.isDiscoveryFilterActive()) {
                                            viewModel.loadMoreDiscoveryResults()
                                        } else {
                                            viewModel.loadMoreRecommendations()
                                        }
                                    }
                                }

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
                            
                            // Loading indicator at the bottom
                            if (screenState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            LoadingIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = ButtonPrimary
                                            )
                                            Text(
                                                text = "Loading games based on your library...",
                                                color = TextSecondary.copy(alpha = 0.7f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    screenState.recommendedGames is RequestState.Error -> {
                        item {
                            ErrorSection(
                                message = (screenState.recommendedGames as RequestState.Error).message,
                                onRetry = { viewModel.retry() }
                            )
                        }
                    }
                }
            }
        }

        // Discovery Filter Dialog
        if (showFilterDialog) {
            DiscoveryFilterDialog(
                state = discoveryFilterState,
                onStateChange = { viewModel.updateDiscoveryFilterState(it) },
                onDismiss = { viewModel.dismissDiscoveryFilterDialog() },
                onApply = { viewModel.applyDiscoveryFilters() },
                onDeveloperSearch = { viewModel.searchDevelopers(it) },
                onSelectDeveloperWithStudios = { developer, _ -> 
                    viewModel.selectDeveloperWithStudios(developer)
                },
                onClearAllFilters = { viewModel.clearDiscoveryFilters() }
            )
        }
        
        // Game Rating Sheet for adding games (Discover Tab)
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
