package com.example.arcadia.presentation.screens.roast.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.domain.model.ai.Badge

/**
 * Badge selector component allowing users to select badges to feature on their profile.
 * Displays selectable badges with a count indicator showing selected/max badges.
 * 
 * Requirements: 8.1 - WHEN a user views their generated badges on the Roast Screen 
 * THEN the System SHALL allow selection of badges to feature
 * 
 * Requirements: 8.2 - WHEN selecting featured badges THEN the System SHALL enforce 
 * a maximum of three badges
 * 
 * Requirements: 8.3 - WHEN a user confirms featured badge selection THEN the System 
 * SHALL save the selection to Appwrite
 * 
 * Requirements: 8.5 - WHEN selecting badges THEN the System SHALL display a count 
 * indicator showing selected badges out of maximum allowed
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgeSelector(
    badges: List<Badge>,
    selectedBadges: List<Badge>,
    onBadgeClick: (Badge) -> Unit,
    onSaveBadges: () -> Unit,
    isSaving: Boolean = false,
    isSaved: Boolean = false,
    isFriendRoast: Boolean = false,
    maxBadges: Int = 3,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2A47)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header with count indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🏆 Your Badges",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isFriendRoast) "Badges generated for your friend" 
                               else "Select up to $maxBadges to feature on your profile",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
                
                // Count indicator (Requirements: 8.5) - only show for self-roasts
                if (!isFriendRoast) {
                    BadgeCountIndicator(
                        selected = selectedBadges.size,
                        max = maxBadges
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Badge grid
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                badges.forEach { badge ->
                    val isSelected = selectedBadges.contains(badge)
                    val canSelect = isSelected || selectedBadges.size < maxBadges
                    
                    SelectableBadgeItem(
                        badge = badge,
                        isSelected = isSelected,
                        enabled = if (isFriendRoast) false else canSelect,
                        onClick = { if (!isFriendRoast) onBadgeClick(badge) }
                    )
                }
            }
            
            // Save button - only show for self-roasts with selected badges (Requirements: 8.3)
            if (!isFriendRoast && selectedBadges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onSaveBadges,
                    enabled = !isSaving && !isSaved,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaved) Color(0xFF4ADE80) else Color(0xFF6366F1),
                        disabledContainerColor = if (isSaved) Color(0xFF4ADE80) else Color(0xFF444444)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saving...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else if (isSaved) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saved!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = "💾 Save to Profile",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeCountIndicator(
    selected: Int,
    max: Int
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected == max) Color(0xFF4ADE80).copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.1f)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$selected/$max selected",
            color = if (selected == max) Color(0xFF4ADE80) else Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}


@Composable
private fun SelectableBadgeItem(
    badge: Badge,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFF4ADE80).copy(alpha = 0.2f)
            else -> Color.White.copy(alpha = 0.05f)
        },
        label = "badge_bg"
    )
    
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFF4ADE80)
            else -> Color.White.copy(alpha = 0.2f)
        },
        label = "badge_border"
    )
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji
            Text(
                text = badge.emoji,
                fontSize = 18.sp
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // Title
            Column {
                Text(
                    text = badge.title,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = badge.reason,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            
            // Selection indicator
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4ADE80)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
