package com.example.arcadia.presentation.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.R
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary

private val CardBackground = Color(0xFF0A1F4D)
private val CardBorder = Color(0xFF1E3A8A)
private val NeonBlue = Color(0xFF00D4FF)
private val SteamColor = Color(0xFF1B2838)
private val XboxColor = Color(0xFF107C10)
private val PlayStationColor = Color(0xFF003087)

@Composable
fun GamingPlatformsCard(steamId: String?, xboxGamertag: String?, psnId: String?) {
    val hasAnyPlatform = !steamId.isNullOrEmpty() || !xboxGamertag.isNullOrEmpty() || !psnId.isNullOrEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(painter = painterResource(id = R.drawable.controller), contentDescription = null, tint = NeonBlue, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "GAMING PLATFORMS", fontSize = 18.sp, fontFamily = BebasNeueFont, color = NeonBlue, letterSpacing = 2.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (hasAnyPlatform) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!steamId.isNullOrEmpty()) {
                        PlatformBadge("Steam", steamId, SteamColor, R.drawable.pc_ic)
                    }
                    if (!xboxGamertag.isNullOrEmpty()) {
                        PlatformBadge("Xbox", xboxGamertag, XboxColor, R.drawable.xbox_ic)
                    }
                    if (!psnId.isNullOrEmpty()) {
                        PlatformBadge("PlayStation", psnId, PlayStationColor, R.drawable.playstation_ic)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .border(1.dp, TextSecondary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(24.dp), 
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "No gaming platforms linked", fontSize = 16.sp, color = TextSecondary.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Add your Steam, Xbox, or PSN ID", fontSize = 14.sp, color = ButtonPrimary.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformBadge(platformName: String, gamertag: String, platformColor: Color, iconRes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(platformColor.copy(alpha = 0.15f))
            .border(1.dp, platformColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(platformColor)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape), 
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(id = iconRes), contentDescription = platformName, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = platformName, fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
            Text(text = gamertag, fontSize = 18.sp, color = TextSecondary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
