package com.example.arcadia.presentation.screens.home.tabs.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.R
import com.example.arcadia.ui.theme.ButtonPrimary

/**
 * Section header for discovery/recommended games with filter button.
 * Shows active filter count as a chip when filters are active.
 */
@Composable
fun DiscoverySectionHeader(
    title: String,
    filterActive: Boolean,
    activeFilterCount: Int,
    isAiRecommendation: Boolean = false,
    onFilterClick: () -> Unit,
    onClearFilter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (filterActive) {
                // Check if AI is the only active filter (count == 1 and AI is selected)
                val isOnlyAiFilter = isAiRecommendation && activeFilterCount == 1
                
                FilterChip(
                    selected = true,
                    onClick = onFilterClick,
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isAiRecommendation) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ai_controller),
                                    contentDescription = "AI",
                                    modifier = Modifier.size(16.dp),
                                    tint = ButtonPrimary
                                )
                                // Only show filter count if there are other filters besides AI
                                if (!isOnlyAiFilter) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$activeFilterCount Filters", maxLines = 1)
                                }
                            } else {
                                Text("$activeFilterCount Filters", maxLines = 1)
                            }
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = onClearFilter,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear filters",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                )
            } else {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Open filters",
                        tint = ButtonPrimary
                    )
                }
            }
        }
    }
}
