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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.arcadia.navigation.HomeTabsNavContent
import com.example.arcadia.presentation.components.TopNotification
import com.example.arcadia.presentation.screens.home.components.HomeBottomBar
import com.example.arcadia.presentation.screens.home.components.HomeTopBar
import com.example.arcadia.ui.theme.Surface
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

data class NotificationData(
    val message: String,
    val isSuccess: Boolean
)

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
    val notificationQueue = remember { mutableStateListOf<NotificationData>() }
    var isProcessingQueue by remember { mutableStateOf(false) }

    // Process notification queue
    LaunchedEffect(Unit) {
        snapshotFlow { notificationQueue.size }
            .collect {
                if (!isProcessingQueue && notificationQueue.isNotEmpty()) {
                    isProcessingQueue = true
                    while (notificationQueue.isNotEmpty()) {
                        val notification = notificationQueue.removeAt(0)
                        notificationMessage = notification.message
                        isSuccess = notification.isSuccess
                        showNotification = true

                        // Wait until notification is dismissed
                        snapshotFlow { showNotification }
                            .filter { !it }
                            .first()

                        // Small gap between notifications
                        delay(100)
                    }
                    isProcessingQueue = false
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
                    onShowNotification = { message, success ->
                        notificationQueue.add(NotificationData(message, success))
                    },
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

