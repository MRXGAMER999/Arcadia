package com.example.arcadia.presentation.screens.authScreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.arcadia.R
import com.example.arcadia.ui.theme.FontSize
import com.example.arcadia.ui.theme.RobotoCondensedFont
import com.example.arcadia.ui.theme.TextSecondary

@Composable
fun AuthContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier,
            painter = painterResource(R.drawable.logo),
            contentDescription = "App Logo",
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = "Sign in to Continue",
            textAlign = TextAlign.Center,
            fontFamily = RobotoCondensedFont,
            fontWeight = FontWeight.Bold,
            fontSize = FontSize.EXTRA_LARGE,
            color = TextSecondary
        )
    }
}
