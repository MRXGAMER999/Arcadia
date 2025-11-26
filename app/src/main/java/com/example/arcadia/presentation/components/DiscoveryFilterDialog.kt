package com.example.arcadia.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.arcadia.domain.model.DiscoveryFilterState
import com.example.arcadia.domain.model.DiscoverySortOrder
import com.example.arcadia.domain.model.DiscoverySortType
import com.example.arcadia.domain.model.ReleaseTimeframe
import com.example.arcadia.ui.theme.ButtonPrimary

@Composable
fun DiscoveryFilterDialog(
    state: DiscoveryFilterState,
    onStateChange: (DiscoveryFilterState) -> Unit,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onDeveloperSearch: (String) -> Unit,
    onSelectDeveloperWithStudios: (String, Set<String>) -> Unit,
    onClearAllFilters: () -> Unit
) {
    var genresExpanded by remember { mutableStateOf(false) }
    var timeframeExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    // Scroll to bottom when sections expand
    LaunchedEffect(genresExpanded, timeframeExpanded) {
        if (genresExpanded || timeframeExpanded) {
            kotlinx.coroutines.delay(100)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0.75f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) +
                    slideInVertically(
                        initialOffsetY = { it / 3 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ),
            exit = fadeOut(animationSpec = tween(250)) +
                   scaleOut(
                       targetScale = 0.85f,
                       animationSpec = tween(250, easing = FastOutSlowInEasing)
                   ) +
                   slideOutVertically(
                       targetOffsetY = { it / 3 },
                       animationSpec = tween(250, easing = FastOutSlowInEasing)
                   )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF2A2E35)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp)
                            .padding(top = 20.dp, bottom = 12.dp)
                    ) {
                        // Title
                        Text(
                            text = "Discovery Filters",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Developer Search Section
                        SectionTitle("Developers / Publishers")
                        Spacer(modifier = Modifier.height(8.dp))
                        DeveloperSearchField(
                            searchQuery = state.developerSearchQuery,
                            isLoading = state.isLoadingDevelopers,
                            onSearchChange = { query ->
                                onStateChange(state.copy(developerSearchQuery = query))
                                if (query.length >= 2) {
                                    onDeveloperSearch(query)
                                }
                            },
                            onClearSearch = {
                                onStateChange(state.copy(
                                    developerSearchQuery = "",
                                    searchResults = emptyList()
                                ))
                            }
                        )
                        
                        // Selected developers
                        if (state.selectedDevelopers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.selectedDevelopers.forEach { (developer, subStudios) ->
                                    SelectedDeveloperChip(
                                        name = developer,
                                        subStudioCount = subStudios.size,
                                        onRemove = {
                                            onStateChange(state.copy(
                                                selectedDevelopers = state.selectedDevelopers - developer
                                            ))
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Search Results
                        AnimatedVisibility(
                            visible = state.searchResults.isNotEmpty() && state.developerSearchQuery.isNotEmpty(),
                            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF353940))
                            ) {
                                state.searchResults
                                    .filter { it !in state.selectedDevelopers.keys }
                                    .take(6)
                                    .forEachIndexed { index, developer ->
                                        if (index > 0) {
                                            HorizontalDivider(color = Color(0xFF3A3E45))
                                        }
                                        DeveloperSearchResultItem(
                                            name = developer,
                                            onClick = { 
                                                onSelectDeveloperWithStudios(developer, state.expandedStudios)
                                            }
                                        )
                                    }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Sort
                        SectionTitle("Sort")
                        Spacer(modifier = Modifier.height(8.dp))
                        SortOptions(
                            selectedType = state.sortType,
                            onTypeChange = { onStateChange(state.copy(sortType = it)) }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        SortOrderButton(
                            sortType = state.sortType,
                            sortOrder = state.sortOrder,
                            onOrderChange = { onStateChange(state.copy(sortOrder = it)) }
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Genre Filters
                        FilterSection(
                            title = "Genre Filters",
                            isExpanded = genresExpanded,
                            selectedCount = state.selectedGenres.size,
                            onToggleExpanded = { genresExpanded = !genresExpanded },
                            onClear = { onStateChange(state.copy(selectedGenres = emptySet())) }
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                state.availableGenres.forEach { genre ->
                                    FilterChipItem(
                                        text = genre,
                                        isSelected = genre in state.selectedGenres,
                                        onClick = {
                                            val newGenres = if (state.selectedGenres.contains(genre)) {
                                                state.selectedGenres - genre
                                            } else {
                                                state.selectedGenres + genre
                                            }
                                            onStateChange(state.copy(selectedGenres = newGenres))
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Release Period Filters
                        FilterSection(
                            title = "Release Period",
                            isExpanded = timeframeExpanded,
                            selectedCount = if (state.releaseTimeframe != ReleaseTimeframe.ALL) 1 else 0,
                            onToggleExpanded = { timeframeExpanded = !timeframeExpanded },
                            onClear = { onStateChange(state.copy(releaseTimeframe = ReleaseTimeframe.ALL)) }
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ReleaseTimeframe.entries.forEach { timeframe ->
                                    TimeframeOptionItem(
                                        timeframe = timeframe,
                                        isSelected = state.releaseTimeframe == timeframe,
                                        onClick = { onStateChange(state.copy(releaseTimeframe = timeframe)) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Clear All Filters Button
                        AnimatedVisibility(
                            visible = state.hasActiveFilters,
                            enter = fadeIn(tween(300)) + expandVertically(tween(300)) + scaleIn(initialScale = 0.9f),
                            exit = fadeOut(tween(200)) + shrinkVertically(tween(200)) + scaleOut(targetScale = 0.9f)
                        ) {
                            Column {
                                Button(
                                    onClick = onClearAllFilters,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF3A3E45),
                                        contentColor = Color(0xFFFF6B6B)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Clear All Filters",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Fixed action buttons at bottom
                    HorizontalDivider(
                        color = Color(0xFF3A3E45),
                        thickness = 1.dp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Cancel",
                                color = Color(0xFF8AB4F8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = onApply,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonPrimary,
                                contentColor = Color(0xFF1A1E25)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text(
                                text = if (state.hasActiveFilters) "Apply (${state.activeFilterCount})" else "Done",
                                fontSize = 14.sp,
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
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFFB8B8B8)
    )
}

@Composable
private fun DeveloperSearchField(
    searchQuery: String,
    isLoading: Boolean,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        placeholder = {
            Text(
                text = "Search developers...",
                color = Color(0xFF6B7178),
                fontSize = 14.sp,
                maxLines = 1
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF6B7178),
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = ButtonPrimary,
                    strokeWidth = 2.dp
                )
                searchQuery.isNotEmpty() -> IconButton(
                    onClick = onClearSearch,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = Color(0xFF6B7178),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ButtonPrimary,
            unfocusedBorderColor = Color(0xFF4A5057),
            focusedContainerColor = Color(0xFF353940),
            unfocusedContainerColor = Color(0xFF353940),
            cursorColor = ButtonPrimary,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun SelectedDeveloperChip(
    name: String,
    subStudioCount: Int,
    onRemove: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    Surface(
        modifier = Modifier.scale(scale),
        shape = RoundedCornerShape(18.dp),
        color = ButtonPrimary
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1E25),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subStudioCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1A1E25).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "+$subStudioCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1E25),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color(0xFF1A1E25),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DeveloperSearchResultItem(
    name: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Business,
            contentDescription = null,
            tint = Color(0xFF8AB4F8),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = name,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.Default.Add,
            contentDescription = "Add",
            tint = ButtonPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SortOptions(
    selectedType: DiscoverySortType,
    onTypeChange: (DiscoverySortType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SortOption(
            icon = Icons.Default.AutoAwesome,
            label = "AI",
            isSelected = selectedType == DiscoverySortType.AI_RECOMMENDATION,
            onClick = { onTypeChange(DiscoverySortType.AI_RECOMMENDATION) }
        )
        SortOption(
            icon = Icons.Default.Star,
            label = "Rating",
            isSelected = selectedType == DiscoverySortType.RATING,
            onClick = { onTypeChange(DiscoverySortType.RATING) }
        )
        SortOption(
            icon = Icons.Default.DateRange,
            label = "Released",
            isSelected = selectedType == DiscoverySortType.RELEASE_DATE,
            onClick = { onTypeChange(DiscoverySortType.RELEASE_DATE) }
        )
        SortOption(
            icon = Icons.Default.TrendingUp,
            label = "Popular",
            isSelected = selectedType == DiscoverySortType.POPULARITY,
            onClick = { onTypeChange(DiscoverySortType.POPULARITY) }
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
        targetValue = if (isPressed) 0.88f else if (isSelected) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF8AB4F8) else Color(0xFF3A3E45),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "backgroundColor"
    )
    
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1A1E25) else Color(0xFF8AB4F8),
        animationSpec = tween(350),
        label = "iconTint"
    )
    
    // Pulsing animation for selected item
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSelected) 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale * pulseScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = if (isSelected) ButtonPrimary else Color(0xFFB8B8B8),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SortOrderButton(
    sortType: DiscoverySortType,
    sortOrder: DiscoverySortOrder,
    onOrderChange: (DiscoverySortOrder) -> Unit
) {
    val (leftText, rightText) = when (sortType) {
        DiscoverySortType.NAME -> "A-Z" to "Z-A"
        DiscoverySortType.RELEASE_DATE -> "Oldest" to "Newest"
        DiscoverySortType.RATING -> "Lowest" to "Highest"
        DiscoverySortType.POPULARITY -> "Least" to "Most"
        DiscoverySortType.AI_RECOMMENDATION -> "Low" to "High"
    }
    
    val leftBackgroundColor by animateColorAsState(
        targetValue = if (sortOrder == DiscoverySortOrder.ASCENDING) Color(0xFF4A5A6A) else Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "leftBackground"
    )
    val rightBackgroundColor by animateColorAsState(
        targetValue = if (sortOrder == DiscoverySortOrder.DESCENDING) Color(0xFF4A5A6A) else Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rightBackground"
    )
    
    val leftIconOffset by animateDpAsState(
        targetValue = if (sortOrder == DiscoverySortOrder.ASCENDING) (-2).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "leftIconOffset"
    )

    val rightIconOffset by animateDpAsState(
        targetValue = if (sortOrder == DiscoverySortOrder.DESCENDING) 2.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rightIconOffset"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .border(1.5.dp, ButtonPrimary, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(leftBackgroundColor)
                .clickable { onOrderChange(DiscoverySortOrder.ASCENDING) },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = if (sortOrder == DiscoverySortOrder.ASCENDING) ButtonPrimary else Color(0xFF6B7178),
                    modifier = Modifier
                        .size(19.dp)
                        .offset(y = leftIconOffset)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = leftText,
                    color = if (sortOrder == DiscoverySortOrder.ASCENDING) ButtonPrimary else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(rightBackgroundColor)
                .clickable { onOrderChange(DiscoverySortOrder.DESCENDING) },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (sortOrder == DiscoverySortOrder.DESCENDING) ButtonPrimary else Color(0xFF6B7178),
                    modifier = Modifier
                        .size(19.dp)
                        .offset(y = rightIconOffset)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = rightText,
                    color = if (sortOrder == DiscoverySortOrder.DESCENDING) ButtonPrimary else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    isExpanded: Boolean,
    selectedCount: Int,
    onToggleExpanded: () -> Unit,
    onClear: () -> Unit,
    content: @Composable () -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rotationAngle"
    )
    
    val badgeScale by animateFloatAsState(
        targetValue = if (selectedCount > 0) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "badgeScale"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF353940))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleExpanded
                )
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (selectedCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .scale(badgeScale)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(ButtonPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedCount.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1E25)
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(
                    visible = selectedCount > 0,
                    enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f),
                    exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.8f)
                ) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = ButtonPrimary,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer(rotationZ = rotationAngle)
                )
            }
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) ButtonPrimary else Color(0xFF3A3E45),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "backgroundColor"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ButtonPrimary else Color(0xFF4A5057),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "borderColor"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1A1E25) else Color(0xFFB8B8B8),
        animationSpec = tween(300),
        label = "textColor"
    )
    
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.92f
            isSelected -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "elevation"
    )
    
    Surface(
        modifier = Modifier.scale(scale),
        shape = RoundedCornerShape(18.dp),
        color = backgroundColor,
        shadowElevation = elevation,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.5f, animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    )),
                    exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.5f)
                ) {
                    Row {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF1A1E25),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
                Text(
                    text = text,
                    fontSize = 14.sp,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun TimeframeOptionItem(
    timeframe: ReleaseTimeframe,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF4A5A6A) else Color.Transparent,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "backgroundColor"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) ButtonPrimary else Color(0xFF6B7178),
        animationSpec = tween(300),
        label = "iconColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = timeframe.displayName,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFFB8B8B8)
        )
    }
}
