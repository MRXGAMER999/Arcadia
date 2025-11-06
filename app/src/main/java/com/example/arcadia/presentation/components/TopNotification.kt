package com.example.arcadia.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun TopNotification(
    visible: Boolean,
    message: String,
    isSuccess: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    durationMillis: Long = 2000L,
    color: Color = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFE53935)
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(durationMillis)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(color)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .systemBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color.White
            )
        }
    }
}

