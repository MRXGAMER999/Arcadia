package com.example.arcadia.presentation.screens.roast.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Error type enum for different roast error scenarios.
 */
enum class RoastErrorType {
    NETWORK,
    INSUFFICIENT_STATS,
    AI_FAILURE,
    UNKNOWN
}

/**
 * Error state component for the Roast Screen with friendly messages and retry button.
 * Displays different messages based on error type (network, insufficient stats, etc.)
 * 
 * Requirements: 13.1 - WHEN AI roast generation fails THEN the System SHALL display 
 * an error message to the user
 * 
 * Requirements: 13.2 - WHEN an error occurs THEN the System SHALL provide a retry option
 * 
 * Requirements: 13.3 - WHEN the network is unavailable and no cached roast exists 
 * THEN the System SHALL inform the user that internet is required
 */
@Composable
fun RoastErrorState(
    errorMessage: String,
    errorType: RoastErrorType = RoastErrorType.UNKNOWN,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (emoji, title, description) = getErrorContent(errorType, errorMessage)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0A0A),
                        Color(0xFF0A1929)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Error emoji
            Text(
                text = emoji,
                fontSize = 64.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Error title
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Error description
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Retry button (Requirements: 13.2)
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B35)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = getRetryButtonText(errorType),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

/**
 * Returns appropriate error content based on error type.
 */
private fun getErrorContent(
    errorType: RoastErrorType,
    errorMessage: String
): Triple<String, String, String> {
    return when (errorType) {
        RoastErrorType.NETWORK -> Triple(
            "📡",
            "No Internet Connection",
            "We need internet to roast you properly. Check your connection and try again."
        )
        RoastErrorType.INSUFFICIENT_STATS -> Triple(
            "📊",
            "Not Enough Data",
            errorMessage.ifEmpty { 
                "Add more games to your library first! You need at least 3 games and 5 hours played to get roasted."
            }
        )
        RoastErrorType.AI_FAILURE -> Triple(
            "🤖",
            "AI Had a Moment",
            "Our roast generator is taking a break. Give it a moment and try again."
        )
        RoastErrorType.UNKNOWN -> Triple(
            "😅",
            "Oops, Something Went Wrong",
            errorMessage.ifEmpty { "We couldn't generate your roast. Please try again." }
        )
    }
}

/**
 * Returns appropriate retry button text based on error type.
 */
private fun getRetryButtonText(errorType: RoastErrorType): String {
    return when (errorType) {
        RoastErrorType.NETWORK -> "Try Again"
        RoastErrorType.INSUFFICIENT_STATS -> "Got It"
        RoastErrorType.AI_FAILURE -> "Retry"
        RoastErrorType.UNKNOWN -> "Try Again"
    }
}

/**
 * Helper function to determine error type from error message.
 */
fun determineErrorType(errorMessage: String?, hasInsufficientStats: Boolean): RoastErrorType {
    return when {
        hasInsufficientStats -> RoastErrorType.INSUFFICIENT_STATS
        errorMessage?.contains("network", ignoreCase = true) == true -> RoastErrorType.NETWORK
        errorMessage?.contains("internet", ignoreCase = true) == true -> RoastErrorType.NETWORK
        errorMessage?.contains("connection", ignoreCase = true) == true -> RoastErrorType.NETWORK
        errorMessage?.contains("AI", ignoreCase = true) == true -> RoastErrorType.AI_FAILURE
        errorMessage?.contains("generate", ignoreCase = true) == true -> RoastErrorType.AI_FAILURE
        else -> RoastErrorType.UNKNOWN
    }
}
