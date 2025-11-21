package com.example.arcadia.presentation.screens.detailsScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.arcadia.presentation.components.TopNotification
import com.example.arcadia.presentation.components.game_rating.GameRatingSheet
import com.example.arcadia.presentation.screens.detailsScreen.components.ErrorState
import com.example.arcadia.presentation.screens.detailsScreen.components.GameDetailsContent
import com.example.arcadia.presentation.screens.detailsScreen.components.LoadingState
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.util.RequestState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    gameId: Int,
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: DetailsScreenViewModel = koinViewModel()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(gameId) {
        viewModel.loadGameDetails(gameId)
    }

    val uiState = viewModel.uiState

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ButtonPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Surface.copy(alpha = 0.95f)
                ),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = Surface
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState.gameState) {
                is RequestState.Loading -> LoadingState(modifier = Modifier.padding(paddingValues))
                is RequestState.Success -> {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 })
                    ) {
                        GameDetailsContent(
                            game = state.data,
                            isInLibrary = uiState.isInLibrary,
                            gameEntry = uiState.tempGameEntry,
                            addToLibraryState = uiState.addToLibraryState,
                            onAddToLibrary = { viewModel.onAddToLibraryClick() },
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
                is RequestState.Error -> ErrorState(
                    message = state.message,
                    onRetry = { viewModel.retry() },
                    modifier = Modifier.padding(paddingValues)
                )
                else -> {}
            }
            
            // Top Notification for success/error feedback
            TopNotification(
                visible = uiState.addToLibraryState is AddToLibraryState.Success,
                message = (uiState.addToLibraryState as? AddToLibraryState.Success)?.message ?: "",
                isSuccess = true,
                onDismiss = { /* Auto-dismiss handled by ViewModel */ },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        
        // Game Rating Sheet
        if (uiState.showRatingSheet && uiState.tempGameEntry != null) {
            GameRatingSheet(
                game = uiState.tempGameEntry,
                isOpen = true,
                onDismiss = { viewModel.dismissRatingSheet() },
                onSave = { updatedEntry ->
                    viewModel.saveGameEntry(updatedEntry)
                }
            )
        }
    }
}
