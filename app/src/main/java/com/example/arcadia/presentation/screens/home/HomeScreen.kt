package com.example.arcadia.presentation.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.arcadia.presentation.components.AddGameSnackbar
import com.example.arcadia.presentation.components.BottomSlideSnackbarHost
import com.example.arcadia.presentation.screens.home.components.HomeBottomBar
import com.example.arcadia.presentation.screens.home.components.HomeTopBar
import com.example.arcadia.presentation.screens.home.tabs.DiscoverTabContent
import com.example.arcadia.presentation.screens.home.tabs.HomeTabContent
import com.example.arcadia.presentation.screens.home.tabs.LibraryTabContent
import com.example.arcadia.ui.theme.Surface
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Custom Saver for LazyListState to ensure scroll position is preserved
private val LazyListStateSaver = listSaver<LazyListState, Int>(
    save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
    restore = { LazyListState(it[0], it[1]) }
)

// Custom Saver for LazyGridState
private val LazyGridStateSaver = listSaver<LazyGridState, Int>(
    save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
    restore = { LazyGridState(it[0], it[1]) }
)

@Composable
fun NewHomeScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToMyGames: () -> Unit = {},
    onNavigateToSearch: (String?) -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onGameClick: (Int) -> Unit = {},
    viewModel: HomeViewModel = org.koin.androidx.compose.koinViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val screenState = viewModel.screenState
    val snackbarState by viewModel.snackbarState.collectAsState()
    val pendingFriendRequestCount by viewModel.pendingFriendRequestCount.collectAsState()
    val scope = rememberCoroutineScope()
    
    // Keep paging collection alive across tab switches to avoid list refresh
    val aiPagingItems = viewModel.aiRecommendationsPaged.collectAsLazyPagingItems()
    
    // Double back to exit logic
    var backPressedOnce by remember { mutableStateOf(false) }
    
    BackHandler {
        if (backPressedOnce) {
            // Exit app (default behavior if not handled, but here we need to finish activity)
            // Since we can't easily finish activity from here without context, we can let the system handle it
            // by disabling the BackHandler or using a side effect.
            // However, BackHandler captures the back press. 
            // A common pattern is to just let it bubble up if we don't want to handle it, 
            // but BackHandler is enabled by default.
            // We can use System.exit(0) or (context as Activity).finish()
            android.os.Process.killProcess(android.os.Process.myPid())
        } else {
            backPressedOnce = true
            scope.launch {
                snackbarHostState.showSnackbar("Press back again to exit", duration = SnackbarDuration.Short)
                delay(2000)
                backPressedOnce = false
            }
        }
    }
    
    // For Paging tabs (Home, Discover), scroll state is managed internally by the tab content
    // to handle the async loading nature of Paging 3
    val homeTabListState = rememberLazyListState()
    val discoverTabListState = rememberLazyListState()
    
    // For Library tab (non-paging), we use custom savers to preserve state
    val libraryTabListState = rememberSaveable(saver = LazyListStateSaver) { LazyListState() }
    val libraryTabGridState = rememberSaveable(saver = LazyGridStateSaver) { LazyGridState() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = Surface,
            topBar = {
                HomeTopBar(
                    selectedIndex = selectedTab,
                    pendingFriendRequestCount = pendingFriendRequestCount,
                    onSearchClick = { onNavigateToSearch(null) },
                    onFriendsClick = onNavigateToFriends,
                    onSettingsClick = { onNavigateToProfile() }
                )
            },
            bottomBar = {
                HomeBottomBar(
                    selectedItemIndex = selectedTab,
                    onSelectedItemIndexChange = { selectedTab = it }
                )
            },
            snackbarHost = {
                // We use a custom snackbar host to display our custom AddGameSnackbar
                // But we anchor it to the Scaffold
                Box(modifier = Modifier.fillMaxSize()) {
                    // Standard SnackbarHost for system messages (like "Press back again")
                    BottomSlideSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    
                    // Custom Snackbar for Game Added
                    AddGameSnackbar(
                        visible = snackbarState.show,
                        gameName = snackbarState.gameName,
                        onUndo = { viewModel.undoAddGame() },
                        onDismiss = { viewModel.dismissSnackbar() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier.padding(paddingValues)
            ) {
                // Simplified AnimatedContent for tab switching
                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
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
                    },
                    label = "tab_transition"
                ) { targetIndex ->
                    when (targetIndex) {
                        0 -> HomeTabContent(
                            viewModel = viewModel,
                            onGameClick = onGameClick,
                            snackbarHostState = snackbarHostState,
                            listState = homeTabListState,
                            aiPagingItems = aiPagingItems
                        )
                        1 -> DiscoverTabContent(
                            viewModel = viewModel,
                            onGameClick = onGameClick,
                            snackbarHostState = snackbarHostState,
                            listState = discoverTabListState,
                            aiPagingItems = aiPagingItems
                        )
                        2 -> LibraryTabContent(
                            onGameClick = onGameClick,
                            onNavigateToAnalytics = onNavigateToAnalytics,
                            listState = libraryTabListState,
                            gridState = libraryTabGridState
                        )
                    }
                }
            }
        }
    }
}

