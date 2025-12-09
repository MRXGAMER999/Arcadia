package com.example.arcadia.presentation.screens.friends

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.presentation.components.common.EmptyState
import com.example.arcadia.presentation.components.common.ErrorState
import com.example.arcadia.presentation.components.common.LoadingState
import com.example.arcadia.presentation.components.common.OfflineBanner
import com.example.arcadia.presentation.screens.friends.components.AddFriendsBottomSheet
import com.example.arcadia.presentation.screens.friends.components.FriendListItem
import com.example.arcadia.presentation.screens.friends.components.LimitReachedDialog
import com.example.arcadia.presentation.screens.friends.components.LimitType
import com.example.arcadia.presentation.screens.friends.components.ReciprocalRequestDialog
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent
import org.koin.androidx.compose.koinViewModel


/**
 * Friends Screen displaying the user's friends list.
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.5, 1.6, 11.1, 11.2, 11.3
 * 
 * @param onNavigateBack Callback to navigate back to the previous screen
 * @param onNavigateToFriendRequests Callback to navigate to the Friend Requests screen
 * @param onNavigateToProfile Callback to navigate to a friend's profile
 * @param currentUserId The current user's ID for QR code generation
 * @param viewModel The FriendsViewModel instance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFriendRequests: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    currentUserId: String,
    viewModel: FriendsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Detect when user scrolls near the end for pagination
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && 
                lastVisibleItem.index >= uiState.friends.size - 5 &&
                uiState.canLoadMore &&
                !uiState.isLoadingMore &&
                !uiState.isLoading
        }
    }
    
    // Trigger load more when needed
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMoreFriends()
        }
    }
    
    // Show snackbar for action errors
    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearActionError()
        }
    }
    
    // Show snackbar for action success
    LaunchedEffect(uiState.actionSuccess) {
        uiState.actionSuccess?.let { success ->
            snackbarHostState.showSnackbar(success)
            viewModel.clearActionSuccess()
        }
    }
    
    Scaffold(
        containerColor = Surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FriendsTopBar(
                pendingRequestCount = uiState.pendingRequestCount,
                onNavigateBack = onNavigateBack,
                onNotificationsClick = onNavigateToFriendRequests,
                formatBadgeCount = viewModel::formatBadgeCount
            )
        },
        floatingActionButton = {
            // FAB for adding friends - Requirements: 3.1
            // Disable when offline - Requirements: 13.4
            FloatingActionButton(
                onClick = { 
                    if (!uiState.isOffline) {
                        viewModel.showAddFriendsSheet() 
                    }
                },
                containerColor = if (uiState.isOffline) ButtonPrimary.copy(alpha = 0.5f) else ButtonPrimary,
                contentColor = Surface
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Friends"
                )
            }
        }
    ) { paddingValues ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Offline banner - Requirements: 13.3, 13.4
            OfflineBanner(isOffline = uiState.isOffline)
            
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                // Loading state - Requirements: 1.5
                uiState.isLoading && uiState.friends.isEmpty() -> {
                    LoadingState(message = "Loading friends...")
                }
                
                // Error state - Requirements: 1.6
                uiState.error != null && uiState.friends.isEmpty() -> {
                    ErrorState(
                        message = uiState.error ?: "Failed to load friends",
                        onRetry = { viewModel.retry() }
                    )
                }
                
                // Empty state - Requirements: 1.3
                uiState.friends.isEmpty() -> {
                    EmptyState(
                        title = "No friends yet",
                        subtitle = "Tap + to add!",
                        icon = Icons.Default.People
                    )
                }
                
                // Friends list - Requirements: 1.1, 1.2
                else -> {
                    FriendsList(
                        friends = uiState.friends,
                        isLoadingMore = uiState.isLoadingMore,
                        listState = listState,
                        onFriendClick = onNavigateToProfile
                    )
                }
            }
            }
        }
    }
    
    // Add Friends Bottom Sheet
    AddFriendsBottomSheet(
        isVisible = uiState.bottomSheetMode != BottomSheetMode.HIDDEN,
        mode = uiState.bottomSheetMode,
        qrCodeMode = uiState.qrCodeMode,
        currentUserId = currentUserId,
        searchQuery = uiState.searchQuery,
        searchResults = uiState.searchResults,
        isSearching = uiState.isSearching,
        searchHint = uiState.searchHint,
        isActionInProgress = uiState.isActionInProgress,
        onDismiss = { viewModel.hideBottomSheet() },
        onModeChange = { mode ->
            when (mode) {
                BottomSheetMode.OPTIONS -> viewModel.backToOptions()
                BottomSheetMode.SEARCH -> viewModel.showSearchMode()
                BottomSheetMode.QR_CODE -> viewModel.showQRCodeMode()
                BottomSheetMode.HIDDEN -> viewModel.hideBottomSheet()
            }
        },
        onQRCodeModeChange = { viewModel.setQRCodeMode(it) },
        onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
        onClearSearch = { viewModel.clearSearch() },
        onSendFriendRequest = { viewModel.sendFriendRequest(it) },
        onAcceptFriendRequest = { viewModel.acceptFriendRequestFromSearch(it) },
        onNavigateToProfile = { userId ->
            viewModel.hideBottomSheet()
            onNavigateToProfile(userId)
        }
    )
    
    // Reciprocal Request Dialog
    if (uiState.reciprocalRequest != null && uiState.reciprocalRequestTargetUser != null) {
        ReciprocalRequestDialog(
            username = uiState.reciprocalRequestTargetUser!!.username,
            onAccept = { viewModel.acceptReciprocalRequest() },
            onDismiss = { viewModel.dismissReciprocalRequestDialog() }
        )
    }
    
    // Limit Reached Dialog - Requirements: 3.21, 3.22, 3.26, 6.11
    uiState.limitReachedType?.let { limitType ->
        LimitReachedDialog(
            limitType = limitType,
            cooldownHours = uiState.declinedCooldownHours,
            onDismiss = { viewModel.dismissLimitDialog() }
        )
    }
}

/**
 * Top bar for the Friends screen.
 * 
 * Requirements: 11.1, 11.2, 11.3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendsTopBar(
    pendingRequestCount: Int,
    onNavigateBack: () -> Unit,
    onNotificationsClick: () -> Unit,
    formatBadgeCount: (Int) -> String
) {
    TopAppBar(
        title = {
            Text(
                text = "FRIENDS",
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
            // Notification bell with badge - Requirements: 11.3, 2.3, 2.4, 2.5
            IconButton(onClick = onNotificationsClick) {
                if (pendingRequestCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = YellowAccent,
                                contentColor = Surface
                            ) {
                                Text(
                                    text = formatBadgeCount(pendingRequestCount),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Friend Requests",
                            tint = ButtonPrimary
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Friend Requests",
                        tint = ButtonPrimary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Surface
        )
    )
}

/**
 * LazyColumn displaying the friends list.
 * 
 * Requirements: 1.1, 1.2
 */
@Composable
private fun FriendsList(
    friends: List<com.example.arcadia.domain.model.friend.Friend>,
    isLoadingMore: Boolean,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onFriendClick: (String) -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = friends,
            key = { it.userId }
        ) { friend ->
            FriendListItem(
                friend = friend,
                onClick = { onFriendClick(friend.userId) }
            )
        }
        
        // Loading more indicator
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = ButtonPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
