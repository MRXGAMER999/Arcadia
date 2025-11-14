package com.example.arcadia.presentation.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
        // Home Tab
        val homeScale by animateFloatAsState(
            targetValue = if (selectedItemIndex == 0) 1.15f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "home_scale"
        )
        
        NavigationBarItem(
            selected = selectedItemIndex == 0,
            onClick = { onSelectedItemIndexChange(0) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier
                        .scale(homeScale)
                        .size(24.dp)
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
        
        // Discover Tab
        val discoverScale by animateFloatAsState(
            targetValue = if (selectedItemIndex == 1) 1.15f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "discover_scale"
        )
        
        NavigationBarItem(
            selected = selectedItemIndex == 1,
            onClick = { onSelectedItemIndexChange(1) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Discover",
                    modifier = Modifier
                        .scale(discoverScale)
                        .size(24.dp)
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
        
        // Library Tab
        val libraryScale by animateFloatAsState(
            targetValue = if (selectedItemIndex == 2) 1.15f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "library_scale"
        )
        
        NavigationBarItem(
            selected = selectedItemIndex == 2,
            onClick = { onSelectedItemIndexChange(2) },
            icon = {
                Icon(
                    imageVector = Icons.Default.VideogameAsset,
                    contentDescription = "My Games",
                    modifier = Modifier
                        .scale(libraryScale)
                        .size(24.dp)
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

