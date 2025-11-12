package com.example.arcadia.presentation.screens.onBoarding.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.domain.model.OnBoardingPage
import com.example.arcadia.ui.theme.EXTRA_LARGE_PADDING
import com.example.arcadia.ui.theme.RobotoCondensedFont
import com.example.arcadia.ui.theme.TextSecondary

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
                fontFamily = RobotoCondensedFont,
                lineHeight = 28.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
