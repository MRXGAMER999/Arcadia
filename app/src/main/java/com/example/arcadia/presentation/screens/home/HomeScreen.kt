package com.example.arcadia.presentation.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.arcadia.navigation.HomeTabsNavContent
import com.example.arcadia.presentation.components.AddGameSnackbar
import com.example.arcadia.presentation.screens.home.components.HomeBottomBar
import com.example.arcadia.presentation.screens.home.components.HomeTopBar
import com.example.arcadia.ui.theme.Surface

@Composable
fun NewHomeScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToMyGames: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onGameClick: (Int) -> Unit = {},
    viewModel: HomeViewModel = org.koin.androidx.compose.koinViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val screenState = viewModel.screenState
    val snackbarState by viewModel.snackbarState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = Surface,
            topBar = {
                HomeTopBar(
                    selectedIndex = selectedTab,
                    onSearchClick = { onNavigateToSearch() },
                    onNotificationsClick = { /* TODO: Notifications */ },
                    onSettingsClick = { onNavigateToProfile() }
                )
            },
            bottomBar = {
                HomeBottomBar(
                    selectedItemIndex = selectedTab,
                    onSelectedItemIndexChange = { selectedTab = it }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier.padding(paddingValues)
            ) {
                HomeTabsNavContent(
                    selectedIndex = selectedTab,
                    onGameClick = onGameClick,
                    onNavigateToAnalytics = onNavigateToAnalytics,
                    snackbarHostState = snackbarHostState,
                    viewModel = viewModel
                )
            }
        }

        // Snackbar with undo for game additions - positioned above bottom bar
        // Placed at the screen level so it shows consistently across all tabs
        AddGameSnackbar(
            visible = snackbarState.show,
            gameName = snackbarState.gameName,
            onUndo = { viewModel.undoAddGame() },
            onDismiss = { viewModel.dismissSnackbar() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp) // Above bottom navigation bar
        )
    }
}

