package com.example.arcadia.presentation.screens.onBoarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.domain.model.OnBoardingPage
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.EXTRA_LARGE_PADDING
import com.example.arcadia.ui.theme.MEDIUM_PADDING
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextPrimary
import com.example.arcadia.ui.theme.YellowAccent
import com.example.arcadia.util.Constants.LAST_ON_BOARDING_PAGE
import com.example.arcadia.util.Constants.ON_BOARDING_PAGE_COUNT

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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Image and text content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { position ->
                    PagerScreen(onBoardingPage = pages[position])
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Page indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    val animatedColor by animateColorAsState(
                        targetValue = if (isSelected) YellowAccent else Color.White,
                        label = "Page indicator color"
                    )
                    val animatedSize by animateDpAsState(
                        targetValue = if (isSelected) 32.dp else 25.dp,
                        label = "Page indicator size"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clip(CircleShape)
                            .background(color = animatedColor)
                            .size(animatedSize)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                FinishButton(
                    modifier = Modifier.fillMaxWidth(),
                    pagerState = pagerState,
                    onClick = onFinish
                )
            }
        }
    }
}

@Composable
fun PagerScreen(onBoardingPage: OnBoardingPage) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier
                    .width(284.32.dp)
                    .height(228.55.dp),
                painter = painterResource(id = onBoardingPage.image),
                contentDescription = "Onboarding Image"
            )
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EXTRA_LARGE_PADDING),
                text = onBoardingPage.description,
                fontSize = 24.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

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
            Button(
                onClick = onClick,
                colors = ButtonColors(
                    containerColor = ButtonPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = ButtonPrimary,
                    disabledContentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EXTRA_LARGE_PADDING)
            ) {
                Text(
                    text = "Get Started",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
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