package com.example.arcadia.presentation.screens.friends.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.rememberResponsiveDimens

/**
 * Dialog shown when a user tries to send a friend request to someone
 * who has already sent them a request.
 * Responsive design that adapts to all screen sizes.
 * 
 * Requirements: 3.13, 3.14, 3.15
 * 
 * @param username The username of the user who sent the request
 * @param onAccept Callback when the user accepts the existing request
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
fun ReciprocalRequestDialog(
    username: String,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val dimens = rememberResponsiveDimens()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dimens.cardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = Surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimens.paddingXLarge)
            ) {
                Text(
                    text = "Friend Request Exists",
                    color = TextSecondary,
                    fontSize = dimens.fontSizeLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(dimens.paddingLarge))
                
                Text(
                    text = "This user has already sent you a request! Would you like to accept it?",
                    color = TextSecondary.copy(alpha = 0.8f),
                    fontSize = dimens.fontSizeSmall,
                    textAlign = TextAlign.Start
                )
                
                Spacer(modifier = Modifier.height(dimens.sectionSpacing))
                
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.buttonHeightMedium)
                    ) {
                        Text(
                            text = "Cancel",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = dimens.fontSizeMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(dimens.itemSpacing))
                    
                    Button(
                        onClick = onAccept,
                        modifier = Modifier
                            .weight(1f)
                            .height(dimens.buttonHeightMedium),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonPrimary,
                            contentColor = Surface
                        )
                    ) {
                        Text("Accept", fontSize = dimens.fontSizeMedium)
                    }
                }
            }
        }
    }
}
