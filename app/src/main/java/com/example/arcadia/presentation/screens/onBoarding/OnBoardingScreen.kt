package com.example.arcadia.presentation.screens.onBoarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.arcadia.domain.model.OnBoardingPage
import com.example.arcadia.presentation.screens.onBoarding.components.FinishButton
import com.example.arcadia.presentation.screens.onBoarding.components.PageIndicators
import com.example.arcadia.presentation.screens.onBoarding.components.PagerScreen
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.util.Constants.ON_BOARDING_PAGE_COUNT
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun OnBoardingScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnBoardingPage.First,
        OnBoardingPage.Second,
        OnBoardingPage.Third
    )
    val pagerState = rememberPagerState(initialPage = 0) { ON_BOARDING_PAGE_COUNT }
    val coroutineScope = rememberCoroutineScope()
    
    // Animation for initial appearance
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        // Skip button at the top
        AnimatedVisibility(
            visible = pagerState.currentPage < pages.size - 1,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            TextButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pages.size - 1)
                    }
                }
            ) {
                Text(
                    text = "Skip",
                    color = ButtonPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Pager with parallax effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    pageSpacing = 16.dp
                ) { page ->
                    // Calculate page offset for parallax effect
                    val pageOffset = (
                        (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    ).absoluteValue
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                // Parallax effect
                                alpha = lerp(
                                    start = 0.5f,
                                    stop = 1f,
                                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                )
                                // Scale effect
                                val scale = lerp(
                                    start = 0.85f,
                                    stop = 1f,
                                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                )
                                scaleX = scale
                                scaleY = scale
                                
                                // Slight rotation for fun
                                rotationY = lerp(
                                    start = 0f,
                                    stop = 15f,
                                    fraction = pageOffset.coerceIn(0f, 1f)
                                ) * if (pagerState.currentPage > page) -1f else 1f
                            }
                    ) {
                        PagerScreen(onBoardingPage = pages[page])
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Page indicators with animation
            PageIndicators(pagerState = pagerState)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Navigation buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous button
                    AnimatedVisibility(
                        visible = pagerState.currentPage > 0,
                        enter = fadeIn() + slideInHorizontally(),
                        exit = fadeOut() + slideOutHorizontally()
                    ) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        ) {
                            Text(
                                text = "← Back",
                                color = TextSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Next/Finish button
                    FinishButton(
                        modifier = Modifier,
                        pagerState = pagerState,
                        onClick = onFinish,
                        onNext = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun OnBoardingScreenPreview() {
    OnBoardingScreen(
        onFinish = {}
    )
}
