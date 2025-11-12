package com.example.arcadia.presentation.screens.detailsScreen

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

    LaunchedEffect(gameId) {
        viewModel.loadGameDetails(gameId)
    }

    val uiState = viewModel.uiState

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Game Details",
                        color = Color.White,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Surface
    ) { paddingValues ->
        when (val state = uiState.gameState) {
            is RequestState.Loading -> LoadingState(modifier = Modifier.padding(paddingValues))
            is RequestState.Success -> GameDetailsContent(
                game = state.data,
                isInLibrary = uiState.isInLibrary,
                addToLibraryInProgress = uiState.addToLibraryInProgress,
                onAddToLibrary = { viewModel.addToLibrary() },
                modifier = Modifier.padding(paddingValues)
            )
            is RequestState.Error -> ErrorState(
                message = state.message,
                onRetry = { viewModel.retry() },
                modifier = Modifier.padding(paddingValues)
            )
            else -> {}
        }
    }
}
