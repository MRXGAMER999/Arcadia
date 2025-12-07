package com.example.arcadia.presentation.screens.profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary

private val CardBackground = Color(0xFF0A1F4D)
private val CardBorder = Color(0xFF1E3A8A)

@Composable
fun BioCard(bio: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "ABOUT", fontSize = 16.sp, fontFamily = BebasNeueFont, color = ButtonPrimary, letterSpacing = 1.5.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = bio, fontSize = 14.sp, color = TextSecondary.copy(alpha = 0.9f), lineHeight = 20.sp)
        }
    }
}
