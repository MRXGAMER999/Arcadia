package com.example.arcadia.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmptyState(
    emoji: String = "🎮",
    title: String = "Nothing here yet",
    message: String = "Start exploring to add games!",
    modifier: Modifier = Modifier
) {
    // Bouncing animation for emoji
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state_bounce")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    // Scale animation for emoji
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Fade in animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "alpha"
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated emoji
        Text(
            text = emoji,
            fontSize = 120.sp,
            modifier = Modifier
                .offset(y = bounceOffset.dp)
                .scale(scale)
                .graphicsLayer { this.alpha = alpha }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Title
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFDCDCDC),
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { this.alpha = alpha }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Message
        Text(
            text = message,
            fontSize = 16.sp,
            color = Color(0xFFDCDCDC).copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { this.alpha = alpha }
        )
    }
}

@Composable
fun SearchEmptyState(modifier: Modifier = Modifier) {
    EmptyState(
        emoji = "🔍",
        title = "No games found",
        message = "Try searching with different keywords or filters",
        modifier = modifier
    )
}

@Composable
fun LibraryEmptyState(modifier: Modifier = Modifier) {
    EmptyState(
        emoji = "📚",
        title = "Your library is empty",
        message = "Add games to your library to see them here!",
        modifier = modifier
    )
}

@Composable
fun GenericEmptyState(
    emoji: String = "🎯",
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    EmptyState(
        emoji = emoji,
        title = title,
        message = message,
        modifier = modifier
    )
}

