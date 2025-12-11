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
    /** Whether requests are loading */
    val isLoading: Boolean = false,
    /** Error message if loading failed */
    val error: String? = null,
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
    val limitReachedType: LimitType? = null
)

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
     * Switches to the specified tab.
     * Requirements: 6.2, 7.1
     */
    fun selectTab(tab: RequestTab) {
        _uiState.update { it.copy(selectedTab = tab) }
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
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is RequestState.Success -> {
                        _uiState.update { 
                            it.copy(
                                incomingRequests = state.data,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is RequestState.Error -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = state.message
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
                    is RequestState.Success -> {
                        _uiState.update { 
                            it.copy(outgoingRequests = state.data)
                        }
                    }
                    is RequestState.Error -> {
                        // Only update error if we're on the outgoing tab
                        if (_uiState.value.selectedTab == RequestTab.OUTGOING) {
                            _uiState.update { 
                                it.copy(error = state.message)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Retries loading requests after an error.
     * Requirements: 6.16
     */
    fun retry() {
        _uiState.update { it.copy(error = null) }
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
                        _uiState.update { 
                            it.copy(
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
                        _uiState.update { 
                            it.copy(
                                isActionInProgress = false,
                                processingRequestId = null,
                                actionSuccess = "Friend request declined"
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
                        _uiState.update { 
                            it.copy(
                                isActionInProgress = false,
                                processingRequestId = null,
                                actionSuccess = "Friend request cancelled"
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
        _uiState.update { it.copy(limitReachedType = null) }
    }

    fun dismissActionSnackbar() {
        _uiState.update { it.copy(showActionSnackbar = false, actionSnackbarMessage = null, actionTargetName = null) }
    }
}
