package com.example.arcadia.presentation.screens.friends

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.presentation.screens.friends.components.LimitType
import com.example.arcadia.presentation.base.BaseViewModel
import com.example.arcadia.util.NetworkMonitor
import com.example.arcadia.util.PreferencesManager
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Tab selection for the Friend Requests screen.
 */
enum class RequestTab {
    /** Shows incoming friend requests */
    INCOMING,
    /** Shows outgoing (sent) friend requests */
    OUTGOING
}

/**
 * UI state for the Friend Requests screen.
 * Requirements: 6.2, 6.3, 7.1, 13.3, 13.4
 */
data class FriendRequestsUiState(
    /** Currently selected tab */
    val selectedTab: RequestTab = RequestTab.INCOMING,
    /** List of incoming (received) friend requests */
    val incomingRequests: List<FriendRequest> = emptyList(),
    /** List of outgoing (sent) friend requests */
    val outgoingRequests: List<FriendRequest> = emptyList(),
    /** Whether incoming requests are loading */
    val isLoadingIncoming: Boolean = false,
    /** Whether outgoing requests are loading */
    val isLoadingOutgoing: Boolean = false,
    /** Error message for incoming requests */
    val incomingError: String? = null,
    /** Error message for outgoing requests */
    val outgoingError: String? = null,
    /** Whether an action (accept/decline/cancel) is in progress */
    val isActionInProgress: Boolean = false,
    /** ID of the request currently being processed */
    val processingRequestId: String? = null,
    /** Action error message */
    val actionError: String? = null,
    /** Action success message */
    val actionSuccess: String? = null,
    /** Snackbar visibility for action confirmations */
    val showActionSnackbar: Boolean = false,
    /** Message for action snackbar */
    val actionSnackbarMessage: String? = null,
    /** Target username for action snackbar */
    val actionTargetName: String? = null,
    /** Whether the device is currently offline - Requirements: 13.3, 13.4 */
    val isOffline: Boolean = false,
    /** Type of limit reached for showing dialog - Requirements: 6.11 */
    val limitReachedType: LimitType? = null,
    /** Cooldown hours for declined request limit - Requirements: 3.26 */
    val cooldownHours: Int? = null
) {
    /** Combined loading state based on selected tab */
    val isLoading: Boolean
        get() = if (selectedTab == RequestTab.INCOMING) isLoadingIncoming else isLoadingOutgoing
    
    /** Combined error state based on selected tab */
    val error: String?
        get() = if (selectedTab == RequestTab.INCOMING) incomingError else outgoingError
}

/**
 * ViewModel for the Friend Requests screen.
 * Manages incoming and outgoing friend requests with accept/decline/cancel operations.
 * 
 * Requirements: 6.2, 6.3, 6.8, 6.11, 6.13, 7.1, 7.4, 13.1, 13.2, 13.3, 13.4, 17.1
 */
class FriendRequestsViewModel(
    private val friendsRepository: FriendsRepository,
    private val gamerRepository: GamerRepository,
    private val preferencesManager: PreferencesManager,
    private val networkMonitor: NetworkMonitor
) : BaseViewModel() {

    companion object {
        private const val TAG = "FriendRequestsViewModel"
        private const val NETWORK_ERROR_MESSAGE = "Network error. Please check your connection."
    }

    private val _uiState = MutableStateFlow(FriendRequestsUiState())
    val uiState: StateFlow<FriendRequestsUiState> = _uiState.asStateFlow()

    /** Current user ID */
    private val currentUserId: String?
        get() = gamerRepository.getCurrentUserId()

    init {
        loadIncomingRequests()
        loadOutgoingRequests()
        triggerCleanupIfNeeded()
        observeNetworkStatus()
    }
    
    // ==================== Network Monitoring ====================
    
    /**
     * Observes network connectivity status.
     * Requirements: 13.3, 13.4
     */
    private fun observeNetworkStatus() {
        launchWithKey("network_status") {
            networkMonitor.isOnline.collect { isOnline ->
                _uiState.update { it.copy(isOffline = !isOnline) }
            }
        }
    }
    
    /**
     * Checks if the device is currently online.
     * Requirements: 13.3, 13.4
     */
    private fun isOnline(): Boolean = networkMonitor.isCurrentlyOnline()


    // ==================== Tab Selection ====================

    /**
     * Switches to the specified tab and clears tab-specific errors.
     * Requirements: 6.2, 7.1
     */
    fun selectTab(tab: RequestTab) {
        _uiState.update { 
            it.copy(
                selectedTab = tab,
                incomingError = if (tab == RequestTab.INCOMING) null else it.incomingError,
                outgoingError = if (tab == RequestTab.OUTGOING) null else it.outgoingError
            )
        }
    }

    // ==================== Request Loading ====================

    /**
     * Loads incoming friend requests with real-time updates.
     * Requirements: 6.3
     */
    private fun loadIncomingRequests() {
        val userId = currentUserId ?: return
        
        launchWithKey("load_incoming") {
            friendsRepository.getIncomingRequestsRealtime(userId).collect { state ->
                when (state) {
                    is RequestState.Loading -> {
                        _uiState.update { it.copy(isLoadingIncoming = true, incomingError = null) }
                    }
                    is RequestState.Success -> {
                        _uiState.update { 
                            it.copy(
                                incomingRequests = state.data,
                                isLoadingIncoming = false,
                                incomingError = null
                            )
                        }
                    }
                    is RequestState.Error -> {
                        _uiState.update { 
                            it.copy(
                                isLoadingIncoming = false,
                                incomingError = state.message
                            )
                        }
                    }
                    is RequestState.Idle -> {
                        // No-op
                    }
                }
            }
        }
    }

    /**
     * Loads outgoing friend requests with real-time updates.
     * Requirements: 7.1
     */
    private fun loadOutgoingRequests() {
        val userId = currentUserId ?: return
        
        launchWithKey("load_outgoing") {
            friendsRepository.getOutgoingRequestsRealtime(userId).collect { state ->
                when (state) {
                    is RequestState.Loading -> {
                        _uiState.update { it.copy(isLoadingOutgoing = true, outgoingError = null) }
                    }
                    is RequestState.Success -> {
                        _uiState.update { 
                            it.copy(
                                outgoingRequests = state.data,
                                isLoadingOutgoing = false,
                                outgoingError = null
                            )
                        }
                    }
                    is RequestState.Error -> {
                        _uiState.update { 
                            it.copy(
                                isLoadingOutgoing = false,
                                outgoingError = state.message
                            )
                        }
                    }
                    is RequestState.Idle -> {
                        // No-op
                    }
                }
            }
        }
    }

    /**
     * Retries loading requests after an error.
     * Requirements: 6.16
     */
    fun retry() {
        _uiState.update { it.copy(incomingError = null, outgoingError = null) }
        loadIncomingRequests()
        loadOutgoingRequests()
    }

    // ==================== Request Actions ====================

    /**
     * Accepts a friend request.
     * Validates friends limit before accepting.
     * Requirements: 6.8, 6.11, 13.1, 13.2
     */
    fun acceptRequest(request: FriendRequest) {
        val userId = currentUserId ?: return
        
        // Prevent double-taps while an action is in progress
        if (_uiState.value.isActionInProgress) return
        
        // Check network connectivity - Requirements: 13.1, 13.4
        if (!isOnline()) {
            _uiState.update { it.copy(actionError = NETWORK_ERROR_MESSAGE) }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isActionInProgress = true,
                    processingRequestId = request.id,
                    actionError = null
                )
            }
            
            try {
                // Validate friends limit - Requirements: 6.11
                val validation = friendsRepository.canSendRequest(userId)
                if (validation.friendsLimitReached) {
                    _uiState.update { 
                        it.copy(
                            isActionInProgress = false,
                            processingRequestId = null,
                            limitReachedType = LimitType.FRIENDS_LIMIT
                        )
                    }
                    return@launch
                }
                
                // Accept the request
                val result = friendsRepository.acceptFriendRequest(request)
                
                when (result) {
                    is RequestState.Success -> {
                        // Optimistically remove the request from the list immediately
                        // The realtime subscription will also update, but this ensures instant UI feedback
                        _uiState.update { currentState ->
                            currentState.copy(
                                incomingRequests = currentState.incomingRequests.filter { it.id != request.id },
                                isActionInProgress = false,
                                processingRequestId = null,
                                actionSuccess = null,
                                showActionSnackbar = true,
                                actionSnackbarMessage = "Friend request accepted",
                                actionTargetName = request.fromUsername
                            )
                        }
                    }
                    is RequestState.Error -> {
                        _uiState.update { 
                            it.copy(
                                isActionInProgress = false,
                                processingRequestId = null,
                                actionError = result.message
                            )
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error accepting friend request: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isActionInProgress = false,
                        processingRequestId = null,
                        actionError = "Failed to accept friend request"
                    )
                }
            }
        }
    }

    /**
     * Declines a friend request.
     * Updates request status to "declined" (not deleted for cooldown tracking).
     * Requirements: 6.13, 13.1, 13.2
     */
    fun declineRequest(request: FriendRequest) {
        // Prevent double-taps while an action is in progress
        if (_uiState.value.isActionInProgress) return
        
        // Check network connectivity - Requirements: 13.1, 13.4
        if (!isOnline()) {
            _uiState.update { it.copy(actionError = NETWORK_ERROR_MESSAGE) }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isActionInProgress = true,
                    processingRequestId = request.id,
                    actionError = null
                )
            }
            
            try {
                val result = friendsRepository.declineFriendRequest(request.id)
                
                when (result) {
                    is RequestState.Success -> {
                        // Optimistically remove the request from the list immediately
                        // The realtime subscription will also update, but this ensures instant UI feedback
                        _uiState.update { currentState ->
                            currentState.copy(
                                incomingRequests = currentState.incomingRequests.filter { it.id != request.id },
                                isActionInProgress = false,
                                processingRequestId = null,
                                actionSuccess = "Friend request declined",
                                showActionSnackbar = true,
                                actionSnackbarMessage = "Friend request declined",
                                actionTargetName = request.fromUsername
                            )
                        }
                    }
                    is RequestState.Error -> {
                        _uiState.update { 
                            it.copy(
                                isActionInProgress = false,
                                processingRequestId = null,
                                actionError = result.message
                            )
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error declining friend request: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isActionInProgress = false,
                        processingRequestId = null,
                        actionError = "Failed to decline friend request"
                    )
                }
            }
        }
    }

    /**
     * Cancels a sent friend request.
     * Deletes the request document from Appwrite.
     * Requirements: 7.4, 13.1, 13.2
     */
    fun cancelRequest(request: FriendRequest) {
        // Prevent double-taps while an action is in progress
        if (_uiState.value.isActionInProgress) return
        
        // Check network connectivity - Requirements: 13.1, 13.4
        if (!isOnline()) {
            _uiState.update { it.copy(actionError = NETWORK_ERROR_MESSAGE) }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isActionInProgress = true,
                    processingRequestId = request.id,
                    actionError = null
                )
            }
            
            try {
                val result = friendsRepository.cancelFriendRequest(request.id)
                
                when (result) {
                    is RequestState.Success -> {
                        // Optimistically remove the request from the list immediately
                        // The realtime subscription will also update, but this ensures instant UI feedback
                        _uiState.update { currentState ->
                            currentState.copy(
                                outgoingRequests = currentState.outgoingRequests.filter { it.id != request.id },
                                isActionInProgress = false,
                                processingRequestId = null,
                                actionSuccess = "Friend request cancelled",
                                showActionSnackbar = true,
                                actionSnackbarMessage = "Friend request cancelled",
                                actionTargetName = request.toUsername
                            )
                        }
                    }
                    is RequestState.Error -> {
                        _uiState.update { 
                            it.copy(
                                isActionInProgress = false,
                                processingRequestId = null,
                                actionError = result.message
                            )
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling friend request: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isActionInProgress = false,
                        processingRequestId = null,
                        actionError = "Failed to cancel friend request"
                    )
                }
            }
        }
    }

    // ==================== Cleanup Operations ====================

    /**
     * Triggers cleanup of old declined requests if needed.
     * Only runs if last cleanup was more than 7 days ago.
     * Requirements: 17.1
     */
    private fun triggerCleanupIfNeeded() {
        val userId = currentUserId ?: return
        
        if (preferencesManager.shouldCleanupDeclinedRequests()) {
            viewModelScope.launch {
                try {
                    friendsRepository.cleanupOldDeclinedRequests(userId, maxDeletes = 10)
                    preferencesManager.markDeclinedRequestsCleanupComplete()
                    Log.d(TAG, "Declined requests cleanup completed")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during declined requests cleanup: ${e.message}", e)
                    // Don't mark as complete if cleanup failed
                }
            }
        }
    }

    // ==================== Message Clearing ====================

    /**
     * Clears the action error message.
     */
    fun clearActionError() {
        _uiState.update { it.copy(actionError = null) }
    }

    /**
     * Clears the action success message.
     */
    fun clearActionSuccess() {
        _uiState.update { it.copy(actionSuccess = null) }
    }
    
    /**
     * Dismisses the limit reached dialog.
     * Requirements: 6.11
     */
    fun dismissLimitDialog() {
        _uiState.update { it.copy(limitReachedType = null, cooldownHours = null) }
    }

    fun dismissActionSnackbar() {
        _uiState.update { it.copy(showActionSnackbar = false, actionSnackbarMessage = null, actionTargetName = null) }
    }
}
