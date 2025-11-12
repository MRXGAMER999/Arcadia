package com.example.arcadia.presentation.screens.searchScreen.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search games...", color = TextSecondary.copy(alpha = 0.5f)) },
        singleLine = true,
        shape = RoundedCornerShape(50.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(56.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ButtonPrimary,
            unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f),
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface,
            cursorColor = ButtonPrimary
        ),
        textStyle = TextStyle(color = TextSecondary)
    )
}
