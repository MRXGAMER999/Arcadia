package com.example.arcadia.presentation.screens.profile.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.R
import com.example.arcadia.domain.model.ai.Badge
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.YellowAccent

private val CardBackground = Color(0xFF0A1F4D)
private val CardBorder = Color(0xFF1E3A8A)
private val BadgeBackground = Color(0xFF1A2F5A)
private val TextSecondary = Color(0xFFE0E0E0)

/**
 * Displays featured badges on a user's profile.
 * Shows each badge with its emoji and title in a horizontal flow layout.
 * 
 * Requirements: 9.1, 9.2, 9.3
 * - 9.1: Display badges in a dedicated section when viewing a profile with featured badges
 * - 9.2: Show each badge with its emoji and title
 * - 9.3: Hide the section when user has no featured badges (handled by caller)
 * 
 * @param badges List of featured badges to display (max 3)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgesSection(
    badges: List<Badge>,
    modifier: Modifier = Modifier
) {
    // Don't render anything if no badges (Requirement 9.3)
    if (badges.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.controller),
                    contentDescription = null,
                    tint = YellowAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FEATURED BADGES",
                    fontSize = 16.sp,
                    fontFamily = BebasNeueFont,
                    color = YellowAccent,
                    letterSpacing = 1.5.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Badges in a flow row (Requirement 9.1, 9.2)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                badges.forEach { badge ->
                    BadgeItem(badge = badge)
                }
            }
        }
    }
}

/**
 * Individual badge item displaying emoji and title.
 * Requirement 9.2: Show each badge with its emoji and title
 */
@Composable
private fun BadgeItem(
    badge: Badge,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = BadgeBackground
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji (Requirement 9.2)
            Text(
                text = badge.emoji,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Title (Requirement 9.2)
            Text(
                text = badge.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}
