package com.example.arcadia.presentation.screens.friends.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent
import com.example.arcadia.util.TimestampFormatter

/**
 * A list item displaying a sent (outgoing) friend request with Pending badge and Cancel button.
 * 
 * Requirements: 7.2, 7.4, 7.6, 13.4
 * - Display avatar, username, timestamp, Pending badge
 * - Implement Cancel button
 * - Handle tap to navigate to profile
 * - Disable action buttons when offline
 */
@Composable
fun SentRequestListItem(
    request: FriendRequest,
    isProcessing: Boolean,
    isOffline: Boolean = false,
    onCancel: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(enabled = !isProcessing, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1B41)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Avatar + Username + Pending badge + Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                FriendAvatar(
                    imageUrl = request.toProfileImageUrl,
                    username = request.toUsername,
                    size = 48.dp
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Username, timestamp, and Pending badge - takes full available space
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = request.toUsername,
                            color = TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        PendingBadge()
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = TimestampFormatter.format(request.createdAt),
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Row 2: Cancel button (aligned to the right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = ButtonPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isOffline,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary,
                            disabledContentColor = TextSecondary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cancel",
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pending status badge displayed next to the username.
 * 
 * Requirements: 7.2
 */
@Composable
private fun PendingBadge() {
    Row(
        modifier = Modifier
            .background(
                color = YellowAccent.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = null,
            tint = YellowAccent,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "Pending",
            color = YellowAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
