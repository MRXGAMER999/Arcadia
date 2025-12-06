package com.example.arcadia.presentation.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.domain.model.ProfileSection
import com.example.arcadia.presentation.screens.profile.components.AddSectionBottomSheet
import com.example.arcadia.presentation.screens.profile.components.BioCard
import com.example.arcadia.presentation.screens.profile.components.CustomSectionCard
import com.example.arcadia.presentation.screens.profile.components.GamingPlatformsCard
import com.example.arcadia.presentation.screens.profile.components.GamingStatsCard
import com.example.arcadia.presentation.screens.profile.components.LibraryPreviewCard
import com.example.arcadia.presentation.screens.profile.components.ProfileHeader
import com.example.arcadia.presentation.screens.profile.components.ShareProfileDialog
import com.example.arcadia.presentation.screens.profile.components.shareProfileAsText
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent
import com.example.arcadia.util.DisplayResult
import org.koin.androidx.compose.koinViewModel

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
