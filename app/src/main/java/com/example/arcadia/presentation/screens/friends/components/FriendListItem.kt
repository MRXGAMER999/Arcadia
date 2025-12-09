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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.arcadia.domain.model.friend.Friend
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary

/**
 * A list item displaying a friend's avatar and username.
 * 
 * Requirements: 1.1, 1.2
 * - Display avatar and username
 * - Handle tap to navigate to profile
 */
@Composable
fun FriendListItem(
    friend: Friend,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            FriendAvatar(
                imageUrl = friend.profileImageUrl,
                username = friend.username,
                size = 48.dp
            )
            
            // Username
            Text(
                text = friend.username,
                color = TextSecondary,
                fontSize = 16.sp,
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
                modifier = Modifier.size(24.dp)
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
                    DefaultAvatar(size = size)
                }
            )
        } else {
            DefaultAvatar(size = size)
        }
    }
}

/**
 * Default avatar icon when no profile image is available.
 */
@Composable
internal fun DefaultAvatar(size: Dp) {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = null,
        tint = ButtonPrimary.copy(alpha = 0.5f),
        modifier = Modifier.size(size * 0.6f)
    )
}
