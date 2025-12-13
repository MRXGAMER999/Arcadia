package com.example.arcadia.presentation.screens.friends

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.friend.Friend
import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.domain.model.friend.FriendshipStatus
import com.example.arcadia.domain.model.friend.RequestValidation
import com.example.arcadia.domain.model.friend.UserSearchResult
import com.example.arcadia.presentation.screens.friends.components.LimitType
import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.presentation.base.BaseViewModel
import com.example.arcadia.domain.usecase.GetPendingFriendRequestsCountUseCase
import com.example.arcadia.domain.usecase.friend.SearchUsersUseCase
import com.example.arcadia.util.NetworkMonitor
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Mode for the Add Friends bottom sheet.
 */
enum class BottomSheetMode {
    /** Bottom sheet is hidden */
    HIDDEN,
    /** Shows the three options: Search, QR Code, Share Link */
    OPTIONS,
    /** Shows the search interface */
    SEARCH,
    /** Shows the QR code interface with MY CODE / SCAN tabs */
    QR_CODE
}

/**
 * Mode for the QR code tab within the bottom sheet.
 */
enum class QRCodeMode {
    /** Shows the user's QR code */
    MY_CODE,
    /** Shows the camera scanner */
    SCAN
}

/**
 * UI state for the Friends screen.
 */
data class FriendsUiState(
    /** List of friends */
    val friends: List<Friend> = emptyList(),
    /** Whether the friends list is loading */
    val isLoading: Boolean = false,
    /** Error message if loading failed */
    val error: String? = null,
    /** Count of pending incoming friend requests for badge display */
    val pendingRequestCount: Int = 0,
    /** Current mode of the Add Friends bottom sheet */
    val bottomSheetMode: BottomSheetMode = BottomSheetMode.HIDDEN,
    /** Current search query */
    val searchQuery: String = "",
    /** Search results */
    val searchResults: List<UserSearchResult> = emptyList(),
    /** Whether a search is in progress */
    val isSearching: Boolean = false,
    /** Current QR code mode (MY_CODE or SCAN) */
    val qrCodeMode: QRCodeMode = QRCodeMode.MY_CODE,
    /** Search hint message */
    val searchHint: String? = null,
    /** Whether a friend action is in progress */
    val isActionInProgress: Boolean = false,
    /** ID of user currently being actioned (for button loading) */
    val actioningUserId: String? = null,
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
    /** Reciprocal request that needs user confirmation */
    val reciprocalRequest: FriendRequest? = null,
    /** Target user for reciprocal request dialog */
    val reciprocalRequestTargetUser: UserSearchResult? = null,
    /** Validation result for sending requests */
    val requestValidation: RequestValidation? = null,
    /** Cooldown hours remaining for a declined request */
    val declinedCooldownHours: Int? = null,
    /** Whether the device is currently offline - Requirements: 13.3, 13.4 */
    val isOffline: Boolean = false,
    /** Type of limit reached for showing dialog - Requirements: 3.21, 3.22, 3.26, 6.11 */
    val limitReachedType: LimitType? = null
)

/**
 * ViewModel for the Friends screen.
 * Manages friends list, search, and friend request operations.
 * 
 * Requirements: 1.1, 1.5, 1.6, 2.3, 2.4, 2.5, 3.3, 3.4, 3.6, 3.7, 3.13, 3.14, 3.15, 3.18, 3.24, 13.1, 13.2, 13.3, 13.4
 */
class FriendsViewModel(
    private val friendsRepository: FriendsRepository,
    private val gamerRepository: GamerRepository,
    private val networkMonitor: NetworkMonitor,
    private val getPendingFriendRequestsCountUseCase: GetPendingFriendRequestsCountUseCase,
    private val searchUsersUseCase: SearchUsersUseCase
) : BaseViewModel() {

    companion object {
        private const val TAG = "FriendsViewModel"
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val MIN_SEARCH_LENGTH = 2
        private const val NETWORK_ERROR_MESSAGE = "Network error. Please check your connection."
        private const val USER_INFO_TIMEOUT_MS = 5000L
        private const val SESSION_EXPIRED_MESSAGE = "User session expired. Please log in again."
        private const val OFFLINE_ERROR_MESSAGE = "You're offline. Please check your connection."
    }

    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    /** Job for search debouncing */
    private var searchJob: Job? = null

    /** Current user ID */
    private val currentUserId: String?
        get() = gamerRepository.getCurrentUserId()

    init {
        loadFriends()
        observePendingRequestCount()
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

    // ==================== Friends List Operations ====================

    /**
     * Loads the friends list with real-time updates.
     * Requirements: 1.1, 1.5, 1.6
     */
    private fun loadFriends() {
        val userId = currentUserId ?: return
        
        launchWithKey("load_friends") {
            friendsRepository.getFriendsRealtime(userId).collect { state ->
                when (state) {
                    is RequestState.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is RequestState.Success -> {
                        _uiState.update { 
                            it.copy(
                                friends = state.data,
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
     * Retries loading the friends list after an error.
     * Requirements: 1.6
     */
    fun retry() {
        loadFriends()
    }

    // ==================== Pending Request Count ====================

    /**
     * Observes the pending request count for badge display.
     * Requirements: 2.3, 2.4, 2.5
     */
    private fun observePendingRequestCount() {
        launchWithKey("pending_count") {
            getPendingFriendRequestsCountUseCase().collect { state ->
                when (state) {
                    is RequestState.Success -> _uiState.update { it.copy(pendingRequestCount = state.data) }
                    is RequestState.Error -> {
                        Log.w(TAG, "Failed to load pending request count: ${state.message}")
                        _uiState.update { it.copy(pendingRequestCount = 0) }
                    }
                    RequestState.Loading, RequestState.Idle -> {
                        // Keep existing count to avoid flicker
                    }
                }
            }
        }
    }

    /**
     * Formats the pending request count for badge display.
     * Shows actual number up to 99, then "99+" for 100 or more.
     * Requirements: 2.4
     */
    fun formatBadgeCount(count: Int): String {
        return when {
            count <= 0 -> ""
            count <= 99 -> count.toString()
            else -> "99+"
        }
    }


    // ==================== Bottom Sheet Operations ====================

    /**
     * Shows the Add Friends bottom sheet with options.
     * Requirements: 3.1
     */
    fun showAddFriendsSheet() {
        _uiState.update { it.copy(bottomSheetMode = BottomSheetMode.OPTIONS) }
    }

    /**
     * Hides the Add Friends bottom sheet.
     */
    fun hideBottomSheet() {
        _uiState.update { 
            it.copy(
                bottomSheetMode = BottomSheetMode.HIDDEN,
                searchQuery = "",
                searchResults = emptyList(),
                searchHint = null,
                isSearching = false
            )
        }
    }

    /**
     * Switches to search mode in the bottom sheet.
     * Requirements: 3.2
     */
    fun showSearchMode() {
        _uiState.update { 
            it.copy(
                bottomSheetMode = BottomSheetMode.SEARCH,
                searchQuery = "",
                searchResults = emptyList(),
                searchHint = "Type at least 2 characters"
            )
        }
    }

    /**
     * Switches to QR code mode in the bottom sheet.
     * Requirements: 4.1
     */
    fun showQRCodeMode() {
        _uiState.update { 
            it.copy(
                bottomSheetMode = BottomSheetMode.QR_CODE,
                qrCodeMode = QRCodeMode.MY_CODE
            )
        }
    }

    /**
     * Returns to the options view from search or QR mode.
     * Requirements: 4.12
     */
    fun backToOptions() {
        _uiState.update { 
            it.copy(
                bottomSheetMode = BottomSheetMode.OPTIONS,
                searchQuery = "",
                searchResults = emptyList(),
                searchHint = null
            )
        }
    }

    /**
     * Switches between MY CODE and SCAN tabs in QR mode.
     * Requirements: 4.1
     */
    fun setQRCodeMode(mode: QRCodeMode) {
        _uiState.update { it.copy(qrCodeMode = mode) }
    }

    // ==================== Search Operations ====================

    /**
     * Updates the search query with debouncing.
     * Requirements: 3.3, 3.4
     */
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        // Cancel previous search job
        searchJob?.cancel()
        
        // Validate minimum length
        if (query.length < MIN_SEARCH_LENGTH) {
            _uiState.update { 
                it.copy(
                    searchResults = emptyList(),
                    searchHint = "Type at least 2 characters",
                    isSearching = false
                )
            }
            return
        }
        
        // Debounce search
        searchJob = launchWithDebounce("search", SEARCH_DEBOUNCE_MS) {
            performSearch(query)
        }
    }

    /**
     * Performs the actual search operation.
     * Uses SearchUsersUseCase to enrich results with friendship status.
     * Requirements: 3.5, 3.6, 3.7
     */
    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isSearching = true, searchHint = null) }
        
        when (val result = searchUsersUseCase(query)) {
            is RequestState.Success -> {
                val results = result.data
                _uiState.update { 
                    it.copy(
                        searchResults = results,
                        isSearching = false,
                        searchHint = if (results.isEmpty()) "No users found matching your search" else null
                    )
                }
            }
            is RequestState.Error -> {
                _uiState.update { 
                    it.copy(
                        searchResults = emptyList(),
                        isSearching = false,
                        searchHint = "Search failed. Please try again."
                    )
                }
            }
            else -> {}
        }
    }

    /**
     * Clears the search query and results.
     */
    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { 
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                searchHint = "Type at least 2 characters",
                isSearching = false
            )
        }
    }

    // ==================== Friend Request Operations ====================

    /**
     * Shows an error message when user tries to perform action while offline.
     * Requirements: 13.3, 13.4
     */
    fun showOfflineError() {
        _uiState.update { it.copy(actionError = OFFLINE_ERROR_MESSAGE) }
    }

    /**
     * Sends a friend request to a user from search results.
     * Checks for reciprocal requests and validates limits first.
     * Requirements: 3.13, 3.14, 3.15, 3.18, 13.1, 13.2
     */
    fun sendFriendRequest(targetUser: UserSearchResult) {
        val userId = currentUserId
        if (userId == null) {
            _uiState.update { it.copy(actionError = SESSION_EXPIRED_MESSAGE) }
            return
        }

        // Prevent double-taps / repeated requests while an action is running
        if (_uiState.value.isActionInProgress) return

        // If UI already thinks it's sent/received/friends, don't send again
        if (targetUser.friendshipStatus != FriendshipStatus.NOT_FRIENDS) return
        
        // Prevent self-request
        if (targetUser.userId == userId) {
            _uiState.update { it.copy(actionError = "You cannot send a friend request to yourself") }
            return
        }
        
        // Check network connectivity - Requirements: 13.1, 13.4
        if (!isOnline()) {
            _uiState.update { it.copy(actionError = NETWORK_ERROR_MESSAGE) }
            return
        }
        
        // Lock UI immediately + optimistically flip the button state so it can't be clicked again
        _uiState.update { state ->
            state.copy(
                isActionInProgress = true,
                actioningUserId = targetUser.userId,
                actionError = null,
                // Optimistic UI: mark as REQUEST_SENT immediately
                searchResults = state.searchResults.map { user ->
                    if (user.userId == targetUser.userId) user.copy(friendshipStatus = FriendshipStatus.REQUEST_SENT)
                    else user
                }
            )
        }

        viewModelScope.launch {
            
            try {
                // Check for reciprocal request first
                val reciprocalRequest = friendsRepository.checkReciprocalRequest(userId, targetUser.userId)
                if (reciprocalRequest != null) {
                    // Show dialog to accept existing request
                    _uiState.update { 
                        it.copy(
                            isActionInProgress = false,
                            actioningUserId = null,
                            reciprocalRequest = reciprocalRequest,
                            reciprocalRequestTargetUser = targetUser
                        )
                    }
                    return@launch
                }
                
                // Check for declined request cooldown - Requirements: 3.26
                val cooldownHours = friendsRepository.getDeclinedRequestCooldown(userId, targetUser.userId)
                if (cooldownHours != null && cooldownHours > 0) {
                    _uiState.update { 
                        it.copy(
                            isActionInProgress = false,
                            actioningUserId = null,
                            declinedCooldownHours = cooldownHours,
                            limitReachedType = LimitType.COOLDOWN
                        )
                    }
                    return@launch
                }
                
                // Validate limits - Requirements: 3.21, 3.22
                val validation = friendsRepository.canSendRequest(userId)
                if (!validation.canSend) {
                    val limitType = when {
                        validation.dailyLimitReached -> LimitType.DAILY_LIMIT
                        validation.pendingLimitReached -> LimitType.PENDING_LIMIT
                        validation.friendsLimitReached -> LimitType.FRIENDS_LIMIT
                        else -> null
                    }
                    _uiState.update { 
                        it.copy(
                            isActionInProgress = false,
                            actioningUserId = null,
                            requestValidation = validation,
                            limitReachedType = limitType
                        )
                    }
                    return@launch
                }
                
                // Get current user info
                val currentUser = getCurrentUserInfo()
                if (currentUser == null) {
                    _uiState.update { 
                        it.copy(
                            isActionInProgress = false,
                            actioningUserId = null,
                            actionError = "Failed to get user information"
                        )
                    }
                    return@launch
                }
                
                // Send the request
                val result = friendsRepository.sendFriendRequest(
                    fromUserId = userId,
                    fromUsername = currentUser.first,
                    fromProfileImageUrl = currentUser.second,
                    toUserId = targetUser.userId,
                    toUsername = targetUser.username,
                    toProfileImageUrl = targetUser.profileImageUrl
                )
                
                when (result) {
                    is RequestState.Success -> {
                        _uiState.update { 
                            it.copy(
                                isActionInProgress = false,
                                actioningUserId = null,
                                actionSuccess = null,
                                showActionSnackbar = true,
                                actionSnackbarMessage = "Friend request sent",
                                actionTargetName = targetUser.username
                            )
                        }
                        // No need to re-search for UI; we already set REQUEST_SENT optimistically.
                    }
                    is RequestState.Error -> {
                        _uiState.update { 
                            it.copy(
                                isActionInProgress = false,
                                actioningUserId = null,
                                actionError = result.message,
                                // Revert optimistic state on failure
                                searchResults = it.searchResults.map { user ->
                                    if (user.userId == targetUser.userId) user.copy(friendshipStatus = FriendshipStatus.NOT_FRIENDS)
                                    else user
                                }
                            )
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending friend request: ${e.message}", e)
                _uiState.update { 
                    it.copy(
                        isActionInProgress = false,
                        actioningUserId = null,
                        actionError = "Failed to send friend request",
                        // Revert optimistic state on failure
                        searchResults = it.searchResults.map { user ->
                            if (user.userId == targetUser.userId) user.copy(friendshipStatus = FriendshipStatus.NOT_FRIENDS)
                            else user
                        }
                    )
                }
            }
        }
    }

    /**
     * Accepts a friend request from search results.
     * Requirements: 3.24, 13.1, 13.2
     */
    fun acceptFriendRequestFromSearch(targetUser: UserSearchResult) {
        val userId = currentUserId
        if (userId == null) {
            _uiState.update { it.copy(actionError = SESSION_EXPIRED_MESSAGE) }
            return
        }
        
        // Check network connectivity - Requirements: 13.1, 13.4
        if (!isOnline()) {
            _uiState.update { it.copy(actionError = NETWORK_ERROR_MESSAGE) }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, actioningUserId = targetUser.userId, actionError = null) }
            
            try {
                // Find the pending request from target user
                val request = friendsRepository.checkReciprocalRequest(userId, targetUser.userId)
                if (request == null) {
                    _uiState.update { 
                        it.copy(
                            isActionInProgress = false,
                            actioningUserId = null,
                            actionError = "Request not found"
                        )
                    }
                    return@launch
                }
                
                // Validate friends limit - Requirements: 6.11
                val validation = friendsRepository.canSendRequest(userId)
                if (validation.friendsLimitReached) {
                    _uiState.update { 
                        it.copy(
                            isActionInProgress = false,
                            actioningUserId = null,
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
                                actioningUserId = null,
                                actionSuccess = null,
                                showActionSnackbar = true,
                                actionSnackbarMessage = "Friend added",
                                actionTargetName = targetUser.username
                            )
                        }
                        // Refresh search results to update status
                        if (_uiState.value.searchQuery.length >= MIN_SEARCH_LENGTH) {
                            performSearch(_uiState.value.searchQuery)
                        }
                    }
                    is RequestState.Error -> {
                        _uiState.update { 
                            it.copy(
                                isActionInProgress = false,
                                actioningUserId = null,
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
                        actioningUserId = null,
                        actionError = "Failed to accept friend request"
                    )
                }
            }
        }
    }

    /**
     * Accepts the reciprocal request from the confirmation dialog.
     * Requirements: 3.15, 13.1, 13.2
     */
    fun acceptReciprocalRequest() {
        val request = _uiState.value.reciprocalRequest ?: return
        val targetUser = _uiState.value.reciprocalRequestTargetUser
        val targetUsername = targetUser?.username
        
        // Check network connectivity - Requirements: 13.1, 13.4
        if (!isOnline()) {
            _uiState.update { it.copy(actionError = NETWORK_ERROR_MESSAGE) }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isActionInProgress = true,
                    reciprocalRequest = null,
                    reciprocalRequestTargetUser = null
                )
            }
            
            val result = friendsRepository.acceptFriendRequest(request)
            
            when (result) {
                is RequestState.Success -> {
                    _uiState.update { 
                        it.copy(
                            isActionInProgress = false,
                            actionSuccess = null,
                            showActionSnackbar = true,
                            actionSnackbarMessage = "Friend added",
                            actionTargetName = targetUsername
                        )
                    }
                    // Refresh search results
                    if (_uiState.value.searchQuery.length >= MIN_SEARCH_LENGTH) {
                        performSearch(_uiState.value.searchQuery)
                    }
                }
                is RequestState.Error -> {
                    _uiState.update { 
                        it.copy(
                            isActionInProgress = false,
                            actionError = result.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    /**
     * Dismisses the reciprocal request dialog.
     * Requirements: 3.14
     */
    fun dismissReciprocalRequestDialog() {
        _uiState.update { 
            it.copy(
                reciprocalRequest = null,
                reciprocalRequestTargetUser = null
            )
        }
    }

    /**
     * Clears the action error message.
     */
    fun clearActionError() {
        _uiState.update { it.copy(actionError = null, declinedCooldownHours = null) }
    }

    /**
     * Clears the action success message.
     */
    fun clearActionSuccess() {
        _uiState.update { it.copy(actionSuccess = null) }
    }

    fun dismissActionSnackbar() {
        _uiState.update { it.copy(showActionSnackbar = false, actionSnackbarMessage = null, actionTargetName = null) }
    }
    
    /**
     * Dismisses the limit reached dialog.
     * Requirements: 3.21, 3.22, 3.26, 6.11
     */
    fun dismissLimitDialog() {
        _uiState.update { it.copy(limitReachedType = null, declinedCooldownHours = null) }
    }

    // ==================== Helper Methods ====================

    /**
     * Gets the appropriate error message for limit validation.
     * Requirements: 3.21, 3.22
     */
    private fun getLimitErrorMessage(validation: RequestValidation): String {
        return when {
            validation.dailyLimitReached -> 
                "You've reached the daily limit for friend requests. Try again tomorrow."
            validation.pendingLimitReached -> 
                "You have too many pending requests. Cancel some or wait for responses."
            validation.friendsLimitReached -> 
                "You've reached the maximum number of friends (500). Remove a friend to add new ones."
            else -> "Cannot send friend request at this time."
        }
    }

    /**
     * Gets the current user's username and profile image URL.
     * Uses timeout to prevent indefinite blocking.
     */
    private suspend fun getCurrentUserInfo(): Pair<String, String?>? {
        val userId = currentUserId ?: return null
        
        return try {
            val state = withTimeoutOrNull(USER_INFO_TIMEOUT_MS) {
                gamerRepository.readCustomerFlow()
                    .filter { it is RequestState.Success }
                    .first()
            }
            
            if (state is RequestState.Success) {
                Pair(state.data.username, state.data.profileImageUrl)
            } else {
                Log.w(TAG, "Timeout or null state when getting current user info")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user info: ${e.message}", e)
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}
