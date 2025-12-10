package com.example.arcadia.presentation.screens.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.presentation.components.AddGameSnackbar
import com.example.arcadia.presentation.components.BottomSlideSnackbarHost
import com.example.arcadia.presentation.components.common.EmptyState
import com.example.arcadia.presentation.components.common.ErrorState
import com.example.arcadia.presentation.components.common.LoadingState
import com.example.arcadia.presentation.components.common.OfflineBanner
import com.example.arcadia.presentation.screens.friends.components.LimitReachedDialog
import com.example.arcadia.presentation.screens.friends.components.RequestListItem
import com.example.arcadia.presentation.screens.friends.components.SentRequestListItem
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import org.koin.androidx.compose.koinViewModel

import androidx.compose.foundation.lazy.itemsIndexed
import com.example.arcadia.presentation.components.common.PremiumSlideInItem
import com.example.arcadia.presentation.components.common.PremiumTabTransition

/**
 * Friend Requests Screen displaying incoming and outgoing friend requests.
 * 
 * Requirements: 6.1, 6.2, 6.15, 6.16, 11.4, 11.5
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

    // Show snackbar for action errors
    LaunchedEffect(uiState.actionError) {
        uiState.actionError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearActionError()
        }
    }
    
    // Show snackbar for action success
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess != null) {
            viewModel.clearActionSuccess()
        }
    }
    
    Scaffold(
        containerColor = Surface,
        snackbarHost = { BottomSlideSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            FriendRequestsTopBar(onNavigateBack = onNavigateBack)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Offline banner
                OfflineBanner(isOffline = uiState.isOffline)
                
                // Segmented tabs
                RequestTabSelector(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.selectTab(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                // Content based on selected tab
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        uiState.isLoading -> {
                            LoadingState(message = "Loading requests...")
                        }
                        uiState.error != null -> {
                            ErrorState(
                                message = uiState.error ?: "Failed to load requests",
                                onRetry = { viewModel.retry() }
                            )
                        }
                        else -> {
                            PremiumTabTransition(targetState = uiState.selectedTab) { tab ->
                                when (tab) {
                                    RequestTab.INCOMING -> IncomingRequestsContent(
                                        requests = uiState.incomingRequests,
                                        isActionInProgress = uiState.isActionInProgress,
                                        processingRequestId = uiState.processingRequestId,
                                        isOffline = uiState.isOffline,
                                        onAccept = { viewModel.acceptRequest(it) },
                                        onDecline = { viewModel.declineRequest(it) },
                                        onNavigateToProfile = onNavigateToProfile
                                    )
                                    RequestTab.OUTGOING -> OutgoingRequestsContent(
                                        requests = uiState.outgoingRequests,
                                        isActionInProgress = uiState.isActionInProgress,
                                        processingRequestId = uiState.processingRequestId,
                                        isOffline = uiState.isOffline,
                                        onCancel = { viewModel.cancelRequest(it) },
                                        onNavigateToProfile = onNavigateToProfile
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.showActionSnackbar && uiState.actionSnackbarMessage != null) {
                AddGameSnackbar(
                    visible = uiState.showActionSnackbar,
                    gameName = uiState.actionTargetName ?: "Friend",
                    message = uiState.actionSnackbarMessage,
                    showUndo = false,
                    onDismiss = { viewModel.dismissActionSnackbar() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
    
    // Limit Reached Dialog
    uiState.limitReachedType?.let { limitType ->
        LimitReachedDialog(
            limitType = limitType,
            onDismiss = { viewModel.dismissLimitDialog() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendRequestsTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "FRIEND REQUESTS",
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
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
    )
}

@Composable
private fun RequestTabSelector(
    selectedTab: RequestTab,
    onTabSelected: (RequestTab) -> Unit,
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
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        ) {
            Text(text = "REQUESTS")
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
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        ) {
            Text(text = "SENT")
        }
    }
}

@Composable
private fun IncomingRequestsContent(
    requests: List<com.example.arcadia.domain.model.friend.FriendRequest>,
    isActionInProgress: Boolean,
    processingRequestId: String?,
    isOffline: Boolean,
    onAccept: (com.example.arcadia.domain.model.friend.FriendRequest) -> Unit,
    onDecline: (com.example.arcadia.domain.model.friend.FriendRequest) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyState(
            title = "No pending requests",
            subtitle = "When someone sends you a friend request, it will appear here",
            icon = Icons.Default.Inbox
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(items = requests, key = { _, request -> request.id }) { index, request ->
                PremiumSlideInItem(index = index) {
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
}

@Composable
private fun OutgoingRequestsContent(
    requests: List<com.example.arcadia.domain.model.friend.FriendRequest>,
    isActionInProgress: Boolean,
    processingRequestId: String?,
    isOffline: Boolean,
    onCancel: (com.example.arcadia.domain.model.friend.FriendRequest) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    if (requests.isEmpty()) {
        EmptyState(
            title = "No sent requests",
            subtitle = "Friend requests you send will appear here until they're accepted",
            icon = Icons.Default.Send
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(items = requests, key = { _, request -> request.id }) { index, request ->
                PremiumSlideInItem(index = index) {
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
}
