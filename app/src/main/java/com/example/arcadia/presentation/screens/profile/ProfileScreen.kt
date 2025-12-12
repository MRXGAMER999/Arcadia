package com.example.arcadia.presentation.screens.profile

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.domain.model.ProfileSection
import com.example.arcadia.domain.model.friend.FriendshipStatus
import com.example.arcadia.presentation.screens.profile.components.AddSectionBottomSheet
import com.example.arcadia.presentation.screens.profile.components.BadgesSection
import com.example.arcadia.presentation.screens.profile.components.BioCard
import com.example.arcadia.presentation.screens.profile.components.CustomSectionCard
import com.example.arcadia.presentation.screens.profile.components.GamingPlatformsCard
import com.example.arcadia.presentation.screens.profile.components.GamingStatsCard
import com.example.arcadia.presentation.screens.profile.components.LibraryPreviewCard
import com.example.arcadia.presentation.screens.friends.components.LimitReachedDialog
import com.example.arcadia.presentation.screens.profile.components.ProfileHeader
import com.example.arcadia.presentation.screens.profile.components.ShareProfileDialog
import com.example.arcadia.presentation.screens.profile.components.UnfriendConfirmationDialog
import com.example.arcadia.presentation.screens.profile.components.shareProfileAsText
import com.example.arcadia.presentation.components.AddGameSnackbar
import com.example.arcadia.presentation.components.BottomSlideSnackbarHost
import com.example.arcadia.presentation.components.UnsavedChangesSnackbar
import com.example.arcadia.presentation.screens.profile.SectionDraft
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent
import com.example.arcadia.util.DisplayResult
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.delay

import com.example.arcadia.presentation.components.common.PremiumSlideInItem
import com.example.arcadia.presentation.components.common.PremiumFloatingActionButton
import com.example.arcadia.presentation.components.common.PremiumScaleButton
import com.example.arcadia.presentation.components.common.PremiumScaleWrapper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProfileScreen(
    userId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToMyGames: (userId: String?, username: String?) -> Unit = { _, _ -> },
    onNavigateToRoast: (targetUserId: String?) -> Unit = {},
    onGameClick: (Int) -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(key = userId) { parametersOf(userId) }
) {
    val screenReady = viewModel.screenReady
    val profileState = viewModel.profileState
    val statsState = viewModel.statsState
    val libraryGames by viewModel.libraryGames.collectAsState()
    val featuredBadges by viewModel.featuredBadges.collectAsState()
    val customSections = viewModel.customSections
    val isCurrentUser = viewModel.isCurrentUser
    val context = LocalContext.current
    
    // Friendship state - Requirements: 8.1-8.4, 8.10, 8.11
    val friendshipState by viewModel.friendshipState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddSectionSheet by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showLibrarySection by remember { mutableStateOf(true) }
    var sectionToEdit by remember { mutableStateOf<ProfileSection?>(null) }
    var sectionDraftOverride by remember { mutableStateOf<SectionDraft?>(null) }
    
    // Animation state
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(userId) {
        // Profile is now loaded in init block of ViewModel
        isVisible = true
    }
    
    // Show error messages via snackbar
    LaunchedEffect(friendshipState.error) {
        friendshipState.error?.let { error ->
            try {
                snackbarHostState.showSnackbar(error)
            } finally {
                viewModel.clearFriendshipError()
            }
        }
    }
    
    // Reopen section sheet with draft when requested by ViewModel
    LaunchedEffect(viewModel.reopenSectionDraft) {
        viewModel.reopenSectionDraft?.let { draft ->
            sectionDraftOverride = draft
            sectionToEdit = viewModel.customSections.find { it.id == draft.id }
            showAddSectionSheet = true
            viewModel.consumeReopenSectionDraft()
        }
    }

    Scaffold(
        containerColor = Surface,
        snackbarHost = { BottomSlideSnackbarHost(hostState = snackbarHostState) },
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
                    // Roast Button (Visible for Self and Friends)
                    val showSelfRoast = isCurrentUser
                    val showFriendRoast = viewModel.shouldShowRoastButton()
                    
                    if (showSelfRoast || showFriendRoast) {
                        CoolRoastButton(
                            onClick = {
                                if (showSelfRoast) {
                                    onNavigateToRoast(null) // Self roast
                                } else {
                                    profileState.id.takeIf { it.isNotEmpty() }?.let { targetId ->
                                        onNavigateToRoast(targetId) // Friend roast
                                    }
                                }
                            }
                        )
                    }

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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            screenReady.DisplayResult(
                modifier = Modifier.fillMaxSize(),
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
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            // Profile Header
                            PremiumSlideInItem(index = 0) {
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
                            }
                            
                            // Friend Action Button - Requirements: 8.1-8.4, 8.10, 8.11
                            if (viewModel.shouldShowFriendActionButton()) {
                                PremiumSlideInItem(index = 1) {
                                    FriendActionButton(
                                        status = friendshipState.status,
                                        isLoading = friendshipState.isLoading,
                                        onClick = { viewModel.onFriendActionButtonClick() }
                                    )
                                }
                            }

                            // Bio Section
                            if (!profileState.description.isNullOrEmpty()) {
                                PremiumSlideInItem(index = 2) {
                                    BioCard(bio = profileState.description)
                                }
                            }

                            // Gaming Stats Card
                            PremiumSlideInItem(index = 3) {
                                GamingStatsCard(statsState = statsState)
                            }

                            // Featured Badges Section (Requirements: 9.1, 9.3)
                            // BadgesSection handles hiding itself when badges list is empty
                            PremiumSlideInItem(index = 4) {
                                BadgesSection(badges = featuredBadges)
                            }

                            // Gaming Platforms Section
                            PremiumSlideInItem(index = 5) {
                                GamingPlatformsCard(
                                    steamId = profileState.steamId,
                                    xboxGamertag = profileState.xboxGamertag,
                                    psnId = profileState.psnId
                                )
                            }

                            // Custom Profile Sections
                            customSections.forEachIndexed { index, section ->
                                key(section.id) {
                                    var isVisible by remember { mutableStateOf(true) }
                                    
                                    AnimatedVisibility(
                                        visible = isVisible,
                                        exit = shrinkVertically() + fadeOut(),
                                        enter = expandVertically() + fadeIn()
                                    ) {
                                        PremiumSlideInItem(index = 6 + index) {
                                            CustomSectionCard(
                                                section = section,
                                                games = libraryGames.filter { it.rawgId in section.gameIds },
                                                onGameClick = onGameClick,
                                                onEditClick = if (isCurrentUser) { { 
                                                    sectionDraftOverride = null
                                                    sectionToEdit = section
                                                    showAddSectionSheet = true
                                                } } else null,
                                                onDeleteClick = if (isCurrentUser) { { isVisible = false } } else null
                                            )
                                        }
                                    }
                                    
                                    LaunchedEffect(isVisible) {
                                        if (!isVisible) {
                                            delay(300) // Wait for animation
                                            viewModel.deleteCustomSection(section.id)
                                        }
                                    }
                                }
                            }

                            // My Library Preview
                            PremiumSlideInItem(index = 6 + customSections.size) {
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
                            }

                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            )

            if (isCurrentUser) {
                PremiumFloatingActionButton(
                    onClick = { 
                        sectionDraftOverride = null
                        sectionToEdit = null
                        showAddSectionSheet = true 
                    },
                    containerColor = ButtonPrimary,
                    contentColor = Surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Section")
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                viewModel.sectionSnackbarState?.let { state ->
                    AddGameSnackbar(
                        visible = true,
                        gameName = state.title,
                        message = state.message,
                        showUndo = state.showUndo,
                        onUndo = if (state.showUndo) {
                            { viewModel.undoDeleteSection() }
                        } else null,
                        onDismiss = { viewModel.onSectionSnackbarDismiss() }
                    )
                }

                viewModel.friendActionSnackbarState?.let { state ->
                    AddGameSnackbar(
                        visible = true,
                        gameName = state.username,
                        message = state.message,
                        showUndo = false,
                        onDismiss = { viewModel.dismissFriendActionSnackbar() }
                    )
                }

                UnsavedChangesSnackbar(
                    visible = viewModel.showUnsavedSectionSnackbar,
                    onReopen = { viewModel.reopenUnsavedSectionDraft() },
                    onSave = { viewModel.saveUnsavedSection() },
                    onDismiss = { viewModel.dismissUnsavedSectionSnackbar() },
                    bottomPadding = 0.dp
                )
            }
        }
    }

    // Add/Edit Section Bottom Sheet with unsaved-changes handling
    if (showAddSectionSheet) {
        AddSectionBottomSheet(
            libraryGames = libraryGames,
            existingSection = sectionToEdit,
            initialTitle = sectionDraftOverride?.title,
            initialType = sectionDraftOverride?.type,
            initialGameIds = sectionDraftOverride?.gameIds,
            baselineTitle = sectionToEdit?.title,
            baselineType = sectionToEdit?.type,
            baselineGameIds = sectionToEdit?.gameIds,
            onDismiss = { 
                sectionDraftOverride = null
                sectionToEdit = null
                showAddSectionSheet = false 
            },
            onSave = { title, type, gameIds ->
                val targetSectionId = sectionToEdit?.id ?: sectionDraftOverride?.id
                if (targetSectionId != null) {
                    viewModel.updateCustomSection(targetSectionId, title, type, gameIds)
                } else {
                    viewModel.addCustomSection(title, type, gameIds)
                }
                sectionDraftOverride = null
                sectionToEdit = null
                showAddSectionSheet = false
            },
            onDismissWithUnsavedChanges = { draft ->
                viewModel.showUnsavedSectionSnackbar(draft)
                sectionDraftOverride = null
                sectionToEdit = null
                showAddSectionSheet = false
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
    
    // Unfriend Confirmation Dialog - Requirements: 9.1, 9.3, 9.4
    if (friendshipState.showUnfriendDialog) {
        UnfriendConfirmationDialog(
            username = profileState.username,
            onConfirm = { viewModel.unfriend() },
            onDismiss = { viewModel.dismissUnfriendDialog() }
        )
    }
    
    // Limit Reached Dialog - Requirements: 3.21, 3.22, 3.26, 6.11
    friendshipState.limitReachedType?.let { limitType ->
        LimitReachedDialog(
            limitType = limitType,
            cooldownHours = friendshipState.cooldownHours,
            onDismiss = { viewModel.dismissLimitDialog() }
        )
    }
}

@Composable
private fun CoolRoastButton(onClick: () -> Unit) {
    PremiumScaleWrapper(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFF5722), CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🔥",
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Friend action button displayed on other users' profiles.
 * Shows different states based on friendship status.
 * 
 * Requirements: 8.1-8.4, 8.10
 * - 8.1: NOT_FRIENDS → "Add as Friend" button with plus icon
 * - 8.2: REQUEST_SENT → disabled "Request Pending" button with hourglass icon
 * - 8.3: REQUEST_RECEIVED → "Accept Request" button with checkmark icon
 * - 8.4: FRIENDS → "Friends" button with checkmark icon
 * - 8.10: Show loading indicator during operations
 */
@Composable
private fun FriendActionButton(
    status: FriendshipStatus,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val (text, icon, containerColor, enabled) = when (status) {
        FriendshipStatus.NOT_FRIENDS -> FriendButtonConfig(
            text = "Add as Friend",
            icon = Icons.Default.PersonAdd,
            containerColor = ButtonPrimary,
            enabled = true
        )
        FriendshipStatus.REQUEST_SENT -> FriendButtonConfig(
            text = "Request Pending",
            icon = Icons.Default.HourglassEmpty,
            containerColor = TextSecondary.copy(alpha = 0.5f),
            enabled = false
        )
        FriendshipStatus.REQUEST_RECEIVED -> FriendButtonConfig(
            text = "Accept Request",
            icon = Icons.Default.Check,
            containerColor = Color(0xFF4CAF50), // Green for accept
            enabled = true
        )
        FriendshipStatus.FRIENDS -> FriendButtonConfig(
            text = "Friends",
            icon = Icons.Default.Check,
            containerColor = ButtonPrimary,
            enabled = true
        )
    }
    
    PremiumScaleButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Surface,
            disabledContainerColor = containerColor.copy(alpha = 0.6f),
            disabledContentColor = Surface.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Surface,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text)
    }
}

/**
 * Configuration data class for friend button appearance.
 */
private data class FriendButtonConfig(
    val text: String,
    val icon: ImageVector,
    val containerColor: Color,
    val enabled: Boolean
)