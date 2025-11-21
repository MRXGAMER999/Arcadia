package com.example.arcadia.presentation.components.game_rating

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary

@Composable
fun AddAspectDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Add New Aspect",
                color = TextSecondary
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Aspect name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (text.isNotBlank()) {
                            onAdd(text)
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ButtonPrimary,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                    focusedLabelColor = ButtonPrimary,
                    unfocusedLabelColor = TextSecondary.copy(alpha = 0.6f),
                    cursorColor = ButtonPrimary,
                    focusedTextColor = TextSecondary,
                    unfocusedTextColor = TextSecondary
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onAdd(text)
                    }
                }
            ) {
                Text(
                    text = "Add",
                    color = ButtonPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        },
        containerColor = Color(0xFF0A1929),
        textContentColor = TextSecondary
    )
}

@Composable
fun EditAspectDialog(
    aspect: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit
) {
    var text by remember { mutableStateOf(aspect) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Edit Aspect",
                color = TextSecondary
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Aspect name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (text.isNotBlank()) {
                            onSave(text)
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ButtonPrimary,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                    focusedLabelColor = ButtonPrimary,
                    unfocusedLabelColor = TextSecondary.copy(alpha = 0.6f),
                    cursorColor = ButtonPrimary,
                    focusedTextColor = TextSecondary,
                    unfocusedTextColor = TextSecondary
                )
            )
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
                TextButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onSave(text)
                        }
                    }
                ) {
                    Text(
                        text = "Save",
                        color = ButtonPrimary
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        containerColor = Color(0xFF0A1929),
        textContentColor = TextSecondary
    )
}

@Composable
fun DeleteConfirmationDialog(
    aspectName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Delete Aspect",
                color = TextSecondary
            )
        },
        text = { 
            Text(
                text = "Are you sure you want to delete \"$aspectName\"?",
                color = TextSecondary.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        },
        containerColor = Color(0xFF0A1929),
        textContentColor = TextSecondary
    )
}

