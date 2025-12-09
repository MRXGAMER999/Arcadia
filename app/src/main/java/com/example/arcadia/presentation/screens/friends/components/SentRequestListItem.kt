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
import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent
import com.example.arcadia.ui.theme.rememberResponsiveDimens
import com.example.arcadia.util.TimestampFormatter

/**
 * A list item displaying a sent (outgoing) friend request with Pending badge and Cancel button.
 * Responsive design that adapts to all screen sizes.
 * 
 * Requirements: 7.2, 7.4, 7.6, 13.4
 * - Display avatar, username, timestamp, Pending badge
 * - Implement Cancel button
 * - Handle tap to navigate to profile
 * - Disable action buttons when offline
 * 
 * @param request The friend request data to display
 * @param isProcessing Whether an action is being processed for this request
 * @param isOffline Whether the device is currently offline
 * @param onCancel Callback when Cancel button is tapped
 * @param onClick Callback when the item is tapped (navigate to profile)
 * @param modifier Modifier for customization
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
        // Use Column layout on very compact screens
        if (dimens.isCompact && dimens.screenWidth < 360.dp) {
            // Compact vertical layout for very small screens
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
                        imageUrl = request.toProfileImageUrl,
                        username = request.toUsername,
                        size = dimens.avatarMedium,
                        dimens = dimens
                    )
                    
                    Spacer(modifier = Modifier.width(dimens.itemSpacing))
                    
                    // Username, timestamp, and Pending badge
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall)
                        ) {
                            Text(
                                text = request.toUsername,
                                color = TextSecondary,
                                fontSize = dimens.fontSizeMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            
                            PendingBadge()
                        }
                        
                        Spacer(modifier = Modifier.height(dimens.paddingXSmall))
                        
                        Text(
                            text = TimestampFormatter.format(request.createdAt),
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontSize = dimens.fontSizeSmall
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(dimens.itemSpacing))
                
                // Cancel button full width
                SentRequestCancelButton(
                    isProcessing = isProcessing,
                    isOffline = isOffline,
                    onCancel = onCancel,
                    showText = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Standard horizontal layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.cardHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                FriendAvatar(
                    imageUrl = request.toProfileImageUrl,
                    username = request.toUsername,
                    size = dimens.avatarMedium,
                    dimens = dimens
                )
                
                Spacer(modifier = Modifier.width(dimens.itemSpacing))
                
                // Username, timestamp, and Pending badge
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall)
                    ) {
                        Text(
                            text = request.toUsername,
                            color = TextSecondary,
                            fontSize = dimens.fontSizeMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        PendingBadge()
                    }
                    
                    Spacer(modifier = Modifier.height(dimens.paddingXSmall))
                    
                    Text(
                        text = TimestampFormatter.format(request.createdAt),
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = dimens.fontSizeSmall
                    )
                }
                
                Spacer(modifier = Modifier.width(dimens.itemSpacing))
                
                // Cancel button
                SentRequestCancelButton(
                    isProcessing = isProcessing,
                    isOffline = isOffline,
                    onCancel = onCancel,
                    showText = !dimens.isCompact || dimens.screenWidth >= 400.dp
                )
            }
        }
    }
}

/**
 * Cancel button for sent requests.
 */
@Composable
private fun SentRequestCancelButton(
    isProcessing: Boolean,
    isOffline: Boolean,
    onCancel: () -> Unit,
    showText: Boolean,
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
        OutlinedButton(
            onClick = onCancel,
            enabled = !isOffline,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextSecondary,
                disabledContentColor = TextSecondary.copy(alpha = 0.5f)
            ),
            modifier = modifier.height(dimens.buttonHeightSmall)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel",
                modifier = Modifier.size(dimens.iconSmall)
            )
            if (showText) {
                Spacer(modifier = Modifier.width(dimens.paddingXSmall))
                Text(
                    text = "Cancel",
                    fontSize = dimens.fontSizeSmall
                )
            }
        }
    }
}

/**
 * Pending status badge displayed next to the username.
 * Responsive sizing based on screen dimensions.
 * 
 * Requirements: 7.2
 */
@Composable
private fun PendingBadge() {
    val dimens = rememberResponsiveDimens()
    
    Row(
        modifier = Modifier
            .background(
                color = YellowAccent.copy(alpha = 0.2f),
                shape = RoundedCornerShape(dimens.paddingXSmall)
            )
            .padding(
                horizontal = dimens.paddingSmall,
                vertical = dimens.paddingXSmall
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimens.paddingXSmall)
    ) {
        Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = null,
            tint = YellowAccent,
            modifier = Modifier.size(dimens.iconSmall * 0.75f)
        )
        Text(
            text = "Pending",
            color = YellowAccent,
            fontSize = dimens.fontSizeSmall * 0.85f,
            fontWeight = FontWeight.Medium
        )
    }
}
