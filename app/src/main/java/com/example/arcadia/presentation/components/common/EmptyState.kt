package com.example.arcadia.presentation.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.ui.theme.TextSecondary

/**
 * A reusable empty state component for displaying when there's no data.
 * 
 * @param title Main title text
 * @param subtitle Secondary description text
 * @param icon Optional icon to display
 * @param modifier Modifier for customization
 */
@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = TextSecondary.copy(alpha = 0.3f)
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text(
            text = title,
            color = TextSecondary.copy(alpha = 0.8f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = subtitle,
            color = TextSecondary.copy(alpha = 0.5f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/**
 * Predefined empty state for search results.
 */
@Composable
fun EmptySearchState(
    query: String,
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "No results found",
        subtitle = "Try searching for something else",
        icon = Icons.Default.Search,
        modifier = modifier
    )
}

/**
 * Predefined empty state for game library.
 */
@Composable
fun EmptyLibraryState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        title = "Your library is empty",
        subtitle = "Start adding games to build your collection",
        modifier = modifier
    )
}
