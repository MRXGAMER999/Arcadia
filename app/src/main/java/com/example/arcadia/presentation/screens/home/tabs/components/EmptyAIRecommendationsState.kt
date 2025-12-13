package com.example.arcadia.presentation.screens.home.tabs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.R
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary

/**
 * Empty state component shown when AI recommendations cannot be generated.
 * Shows different messages based on whether the library is empty or if the AI simply has no results.
 */
@Composable
fun EmptyAIRecommendationsState(
    isLibraryEmpty: Boolean,
    onAddGames: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(
                id = if (isLibraryEmpty) R.drawable.crafting_table else R.drawable.ai_controller
            ),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(48.dp)
        )
        
        Text(
            text = if (isLibraryEmpty) {
                "Add games to unlock AI recommendations"
            } else {
                "No recommendations available"
            },
            color = TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = if (isLibraryEmpty) {
                "The AI needs to learn your gaming preferences. Add some games to your library and we'll recommend similar titles you'll love!"
            } else {
                "We couldn't generate recommendations at this time. Please try again later."
            },
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        if (isLibraryEmpty && onAddGames != null) {
            TextButton(
                onClick = onAddGames,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Browse Games",
                    color = ButtonPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (!isLibraryEmpty && onRetry != null) {
            TextButton(
                onClick = onRetry,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Try Again",
                    color = ButtonPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
