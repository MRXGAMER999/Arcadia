package com.example.arcadia.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search in list",
    backgroundColor: Color = Color(0xFF2A2A2A),
    textColor: Color = Color.White,
    placeholderColor: Color = Color(0xFF6B6B6B),
    iconColor: Color = Color(0xFF6B6B6B)
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp),
        textStyle = TextStyle(
            color = textColor,
            fontSize = 16.sp
        ),
        cursorBrush = SolidColor(textColor),
        singleLine = true,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = TextStyle(
                                color = placeholderColor,
                                fontSize = 16.sp
                            )
                        )
                    }
                    innerTextField()
                }
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    )
}


@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun SearchBarPreview() {
    var searchText by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(16.dp)
    ) {
        SearchBar(
            value = searchText,
            onValueChange = { searchText = it }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1A1A1A)
@Composable
private fun SearchBarWithTextPreview() {
    var searchText by remember { mutableStateOf("Life is Strange") }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .padding(16.dp)
    ) {
        SearchBar(
            value = searchText,
            onValueChange = { searchText = it }
        )
    }
}
