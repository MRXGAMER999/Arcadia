package com.example.arcadia.presentation.screens.friends.components

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
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
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.util.TimestampFormatter

import com.example.arcadia.presentation.components.common.PremiumScaleButton
import com.example.arcadia.presentation.components.common.PremiumScaleOutlinedButton
import com.example.arcadia.presentation.components.common.PremiumScaleWrapper

/**
 * A list item displaying an incoming friend request with Accept and Decline buttons.
 * 
 * Requirements: 6.4, 6.5, 6.6, 6.8, 6.13, 6.14, 13.4
 * - Display avatar, username, timestamp
 * - Implement Accept and Decline buttons
 * - Handle tap to navigate to profile
 * - Disable action buttons when offline
 */
@Composable
fun RequestListItem(
    request: FriendRequest,
    isProcessing: Boolean,
    isOffline: Boolean = false,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PremiumScaleWrapper(
        onClick = onClick,
        enabled = !isProcessing,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
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
                // Row 1: Avatar + Username + Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    FriendAvatar(
                        imageUrl = request.fromProfileImageUrl,
                        username = request.fromUsername,
                        size = 48.dp
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Username and timestamp - takes full available space
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = request.fromUsername,
                            color = TextSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
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
                
                // Row 2: Action buttons (aligned to the right)
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
                        // Decline button
                        PremiumScaleOutlinedButton(
                            onClick = onDecline,
                            enabled = !isOffline,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextSecondary,
                                disabledContentColor = TextSecondary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Decline",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Decline",
                                fontSize = 14.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Accept button
                        PremiumScaleButton(
                            onClick = onAccept,
                            enabled = !isOffline,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonPrimary,
                                contentColor = Surface,
                                disabledContainerColor = ButtonPrimary.copy(alpha = 0.5f),
                                disabledContentColor = Surface.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Accept",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Accept",
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
