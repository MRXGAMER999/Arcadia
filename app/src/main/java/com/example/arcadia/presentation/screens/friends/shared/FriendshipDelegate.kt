package com.example.arcadia.presentation.screens.friends.shared

import android.util.Log
import com.example.arcadia.domain.model.friend.CooldownException
import com.example.arcadia.domain.model.friend.FriendshipStatus
import com.example.arcadia.domain.model.friend.LimitReachedException
import com.example.arcadia.domain.model.friend.ReciprocalRequestException
import com.example.arcadia.domain.usecase.friend.AcceptFriendRequestUseCase
import com.example.arcadia.domain.usecase.friend.GetFriendshipStatusUseCase
import com.example.arcadia.domain.usecase.friend.RemoveFriendUseCase
import com.example.arcadia.domain.usecase.friend.SendFriendRequestUseCase
import com.example.arcadia.presentation.screens.friends.state.FriendActionEvent
import com.example.arcadia.presentation.screens.friends.state.FriendActionState
import com.example.arcadia.presentation.screens.friends.state.LimitDialogState
import com.example.arcadia.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Delegate for friendship operations on profile screens.
 * Encapsulates all friendship logic to keep ProfileViewModel focused.
 * 
 * This addresses the issue of ProfileViewModel being too large (835 lines)
 * by extracting friendship-related operations into a dedicated component.
 * 
 * Usage in ProfileViewModel:
 * ```
 * class ProfileViewModel(
 *     private val friendshipDelegate: FriendshipDelegate,
 *     ...
 * ) {
 *     val friendActionState = friendshipDelegate.state
 *     
 *     fun initFriendship(targetUserId: String, username: String, imageUrl: String?) {
 *         friendshipDelegate.initialize(viewModelScope, targetUserId, username, imageUrl)
 *     }
 *     
 *     fun onFriendActionClick() = friendshipDelegate.onActionClick()
 * }
 * ```
 */
class FriendshipDelegate(
    private val getFriendshipStatusUseCase: GetFriendshipStatusUseCase,
    private val sendFriendRequestUseCase: SendFriendRequestUseCase,
    private val acceptFriendRequestUseCase: AcceptFriendRequestUseCase,
    private val removeFriendUseCase: RemoveFriendUseCase,
    private val networkMonitor: NetworkMonitor
) {
    companion object {
        private const val TAG = "FriendshipDelegate"
        private const val NETWORK_ERROR = "You're offline. Please check your connection."
    }
    
    private val _state = MutableStateFlow(FriendActionState())
    val state: StateFlow<FriendActionState> = _state.asStateFlow()

    private var targetUserId: String? = null
    private var targetUsername: String? = null
    private var targetProfileImageUrl: String? = null
    private var scope: CoroutineScope? = null
    private var statusJob: Job? = null

    /**
     * Initialize the delegate for a specific target user.
     * Call this when loading another user's profile.
     */
    fun initialize(
        scope: CoroutineScope,
        targetUserId: String,
        targetUsername: String,
        targetProfileImageUrl: String?
    ) {
        this.scope = scope
        this.targetUserId = targetUserId
        this.targetUsername = targetUsername
        this.targetProfileImageUrl = targetProfileImageUrl
        
        observeFriendshipStatus(scope, targetUserId)
    }
    
    /**
     * Update target user info (e.g., when profile data loads).
     */
    fun updateTargetInfo(username: String, profileImageUrl: String?) {
        this.targetUsername = username
        this.targetProfileImageUrl = profileImageUrl
    }

    /**
     * Reset state when viewing own profile or leaving screen.
     */
    fun reset() {
        statusJob?.cancel()
        statusJob = null
        targetUserId = null
        targetUsername = null
        targetProfileImageUrl = null
        _state.value = FriendActionState()
    }

    private fun observeFriendshipStatus(scope: CoroutineScope, targetUserId: String) {
        statusJob?.cancel()
        statusJob = scope.launch {
            getFriendshipStatusUseCase(targetUserId).collect { result ->
                result.fold(
                    onSuccess = { (status, pendingRequest) ->
                        _state.update {
                            it.copy(
                                status = status,
                                pendingRequest = pendingRequest,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.w(TAG, "Error observing friendship status: ${error.message}")
                    }
                )
            }
        }
    }

    /**
     * Handle the main action button click based on current status.
     */
    fun onActionClick() {
        when (_state.value.status) {
            FriendshipStatus.NOT_FRIENDS -> sendFriendRequest()
            FriendshipStatus.REQUEST_RECEIVED -> acceptFriendRequest()
            FriendshipStatus.FRIENDS -> showUnfriendDialog()
            FriendshipStatus.REQUEST_SENT -> {
                // Button should be disabled in this state
            }
        }
    }

    /**
     * Send a friend request to the target user.
     */
    fun sendFriendRequest() {
        val targetId = targetUserId ?: return
        val username = targetUsername ?: return
        val imageUrl = targetProfileImageUrl
        val currentScope = scope ?: return
        
        if (!checkOnline()) return
        
        currentScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            sendFriendRequestUseCase(targetId, username, imageUrl).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            status = FriendshipStatus.REQUEST_SENT,
                            event = FriendActionEvent.ShowSnackbar("Friend request sent", username)
                        )
                    }
                },
                onFailure = { error ->
                    handleError(error)
                }
            )
        }
    }

    /**
     * Accept a friend request from the target user.
     */
    fun acceptFriendRequest() {
        val request = _state.value.pendingRequest ?: return
        val username = targetUsername ?: return
        val currentScope = scope ?: return
        
        if (!checkOnline()) return
        
        currentScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            acceptFriendRequestUseCase(request).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            status = FriendshipStatus.FRIENDS,
                            pendingRequest = null,
                            event = FriendActionEvent.ShowSnackbar("Friend added", username)
                        )
                    }
                },
                onFailure = { error ->
                    handleError(error)
                }
            )
        }
    }

    /**
     * Show the unfriend confirmation dialog.
     */
    fun showUnfriendDialog() {
        _state.update { it.copy(showUnfriendDialog = true) }
    }

    /**
     * Dismiss the unfriend confirmation dialog.
     */
    fun dismissUnfriendDialog() {
        _state.update { it.copy(showUnfriendDialog = false) }
    }

    /**
     * Remove the target user from friends.
     */
    fun unfriend() {
        val targetId = targetUserId ?: return
        val username = targetUsername ?: return
        val currentScope = scope ?: return
        
        if (!checkOnline()) {
            _state.update { it.copy(showUnfriendDialog = false) }
            return
        }
        
        currentScope.launch {
            _state.update { it.copy(isLoading = true, showUnfriendDialog = false) }
            
            removeFriendUseCase(targetId).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            status = FriendshipStatus.NOT_FRIENDS,
                            event = FriendActionEvent.ShowSnackbar("Removed from friends", username)
                        )
                    }
                },
                onFailure = { error ->
                    handleError(error)
                }
            )
        }
    }

    /**
     * Dismiss the limit reached dialog.
     */
    fun dismissLimitDialog() {
        _state.update { it.copy(limitDialog = null) }
    }

    /**
     * Consume the current event.
     */
    fun consumeEvent() {
        _state.update { it.copy(event = null) }
    }

    private fun checkOnline(): Boolean {
        if (!networkMonitor.isCurrentlyOnline()) {
            _state.update { it.copy(event = FriendActionEvent.ShowError(NETWORK_ERROR)) }
            return false
        }
        return true
    }

    private fun handleError(error: Throwable) {
        _state.update { current ->
            current.copy(
                isLoading = false,
                limitDialog = when (error) {
                    is LimitReachedException -> LimitDialogState(error.limitType)
                    is CooldownException -> LimitDialogState(
                        com.example.arcadia.presentation.screens.friends.components.LimitType.COOLDOWN,
                        error.hoursRemaining
                    )
                    else -> null
                },
                event = when (error) {
                    is ReciprocalRequestException -> {
                        // Update state to show accept button
                        _state.update {
                            it.copy(
                                status = FriendshipStatus.REQUEST_RECEIVED,
                                pendingRequest = error.request
                            )
                        }
                        null
                    }
                    is LimitReachedException, is CooldownException -> null // Handled by dialog
                    else -> FriendActionEvent.ShowError(error.message ?: "Action failed")
                }
            )
        }
    }
}
