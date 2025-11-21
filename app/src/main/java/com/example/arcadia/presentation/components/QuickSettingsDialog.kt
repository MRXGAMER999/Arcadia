package com.example.arcadia.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.arcadia.ui.theme.ButtonPrimary

enum class MediaLayout { LIST, GRID }
enum class SortType { TITLE, ADDED, DATE, RATING }
enum class SortOrder { ASCENDING, DESCENDING }

data class QuickSettingsState(
    val mediaLayout: MediaLayout = MediaLayout.LIST,
    val sortType: SortType = SortType.TITLE,
    val sortOrder: SortOrder = SortOrder.ASCENDING,
    val showDateAdded: Boolean = true,
    val showReleaseDate: Boolean = false
)

@Composable
fun QuickSettingsDialog(
    state: QuickSettingsState,
    onStateChange: (QuickSettingsState) -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(300)) +
                    scaleIn(
                        initialScale = 0.85f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) +
                    slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ),
            exit = fadeOut(animationSpec = tween(200)) +
                   scaleOut(
                       targetScale = 0.9f,
                       animationSpec = tween(200)
                   ) +
                   slideOutVertically(
                       targetOffsetY = { it / 4 },
                       animationSpec = tween(200)
                   )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight()
                    .heightIn(max = 650.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF2A2E35)
            ) {
            Column {
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 16.dp)
                ) {
                    // Title
                    Text(
                        text = "Quick Settings",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Media Layout
                    SectionTitle("Media Layout")
                    Spacer(modifier = Modifier.height(12.dp))
                    DualSegmentedButton(
                        leftText = "List",
                        rightText = "Grid",
                        isLeftSelected = state.mediaLayout == MediaLayout.LIST,
                        onSelectionChange = { isLeft ->
                            val newLayout = if (isLeft) MediaLayout.LIST else MediaLayout.GRID
                            // When switching to GRID, ensure only one date option is selected
                            val newState = if (!isLeft && state.showDateAdded && state.showReleaseDate) {
                                state.copy(
                                    mediaLayout = newLayout,
                                    showReleaseDate = false // Keep only Date Added by default
                                )
                            } else {
                                state.copy(mediaLayout = newLayout)
                            }
                            onStateChange(newState)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sort
                    SectionTitle("Sort")
                    Spacer(modifier = Modifier.height(12.dp))
                    SortOptions(
                        selectedType = state.sortType,
                        onTypeChange = { onStateChange(state.copy(sortType = it)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SortOrderButton(
                        sortType = state.sortType,
                        sortOrder = state.sortOrder,
                        onOrderChange = { onStateChange(state.copy(sortOrder = it)) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Fields
                    SectionTitle("Fields")
                    if (state.mediaLayout == MediaLayout.GRID) {
                        Text(
                            text = "Select one date display option",
                            fontSize = 12.sp,
                            color = Color(0xFF8B8B8B),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldsSelection(
                        mediaLayout = state.mediaLayout,
                        showDateAdded = state.showDateAdded,
                        showReleaseDate = state.showReleaseDate,
                        onDateAddedChange = { checked ->
                            if (state.mediaLayout == MediaLayout.GRID) {
                                // Grid mode: Radio behavior - selecting one deselects the other
                                if (checked) {
                                    onStateChange(state.copy(showDateAdded = true, showReleaseDate = false))
                                }
                            } else {
                                // List mode: Independent toggles
                                onStateChange(state.copy(showDateAdded = checked))
                            }
                        },
                        onReleaseDateChange = { checked ->
                            if (state.mediaLayout == MediaLayout.GRID) {
                                // Grid mode: Radio behavior - selecting one deselects the other
                                if (checked) {
                                    onStateChange(state.copy(showDateAdded = false, showReleaseDate = true))
                                }
                            } else {
                                // List mode: Independent toggles
                                onStateChange(state.copy(showReleaseDate = checked))
                            }
                        }
                    )
                }

                // Fixed action buttons at bottom
                HorizontalDivider(
                    color = Color(0xFF3A3E45),
                    thickness = 1.dp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF8AB4F8),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonPrimary,
                            contentColor = Color(0xFF1A1E25)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 16.sp,
        color = Color(0xFFB8B8B8)
    )
}

@Composable
private fun TripleSegmentedButton(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(
                1.dp,
                color = ButtonPrimary,
                RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
    ) {
        options.forEachIndexed { index, option ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (selectedIndex == index) Color(0xFF4A5A6A) else Color.Transparent
                    )
                    .clickable { onSelectionChange(index) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (selectedIndex == index) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF8AB4F8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = option,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedIndex == index) Color(0xFF8AB4F8) else Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DualSegmentedButton(
    leftText: String,
    rightText: String,
    isLeftSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    val leftBackgroundColor by animateColorAsState(
        targetValue = if (isLeftSelected) Color(0xFF4A5A6A) else Color.Transparent,
        animationSpec = tween(300),
        label = "leftBackground"
    )
    val rightBackgroundColor by animateColorAsState(
        targetValue = if (!isLeftSelected) Color(0xFF4A5A6A) else Color.Transparent,
        animationSpec = tween(300),
        label = "rightBackground"
    )
    
    val leftTextColor by animateColorAsState(
        targetValue = if (isLeftSelected) ButtonPrimary else Color.White,
        animationSpec = tween(300),
        label = "leftTextColor"
    )
    
    val rightTextColor by animateColorAsState(
        targetValue = if (!isLeftSelected) ButtonPrimary else Color.White,
        animationSpec = tween(300),
        label = "rightTextColor"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(
                1.dp,
                ButtonPrimary,
                RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(leftBackgroundColor)
                .clickable { onSelectionChange(true) },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = leftTextColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = leftText,
                    color = leftTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(rightBackgroundColor)
                .clickable { onSelectionChange(false) },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = null,
                    tint = rightTextColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = rightText,
                    color = rightTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SortOptions(
    selectedType: SortType,
    onTypeChange: (SortType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SortOption(
            icon = Icons.Default.Title,
            label = "Title",
            isSelected = selectedType == SortType.TITLE,
            onClick = { onTypeChange(SortType.TITLE) }
        )
        SortOption(
            icon = Icons.Default.AddCircle,
            label = "Added",
            isSelected = selectedType == SortType.ADDED,
            onClick = { onTypeChange(SortType.ADDED) }
        )
        SortOption(
            icon = Icons.Default.DateRange,
            label = "Released",
            isSelected = selectedType == SortType.DATE,
            onClick = { onTypeChange(SortType.DATE) }
        )
        SortOption(
            icon = Icons.Default.Star,
            label = "Rating",
            isSelected = selectedType == SortType.RATING,
            onClick = { onTypeChange(SortType.RATING) }
        )
    }
}

@Composable
private fun SortOption(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF8AB4F8) else Color(0xFF3A3E45),
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1A1E25) else Color(0xFF8AB4F8),
        animationSpec = tween(300),
        label = "iconTint"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = if (isSelected) ButtonPrimary else Color(0xFFB8B8B8),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun SortOrderButton(
    sortType: SortType,
    sortOrder: SortOrder,
    onOrderChange: (SortOrder) -> Unit
) {
    val (leftText, rightText) = when (sortType) {
        SortType.TITLE -> "A-Z" to "Z-A"
        SortType.ADDED -> "Oldest" to "Newest"
        SortType.DATE -> "Oldest" to "Newest"
        SortType.RATING -> "Lowest" to "Highest"
    }
    
    val leftBackgroundColor by animateColorAsState(
        targetValue = if (sortOrder == SortOrder.ASCENDING) Color(0xFF4A5A6A) else Color.Transparent,
        animationSpec = tween(300),
        label = "leftBackground"
    )
    val rightBackgroundColor by animateColorAsState(
        targetValue = if (sortOrder == SortOrder.DESCENDING) Color(0xFF4A5A6A) else Color.Transparent,
        animationSpec = tween(300),
        label = "rightBackground"
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, ButtonPrimary, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(leftBackgroundColor)
                .clickable { onOrderChange(SortOrder.ASCENDING) },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = if (sortOrder == SortOrder.ASCENDING) ButtonPrimary else Color(0xFF6B7178),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = leftText,
                    color = if (sortOrder == SortOrder.ASCENDING) ButtonPrimary else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(rightBackgroundColor)
                .clickable { onOrderChange(SortOrder.DESCENDING) },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (sortOrder == SortOrder.DESCENDING) ButtonPrimary else Color(0xFF6B7178),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = rightText,
                    color = if (sortOrder == SortOrder.DESCENDING) ButtonPrimary else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldsSelection(
    mediaLayout: MediaLayout,
    showDateAdded: Boolean,
    showReleaseDate: Boolean,
    onDateAddedChange: (Boolean) -> Unit,
    onReleaseDateChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date Added chip
        FilterChip(
            selected = showDateAdded,
            onClick = { onDateAddedChange(!showDateAdded) },
            label = {
                Text(
                    text = "Date Added",
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                if (mediaLayout == MediaLayout.GRID) {
                    // Radio button icon for GRID mode
                    Icon(
                        imageVector = if (showDateAdded) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    // Checkbox icon for LIST mode
                    if (showDateAdded) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF4A5A6A),
                selectedLabelColor = ButtonPrimary,
                selectedLeadingIconColor = ButtonPrimary,
                containerColor = Color(0xFF353940),
                labelColor = Color(0xFFB8B8B8),
                iconColor = Color(0xFF6B7178)
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = showDateAdded,
                borderColor = if (showDateAdded) ButtonPrimary else Color(0xFF4A5057),
                selectedBorderColor = ButtonPrimary,
                borderWidth = 1.5.dp,
                selectedBorderWidth = 1.5.dp
            ),
            modifier = Modifier.weight(1f)
        )

        // Release Date chip
        FilterChip(
            selected = showReleaseDate,
            onClick = { onReleaseDateChange(!showReleaseDate) },
            label = {
                Text(
                    text = "Release Date",
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                if (mediaLayout == MediaLayout.GRID) {
                    // Radio button icon for GRID mode
                    Icon(
                        imageVector = if (showReleaseDate) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    // Checkbox icon for LIST mode
                    if (showReleaseDate) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF4A5A6A),
                selectedLabelColor = ButtonPrimary,
                selectedLeadingIconColor = ButtonPrimary,
                containerColor = Color(0xFF353940),
                labelColor = Color(0xFFB8B8B8),
                iconColor = Color(0xFF6B7178)
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = showReleaseDate,
                borderColor = if (showReleaseDate) ButtonPrimary else Color(0xFF4A5057),
                selectedBorderColor = ButtonPrimary,
                borderWidth = 1.5.dp,
                selectedBorderWidth = 1.5.dp
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FieldChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ButtonPrimary else Color(0xFF4A5057),
        animationSpec = tween(300),
        label = "borderColor"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF4A5A6A) else Color.Transparent,
        animationSpec = tween(300),
        label = "backgroundColor"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF8AB4F8) else Color.White,
        animationSpec = tween(300),
        label = "textColor"
    )
    
    Box(
        modifier = modifier
            .height(48.dp)
            .scale(scale)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(24.dp)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Crossfade(
                targetState = isSelected,
                animationSpec = tween(200),
                label = "iconCrossfade"
            ) { selected ->
                Icon(
                    imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (selected) Color(0xFF8AB4F8) else Color(0xFF6B7178),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun QuickSettingsDialogPreview() {
    var state by remember { mutableStateOf(QuickSettingsState()) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF2A2E35)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Quick Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SectionTitle("Media Layout")
                Spacer(modifier = Modifier.height(12.dp))
                DualSegmentedButton(
                    leftText = "List",
                    rightText = "Grid",
                    isLeftSelected = state.mediaLayout == MediaLayout.LIST,
                    onSelectionChange = { isLeft ->
                        state = state.copy(mediaLayout = if (isLeft) MediaLayout.LIST else MediaLayout.GRID)
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SectionTitle("Sort")
                Spacer(modifier = Modifier.height(12.dp))
                SortOptions(
                    selectedType = state.sortType,
                    onTypeChange = { state = state.copy(sortType = it) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SortOrderButton(
                    sortType = state.sortType,
                    sortOrder = state.sortOrder,
                    onOrderChange = { state = state.copy(sortOrder = it) }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SectionTitle("Fields")
                Spacer(modifier = Modifier.height(12.dp))
                FieldsSelection(
                    mediaLayout = state.mediaLayout,
                    showDateAdded = state.showDateAdded,
                    showReleaseDate = state.showReleaseDate,
                    onDateAddedChange = { state = state.copy(showDateAdded = it) },
                    onReleaseDateChange = { state = state.copy(showReleaseDate = it) }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {}) {
                        Text("Cancel", color = Color(0xFF8AB4F8))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(onClick = {}) {
                        Text("Done", color = Color(0xFF8AB4F8))
                    }
                }
            }
        }
    }
}
