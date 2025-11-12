package com.example.arcadia.presentation.screens.detailsScreen.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.arcadia.domain.model.Game
import com.example.arcadia.presentation.components.PrimaryButton

@Composable
fun GameDetailsContent(
    game: Game,
    isInLibrary: Boolean,
    addToLibraryInProgress: Boolean,
    onAddToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        GameHeaderSection(game = game)

        GameStatsSection(game = game)

        Spacer(modifier = Modifier.height(16.dp))
        MediaCarouselSection(game = game)

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            PrimaryButton(
                text = if (isInLibrary) "Game is already in your library" else "Add to Library",
                enabled = !isInLibrary && !addToLibraryInProgress,
                onClick = onAddToLibrary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        GameDescriptionSection(game = game)
        Spacer(modifier = Modifier.height(24.dp))
        UserRatingSection()
        Spacer(modifier = Modifier.height(32.dp))
    }
}
