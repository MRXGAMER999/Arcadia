package com.example.arcadia.presentation.screens.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.presentation.components.common.EmptyState
import com.example.arcadia.presentation.components.common.ErrorState
import com.example.arcadia.presentation.components.common.LoadingState
import com.example.arcadia.presentation.components.common.OfflineBanner
import com.example.arcadia.presentation.screens.friends.components.LimitReachedDialog
import com.example.arcadia.presentation.screens.friends.components.RequestListItem
import com.example.arcadia.presentation.screens.friends.components.SentRequestListItem
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.ResponsiveDimens
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.rememberResponsiveDimens
import org.koin.androidx.compose.koinViewModel

/**
 * Friend Requests Screen displaying incoming and outgoing friend requests.
 * Responsive design that adapts to all screen sizes.
 * 
 * Requirements: 6.1, 6.2, 6.15, 6.16, 11.4, 11.5
 * 
 * @param onNavigateBack Callback to navigate back to the Friends Screen
 * @param onNavigateToProfile Callback to navigate to a user's profile
 * @param viewModel The FriendRequestsViewModel instance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendRequestsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: FriendRequestsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val dimens = rememberResponsiveDimens()

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
            FriendRequestsTopBar(
                onNavigateBack = onNavigateBack,
                dimens = dimens
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Offline banner - Requirements: 13.3, 13.4
            OfflineBanner(isOffline = uiState.isOffline)
            
            // Segmented tabs - Requirements: 6.2
            RequestTabSelector(
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.selectTab(it) },
                dimens = dimens,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimens.horizontalPadding,
                        vertical = dimens.paddingSmall
                    )
            )
            
            // Content based on selected tab
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // Loading state - Requirements: 6.15
                    uiState.isLoading -> {
                        LoadingState(message = "Loading requests...")
                    }
                    
                    // Error state - Requirements: 6.16
                    uiState.error != null -> {
                        ErrorState(
                            message = uiState.error ?: "Failed to load requests",
                            onRetry = { viewModel.retry() }
                        )
                    }
                    
                    // Tab content
                    else -> {
                        when (uiState.selectedTab) {
                            RequestTab.INCOMING -> IncomingRequestsContent(
                                requests = uiState.incomingRequests,
                                isActionInProgress = uiState.isActionInProgress,
                                processingRequestId = uiState.processingRequestId,
                                isOffline = uiState.isOffline,
                                onAccept = { viewModel.acceptRequest(it) },
                                onDecline = { viewModel.declineRequest(it) },
                                onNavigateToProfile = onNavigateToProfile,
                                dimens = dimens
                            )
                            RequestTab.OUTGOING -> OutgoingRequestsContent(
                                requests = uiState.outgoingRequests,
                                isActionInProgress = uiState.isActionInProgress,
                                processingRequestId = uiState.processingRequestId,
                                isOffline = uiState.isOffline,
                                onCancel = { viewModel.cancelRequest(it) },
                                onNavigateToProfile = onNavigateToProfile,
                                dimens = dimens
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Limit Reached Dialog - Requirements: 6.11
    uiState.limitReachedType?.let { limitType ->
        LimitReachedDialog(
            limitType = limitType,
            onDismiss = { viewModel.dismissLimitDialog() }
        )
    }
}

/**
 * Top bar for the Friend Requests screen.
 * Responsive design with adaptive sizing.
 * 
 * Requirements: 11.4, 11.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendRequestsTopBar(
    onNavigateBack: () -> Unit,
    dimens: ResponsiveDimens
) {
    TopAppBar(
        title = {
            Text(
                text = if (dimens.isCompact && dimens.screenWidth < 360.dp) "REQUESTS" else "FRIEND REQUESTS",
                fontSize = dimens.fontSizeTitle,
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
                    tint = TextSecondary,
                    modifier = Modifier.size(dimens.iconMedium)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Surface
        )
    )
}


/**
 * Segmented button row for switching between REQUESTS and SENT tabs.
 * Responsive design with adaptive sizing.
 * 
 * Requirements: 6.2
 */
@Composable
private fun RequestTabSelector(
    selectedTab: RequestTab,
    onTabSelected: (RequestTab) -> Unit,
    dimens: ResponsiveDimens,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        SegmentedButton(
            selected = selectedTab == RequestTab.INCOMING,
            onClick = { onTabSelected(RequestTab.INCOMING) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = ButtonPrimary,
                activeContentColor = Surface,
                inactiveContainerColor = Color(0xFF0F1B41),
                inactiveContentColor = TextSecondary
            ),
            icon = {
                Icon(
                    imageVector = Icons.Default.Inbox,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = dimens.paddingXSmall)
                        .size(dimens.iconSmall)
                )
            }
        ) {
            Text(
                text = "REQUESTS",
                fontSize = dimens.fontSizeSmall
            )
        }
        
        SegmentedButton(
            selected = selectedTab == RequestTab.OUTGOING,
            onClick = { onTabSelected(RequestTab.OUTGOING) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors = SegmentedButtonDefaults.colors(
                activeContainerColor = ButtonPrimary,
                activeContentColor = Surface,
                inactiveContainerColor = Color(0xFF0F1B41),
                inactiveContentColor = TextSecondary
            ),
            icon = {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = dimens.paddingXSmall)
                        .size(dimens.iconSmall)
                )
            }
        ) {
            Text(
                text = "SENT",
                fontSize = dimens.fontSizeSmall
            )
        }
    }
}

/**
 * Content for the incoming requests tab.
 * Responsive layout with adaptive spacing.
 * 
 * Requirements: 6.3, 6.4, 6.7, 6.8, 6.13, 6.14, 13.4
 */
@Composable
private fun IncomingRequestsContent(
    requests: List<com.example.arcadia.domain.model.friend.FriendRequest>,
    isActionInProgress: Boolean,
    processingRequestId: String?,
    isOffline: Boolean,
    onAccept: (com.example.arcadia.domain.model.friend.FriendRequest) -> Unit,
    onDecline: (com.example.arcadia.domain.model.friend.FriendRequest) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    dimens: ResponsiveDimens
) {
    if (requests.isEmpty()) {
        // Empty state - Requirements: 6.7
        EmptyState(
            title = "No pending requests",
            subtitle = "When someone sends you a friend request, it will appear here",
            icon = Icons.Default.Inbox
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = dimens.paddingSmall,
                bottom = dimens.paddingXLarge
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.paddingXSmall)
        ) {
            items(
                items = requests,
                key = { it.id }
            ) { request ->
                RequestListItem(
                    request = request,
                    isProcessing = isActionInProgress && processingRequestId == request.id,
                    isOffline = isOffline,
                    onAccept = { onAccept(request) },
                    onDecline = { onDecline(request) },
                    onClick = { onNavigateToProfile(request.fromUserId) }
                )
            }
        }
    }
}

/**
 * Content for the outgoing (sent) requests tab.
 * Responsive layout with adaptive spacing.
 * 
 * Requirements: 7.1, 7.2, 7.3, 7.4, 7.6, 13.4
 */
@Composable
private fun OutgoingRequestsContent(
    requests: List<com.example.arcadia.domain.model.friend.FriendRequest>,
    isActionInProgress: Boolean,
    processingRequestId: String?,
    isOffline: Boolean,
    onCancel: (com.example.arcadia.domain.model.friend.FriendRequest) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    dimens: ResponsiveDimens
) {
    if (requests.isEmpty()) {
        // Empty state - Requirements: 7.3
        EmptyState(
            title = "No sent requests",
            subtitle = "Friend requests you send will appear here until they're accepted",
            icon = Icons.Default.Send
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = dimens.paddingSmall,
                bottom = dimens.paddingXLarge
            ),
            verticalArrangement = Arrangement.spacedBy(dimens.paddingXSmall)
        ) {
            items(
                items = requests,
                key = { it.id }
            ) { request ->
                SentRequestListItem(
                    request = request,
                    isProcessing = isActionInProgress && processingRequestId == request.id,
                    isOffline = isOffline,
                    onCancel = { onCancel(request) },
                    onClick = { onNavigateToProfile(request.toUserId) }
                )
            }
        }
    }
}
