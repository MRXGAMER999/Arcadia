package com.example.arcadia.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.arcadia.R
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.FontSize
import com.example.arcadia.ui.theme.GrayDarker
import com.example.arcadia.ui.theme.TextPrimary

@ExperimentalMaterial3ExpressiveApi
@Composable
fun GoogleButton(
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    primaryText: String = "Sign in with Google",
    secondaryText: String = "Please wait...",
    icon: Int = R.drawable.google_logo,
    shape: Shape = RoundedCornerShape(size = 99.dp),
    backgroundColor: Color = GrayDarker,
    borderColor: Color = ButtonPrimary,
    progressIndicatorColor: Color = ButtonPrimary,
    onClick: () -> Unit
) {
    var buttonText by remember { mutableStateOf(primaryText) }

    LaunchedEffect(loading) {
        buttonText = if (loading) secondaryText else primaryText
    }

    Surface(
        modifier = Modifier
            .clip(shape)
            .border(
                width = 3.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(enabled = !loading) { onClick() },
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .animateContentSize(
                    animationSpec = tween(durationMillis = 200)
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = !loading
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = "Google Logo",
                    tint = Color.Unspecified
                )
            }
            AnimatedVisibility(
                visible = loading
            ) {
                CircularProgressIndicator(
                    color = progressIndicatorColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = buttonText,
                color = TextPrimary,
                fontSize = FontSize.REGULAR
            )
        }

    }
}