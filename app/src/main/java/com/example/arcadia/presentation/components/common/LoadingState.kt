package com.example.arcadia.presentation.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary

/**
 * A reusable loading state component for displaying loading indicators across screens.
 * 
 * @param modifier Modifier for customization
 * @param message Optional message to display below the spinner
 * @param indicatorColor Color of the loading indicator
 * @param indicatorSize Size of the loading indicator
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    message: String? = null,
    indicatorColor: Color = ButtonPrimary,
    indicatorSize: Dp = 48.dp
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(indicatorSize),
                color = indicatorColor,
                strokeWidth = 3.dp
            )
            
            if (message != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

/**
 * A compact version of LoadingState for use in cards or smaller containers.
 */
@Composable
fun CompactLoadingState(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = ButtonPrimary,
            strokeWidth = 2.dp
        )
        
        if (message != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * An inline loading indicator with text.
 */
@Composable
fun InlineLoadingIndicator(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = ButtonPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                color = TextSecondary.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
    }
}
