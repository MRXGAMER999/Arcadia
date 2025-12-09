package com.example.arcadia.presentation.screens.friends.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent

/**
 * Type of limit that has been reached.
 */
enum class LimitType {
    /** Daily friend request limit (20 requests per day) */
    DAILY_LIMIT,
    /** Pending outgoing requests limit (100 pending requests) */
    PENDING_LIMIT,
    /** Maximum friends limit (500 friends) */
    FRIENDS_LIMIT,
    /** 24-hour cooldown after being declined */
    COOLDOWN
}

/**
 * Dialog shown when a user has reached a limit for friend operations.
 * 
 * Requirements: 3.21, 3.22, 3.26, 6.11
 * 
 * @param limitType The type of limit that was reached
 * @param cooldownHours Hours remaining for cooldown (only used when limitType is COOLDOWN)
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
fun LimitReachedDialog(
    limitType: LimitType,
    cooldownHours: Int? = null,
    onDismiss: () -> Unit
) {
    val (icon, title, message) = when (limitType) {
        LimitType.DAILY_LIMIT -> Triple(
            Icons.Default.Schedule,
            "Daily Limit Reached",
            "You've reached the daily limit for friend requests. Try again tomorrow."
        )
        LimitType.PENDING_LIMIT -> Triple(
            Icons.Default.HourglassEmpty,
            "Too Many Pending Requests",
            "You have too many pending requests. Cancel some or wait for responses."
        )
        LimitType.FRIENDS_LIMIT -> Triple(
            Icons.Default.People,
            "Friends Limit Reached",
            "You've reached the maximum number of friends (500). Remove a friend to accept new requests."
        )
        LimitType.COOLDOWN -> Triple(
            Icons.Default.Block,
            "Please Wait",
            "You can send another request to this user in ${cooldownHours ?: 0} hours"
        )
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = YellowAccent,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Message
                Text(
                    text = message,
                    color = TextSecondary.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // OK Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonPrimary,
                        contentColor = Surface
                    )
                ) {
                    Text("OK")
                }
            }
        }
    }
}

/**
 * Helper function to get the appropriate limit message.
 * Can be used when showing snackbars instead of dialogs.
 * 
 * Requirements: 3.21, 3.22, 3.26, 6.11
 */
object LimitMessages {
    /** Daily limit (20 requests per day) - Requirements: 3.21 */
    const val DAILY_LIMIT = "You've reached the daily limit for friend requests. Try again tomorrow."
    
    /** Pending limit (100 pending requests) - Requirements: 3.22 */
    const val PENDING_LIMIT = "You have too many pending requests. Cancel some or wait for responses."
    
    /** Friends limit (500 friends) - Requirements: 6.11 */
    const val FRIENDS_LIMIT = "You've reached the maximum number of friends (500). Remove a friend to accept new requests."
    
    /**
     * Gets the cooldown message with the remaining hours.
     * Requirements: 3.26
     */
    fun getCooldownMessage(hours: Int): String {
        return "You can send another request to this user in $hours hours"
    }
}
