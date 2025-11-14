package com.example.arcadia.presentation.screens.onBoarding.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.domain.model.OnBoardingPage
import com.example.arcadia.ui.theme.EXTRA_LARGE_PADDING
import com.example.arcadia.ui.theme.RobotoCondensedFont
import com.example.arcadia.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun PagerScreen(onBoardingPage: OnBoardingPage) {
    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(onBoardingPage) {
        isVisible = false
        delay(100)
        isVisible = true
    }
    
    // Bouncing animation for image
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    // Rotation animation for image
    val rotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated image
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600)) + 
                        scaleIn(
                            initialScale = 0.8f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ),
                exit = fadeOut() + scaleOut()
            ) {
                Image(
                    modifier = Modifier
                        .width(284.32.dp)
                        .height(228.55.dp)
                        .graphicsLayer {
                            translationY = bounceOffset
                            rotationZ = rotation
                        },
                    painter = painterResource(id = onBoardingPage.image),
                    contentDescription = "Onboarding Image"
                )
            }
            
            Spacer(modifier = Modifier.height(56.dp))

            // Animated text
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + 
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ),
                exit = fadeOut() + slideOutVertically()
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EXTRA_LARGE_PADDING),
                    text = onBoardingPage.description,
                    fontSize = 22.sp,
                    fontFamily = RobotoCondensedFont,
                    lineHeight = 28.sp,
                    letterSpacing = 0.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
