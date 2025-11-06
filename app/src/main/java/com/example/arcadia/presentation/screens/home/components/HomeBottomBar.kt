package com.example.arcadia.presentation.screens.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary

@Composable
fun HomeBottomBar(
    selectedItemIndex: Int,
    onSelectedItemIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color(0xFF001949),
        contentColor = TextSecondary
    ) {
        NavigationBarItem(
            selected = selectedItemIndex == 0,
            onClick = { onSelectedItemIndexChange(0) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ButtonPrimary,
                selectedTextColor = ButtonPrimary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = ButtonPrimary.copy(alpha = 0.2f)
            )
        )
        NavigationBarItem(
            selected = selectedItemIndex == 1,
            onClick = { onSelectedItemIndexChange(1) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Discover"
                )
            },
            label = { Text("Discover") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ButtonPrimary,
                selectedTextColor = ButtonPrimary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = ButtonPrimary.copy(alpha = 0.2f)
            )
        )
        NavigationBarItem(
            selected = selectedItemIndex == 2,
            onClick = { onSelectedItemIndexChange(2) },
            icon = {
                Icon(
                    imageVector = Icons.Default.VideogameAsset,
                    contentDescription = "My Games"
                )
            },
            label = { Text("My Games") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ButtonPrimary,
                selectedTextColor = ButtonPrimary,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = ButtonPrimary.copy(alpha = 0.2f)
            )
        )
    }
}

