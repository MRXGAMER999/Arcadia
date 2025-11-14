package com.example.arcadia.presentation.screens.onBoarding.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.arcadia.ui.theme.YellowAccent

@Composable
fun PageIndicators(pagerState: PagerState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pagerState.pageCount) { iteration ->
            val isSelected = pagerState.currentPage == iteration
            
            // Color animation
            val animatedColor by animateColorAsState(
                targetValue = if (isSelected) YellowAccent else Color.White.copy(alpha = 0.5f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "Page indicator color"
            )
            
            // Width animation for selected indicator
            val animatedWidth by animateDpAsState(
                targetValue = if (isSelected) 40.dp else 12.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "Page indicator width"
            )
            
            // Scale animation
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "Page indicator scale"
            )
            
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color = animatedColor)
                    .height(12.dp)
                    .width(animatedWidth)
                    .scale(scale)
            )
        }
    }
}
