package com.example.arcadia.presentation.screens.profile

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ModalBottomSheet
import com.example.arcadia.presentation.screens.searchScreen.components.SearchField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.arcadia.R
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.ProfileSection
import com.example.arcadia.domain.model.ProfileSectionType
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent
import com.example.arcadia.ui.theme.getRatingColor
import com.example.arcadia.util.DisplayResult
import org.koin.androidx.compose.koinViewModel

// Platform colors
private val SteamColor = Color(0xFF1B2838)
private val XboxColor = Color(0xFF107C10)
private val PlayStationColor = Color(0xFF003087)
private val NeonBlue = Color(0xFF00D4FF)
private val NeonPurple = Color(0xFFBD00FF)
private val NeonPink = Color(0xFFFF006E)
private val CardBackground = Color(0xFF0A1F4D)
private val CardBorder = Color(0xFF1E3A8A)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(
    userId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToMyGames: (userId: String?, username: String?) -> Unit = { _, _ -> },
    onGameClick: (Int) -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val screenReady = viewModel.screenReady
    val profileState = viewModel.profileState
    val statsState = viewModel.statsState
    val libraryGames by viewModel.libraryGames.collectAsState()
    val customSections = viewModel.customSections
    val isCurrentUser = viewModel.isCurrentUser
    val context = LocalContext.current

    var showAddSectionSheet by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showLibrarySection by remember { mutableStateOf(true) }
    var sectionToEdit by remember { mutableStateOf<ProfileSection?>(null) }
    
    // Animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
        isVisible = true
    }

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PROFILE",
                        fontSize = 28.sp,
                        fontFamily = BebasNeueFont,
                        color = TextSecondary,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextSecondary
                        )
                    }
                },
                actions = {
                    if (isCurrentUser) {
                        IconButton(onClick = onNavigateToEditProfile) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile",
                                tint = ButtonPrimary
                            )
                        }
                    }
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Profile",
                            tint = YellowAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface
                )
            )
        },
        floatingActionButton = {
            if (isCurrentUser) {
                FloatingActionButton(
                    onClick = { showAddSectionSheet = true },
                    containerColor = ButtonPrimary,
                    contentColor = Surface,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Section")
                }
            }
        }
    ) { paddingValues ->
        screenReady.DisplayResult(
            modifier = Modifier.padding(paddingValues),
            onLoading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(color = ButtonPrimary)
                }
            },
            onError = { errorMessage ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF3535),
                        textAlign = TextAlign.Center
                    )
                }
            },
            onSuccess = {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 10 }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Profile Header
                        ProfileHeader(
                            imageUrl = profileState.profileImageUrl,
                            name = profileState.name,
                            username = profileState.username,
                            location = buildString {
                                if (!profileState.city.isNullOrEmpty()) append(profileState.city)
                                if (!profileState.city.isNullOrEmpty() && !profileState.country.isNullOrEmpty()) append(", ")
                                if (!profileState.country.isNullOrEmpty()) append(profileState.country)
                            }.takeIf { it.isNotEmpty() }
                        )

                        // Bio Section
                        if (!profileState.description.isNullOrEmpty()) {
                            BioCard(bio = profileState.description)
                        }

                        // Gaming Stats Card
                        GamingStatsCard(statsState = statsState)

                        // Gaming Platforms Section
                        GamingPlatformsCard(
                            steamId = profileState.steamId,
                            xboxGamertag = profileState.xboxGamertag,
                            psnId = profileState.psnId
                        )

                        // Custom Profile Sections
                        customSections.forEach { section ->
                            CustomSectionCard(
                                section = section,
                                games = libraryGames.filter { it.rawgId in section.gameIds },
                                onGameClick = onGameClick,
                                onEditClick = if (isCurrentUser) { { sectionToEdit = section } } else null,
                                onDeleteClick = if (isCurrentUser) { { viewModel.deleteCustomSection(section.id) } } else null
                            )
                        }

                        // My Library Preview
                        LibraryPreviewCard(
                            games = libraryGames.take(10),
                            totalGames = statsState.totalGames,
                            onGameClick = onGameClick,
                            onSeeAllClick = { 
                                // Pass userId and username only if viewing another user's profile
                                if (isCurrentUser) {
                                    onNavigateToMyGames(null, null)
                                } else {
                                    onNavigateToMyGames(userId, profileState.username)
                                }
                            },
                            expanded = showLibrarySection,
                            onExpandClick = { showLibrarySection = !showLibrarySection }
                        )

                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        )
    }

    // Add Section Bottom Sheet
    if (showAddSectionSheet) {
        AddSectionBottomSheet(
            libraryGames = libraryGames,
            onDismiss = { showAddSectionSheet = false },
            onSave = { title, type, gameIds ->
                viewModel.addCustomSection(title, type, gameIds)
                showAddSectionSheet = false
            }
        )
    }

    // Edit Section Bottom Sheet
    sectionToEdit?.let { section ->
        AddSectionBottomSheet(
            libraryGames = libraryGames,
            existingSection = section,
            onDismiss = { sectionToEdit = null },
            onSave = { title, type, gameIds ->
                viewModel.updateCustomSection(section.id, title, type, gameIds)
                sectionToEdit = null
            }
        )
    }

    // Share Dialog
    if (showShareDialog) {
        ShareProfileDialog(
            profileState = profileState,
            statsState = statsState,
            libraryGames = libraryGames,
            customSections = customSections,
            onDismiss = { showShareDialog = false },
            onShareText = {
                shareProfileAsText(context, profileState, statsState, libraryGames, customSections)
                showShareDialog = false
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    imageUrl: String?,
    name: String,
    username: String,
    location: String?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(160.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ButtonPrimary.copy(alpha = glowAlpha),
                                NeonPurple.copy(alpha = glowAlpha * 0.6f),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = (size.width / 2) * glowScale
                        )
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(colors = listOf(ButtonPrimary, NeonPurple)))
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Surface),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null && imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.size(132.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        error = rememberVectorPainter(Icons.Default.Person),
                        placeholder = rememberVectorPainter(Icons.Default.Person)
                    )
                } else {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 56.sp,
                        fontFamily = BebasNeueFont,
                        color = ButtonPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = name, 
            fontSize = 32.sp, 
            fontFamily = BebasNeueFont, 
            color = TextSecondary, 
            letterSpacing = 1.5.sp
        )

        if (username.isNotEmpty()) {
            Text(
                text = "@$username", 
                fontSize = 18.sp, 
                color = ButtonPrimary, 
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (location != null && location.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier
                    .padding(top = 12.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = location, fontSize = 14.sp, color = TextSecondary.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun BioCard(bio: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = "ABOUT", fontSize = 16.sp, fontFamily = BebasNeueFont, color = ButtonPrimary, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = bio, fontSize = 15.sp, color = TextSecondary.copy(alpha = 0.9f), lineHeight = 24.sp)
        }
    }
}

@Composable
private fun GamingStatsCard(statsState: ProfileStatsState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(painter = painterResource(id = R.drawable.controller), contentDescription = null, tint = YellowAccent, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "GAMING STATS", fontSize = 18.sp, fontFamily = BebasNeueFont, color = YellowAccent, letterSpacing = 2.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(value = statsState.totalGames.toString(), label = "Games", color = ButtonPrimary, modifier = Modifier.weight(1f))
                StatItem(value = statsState.finishedGames.toString(), label = "Finished", color = YellowAccent, modifier = Modifier.weight(1f))
                StatItem(value = statsState.playingGames.toString(), label = "Playing", color = NeonPink, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(value = statsState.droppedGames.toString(), label = "Dropped", color = Color(0xFFFF5555), modifier = Modifier.weight(1f))
                StatItem(value = statsState.onHoldGames.toString(), label = "On Hold", color = Color(0xFFFFB74D), modifier = Modifier.weight(1f))
                StatItem(value = statsState.wantToPlayGames.toString(), label = "Want to Play", color = Color(0xFF64B5F6), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Divider
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            
            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatItem(value = "${statsState.hoursPlayed}h", label = "Hours", color = NeonBlue, modifier = Modifier.weight(1f))
                StatItem(
                    value = if (statsState.avgRating > 0) String.format("%.1f", statsState.avgRating) else "-",
                    label = "Avg Rating",
                    color = Color(0xFF4ADE80),
                    modifier = Modifier.weight(1f)
                )
                StatItem(value = "${statsState.completionRate.toInt()}%", label = "Completion", color = NeonPurple, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = value, 
            fontSize = 32.sp, 
            fontFamily = BebasNeueFont, 
            color = color,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = color.copy(alpha = 0.5f),
                    blurRadius = 10f
                )
            )
        )
        Text(text = label, fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.7f), textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GamingPlatformsCard(steamId: String?, xboxGamertag: String?, psnId: String?) {
    val hasAnyPlatform = !steamId.isNullOrEmpty() || !xboxGamertag.isNullOrEmpty() || !psnId.isNullOrEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(painter = painterResource(id = R.drawable.controller), contentDescription = null, tint = NeonBlue, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "GAMING PLATFORMS", fontSize = 18.sp, fontFamily = BebasNeueFont, color = NeonBlue, letterSpacing = 2.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (hasAnyPlatform) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!steamId.isNullOrEmpty()) {
                        PlatformBadge("Steam", steamId, SteamColor, R.drawable.pc_ic)
                    }
                    if (!xboxGamertag.isNullOrEmpty()) {
                        PlatformBadge("Xbox", xboxGamertag, XboxColor, R.drawable.xbox_ic)
                    }
                    if (!psnId.isNullOrEmpty()) {
                        PlatformBadge("PlayStation", psnId, PlayStationColor, R.drawable.playstation_ic)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                        .border(1.dp, TextSecondary.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(24.dp), 
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "No gaming platforms linked", fontSize = 16.sp, color = TextSecondary.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Add your Steam, Xbox, or PSN ID", fontSize = 14.sp, color = ButtonPrimary.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformBadge(platformName: String, gamertag: String, platformColor: Color, iconRes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(platformColor.copy(alpha = 0.15f))
            .border(1.dp, platformColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(platformColor)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape), 
            contentAlignment = Alignment.Center
        ) {
            Icon(painter = painterResource(id = iconRes), contentDescription = platformName, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = platformName, fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
            Text(text = gamertag, fontSize = 18.sp, color = TextSecondary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CustomSectionCard(
    section: ProfileSection,
    games: List<GameListEntry>,
    onGameClick: (Int) -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(painter = painterResource(id = R.drawable.controller), contentDescription = null, tint = NeonPurple, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = section.title.uppercase(),
                    fontSize = 18.sp,
                    fontFamily = BebasNeueFont,
                    color = NeonPurple,
                    letterSpacing = 2.sp,
                    modifier = Modifier.weight(1f)
                )
                // Only show edit/delete icons if callbacks are provided (own profile)
                if (onEditClick != null) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                    }
                }
                if (onDeleteClick != null) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5555).copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (games.isEmpty()) {
                Text(text = "No games added yet", fontSize = 14.sp, color = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 16.dp))
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp), 
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(games) { game ->
                        GameCardBig(game = game, onClick = { onGameClick(game.rawgId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCardBig(game: GameListEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(200.dp)) {
                AsyncImage(
                    model = game.backgroundImage,
                    contentDescription = game.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
                
                // Rating badge
                game.rating?.let { rating ->
                    if (rating > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = YellowAccent, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", rating),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = game.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun LibraryPreviewCard(
    games: List<GameListEntry>,
    totalGames: Int,
    onGameClick: (Int) -> Unit,
    onSeeAllClick: () -> Unit,
    expanded: Boolean,
    onExpandClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onExpandClick)
            ) {
                Icon(painter = painterResource(id = R.drawable.controller), contentDescription = null, tint = YellowAccent, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "MY LIBRARY", fontSize = 18.sp, fontFamily = BebasNeueFont, color = YellowAccent, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onSeeAllClick) {
                    Text(text = "See All ($totalGames)", fontSize = 14.sp, color = ButtonPrimary)
                }
            }

            AnimatedVisibility(
                visible = expanded || games.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    if (games.isEmpty()) {
                        Text(text = "Your library is empty", fontSize = 14.sp, color = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp), 
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(games) { game ->
                                GameCardBig(game = game, onClick = { onGameClick(game.rawgId) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSectionBottomSheet(
    libraryGames: List<GameListEntry>,
    existingSection: ProfileSection? = null,
    onDismiss: () -> Unit,
    onSave: (String, ProfileSectionType, List<Int>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(existingSection?.title ?: "") }
    var selectedType by remember { mutableStateOf(existingSection?.type ?: ProfileSectionType.SINGLE_GAME) }
    var selectedGameIds by remember { mutableStateOf(existingSection?.gameIds ?: emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter games based on search query
    val filteredGames = remember(libraryGames, searchQuery) {
        if (searchQuery.isBlank()) {
            libraryGames
        } else {
            libraryGames.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Surface) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(24.dp)
        ) {
            item {
                Text(
                    text = if (existingSection != null) "EDIT SECTION" else "ADD NEW SECTION",
                    fontSize = 28.sp,
                    fontFamily = BebasNeueFont,
                    color = TextSecondary,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Section Title") },
                    placeholder = { Text("e.g., Favorite Game of All Time") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ButtonPrimary,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                        focusedLabelColor = ButtonPrimary,
                        unfocusedLabelColor = TextSecondary.copy(alpha = 0.6f),
                        focusedTextColor = TextSecondary,
                        unfocusedTextColor = TextSecondary
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Section Type", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTypeChip(
                        text = "Single Game",
                        selected = selectedType == ProfileSectionType.SINGLE_GAME,
                        onClick = {
                            selectedType = ProfileSectionType.SINGLE_GAME
                            if (selectedGameIds.size > 1) selectedGameIds = selectedGameIds.take(1)
                        }
                    )
                    SectionTypeChip(
                        text = "Game List",
                        selected = selectedType == ProfileSectionType.GAME_LIST,
                        onClick = { selectedType = ProfileSectionType.GAME_LIST }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Select Games from Your Library", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Search field for filtering library games
                SearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search your library...",
                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (libraryGames.isEmpty()) {
                item {
                    Text(text = "Add games to your library first", fontSize = 14.sp, color = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 16.dp))
                }
            } else if (filteredGames.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    Text(
                        text = "No games found matching \"$searchQuery\"",
                        fontSize = 14.sp,
                        color = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(filteredGames.chunked(3)) { rowGames ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        rowGames.forEach { game ->
                            val isSelected = game.rawgId in selectedGameIds
                            GameSelectionCard(
                                game = game,
                                isSelected = isSelected,
                                onClick = {
                                    selectedGameIds = if (isSelected) {
                                        selectedGameIds - game.rawgId
                                    } else {
                                        if (selectedType == ProfileSectionType.SINGLE_GAME) listOf(game.rawgId)
                                        else selectedGameIds + game.rawgId
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowGames.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { if (title.isNotBlank() && selectedGameIds.isNotEmpty()) onSave(title, selectedType, selectedGameIds) },
                    enabled = title.isNotBlank() && selectedGameIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary, disabledContainerColor = ButtonPrimary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = if (existingSection != null) "UPDATE SECTION" else "ADD SECTION", fontFamily = BebasNeueFont, fontSize = 20.sp, letterSpacing = 1.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionTypeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) ButtonPrimary else Color.Transparent)
            .border(1.dp, if (selected) ButtonPrimary else TextSecondary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(text = text, fontSize = 14.sp, color = if (selected) Surface else TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GameSelectionCard(game: GameListEntry, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .aspectRatio(0.75f)
            .clickable(onClick = onClick)
            .then(if (isSelected) Modifier.border(2.dp, ButtonPrimary, RoundedCornerShape(12.dp)) else Modifier),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) ButtonPrimary.copy(alpha = 0.1f) else CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            AsyncImage(model = game.backgroundImage, contentDescription = game.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 0f, endY = Float.POSITIVE_INFINITY)
                )
            )
            Text(
                text = game.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(ButtonPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(id = R.drawable.finished_ic), contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ShareProfileDialog(
    profileState: ProfileState,
    statsState: ProfileStatsState,
    libraryGames: List<GameListEntry>,
    customSections: List<ProfileSection>,
    onDismiss: () -> Unit,
    onShareText: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = {
            Text(text = "SHARE PROFILE", fontFamily = BebasNeueFont, fontSize = 24.sp, color = TextSecondary, letterSpacing = 2.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Share your gaming profile with friends:", color = TextSecondary.copy(alpha = 0.8f))
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "• ${statsState.totalGames} games in library", color = TextSecondary.copy(alpha = 0.7f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "• ${customSections.size} custom sections", color = TextSecondary.copy(alpha = 0.7f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "• Profile Link included", color = ButtonPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onShareText, 
                colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Profile Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel", color = TextSecondary) }
        }
    )
}

private fun shareProfileAsText(
    context: Context,
    profileState: ProfileState,
    statsState: ProfileStatsState,
    libraryGames: List<GameListEntry>,
    customSections: List<ProfileSection>
) {
    val shareText = buildString {
        appendLine("🎮 Check out ${profileState.name}'s Gaming Profile on Arcadia!")
        appendLine("https://mrxgamer999.github.io/Arcadia/profile?id=${profileState.id}")
        appendLine()
        
        if (profileState.username.isNotEmpty()) appendLine("@${profileState.username}")
        appendLine()

        appendLine("📊 Gaming Stats:")
        appendLine("• Total Games: ${statsState.totalGames}")
        appendLine("• Finished: ${statsState.finishedGames}")
        appendLine("• Hours Played: ${statsState.hoursPlayed}h")
        if (statsState.avgRating > 0) appendLine("• Avg Rating: ${String.format("%.1f", statsState.avgRating)}/10")
        appendLine()

        if (customSections.isNotEmpty()) {
            customSections.forEach { section ->
                val sectionGames = libraryGames.filter { it.rawgId in section.gameIds }
                appendLine("⭐ ${section.title}:")
                sectionGames.take(3).forEach { game ->
                    val ratingStr = game.rating?.let { " (${String.format("%.1f", it)}/10)" } ?: ""
                    appendLine("  • ${game.name}$ratingStr")
                }
                if (sectionGames.size > 3) appendLine("  ...and ${sectionGames.size - 3} more")
                appendLine()
            }
        }

        val topGames = libraryGames.filter { it.rating != null && it.rating > 0 }.sortedByDescending { it.rating }.take(3)
        if (topGames.isNotEmpty()) {
            appendLine("🏆 Top Rated Games:")
            topGames.forEach { game -> appendLine("  • ${game.name} (${String.format("%.1f", game.rating)}/10)") }
            appendLine()
        }

        appendLine("📱 Download Arcadia to view full profile and library!")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share your gaming profile"))
}
