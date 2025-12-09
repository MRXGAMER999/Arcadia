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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF252B3B)),
                contentAlignment = Alignment.Center
            ) {
                if (friend.profileImageUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(friend.profileImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${friend.username}'s avatar",
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
                            DefaultAvatar()
                        }
                    )
                } else {
                    DefaultAvatar()
                }
            }
            
            // Username
            Text(
                text = friend.username,
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Default avatar icon when no profile image is available.
 */
@Composable
private fun DefaultAvatar() {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = null,
        tint = ButtonPrimary.copy(alpha = 0.5f),
        modifier = Modifier.size(28.dp)
    )
}
