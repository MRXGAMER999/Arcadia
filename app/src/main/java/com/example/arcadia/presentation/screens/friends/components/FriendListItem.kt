package com.example.arcadia.presentation.screens.friends.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.arcadia.domain.model.friend.Friend
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.ResponsiveDimens
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.rememberResponsiveDimens

/**
 * A list item displaying a friend's avatar and username.
 * Responsive design that adapts to all screen sizes.
 * 
 * Requirements: 1.1, 1.2
 * - Display avatar and username
 * - Handle tap to navigate to profile
 * 
 * @param friend The friend data to display
 * @param onClick Callback when the item is tapped
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FriendListItem(
    friend: Friend,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalPlatformContext.current
    val dimens = rememberResponsiveDimens()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimens.horizontalPadding,
                vertical = dimens.cardVerticalPadding
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(dimens.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1B41)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimens.cardElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.cardHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.itemSpacing)
        ) {
            // Avatar
            FriendAvatar(
                imageUrl = friend.profileImageUrl,
                username = friend.username,
                size = dimens.avatarMedium,
                dimens = dimens
            )
            
            // Username
            Text(
                text = friend.username,
                color = TextSecondary,
                fontSize = dimens.fontSizeMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            // Navigation indicator
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(dimens.iconMedium)
            )
        }
    }
}

/**
 * Reusable avatar component with loading and error states.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FriendAvatar(
    imageUrl: String?,
    username: String,
    size: Dp,
    dimens: ResponsiveDimens,
    modifier: Modifier = Modifier
) {
    val context = LocalPlatformContext.current
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF252B3B)),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "$username's avatar",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier
                            .size(size)
                            .background(Color(0xFF252B3B)),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator(
                            color = ButtonPrimary,
                            modifier = Modifier.size(size / 2)
                        )
                    }
                },
                error = {
                    DefaultAvatar(size = size, dimens = dimens)
                }
            )
        } else {
            DefaultAvatar(size = size, dimens = dimens)
        }
    }
}

/**
 * Default avatar icon when no profile image is available.
 */
@Composable
internal fun DefaultAvatar(
    size: Dp,
    dimens: ResponsiveDimens
) {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = null,
        tint = ButtonPrimary.copy(alpha = 0.5f),
        modifier = Modifier.size(size * 0.6f)
    )
}
