package com.example.arcadia.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.padding

/**
 * A shared snackbar host that anchors snackbars to the bottom (over FAB/bottom bar)
 * and animates them in/out from the bottom.
 */
@Composable
fun BottomSlideSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbar: @Composable (SnackbarData) -> Unit = { data ->
        Snackbar(
            snackbarData = data,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
) {
    val currentSnackbarData = hostState.currentSnackbarData

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedContent(
            targetState = currentSnackbarData,
            transitionSpec = {
                val enterSpec = spring<androidx.compose.ui.unit.IntOffset>(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
                val exitSpec = spring<androidx.compose.ui.unit.IntOffset>(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )

                (slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = enterSpec
                ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)))
                .togetherWith(
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = exitSpec
                    ) + fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                )
            },
            label = "SnackbarAnimation"
        ) { snackbarData ->
            if (snackbarData != null) {
                snackbar(snackbarData)
            }
        }
    }
}













