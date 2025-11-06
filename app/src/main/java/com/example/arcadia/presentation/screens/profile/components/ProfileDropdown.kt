package com.example.arcadia.presentation.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
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

// 🎨 Colors
private val FieldBg = Color(0xFF00123B)
private val FieldTxt = Color(0xFFDCDCDC)
private val Border = Color(0xFFDCDCDC)
private val BorderCyanOnSelect = Color(0xFF62B4DA)
private val ErrorRed = Color(0xFFFF3535)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
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
            label = { Text(label, color = FieldTxt.copy(alpha = 0.7f)) },
            readOnly = true,
            isError = isError,
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = FieldTxt
                )
            },
            textStyle = LocalTextStyle.current.copy(color = FieldTxt),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) ErrorRed else BorderCyanOnSelect,
                unfocusedBorderColor = if (isError) ErrorRed else Border.copy(alpha = 0.5f),
                focusedTextColor = FieldTxt,
                unfocusedTextColor = FieldTxt,
                cursorColor = BorderCyanOnSelect,
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(FieldBg)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, color = FieldTxt) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
                if (index < options.lastIndex)
                    Divider(color = BorderCyanOnSelect.copy(alpha = 0.3f), thickness = 1.dp)
            }
        }
    }
}

