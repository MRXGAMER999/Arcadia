package com.example.arcadia.presentation.screens.home.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
