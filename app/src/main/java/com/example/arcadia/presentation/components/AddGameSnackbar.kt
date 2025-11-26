package com.example.arcadia.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary

/**
 * Snackbar shown when a game is added to the library.
 * Features an undo button to remove the game if clicked within the timeout period.
 * Styled to match the deletion snackbar in MyGamesScreen for consistency.
 *
 * @param visible Whether the snackbar is visible
 * @param gameName The name of the game that was added
 * @param onUndo Callback when user clicks Undo
 * @param onDismiss Callback when snackbar is dismissed
 * @param modifier Modifier for positioning
 */
@Composable
fun AddGameSnackbar(
    visible: Boolean,
    gameName: String,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(durationMillis = 150)
        ) + fadeIn(animationSpec = tween(durationMillis = 150)),
        exit = slideOutVertically(
            targetOffsetY = { it / 2 },
            animationSpec = tween(durationMillis = 100)
        ) + fadeOut(animationSpec = tween(durationMillis = 100)),
        modifier = modifier
    ) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = Color(0xFF1E2A47),
            contentColor = TextSecondary,
            action = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onUndo) {
                        Text(
                            text = "UNDO",
                            color = ButtonPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissAction = {
                IconButton(onClick = onDismiss) {
                    Text(
                        text = "✕",
                        color = TextSecondary,
                        fontSize = 18.sp
                    )
                }
            }
        ) {
            Text(
                text = "$gameName added",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
