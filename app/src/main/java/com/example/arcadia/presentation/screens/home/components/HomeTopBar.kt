package com.example.arcadia.presentation.screens.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.R
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent

/**
 * Top bar for the Home screen.
 * 
 * Requirements: 2.1, 2.2, 2.3, 2.4, 2.5
 * 
 * @param selectedIndex The currently selected tab index
 * @param pendingFriendRequestCount The count of pending friend requests for badge display
 * @param onSearchClick Callback when search icon is clicked
 * @param onFriendsClick Callback when friends icon is clicked
 * @param onSettingsClick Callback when settings icon is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    selectedIndex: Int,
    pendingFriendRequestCount: Int = 0,
    onSearchClick: () -> Unit = {},
    onFriendsClick: () -> Unit = {},
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
                    painter = painterResource(R.drawable.search),
                    contentDescription = "Search",
                    tint = ButtonPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            // Friends icon with badge - Requirements: 2.1, 2.3, 2.4, 2.5
            IconButton(onClick = onFriendsClick) {
                if (pendingFriendRequestCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = YellowAccent,
                                contentColor = Surface
                            ) {
                                Text(
                                    text = formatBadgeCount(pendingFriendRequestCount),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.users),
                            contentDescription = "Friends",
                            tint = ButtonPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(R.drawable.users),
                        contentDescription = "Friends",
                        tint = ButtonPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = painterResource(R.drawable.profile),
                    contentDescription = "Settings",
                    tint = ButtonPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Surface,
            titleContentColor = TextSecondary
        )
    )
}

/**
 * Formats the pending request count for badge display.
 * Shows actual number up to 99, then "99+" for 100 or more.
 * Requirements: 2.4
 */
private fun formatBadgeCount(count: Int): String {
    return when {
        count <= 0 -> ""
        count <= 99 -> count.toString()
        else -> "99+"
    }
}

