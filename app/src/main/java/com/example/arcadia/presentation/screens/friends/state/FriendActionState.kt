package com.example.arcadia.presentation.screens.friends.state

import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.domain.model.friend.FriendshipStatus
import com.example.arcadia.presentation.screens.friends.components.LimitType

/**
 * Shared state for friendship actions on profiles.
 * Used by ProfileViewModel via FriendshipDelegate.
 */
data class FriendActionState(
    val status: FriendshipStatus = FriendshipStatus.NOT_FRIENDS,
    val isLoading: Boolean = false,
    val pendingRequest: FriendRequest? = null,
    
    /** Dialog states */
    val showUnfriendDialog: Boolean = false,
    val limitDialog: LimitDialogState? = null,
    
    /** One-time events */
    val event: FriendActionEvent? = null
)

data class LimitDialogState(
    val type: LimitType,
    val cooldownHours: Int? = null
)

sealed interface FriendActionEvent {
    data class ShowSnackbar(val message: String, val username: String) : FriendActionEvent
    data class ShowError(val message: String) : FriendActionEvent
}
