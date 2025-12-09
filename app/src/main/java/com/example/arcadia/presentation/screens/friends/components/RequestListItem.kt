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
import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.rememberResponsiveDimens
import com.example.arcadia.util.TimestampFormatter

/**
 * A list item displaying an incoming friend request with Accept and Decline buttons.
 * Responsive design that adapts to all screen sizes.
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
    val dimens = rememberResponsiveDimens()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimens.horizontalPadding,
                vertical = dimens.cardVerticalPadding
            )
            .clickable(enabled = !isProcessing, onClick = onClick),
        shape = RoundedCornerShape(dimens.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1B41)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimens.cardElevation)
    ) {
        // Use Column layout on compact screens for better button visibility
        if (dimens.isCompact && dimens.screenWidth < 380.dp) {
            // Compact vertical layout for small screens
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.cardHorizontalPadding)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar
                    FriendAvatar(
                        imageUrl = request.fromProfileImageUrl,
                        username = request.fromUsername,
                        size = dimens.avatarMedium,
                        dimens = dimens
                    )
                    
                    Spacer(modifier = Modifier.width(dimens.itemSpacing))
                    
                    // Username and timestamp
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = request.fromUsername,
                            color = TextSecondary,
                            fontSize = dimens.fontSizeMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(dimens.paddingXSmall))
                        
                        Text(
                            text = TimestampFormatter.format(request.createdAt),
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontSize = dimens.fontSizeSmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(dimens.itemSpacing))
                
                // Action buttons in full width row
                RequestActionButtons(
                    isProcessing = isProcessing,
                    isOffline = isOffline,
                    onAccept = onAccept,
                    onDecline = onDecline,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Standard horizontal layout for larger screens
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.cardHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                FriendAvatar(
                    imageUrl = request.fromProfileImageUrl,
                    username = request.fromUsername,
                    size = dimens.avatarMedium,
                    dimens = dimens
                )
                
                Spacer(modifier = Modifier.width(dimens.itemSpacing))
                
                // Username and timestamp
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.fromUsername,
                        color = TextSecondary,
                        fontSize = dimens.fontSizeMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(dimens.paddingXSmall))
                    
                    Text(
                        text = TimestampFormatter.format(request.createdAt),
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = dimens.fontSizeSmall
                    )
                }
                
                Spacer(modifier = Modifier.width(dimens.itemSpacing))
                
                // Action buttons
                RequestActionButtons(
                    isProcessing = isProcessing,
                    isOffline = isOffline,
                    onAccept = onAccept,
                    onDecline = onDecline
                )
            }
        }
    }
}

/**
 * Action buttons for accepting or declining a friend request.
 */
@Composable
private fun RequestActionButtons(
    isProcessing: Boolean,
    isOffline: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = rememberResponsiveDimens()
    
    if (isProcessing) {
        CircularProgressIndicator(
            modifier = Modifier.size(dimens.iconMedium),
            color = ButtonPrimary,
            strokeWidth = 2.dp
        )
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accept button
            Button(
                onClick = onAccept,
                enabled = !isOffline,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimary,
                    contentColor = Surface,
                    disabledContainerColor = ButtonPrimary.copy(alpha = 0.5f),
                    disabledContentColor = Surface.copy(alpha = 0.5f)
                ),
                modifier = Modifier.height(dimens.buttonHeightSmall)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Accept",
                    modifier = Modifier.size(dimens.iconSmall)
                )
                if (!dimens.isCompact || dimens.screenWidth >= 380.dp) {
                    Spacer(modifier = Modifier.width(dimens.paddingXSmall))
                    Text(
                        text = "Accept",
                        fontSize = dimens.fontSizeSmall
                    )
                }
            }
            
            // Decline button
            OutlinedButton(
                onClick = onDecline,
                enabled = !isOffline,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextSecondary,
                    disabledContentColor = TextSecondary.copy(alpha = 0.5f)
                ),
                modifier = Modifier.height(dimens.buttonHeightSmall)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Decline",
                    modifier = Modifier.size(dimens.iconSmall)
                )
                if (!dimens.isCompact || dimens.screenWidth >= 380.dp) {
                    Spacer(modifier = Modifier.width(dimens.paddingXSmall))
                    Text(
                        text = "Decline",
                        fontSize = dimens.fontSizeSmall
                    )
                }
            }
        }
    }
}
