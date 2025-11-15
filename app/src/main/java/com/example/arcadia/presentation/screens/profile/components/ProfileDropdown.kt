package com.example.arcadia.presentation.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    placeholder: String = "Select $label"
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) Color(0xFFFF3535) else TextSecondary.copy(alpha = 0.7f)
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary.copy(alpha = 0.4f)
                )
            },
            trailingIcon = {
                Row {
                    if (isError) {
                        // Show error icon when there's an error
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = Color(0xFFFF3535)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    // Always show dropdown arrow
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown arrow",
                        tint = if (isError) Color(0xFFFF3535) else TextSecondary
                    )
                }
            },
            isError = isError,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
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

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
    
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

