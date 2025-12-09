package com.example.arcadia.presentation.screens.friends.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.util.TimestampFormatter

/**
 * A list item displaying an incoming friend request with Accept and Decline buttons.
 * 
 * Requirements: 6.4, 6.5, 6.6, 6.8, 6.13, 6.14, 13.4
 * - Display avatar, username, timestamp
 * - Implement Accept and Decline buttons
 * - Handle tap to navigate to profile
 * - Disable action buttons when offline
 * 
 * @param request The friend request data to display
 * @param isProcessing Whether an action is being processed for this request
 * @param isOffline Whether the device is currently offline
 * @param onAccept Callback when Accept button is tapped
 * @param onDecline Callback when Decline button is tapped
 * @param onClick Callback when the item is tapped (navigate to profile)
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val context = LocalPlatformContext.current
    
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar - Requirements: 6.4
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF252B3B)),
                contentAlignment = Alignment.Center
            ) {
                if (request.fromProfileImageUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(request.fromProfileImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${request.fromUsername}'s avatar",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF252B3B)),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator(
                                    color = ButtonPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        error = {
                            DefaultRequestAvatar()
                        }
                    )
                } else {
                    DefaultRequestAvatar()
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Username and timestamp - Requirements: 6.4, 6.5, 6.6
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = request.fromUsername,
                    color = TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = TimestampFormatter.format(request.createdAt),
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Action buttons - Requirements: 6.8, 6.13, 13.4
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = ButtonPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Accept button - Requirements: 6.8, 13.4
                    Button(
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
                    }
                    
                    // Decline button - Requirements: 6.13, 13.4
                    OutlinedButton(
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
                    }
                }
            }
        }
    }
}

/**
 * Default avatar icon when no profile image is available.
 */
@Composable
private fun DefaultRequestAvatar() {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = null,
        tint = ButtonPrimary.copy(alpha = 0.5f),
        modifier = Modifier.size(28.dp)
    )
}
