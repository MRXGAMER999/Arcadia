package com.example.arcadia.presentation.screens.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    selectedIndex: Int,
    onSearchClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val titles = listOf("Arcadia", "Discover", "My Games")
    
    TopAppBar(
        title = {
            AnimatedContent(
                targetState = selectedIndex,
                label = "title_animation"
            ) { index ->
                Text(
                    text = titles.getOrElse(index) { "Arcadia" },
                    color = TextSecondary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = ButtonPrimary
                )
            }
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = ButtonPrimary
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = ButtonPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Surface,
            titleContentColor = TextSecondary
        )
    )
}

