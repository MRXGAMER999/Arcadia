package com.example.arcadia.presentation.screens.onBoarding.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.presentation.components.PrimaryButton
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.util.Constants.LAST_ON_BOARDING_PAGE

@Composable
fun FinishButton(
    modifier: Modifier,
    pagerState: PagerState,
    onClick: () -> Unit,
    onNext: () -> Unit = {}
) {
    val isLastPage = pagerState.currentPage == LAST_ON_BOARDING_PAGE
    
    // Animated button content
    AnimatedContent(
        targetState = isLastPage,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) + 
            scaleIn(initialScale = 0.8f) togetherWith
            fadeOut(animationSpec = tween(300)) + 
            scaleOut(targetScale = 0.8f)
        },
        label = "buttonContent"
    ) { isLast ->
        if (isLast) {
            PrimaryButton(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = "Get Started 🚀",
                onClick = onClick
            )
        } else {
            TextButton(
                onClick = onNext,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "Next →",
                    color = ButtonPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
