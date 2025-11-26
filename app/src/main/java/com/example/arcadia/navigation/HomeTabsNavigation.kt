package com.example.arcadia.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.arcadia.data.remote.mapper.toGameListEntry
import com.example.arcadia.presentation.components.DiscoveryFilterDialog
import com.example.arcadia.presentation.components.game_rating.GameRatingSheet
import com.example.arcadia.presentation.screens.home.HomeViewModel
import com.example.arcadia.presentation.screens.home.components.GameListItem
import com.example.arcadia.presentation.screens.home.components.LargeGameCard
import com.example.arcadia.presentation.screens.home.components.SectionHeader
import com.example.arcadia.presentation.screens.home.components.SmallGameCard
import com.example.arcadia.presentation.screens.myGames.MyGamesScreen
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.util.RequestState
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
sealed interface HomeTabKey : NavKey

@Serializable
object HomeTab : HomeTabKey

@Serializable
object DiscoverTab : HomeTabKey

@Serializable
object LibraryTab : HomeTabKey

@Composable
fun HomeTabsNavContent(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onGameClick: (Int) -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel
) {
    val homeBackStack = rememberNavBackStack(HomeTab)
    val discoverBackStack = rememberNavBackStack(DiscoverTab)
    val libraryBackStack = rememberNavBackStack(LibraryTab)

    // Animated content with slide and fade transitions
    AnimatedContent(
        targetState = selectedIndex,
        modifier = modifier.fillMaxSize(),
        transitionSpec = tabTransitionSpec(),
        label = "tab_transition"
    ) { targetIndex ->
        val backStack = when (targetIndex) {
            0 -> homeBackStack
            1 -> discoverBackStack
            2 -> libraryBackStack
            else -> homeBackStack
        }

        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            entryProvider = { key ->
                when (key) {
                    is HomeTab -> NavEntry(key) {
                        HomeTabRoot(
                            onGameClick = onGameClick,
                            snackbarHostState = snackbarHostState,
                            viewModel = viewModel
                        )
                    }
                    is DiscoverTab -> NavEntry(key) {
                        DiscoverTabRoot(
                            onGameClick = onGameClick,
                            snackbarHostState = snackbarHostState,
                            viewModel = viewModel
                        )
                    }
                    is LibraryTab -> NavEntry(key) {
                        LibraryTabRoot(
                            onGameClick = onGameClick,
                            onNavigateToAnalytics = onNavigateToAnalytics
                        )
                    }
                    else -> error("Unknown key for Home tabs backstack: $key")
                }
            }
        )
    }
}

	private fun tabTransitionSpec(): AnimatedContentTransitionScope<Int>.() -> ContentTransform = {
		val slideDirection = if (targetState > initialState) 1 else -1
		val animationDuration = 200

		slideInHorizontally(
			initialOffsetX = { fullWidth -> slideDirection * fullWidth / 3 },
			animationSpec = tween(durationMillis = animationDuration)
		) + fadeIn(
			animationSpec = tween(durationMillis = animationDuration)
		) togetherWith slideOutHorizontally(
			targetOffsetX = { fullWidth -> -slideDirection * fullWidth / 3 },
			animationSpec = tween(durationMillis = animationDuration)
		) + fadeOut(
			animationSpec = tween(durationMillis = animationDuration)
		)
	}
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

@Composable
private fun HomeTabRoot(
    onGameClick: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel
) {
    val screenState = viewModel.screenState
    val gameToAddWithStatus by viewModel.gameToAddWithStatus.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PullToRefreshBox(
            isRefreshing = screenState.isRefreshing,
            onRefresh = { viewModel.refreshHome() },
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
            // Recommended Games Section
            when (val state = screenState.recommendedGames) {
                is RequestState.Loading -> {
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
                is RequestState.Success -> {
                    items(
                        items = state.data.take(3),
                        key = { it.id }
                    ) { game ->
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
                is RequestState.Error -> {
                    item {
                        ErrorSection(
                            message = state.message,
                            onRetry = { viewModel.retry() }
                        )
                    }
                }
                else -> {}
            }
        }
        }
        
        // Game Rating Sheet for adding games (Home Tab)
        gameToAddWithStatus?.let { game ->
            GameRatingSheet(
                game = game.toGameListEntry(),
                isOpen = true,
                onDismiss = { viewModel.dismissStatusPicker() },
                onSave = { entry ->
                    viewModel.addGameWithStatus(game, entry.status)
                },
                onRemove = null,
                isInLibrary = false,
                onDismissWithUnsavedChanges = null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverTabRoot(
    onGameClick: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel
) {
    val screenState = viewModel.screenState
    val discoveryFilterState = viewModel.discoveryFilterState
    val showFilterDialog = viewModel.showDiscoveryFilterDialog
    val gameToAddWithStatus by viewModel.gameToAddWithStatus.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

                when (val state = screenState.newReleases) {
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

            // Recommended Games as list items with discovery filter
            item {
                Spacer(modifier = Modifier.height(8.dp))
                DiscoverySectionHeader(
                    title = "Recommended For You",
                    filterActive = viewModel.isDiscoveryFilterActive(),
                    activeFilterCount = discoveryFilterState.activeFilterCount,
                    onFilterClick = { viewModel.showDiscoveryFilterDialog() },
                    onClearFilter = { viewModel.clearDiscoveryFilters() }
                )
            }

            when (val state = screenState.recommendedGames) {
                is RequestState.Loading -> {
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
                is RequestState.Success -> {
                    if (state.data.isEmpty() && viewModel.isDiscoveryFilterActive()) {
                        item {
                            EmptyDiscoveryFilterResult(
                                onClearFilter = { viewModel.clearDiscoveryFilters() }
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = state.data,
                            key = { _, game -> game.id }
                        ) { index, game ->
                            // Load more when reaching 70% of the list
                            if (index >= (state.data.size * 0.7).toInt()) {
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
                    }
                }
                is RequestState.Error -> {
                    item {
                        ErrorSection(
                            message = state.message,
                            onRetry = { viewModel.retry() }
                        )
                    }
                }
                else -> {}
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
        gameToAddWithStatus?.let { game ->
            GameRatingSheet(
                game = game.toGameListEntry(),
                isOpen = true,
                onDismiss = { viewModel.dismissStatusPicker() },
                onSave = { entry ->
                    viewModel.addGameWithStatus(game, entry.status)
                },
                onRemove = null,
                isInLibrary = false,
                onDismissWithUnsavedChanges = null
            )
        }
    }
}

@Composable
private fun LibraryTabRoot(
    onGameClick: (Int) -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MyGamesScreen(
            onNavigateBack = { /* Don't navigate back, we're in a tab */ },
            onGameClick = onGameClick,
            onNavigateToAnalytics = onNavigateToAnalytics
        )
    }
}

@Composable
private fun ErrorSection(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚠️",
            fontSize = 48.sp
        )
        Text(
            text = "Something went wrong",
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

@Composable
private fun SectionHeaderWithFilter(
    title: String,
    filterActive: Boolean,
    filterLabel: String?,
    onFilterClick: () -> Unit,
    onClearFilter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (filterActive && filterLabel != null) {
                FilterChip(
                    selected = true,
                    onClick = onFilterClick,
                    label = { Text(filterLabel, maxLines = 1) },
                    trailingIcon = {
                        IconButton(
                            onClick = onClearFilter,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear filter",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                )
            } else {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filter by studio",
                        tint = ButtonPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFilterResult(
    studioName: String,
    onClearFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎮",
            fontSize = 48.sp
        )
        Text(
            text = "No games found",
            color = TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "No recommended games from $studioName in your current list",
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        TextButton(
            onClick = onClearFilter,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "Clear Filter",
                color = ButtonPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DiscoverySectionHeader(
    title: String,
    filterActive: Boolean,
    activeFilterCount: Int,
    onFilterClick: () -> Unit,
    onClearFilter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (filterActive) {
                FilterChip(
                    selected = true,
                    onClick = onFilterClick,
                    label = { Text("$activeFilterCount Filters", maxLines = 1) },
                    trailingIcon = {
                        IconButton(
                            onClick = onClearFilter,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear filters",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                )
            } else {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Open filters",
                        tint = ButtonPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDiscoveryFilterResult(
    onClearFilter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🔍",
            fontSize = 48.sp
        )
        Text(
            text = "No games found",
            color = TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Try adjusting your filters to find more games",
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        TextButton(
            onClick = onClearFilter,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "Clear All Filters",
                color = ButtonPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

