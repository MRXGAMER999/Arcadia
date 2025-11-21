package com.example.arcadia.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.presentation.components.game_rating.getRatingDescription
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickRateDialog(
    gameName: String,
    currentRating: Float?,
    onDismiss: () -> Unit,
    onRatingSelected: (Float) -> Unit
) {
    var selectedRating by remember { mutableFloatStateOf(currentRating ?: 0f) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A1929),
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Quick Rate",
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = gameName,
                    color = TextSecondary.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Current rating display
                if (selectedRating > 0f) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = YellowAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = String.format("%.1f", selectedRating),
                                color = YellowAccent,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        Text(
                            text = getRatingDescription(selectedRating),
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Rating buttons grid
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (rating in 1..10) {
                        RatingButton(
                            rating = rating.toFloat(),
                            isSelected = selectedRating == rating.toFloat(),
                            onClick = { selectedRating = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedRating > 0f) {
                        onRatingSelected(selectedRating)
                    }
                },
                enabled = selectedRating > 0f,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimary,
                    contentColor = Color.White
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
    )
}

@Composable
private fun RatingButton(
    rating: Float,
    isSelected: Boolean,
    onClick: (Float) -> Unit
) {
    val backgroundColor = if (isSelected) YellowAccent else Color(0xFF2D3E5F)
    val textColor = if (isSelected) Color(0xFF1E2A47) else TextSecondary
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick(rating) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = rating.toInt().toString(),
            color = textColor,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

