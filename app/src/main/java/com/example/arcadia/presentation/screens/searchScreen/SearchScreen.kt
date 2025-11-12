package com.example.arcadia.presentation.screens.searchScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.presentation.components.TopNotification
import com.example.arcadia.presentation.screens.searchScreen.components.SearchField
import com.example.arcadia.presentation.screens.searchScreen.components.SearchResultCard
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.util.DisplayResult


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    onBackClick: (() -> Unit)? = null,
    viewModel: SearchViewModel = org.koin.androidx.compose.koinViewModel()
) {
    val state = viewModel.screenState
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = Surface,
            topBar = {

            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Surface)
            ) {
                SearchField(
                    query = state.query,
                    onQueryChange = { viewModel.updateQuery(it) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                state.results.DisplayResult(
                    modifier = Modifier.fillMaxSize(),
                    onIdle = {
                        EmptySearchState()
                    },
                    onLoading = {
                        LoadingSearchState()
                    },
                    onError = { errorMessage ->
                        ErrorSearchState(errorMessage)
                    },
                    onSuccess = { games ->
                        if (games.isEmpty()) {
                            NoResultsState()
                        } else {
                            SearchResultsList(
                                games = games,
                                viewModel = viewModel,
                                onNotification = { message, success ->
                                    notificationMessage = message
                                    isSuccess = success
                                    showNotification = true
                                }
                            )
                        }
                    }
                )
            }
        }

        TopNotification(
            visible = showNotification,
            message = notificationMessage,
            isSuccess = isSuccess,
            onDismiss = { showNotification = false },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun EmptySearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Type to search for games",
            color = TextSecondary.copy(alpha = 0.6f),
            fontSize = 16.sp
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingSearchState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            color = ButtonPrimary
        )
    }
}

@Composable
private fun ErrorSearchState(errorMessage: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Error",
                color = Color(0xFFE57373),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = errorMessage,
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun NoResultsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No games found",
            color = TextSecondary.copy(alpha = 0.6f),
            fontSize = 16.sp
        )
    }
}

@Composable
private fun SearchResultsList(
    games: List<com.example.arcadia.domain.model.Game>,
    viewModel: SearchViewModel,
    onNotification: (String, Boolean) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(games) { game ->
            SearchResultCard(
                game = game,
                isAdded = viewModel.isGameInLibrary(game.id),
                onToggle = {
                    viewModel.toggleGameInLibrary(
                        game = game,
                        onSuccess = {
                            onNotification("${game.name} added to My Games", true)
                        },
                        onError = { error ->
                            onNotification(error, false)
                        }
                    )
                }
            )

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.2f),
                thickness = 0.6.dp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }
    }
}
