package com.example.arcadia.presentation.screens.friends.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.domain.model.friend.FriendshipStatus
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent

/**
 * Debounced button that prevents rapid double-taps.
 * 
 * This addresses the issue where rapid taps could trigger
 * duplicate API calls before the first one completes.
 */
@Composable
fun DebouncedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    debounceMs: Long = 500L,
    colors: androidx.compose.material3.ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    Button(
        onClick = {
            val now = System.currentTimeMillis()
            if (now - lastClickTime >= debounceMs) {
                lastClickTime = now
                onClick()
            }
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content
    )
}

/**
 * Debounced outlined button variant.
 */
@Composable
fun DebouncedOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    debounceMs: Long = 500L,
    colors: androidx.compose.material3.ButtonColors = ButtonDefaults.outlinedButtonColors(),
    content: @Composable RowScope.() -> Unit
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    OutlinedButton(
        onClick = {
            val now = System.currentTimeMillis()
            if (now - lastClickTime >= debounceMs) {
                lastClickTime = now
                onClick()
            }
        },
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content
    )
}

/**
 * Unified friend action button that displays the correct state
 * based on friendship status.
 * 
 * This component centralizes the friend action button logic
 * that was previously duplicated across multiple screens.
 */
@Composable
fun FriendActionButton(
    status: FriendshipStatus,
    isLoading: Boolean,
    isOffline: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, text, containerColor, contentColor, isEnabled) = remember(status, isLoading, isOffline) {
        when (status) {
            FriendshipStatus.NOT_FRIENDS -> ButtonConfig(
                icon = Icons.Default.PersonAdd,
                text = "Add Friend",
                containerColor = ButtonPrimary,
                contentColor = Surface,
                isEnabled = !isLoading && !isOffline
            )
            FriendshipStatus.REQUEST_SENT -> ButtonConfig(
                icon = Icons.Default.HourglassEmpty,
                text = "Request Sent",
                containerColor = YellowAccent.copy(alpha = 0.2f),
                contentColor = YellowAccent,
                isEnabled = false
            )
            FriendshipStatus.REQUEST_RECEIVED -> ButtonConfig(
                icon = Icons.Default.Check,
                text = "Accept",
                containerColor = ButtonPrimary,
                contentColor = Surface,
                isEnabled = !isLoading && !isOffline
            )
            FriendshipStatus.FRIENDS -> ButtonConfig(
                icon = Icons.Default.PersonRemove,
                text = "Friends",
                containerColor = Color(0xFF0F1B41),
                contentColor = TextSecondary,
                isEnabled = !isLoading && !isOffline
            )
        }
    }
    
    DebouncedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 14.sp
        )
    }
}

/**
 * Configuration for friend action button appearance.
 */
private data class ButtonConfig(
    val icon: ImageVector,
    val text: String,
    val containerColor: Color,
    val contentColor: Color,
    val isEnabled: Boolean
)

/**
 * Compact friend action button for list items.
 */
@Composable
fun CompactFriendActionButton(
    status: FriendshipStatus,
    isLoading: Boolean,
    isOffline: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (status) {
        FriendshipStatus.NOT_FRIENDS -> {
            DebouncedButton(
                onClick = onClick,
                modifier = modifier.height(36.dp),
                enabled = !isLoading && !isOffline,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimary,
                    contentColor = Surface
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Surface,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp)
                }
            }
        }
        FriendshipStatus.REQUEST_SENT -> {
            StatusChip(
                text = "Sent",
                color = YellowAccent
            )
        }
        FriendshipStatus.REQUEST_RECEIVED -> {
            DebouncedButton(
                onClick = onClick,
                modifier = modifier.height(36.dp),
                enabled = !isLoading && !isOffline,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimary,
                    contentColor = Surface
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Surface,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Accept",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept", fontSize = 12.sp)
                }
            }
        }
        FriendshipStatus.FRIENDS -> {
            StatusChip(
                text = "Friends",
                color = ButtonPrimary
            )
        }
    }
}

/**
 * Status chip for non-actionable states.
 */
@Composable
private fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .background(
                color = color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp
        )
    }
}
