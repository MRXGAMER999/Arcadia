package com.example.arcadia.presentation.components.game_rating

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import kotlin.math.roundToInt
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.R
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent
import com.example.arcadia.ui.theme.getRatingGradient
import com.example.arcadia.ui.theme.getRatingColor

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
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E2A47).copy(alpha = 0.4f))
            .padding(14.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = 350f
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
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(350)
            ),
            exit = shrinkVertically(
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(200)
            )
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                ) {
                    aspectsList.forEach { aspect ->
                        val isSelected = selectedAspects.contains(aspect)
                        val interactionSource = remember { MutableInteractionSource() }

                        // Subtle animation for aspect chips to prevent overflow
                        val aspectScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.03f else 1f,
                            animationSpec = spring(
                                dampingRatio = 0.6f,
                                stiffness = 400f
                            ),
                            label = "AspectScale_$aspect"
                        )

                        Box(modifier = Modifier.scale(aspectScale)) {
                            FilterChip(
                                selected = isSelected,
                                onClick = { },
                                label = {
                                    Text(
                                        aspect,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.animateContentSize()
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        // Subtle check icon animation
                                        val checkScale by animateFloatAsState(
                                            targetValue = 1.0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.7f,
                                                stiffness = 400f
                                            ),
                                            label = "CheckScale"
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(17.dp)
                                                .scale(checkScale)
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
                                border = null,
                                modifier = Modifier.animateContentSize()
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
                        label = { Text("Add", fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add aspect",
                                modifier = Modifier.size(17.dp)
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
    // Helper function to get icon for each status
    fun getStatusIcon(status: GameStatus): Int {
        return when (status) {
            GameStatus.FINISHED -> R.drawable.finished_ic
            GameStatus.PLAYING -> R.drawable.playing_ic
            GameStatus.DROPPED -> R.drawable.dropped_ic
            GameStatus.ON_HOLD -> R.drawable.on_hold_ic
            GameStatus.WANT -> R.drawable.want_ic
        }
    }

    // Helper function to get color for each selected status
    fun getStatusColor(status: GameStatus): Color {
        return when (status) {
            GameStatus.FINISHED -> Color(0xFFFBB02E)
            GameStatus.PLAYING -> Color(0xFFD34ECE)
            GameStatus.DROPPED -> Color(0xFFBA5C3E)
            GameStatus.ON_HOLD -> Color(0xFF62B4DA)
            GameStatus.WANT -> Color(0xFF3F77CC)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E2A47).copy(alpha = 0.4f))
            .padding(14.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = 350f
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
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(350)
            ),
            exit = shrinkVertically(
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(200)
            )
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                ) {
                    GameStatus.entries.forEach { status ->
                        val isSelected = selectedClassification == status

                        // Subtle animations for each chip to prevent overflow
                        val chipScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.03f else 1f,
                            animationSpec = spring(
                                dampingRatio = 0.6f,
                                stiffness = 400f
                            ),
                            label = "ChipScale_${status.name}"
                        )

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                // Allow clicking same chip to keep selection (no deselect for classification)
                                onClassificationSelect(status)
                            },
                            label = {
                                Text(
                                    status.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.animateContentSize()
                                )
                            },
                            leadingIcon = {
                                val iconScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.05f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = 0.7f,
                                        stiffness = 400f
                                    ),
                                    label = "IconScale_${status.name}"
                                )
                                Icon(
                                    painter = painterResource(id = getStatusIcon(status)),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(17.dp)
                                        .scale(iconScale)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF2D3E5F),
                                labelColor = TextSecondary,
                                selectedContainerColor = if (isSelected) getStatusColor(status) else ButtonPrimary,
                                selectedLabelColor = Color.White,
                                iconColor = Color.White
                            ),
                            border = null,
                            modifier = Modifier.scale(chipScale)
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
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E2A47).copy(alpha = 0.4f))
            .padding(14.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = 350f
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
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(350)
            ),
            exit = shrinkVertically(
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(200)
            )
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp)
                ) {
                    val playtimeOptions = listOf("10h", "20h", "50h+")

                    playtimeOptions.forEach { option ->
                        val isSelected = selectedPlaytime == option

                        // Subtle animation for playtime chips to prevent overflow
                        val playtimeScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.03f else 1f,
                            animationSpec = spring(
                                dampingRatio = 0.6f,
                                stiffness = 400f
                            ),
                            label = "PlaytimeScale_$option"
                        )

                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                // Toggle deselection - clicking the same chip deselects it
                                if (isSelected) {
                                    onPlaytimeSelect("")  // Deselect by passing empty string
                                } else {
                                    onPlaytimeSelect(option)
                                }
                            },
                            label = {
                                Text(
                                    option,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.animateContentSize()
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    // Subtle check icon animation
                                    val checkScale by animateFloatAsState(
                                        targetValue = 1.0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.7f,
                                            stiffness = 400f
                                        ),
                                        label = "CheckScale"
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(17.dp)
                                            .scale(checkScale)
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
                            border = null,
                            modifier = Modifier.scale(playtimeScale)
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
    
    // Helper function to get icon based on rating
    fun getRatingIcon(rating: Float): Int {
        return when {
            rating == 0f -> R.drawable.no_rating_ic
            rating <= 1f -> R.drawable.between0and2_ic
            rating <= 2f -> R.drawable.between0and2_ic
            rating <= 3f -> R.drawable.from2to4_ic
            rating <= 4f -> R.drawable.from2to4_ic
            rating <= 5f -> R.drawable.from4to6_ic
            rating <= 6f -> R.drawable.from4to6_ic
            rating <= 7f -> R.drawable.from6_5to7_5_ic
            rating <= 7.5f -> R.drawable.from6_5to7_5_ic
            rating <= 8f -> R.drawable.from7_5to8_5_ic
            rating <= 8.5f -> R.drawable.from7_5to8_5_ic
            rating <= 9f -> R.drawable.from8_5to9_5_ic
            rating <= 9.5f -> R.drawable.from8_5to9_5_ic
            else -> R.drawable.from9_5to10_ic
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E2A47).copy(alpha = 0.4f))
            .padding(14.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = 350f
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
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(350)
            ),
            exit = shrinkVertically(
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ) + fadeOut(
                animationSpec = tween(200)
            )
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))

                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    // Smooth scale animation
                    val scale by animateFloatAsState(
                        targetValue = if (sliderValue > 0) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.7f,
                            stiffness = 300f
                        ),
                        label = "ScaleAnimation"
                    )

                    // Smooth color animation for icon
                    val animatedIconColor by androidx.compose.animation.animateColorAsState(
                        targetValue = getRatingColor(sliderValue),
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = FastOutSlowInEasing
                        ),
                        label = "IconColorAnimation"
                    )

                    // Subtle rotation animation
                    val rotation by animateFloatAsState(
                        targetValue = if (sliderValue > 0) 0f else -8f,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = 250f
                        ),
                        label = "RotationAnimation"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { clip = false }
                    ) {
                        // Apply scale and rotation to outer layer to prevent compound scaling during transition
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .graphicsLayer {
                                    clip = false
                                    scaleX = scale
                                    scaleY = scale
                                    rotationZ = rotation
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Cool transition with scale, slide, and bounce
                            androidx.compose.animation.AnimatedContent(
                                targetState = getRatingIcon(sliderValue),
                                transitionSpec = {
                                    (fadeIn(
                                        animationSpec = tween(400, easing = FastOutSlowInEasing)
                                    ) + scaleIn(
                                        initialScale = 0.5f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    ) + slideInVertically(
                                        initialOffsetY = { it / 4 },
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )) togetherWith (fadeOut(
                                        animationSpec = tween(200)
                                    ) + scaleOut(
                                        targetScale = 0.6f,
                                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                                    ) + slideOutVertically(
                                        targetOffsetY = { -it / 4 },
                                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                                    ))
                                },
                                label = "IconTransition",
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.graphicsLayer { clip = false }
                            ) { iconRes ->
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = "Rating icon",
                                    modifier = Modifier
                                        .size(64.dp)
                                        .graphicsLayer { clip = false },
                                    tint = animatedIconColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Rating Text with gradient and animation
                        BasicText(
                            text = if (sliderValue > 0) String.format("%.1f", sliderValue) else "Drag to rate",
                            style = MaterialTheme.typography.titleLarge.copy(
                                brush = getRatingGradient(sliderValue),
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            ),
                            modifier = Modifier.scale(scale)
                        )
                        
                        // Animated description text
                        AnimatedVisibility(
                            visible = sliderValue > 0,
                            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = getRatingDescription(sliderValue),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary.copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Animated slider colors
                val animatedThumbColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (sliderValue > 0) getRatingColor(sliderValue) else YellowAccent,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = FastOutSlowInEasing
                    ),
                    label = "ThumbColorAnimation"
                )

                val animatedTrackColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (sliderValue > 0) getRatingColor(sliderValue).copy(alpha = 0.8f) else YellowAccent,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = FastOutSlowInEasing
                    ),
                    label = "TrackColorAnimation"
                )

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
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = animatedThumbColor,
                        activeTrackColor = animatedTrackColor,
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
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )

        val rotation by animateFloatAsState(
            targetValue = if (isExpanded) 0f else 180f,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = 350f
            ),
            label = "IconRotation"
        )

        val iconScale by animateFloatAsState(
            targetValue = if (isExpanded) 1f else 0.95f,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = 400f
            ),
            label = "IconScale"
        )

        IconButton(onClick = onToggleExpanded, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = TextSecondary.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(22.dp)
                    .rotate(rotation)
                    .scale(iconScale)
            )
        }
    }
}

