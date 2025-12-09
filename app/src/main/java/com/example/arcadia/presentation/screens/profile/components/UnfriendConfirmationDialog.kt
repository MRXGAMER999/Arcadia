package com.example.arcadia.presentation.screens.profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary

/**
 * Confirmation dialog shown when a user taps the "Friends" button to unfriend someone.
 * 
 * Requirements: 9.1, 9.3, 9.4
 * - 9.1: Display confirmation dialog with message "Remove @username from friends?"
 * - 9.3: Update profile button to "Add as Friend" after unfriending
 * - 9.4: Dismiss dialog and maintain friendship status if cancelled
 * 
 * @param username The username of the friend to remove
 * @param onConfirm Callback when the user confirms unfriending
 * @param onDismiss Callback when the dialog is dismissed/cancelled
 */
@Composable
fun UnfriendConfirmationDialog(
    username: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
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
                    .padding(24.dp)
            ) {
                Text(
                    text = "Remove Friend",
                    color = TextSecondary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Remove @$username from friends?",
                    color = TextSecondary.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Start
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cancel",
                            color = TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935), // Red for destructive action
                            contentColor = Color.White
                        )
                    ) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}
