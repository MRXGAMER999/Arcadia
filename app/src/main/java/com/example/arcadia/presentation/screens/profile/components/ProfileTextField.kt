package com.example.arcadia.presentation.screens.profile.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// 🎨 Colors
private val FieldBg = Color(0xFF00123B)
private val FieldTxt = Color(0xFFDCDCDC)
private val Border = Color(0xFFDCDCDC)
private val BorderCyanOnSelect = Color(0xFF62B4DA)
private val ErrorRed = Color(0xFFFF3535)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isError: Boolean = false,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = FieldTxt.copy(alpha = 0.7f)) },
        placeholder = { Text(placeholder, color = FieldTxt.copy(alpha = 0.4f)) },
        singleLine = singleLine,
        trailingIcon = trailingIcon,
        isError = isError,
        readOnly = readOnly,
        enabled = enabled,
        textStyle = LocalTextStyle.current.copy(color = FieldTxt),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) ErrorRed else BorderCyanOnSelect,
            unfocusedBorderColor = if (isError) ErrorRed else Border.copy(alpha = 0.5f),
            focusedTextColor = FieldTxt,
            unfocusedTextColor = FieldTxt,
            cursorColor = BorderCyanOnSelect,
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            disabledContainerColor = FieldBg.copy(alpha = 0.3f),
            disabledTextColor = FieldTxt.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    )
}

