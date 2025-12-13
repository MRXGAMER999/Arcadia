package com.example.arcadia.presentation.screens.friends.state

import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.presentation.screens.friends.components.LimitType

/**
 * Tab selection for requests screen.
 */
enum class RequestTab { INCOMING, OUTGOING }

/**
 * UI state for the Friend Requests screen.
 */
data class FriendRequestsUiState(
    val selectedTab: RequestTab = RequestTab.INCOMING,
    
    /** Incoming requests state */
    val incoming: RequestListState = RequestListState(),
    
    /** Outgoing requests state */
    val outgoing: RequestListState = RequestListState(),
    
    /** Currently processing request ID (for button loading state) */
    val processingRequestId: String? = null,
    
    /** Network connectivity */
    val isOffline: Boolean = false,
    
    /** One-time events */
    val event: RequestsEvent? = null
)

/**
 * State for a list of requests (incoming or outgoing).
 */
data class RequestListState(
    val requests: List<FriendRequest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isEmpty: Boolean get() = !isLoading && error == null && requests.isEmpty()
}

/**
 * One-time events for the Requests screen.
 */
sealed interface RequestsEvent {
    data class ShowSnackbar(val message: String, val username: String) : RequestsEvent
    data class ShowError(val message: String) : RequestsEvent
    data class ShowLimitDialog(val limitType: LimitType, val cooldownHours: Int? = null) : RequestsEvent
    data class NavigateToProfile(val userId: String) : RequestsEvent
}
