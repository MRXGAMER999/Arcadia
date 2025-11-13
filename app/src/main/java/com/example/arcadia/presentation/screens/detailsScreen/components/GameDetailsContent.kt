package com.example.arcadia.presentation.screens.detailsScreen.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.arcadia.domain.model.Game
import com.example.arcadia.presentation.components.PrimaryButton
import com.example.arcadia.presentation.screens.detailsScreen.AddToLibraryState

@Composable
fun GameDetailsContent(
    game: Game,
    isInLibrary: Boolean,
    addToLibraryState: AddToLibraryState,
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

            // Animated button state transitions
            AnimatedContent(
                targetState = when {
                    isInLibrary -> "in_library"
                    addToLibraryState is AddToLibraryState.Loading -> "loading"
                    addToLibraryState is AddToLibraryState.Error -> "error"
                    addToLibraryState is AddToLibraryState.Success -> "success"
                    else -> "idle"
                },
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = tween(300)
                    ) togetherWith fadeOut(animationSpec = tween(200)) + scaleOut(
                        targetScale = 0.8f,
                        animationSpec = tween(200)
                    )
                },
                label = "button_state_transition"
            ) { targetState ->
                when (targetState) {
                    "in_library" -> {
                        // Show library status indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "In Your Library",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    "loading" -> {
                        // Show loading state
                        PrimaryButton(
                            text = "Adding...",
                            enabled = false,
                            onClick = {}
                        )
                    }
                    "error" -> {
                        // Show error with retry button
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            PrimaryButton(
                                text = "Retry",
                                onClick = onAddToLibrary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = (addToLibraryState as? AddToLibraryState.Error)?.message ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                    "success" -> {
                        // Show success message briefly
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = (addToLibraryState as? AddToLibraryState.Success)?.message ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    else -> {
                        // Show add button
                        PrimaryButton(
                            text = "Add to Library",
                            onClick = onAddToLibrary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        GameDescriptionSection(game = game)
        Spacer(modifier = Modifier.height(24.dp))
        UserRatingSection()
        Spacer(modifier = Modifier.height(32.dp))
    }
}
