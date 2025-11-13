package com.example.arcadia.presentation.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.arcadia.navigation.HomeTabsNavContent
import com.example.arcadia.presentation.components.TopNotification
import com.example.arcadia.presentation.screens.home.components.HomeBottomBar
import com.example.arcadia.presentation.screens.home.components.HomeTopBar
import com.example.arcadia.ui.theme.Surface

@Composable
fun NewHomeScreen(
    onNavigateToProfile: () -> Unit = {},
    onNavigateToMyGames: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onGameClick: (Int) -> Unit = {},
    viewModel: HomeViewModel = org.koin.androidx.compose.koinViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    val addToLibraryState = viewModel.screenState.addToLibraryState

    // Observe addToLibraryState and show notifications
    LaunchedEffect(addToLibraryState) {
        when (addToLibraryState) {
            is AddToLibraryState.Success -> {
                notificationMessage = addToLibraryState.message
                isSuccess = true
                showNotification = true
            }
            is AddToLibraryState.Error -> {
                notificationMessage = addToLibraryState.message
                isSuccess = false
                showNotification = true
            }
            else -> {
                // Don't hide notification for Loading or Idle states
                // Let the notification auto-dismiss
            }
        }
    }

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
                    snackbarHostState = snackbarHostState,
                    viewModel = viewModel
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

