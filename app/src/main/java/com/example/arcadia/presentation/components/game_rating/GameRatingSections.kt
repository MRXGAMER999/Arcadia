package com.example.arcadia.presentation.components.game_rating

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun GameBestAspectsSection(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    aspectsList: List<String>,
    selectedAspects: Set<String>,
    onAspectToggle: (String) -> Unit,
    onAspectEdit: (String, String) -> Unit,
    onAspectDelete: (String) -> Unit,
    onAspectAdd: (String) -> Unit
) {
    var aspectToEdit by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2A47).copy(alpha = 0.4f))
            .padding(16.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 300f
                )
            )
    ) {
        SectionHeader(
            title = "Game's best aspects",
            isExpanded = isExpanded,
            onToggleExpanded = onToggleExpanded
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(300)
            ),
            exit = shrinkVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(200)
            )
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    aspectsList.forEach { aspect ->
                        val interactionSource = remember { MutableInteractionSource() }
                        Box {
                            FilterChip(
                                selected = selectedAspects.contains(aspect),
                                onClick = { },
                                label = { Text(aspect) },
                                leadingIcon = if (selectedAspects.contains(aspect)) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                interactionSource = interactionSource,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF2D3E5F),
                                    labelColor = TextSecondary,
                                    selectedContainerColor = ButtonPrimary,
                                    selectedLabelColor = Color.White,
                                    iconColor = Color.White
                                ),
                                border = null
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .combinedClickable(
                                        onLongClick = { aspectToEdit = aspect },
                                        onClick = { onAspectToggle(aspect) },
                                        interactionSource = interactionSource,
                                        indication = null
                                    )
                            )
                        }
                    }

                    // Add new aspect chip
                    AssistChip(
                        onClick = { showAddDialog = true },
                        label = { Text("Add") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add aspect",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color(0xFF2D3E5F),
                            labelColor = TextSecondary,
                            leadingIconContentColor = TextSecondary
                        ),
                        border = null
                    )
                }
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        AddAspectDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newAspect ->
                onAspectAdd(newAspect)
                showAddDialog = false
            }
        )
    }

    aspectToEdit?.let { aspect ->
        EditAspectDialog(
            aspect = aspect,
            onDismiss = { aspectToEdit = null },
            onSave = { newAspect ->
                onAspectEdit(aspect, newAspect)
                aspectToEdit = null
            },
            onDelete = {
                aspectToEdit = null
                showDeleteConfirmation = aspect
            }
        )
    }

    showDeleteConfirmation?.let { aspect ->
        DeleteConfirmationDialog(
            aspectName = aspect,
            onConfirm = {
                onAspectDelete(aspect)
                showDeleteConfirmation = null
            },
            onDismiss = { showDeleteConfirmation = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassificationSection(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    selectedClassification: GameStatus,
    onClassificationSelect: (GameStatus) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2A47).copy(alpha = 0.4f))
            .padding(16.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 300f
                )
            )
    ) {
        SectionHeader(
            title = "Classification",
            isExpanded = isExpanded,
            onToggleExpanded = onToggleExpanded
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(300)
            ),
            exit = shrinkVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(200)
            )
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GameStatus.entries.forEach { status ->
                        FilterChip(
                            selected = selectedClassification == status,
                            onClick = { onClassificationSelect(status) },
                            label = { Text(status.displayName) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (selectedClassification == status) Icons.Default.Check else Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF2D3E5F),
                                labelColor = TextSecondary,
                                selectedContainerColor = ButtonPrimary,
                                selectedLabelColor = Color.White,
                                iconColor = Color.White
                            ),
                            border = null
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaytimeSection(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    selectedPlaytime: String?,
    onPlaytimeSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2A47).copy(alpha = 0.4f))
            .padding(16.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 300f
                )
            )
    ) {
        SectionHeader(
            title = "Playtime",
            isExpanded = isExpanded,
            onToggleExpanded = onToggleExpanded
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(300)
            ),
            exit = shrinkVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(200)
            )
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val playtimeOptions = listOf("10h", "20h", "50h+")

                    playtimeOptions.forEach { option ->
                        FilterChip(
                            selected = selectedPlaytime == option,
                            onClick = { onPlaytimeSelect(option) },
                            label = { Text(option) },
                            leadingIcon = if (selectedPlaytime == option) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF2D3E5F),
                                labelColor = TextSecondary,
                                selectedContainerColor = ButtonPrimary,
                                selectedLabelColor = Color.White,
                                iconColor = Color.White
                            ),
                            border = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SlideToRateSection(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    sliderValue: Float,
    onSliderChange: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var lastHapticValue by remember { mutableFloatStateOf(-1f) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2A47).copy(alpha = 0.4f))
            .padding(16.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 300f
                )
            )
    ) {
        SectionHeader(
            title = "Slide To Rate",
            isExpanded = isExpanded,
            onToggleExpanded = onToggleExpanded
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(300)
            ),
            exit = shrinkVertically(
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(200)
            )
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    val scale by animateFloatAsState(
                        targetValue = if (sliderValue > 0) 1.15f else 1f,
                        animationSpec = tween(200, easing = FastOutSlowInEasing),
                        label = "TextScale"
                    )
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (sliderValue > 0) String.format("%.1f", sliderValue) else "Drag to rate",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (sliderValue > 0) YellowAccent else TextSecondary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            modifier = Modifier.scale(scale)
                        )
                        
                        if (sliderValue > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = getRatingDescription(sliderValue),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Slider(
                    value = sliderValue,
                    onValueChange = { newValue ->
                        // Trigger haptic feedback when crossing integer boundaries
                        val roundedValue = (newValue * 2).roundToInt() / 2f // Round to nearest 0.5
                        if (roundedValue != lastHapticValue) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            lastHapticValue = roundedValue
                        }
                        onSliderChange(newValue)
                    },
                    valueRange = 0f..10f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = YellowAccent,
                        activeTrackColor = YellowAccent,
                        inactiveTrackColor = Color(0xFF2D3E5F)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "10",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        
        val rotation by animateFloatAsState(
            targetValue = if (isExpanded) 0f else 180f,
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            label = "IconRotation"
        )
        
        IconButton(onClick = onToggleExpanded) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = TextSecondary.copy(alpha = 0.7f),
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

