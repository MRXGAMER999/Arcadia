package com.example.arcadia.presentation.components.game_rating

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameRatingSheet(
    game: GameListEntry,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSave: (GameListEntry) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // State management for sections
    var isBestAspectsExpanded by remember { mutableStateOf(true) }
    var isClassificationExpanded by remember { mutableStateOf(true) }
    var isPlaytimeExpanded by remember { mutableStateOf(true) }
    var isSlideToRateExpanded by remember { mutableStateOf(true) }

    // Initialize state from game entry
    var aspectsList by remember(game) { 
        mutableStateOf(listOf(
            "Great Story", 
            "Amazing Gameplay", 
            "Great Sound And Score", 
            "Masterpiece", 
            "Good Graphics", 
            "Challenging"
        )) 
    }
    var selectedAspects by remember(game) { mutableStateOf(game.aspects.toSet()) }
    var selectedClassification by remember(game) { mutableStateOf(game.status) }
    var selectedPlaytime by remember(game) { 
        mutableStateOf(
            when {
                game.hoursPlayed >= 50 -> "50h+"
                game.hoursPlayed >= 20 -> "20h"
                game.hoursPlayed >= 10 -> "10h"
                else -> null
            }
        ) 
    }
    var sliderValue by remember(game) { mutableFloatStateOf(game.rating ?: 0f) }

    // Ensure custom aspects from the game are in the list
    LaunchedEffect(game.aspects) {
        val newAspects = game.aspects.filter { !aspectsList.contains(it) }
        if (newAspects.isNotEmpty()) {
            aspectsList = aspectsList + newAspects
        }
    }

    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color(0xFF0A1929),
            contentColor = TextSecondary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                SheetTitle(
                    gameName = game.name,
                    rating = if (sliderValue > 0) String.format("%.1f", sliderValue) else "Not Rated",
                    ratingDescription = getRatingDescription(sliderValue),
                    onClose = onDismiss
                )

                Spacer(modifier = Modifier.height(24.dp))

                GameBestAspectsSection(
                    isExpanded = isBestAspectsExpanded,
                    onToggleExpanded = { isBestAspectsExpanded = !isBestAspectsExpanded },
                    aspectsList = aspectsList,
                    selectedAspects = selectedAspects,
                    onAspectToggle = { aspect ->
                        selectedAspects = if (selectedAspects.contains(aspect)) {
                            selectedAspects - aspect
                        } else {
                            selectedAspects + aspect
                        }
                    },
                    onAspectEdit = { oldAspect, newAspect ->
                        aspectsList = aspectsList.map { if (it == oldAspect) newAspect else it }
                        if (selectedAspects.contains(oldAspect)) {
                            selectedAspects = selectedAspects - oldAspect + newAspect
                        }
                    },
                    onAspectDelete = { aspect ->
                        aspectsList = aspectsList - aspect
                        selectedAspects = selectedAspects - aspect
                    },
                    onAspectAdd = { newAspect ->
                        if (!aspectsList.contains(newAspect)) {
                            aspectsList = aspectsList + newAspect
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                ClassificationSection(
                    isExpanded = isClassificationExpanded,
                    onToggleExpanded = { isClassificationExpanded = !isClassificationExpanded },
                    selectedClassification = selectedClassification,
                    onClassificationSelect = { selectedClassification = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                PlaytimeSection(
                    isExpanded = isPlaytimeExpanded,
                    onToggleExpanded = { isPlaytimeExpanded = !isPlaytimeExpanded },
                    selectedPlaytime = selectedPlaytime,
                    onPlaytimeSelect = { selectedPlaytime = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                SlideToRateSection(
                    isExpanded = isSlideToRateExpanded,
                    onToggleExpanded = { isSlideToRateExpanded = !isSlideToRateExpanded },
                    sliderValue = sliderValue,
                    onSliderChange = { sliderValue = it }
                )
                
                Spacer(modifier = Modifier.height(32.dp))


                Button(
                    onClick = {
                        val hours = when(selectedPlaytime) {
                            "50h+" -> 50
                            "20h" -> 20
                            "10h" -> 10
                            else -> 0
                        }
                        
                        val updatedGame = game.copy(
                            aspects = selectedAspects.toList(),
                            status = selectedClassification,
                            hoursPlayed = hours,
                            rating = if (sliderValue > 0f) sliderValue else null,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(updatedGame)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonPrimary
                    )){
                    Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SheetTitle(
    gameName: String,
    rating: String,
    ratingDescription: String,
    onClose: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = gameName,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Rating: $rating",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = TextSecondary.copy(alpha = 0.7f)
            )
            if (ratingDescription.isNotEmpty()) {
                Text(
                    text = ratingDescription,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = TextSecondary
            )
        }
    }
}

fun getRatingDescription(rating: Float): String {
    return when {
        rating == 0f -> ""
        rating <= 3f -> "Poor"
        rating <= 6f -> "Good"
        rating <= 8f -> "Great"
        rating <= 10f -> "Masterpiece"
        else -> ""
    }
}
