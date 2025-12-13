package com.example.arcadia.presentation.screens.profile.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        imeAction = ImeAction.Next
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) Color(0xFFFF3535) else TextSecondary.copy(alpha = 0.7f)
            )
        },
        placeholder = {
            if (placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary.copy(alpha = 0.4f)
                )
            }
        },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        leadingIcon = leadingIcon,
        trailingIcon = if (trailingIcon != null) {
            trailingIcon
        } else if (isError) {
            // Show error icon when there's an error
            {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = Color(0xFFFF3535)
                )
            }
        } else if (value.isNotEmpty() && !readOnly && enabled) {
            // Show clear icon when field has text and no error
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear text",
                        tint = TextSecondary.copy(alpha = 0.5f)
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextSecondary,
            unfocusedTextColor = TextSecondary,
            disabledTextColor = TextSecondary.copy(alpha = 0.5f),
            errorTextColor = TextSecondary,
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface,
            disabledContainerColor = Surface.copy(alpha = 0.5f),
            errorContainerColor = Surface,
            cursorColor = ButtonPrimary,
            errorCursorColor = Color(0xFFFF3535),
            focusedBorderColor = if (isError) Color(0xFFFF3535) else ButtonPrimary,
            unfocusedBorderColor = if (isError) Color(0xFFFF3535) else TextSecondary.copy(alpha = 0.5f),
            disabledBorderColor = TextSecondary.copy(alpha = 0.3f),
            errorBorderColor = Color(0xFFFF3535),
            focusedLabelColor = if (isError) Color(0xFFFF3535) else ButtonPrimary,
            unfocusedLabelColor = if (isError) Color(0xFFFF3535) else TextSecondary.copy(alpha = 0.7f),
            disabledLabelColor = TextSecondary.copy(alpha = 0.5f),
            errorLabelColor = Color(0xFFFF3535),
        ),
        shape = RoundedCornerShape(12.dp)
    )
    
    // Error message
    if (isError && errorMessage != null) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFFF3535),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

