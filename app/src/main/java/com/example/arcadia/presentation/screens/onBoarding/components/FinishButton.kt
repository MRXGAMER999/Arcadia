package com.example.arcadia.presentation.screens.onBoarding.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.arcadia.presentation.components.PrimaryButton
import com.example.arcadia.util.Constants.LAST_ON_BOARDING_PAGE

@Composable
fun FinishButton(
    modifier: Modifier,
    pagerState: PagerState,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = pagerState.currentPage == LAST_ON_BOARDING_PAGE
    ) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            PrimaryButton(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = "Get Started",
                onClick = onClick,
            )
        }
    }
}
