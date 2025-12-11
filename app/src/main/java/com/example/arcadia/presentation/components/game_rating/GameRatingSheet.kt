package com.example.arcadia.presentation.components.game_rating

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameRatingSheet(
    game: GameListEntry,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSave: (GameListEntry) -> Unit,
    onRemove: ((GameListEntry) -> Unit)? = null,
    isInLibrary: Boolean = true,
    onDismissWithUnsavedChanges: ((GameListEntry) -> Unit)? = null, // Callback when dismissed with unsaved changes
    originalEntry: GameListEntry? = null // Original entry for change detection (before any unsaved changes)
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    
    // Auto-scroll to bottom when sheet opens
    LaunchedEffect(isOpen) {
        if (isOpen) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    
    // State management for sections
    var isBestAspectsExpanded by remember { mutableStateOf(true) }
    var isClassificationExpanded by remember { mutableStateOf(true) }
    var isPlaytimeExpanded by remember { mutableStateOf(true) }
    var isSlideToRateExpanded by remember { mutableStateOf(true) }
    
    // Track if removal is in progress to prevent auto-save on dismiss
    var isRemovalInProgress by remember { mutableStateOf(false) }
    
    // Track if save was explicitly clicked (to prevent showing unsaved snackbar)
    var saveClicked by remember { mutableStateOf(false) }
    
    // Use stable key (rawgId) to prevent state reset during recomposition
    // This fixes the issue where slider resets when parent recomposes
    val stableKey = game.rawgId

    // Initialize state from game entry - use rawgId as key for stability
    var aspectsList by remember(stableKey) { 
        mutableStateOf(listOf(
            "Great Story", 
            "Amazing Gameplay", 
            "Great Sound And Score", 
            "Masterpiece", 
            "Good Graphics", 
            "Challenging"
        )) 
    }
    var selectedAspects by remember(stableKey) { mutableStateOf(game.aspects.toSet()) }
    var selectedClassification by remember(stableKey) { mutableStateOf(game.status) }
    var selectedPlaytime by remember(stableKey) { 
        mutableStateOf<Int?>(if (game.hoursPlayed > 0) game.hoursPlayed else null)
    }
    var sliderValue by remember(stableKey) { mutableFloatStateOf(game.rating ?: 0f) }
    
    // Store initial values to detect changes
    // Use originalEntry if provided (for reopening with unsaved changes), otherwise use game
    // For new games (not in library), any selection is a "change"
    // For existing games (in library), compare against original values
    val comparisonEntry = originalEntry ?: game
    val comparisonKey = comparisonEntry.rawgId
    val initialAspects = remember(comparisonKey) { comparisonEntry.aspects.toSet() }
    val initialStatus = remember(comparisonKey) { comparisonEntry.status }
    val initialHours = remember(comparisonKey) { comparisonEntry.hoursPlayed }
    val initialRating = remember(comparisonKey) { comparisonEntry.rating ?: 0f }

    // Ensure custom aspects from the game are in the list
    LaunchedEffect(game.aspects) {
        val newAspects = game.aspects.filter { !aspectsList.contains(it) }
        if (newAspects.isNotEmpty()) {
            aspectsList = aspectsList + newAspects
        }
    }
    
    // Build current state for comparison and saving
    fun buildCurrentEntry(): GameListEntry {
        return game.copy(
            aspects = selectedAspects.toList(),
            status = selectedClassification,
            hoursPlayed = selectedPlaytime ?: 0,
            rating = if (sliderValue > 0f) sliderValue else null,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    // Check if there are meaningful changes
    fun hasChanges(): Boolean {
        val currentHours = selectedPlaytime ?: 0
        return selectedAspects != initialAspects ||
               selectedClassification != initialStatus ||
               currentHours != initialHours ||
               sliderValue != initialRating
    }

    // Function to save current state
    val saveCurrentState = {
        val updatedGame = buildCurrentEntry()
        onSave(updatedGame)
    }

    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = {
                // Skip if save was clicked or removal is in progress
                if (saveClicked || isRemovalInProgress) {
                    onDismiss()
                    return@ModalBottomSheet
                }
                
                val currentHasChanges = hasChanges()
                
                // For games NOT in library (adding new game), never auto-save on dismiss
                // Just show unsaved changes snackbar if there are changes
                if (!isInLibrary) {
                    if (currentHasChanges && onDismissWithUnsavedChanges != null) {
                        onDismissWithUnsavedChanges(buildCurrentEntry())
                    }
                    onDismiss()
                    return@ModalBottomSheet
                }
                
                // For games IN library (editing), show unsaved changes snackbar
                if (currentHasChanges && onDismissWithUnsavedChanges != null) {
                    onDismissWithUnsavedChanges(buildCurrentEntry())
                }
                onDismiss()
            },
            sheetState = sheetState,
            containerColor = Color(0xFF0A1929),
            contentColor = TextSecondary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 24.dp, top = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                ) {
                    SheetTitle(
                        gameName = game.name,
                        rating = if (sliderValue > 0) String.format("%.1f", sliderValue) else "Not Rated",
                        ratingDescription = getRatingDescription(sliderValue),
                        onClose = {
                            // Same behavior as dismissing the sheet
                            if (saveClicked || isRemovalInProgress) {
                                onDismiss()
                                return@SheetTitle
                            }
                            
                            val currentHasChanges = hasChanges()
                            
                            if (!isInLibrary) {
                                if (currentHasChanges && onDismissWithUnsavedChanges != null) {
                                    onDismissWithUnsavedChanges(buildCurrentEntry())
                                }
                                onDismiss()
                                return@SheetTitle
                            }
                            
                            if (currentHasChanges && onDismissWithUnsavedChanges != null) {
                                onDismissWithUnsavedChanges(buildCurrentEntry())
                            }
                            onDismiss()
                        }
                    )

                    // Hide Playtime and Aspects sections for "Want to Play" games
                    if (selectedClassification != GameStatus.WANT) {
                        Spacer(modifier = Modifier.height(16.dp))

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

                        Spacer(modifier = Modifier.height(14.dp))

                        PlaytimeSection(
                            isExpanded = isPlaytimeExpanded,
                            onToggleExpanded = { isPlaytimeExpanded = !isPlaytimeExpanded },
                            selectedPlaytime = selectedPlaytime,
                            onPlaytimeSelect = { selectedPlaytime = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Classification section (right above rating)
                    ClassificationSection(
                        isExpanded = isClassificationExpanded,
                        onToggleExpanded = { isClassificationExpanded = !isClassificationExpanded },
                        selectedClassification = selectedClassification,
                        onClassificationSelect = { selectedClassification = it }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SlideToRateSection(
                        isExpanded = isSlideToRateExpanded,
                        onToggleExpanded = { isSlideToRateExpanded = !isSlideToRateExpanded },
                        sliderValue = sliderValue,
                        onSliderChange = { sliderValue = it },
                        onClearRating = { sliderValue = 0f }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Remove button (only if game is in library and onRemove is provided)
                    if (isInLibrary && onRemove != null) {
                        OutlinedButton(
                            onClick = { 
                                isRemovalInProgress = true
                                onRemove(game)
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE57373)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFE57373))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Remove", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    // Save button
                    Button(
                        onClick = {
                            saveClicked = true
                            saveCurrentState()
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonPrimary
                        )
                    ) {
                        Text("Save", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = gameName,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Rating: $rating",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 13.sp,
                color = TextSecondary.copy(alpha = 0.7f)
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = ratingDescription.isNotEmpty(),
                enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) +
                        androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(300)),
                exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)) +
                       androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(200))
            ) {
                Text(
                    text = ratingDescription,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
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
