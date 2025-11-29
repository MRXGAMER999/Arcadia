package com.example.arcadia.presentation.screens.home.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.arcadia.presentation.screens.myGames.MyGamesScreen

/**
 * Library tab content displaying the user's game library.
 * Wraps MyGamesScreen for use within the home tab structure.
 */
@Composable
fun LibraryTabContent(
    onGameClick: (Int) -> Unit,
    onNavigateToAnalytics: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState()
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MyGamesScreen(
            onNavigateBack = { /* Don't navigate back, we're in a tab */ },
            onGameClick = onGameClick,
            onNavigateToAnalytics = onNavigateToAnalytics,
            listState = listState,
            gridState = gridState
        )
    }
}
