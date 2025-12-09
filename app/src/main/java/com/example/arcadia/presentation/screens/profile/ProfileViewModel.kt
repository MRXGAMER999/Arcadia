package com.example.arcadia.presentation.screens.profile

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.model.ProfileSection
import com.example.arcadia.domain.model.ProfileSectionType
import com.example.arcadia.domain.model.ai.Badge
import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.domain.model.friend.FriendshipStatus
import com.example.arcadia.domain.repository.FeaturedBadgesRepository
import com.example.arcadia.presentation.screens.friends.components.LimitType
import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.presentation.base.BaseViewModel
import com.example.arcadia.util.NetworkMonitor
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Draft representation of a profile section, used for unsaved changes and reopening.
 */
data class SectionDraft(
    val id: String? = null,
    val title: String,
    val type: ProfileSectionType,
    val gameIds: List<Int>
)

/**
 * UI state for section action snackbar messaging.
 */
data class SectionSnackbarState(
    val title: String,
    val message: String,
    val showUndo: Boolean = false
)

/**
 * UI state for friendship action snackbar messaging.
 */
data class FriendActionSnackbarState(
    val username: String,
    val message: String
)

data class ProfileState(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val username: String = "",
    val country: String? = null,
    val city: String? = null,
    val gender: String? = null,
    val description: String? = null,
    val profileImageUrl: String? = null,
    val steamId: String? = null,
    val xboxGamertag: String? = null,
    val psnId: String? = null,
    val isProfilePublic: Boolean = true
)

data class ProfileStatsState(
    val totalGames: Int = 0,
    val finishedGames: Int = 0,
    val playingGames: Int = 0,
    val droppedGames: Int = 0,
    val wantToPlayGames: Int = 0,
    val onHoldGames: Int = 0,
    val hoursPlayed: Int = 0,
    val avgRating: Float = 0f,
    val completionRate: Float = 0f
)

/**
 * State for friendship-related operations on a profile.
 * Requirements: 8.1-8.4, 14.6, 14.7
 */
data class ProfileFriendshipState(
    /** Current friendship status with the profile user */
    val status: FriendshipStatus = FriendshipStatus.NOT_FRIENDS,
    /** Whether a friendship operation is in progress */
    val isLoading: Boolean = false,
    /** Error message from the last operation */
    val error: String? = null,
    /** Success message from the last operation */
    val successMessage: String? = null,
    /** Whether the unfriend confirmation dialog should be shown */
    val showUnfriendDialog: Boolean = false,
    /** The pending friend request if status is REQUEST_RECEIVED */
    val pendingRequest: FriendRequest? = null,
    /** Type of limit reached for showing dialog - Requirements: 3.21, 3.22, 3.26, 6.11 */
    val limitReachedType: LimitType? = null,
    /** Cooldown hours remaining for a declined request - Requirements: 3.26 */
    val cooldownHours: Int? = null
)

class ProfileViewModel(
    private val gamerRepository: GamerRepository,
    private val gameListRepository: GameListRepository,
    private val featuredBadgesRepository: FeaturedBadgesRepository,
    private val friendsRepository: FriendsRepository,
    private val networkMonitor: NetworkMonitor,
    private val userIdParam: String? = null
) : BaseViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
        private const val NETWORK_ERROR_MESSAGE = "Network error. Please check your connection."
    }
    
    /**
     * Checks if the device is currently online.
     * Requirements: 13.3, 13.4
     */
    private fun isOnline(): Boolean = networkMonitor.isCurrentlyOnline()

    var screenReady: RequestState<Unit> by mutableStateOf(RequestState.Loading)
        private set

    var profileState: ProfileState by mutableStateOf(ProfileState())
        private set

    var statsState: ProfileStatsState by mutableStateOf(ProfileStatsState())
        private set

    var isCurrentUser: Boolean by mutableStateOf(true)
        private set

    private val _libraryGames = MutableStateFlow<List<GameListEntry>>(emptyList())
    val libraryGames: StateFlow<List<GameListEntry>> = _libraryGames.asStateFlow()

    private val _featuredBadges = MutableStateFlow<List<Badge>>(emptyList())
    val featuredBadges: StateFlow<List<Badge>> = _featuredBadges.asStateFlow()

    var customSections: List<ProfileSection> by mutableStateOf(emptyList())
        private set

    // Section unsaved changes + snackbar state
    var showUnsavedSectionSnackbar: Boolean by mutableStateOf(false)
        private set
    var unsavedSectionDraft: SectionDraft? by mutableStateOf(null)
        private set
    var reopenSectionDraft: SectionDraft? by mutableStateOf(null)
        private set

    // Section success snackbar state
    var sectionSnackbarState: SectionSnackbarState? by mutableStateOf(null)
        private set
    private var pendingSectionActionMessage: String? = null
    private var pendingSectionTitle: String? = null
    private var pendingSectionShowUndo: Boolean? = null
    private var lastDeletedSection: ProfileSection? = null
    private var lastDeletedIndex: Int? = null

    // Friendship state
    private val _friendshipState = MutableStateFlow(ProfileFriendshipState())
    val friendshipState: StateFlow<ProfileFriendshipState> = _friendshipState.asStateFlow()
    var friendActionSnackbarState: FriendActionSnackbarState? by mutableStateOf(null)
        private set

    /** The target user ID for friendship operations */
    private var targetUserId: String? = null

    init {
        // Initialize isCurrentUser immediately to prevent race conditions
        val currentUserId = gamerRepository.getCurrentUserId()
        isCurrentUser = userIdParam == null || userIdParam == currentUserId
        targetUserId = userIdParam
        
        // Load profile data immediately
        loadProfile(userIdParam)
    }

    fun loadProfile(userId: String? = null) {
        // If userId is provided here, update state (though init handles the primary case)
        if (userId != userIdParam) {
            val currentUserId = gamerRepository.getCurrentUserId()
            isCurrentUser = userId == null || userId == currentUserId
            targetUserId = userId
        }
        
        screenReady = RequestState.Loading
        
        loadProfileData(userId)
        loadGamingStats(userId)
        loadFeaturedBadges(userId ?: gamerRepository.getCurrentUserId())
        
        // Load friendship status if viewing another user's profile
        // Requirements: 8.1-8.4, 14.6, 14.7
        val currentUserId = gamerRepository.getCurrentUserId()
        if (!isCurrentUser && userId != null && currentUserId != null) {
            loadFriendshipStatus(currentUserId, userId)
        } else {
            // Reset friendship state for own profile
            _friendshipState.value = ProfileFriendshipState()
        }
    }

    private fun loadProfileData(userId: String? = null) {
        launchWithKey("load_profile") {
            val flow = if (userId != null) {
                gamerRepository.getGamer(userId)
            } else {
                gamerRepository.readCustomerFlow()
            }

            flow.collectLatest { result ->
                if (result.isSuccess()) {
                    val gamer = result.getSuccessData()
                    profileState = ProfileState(
                        id = gamer.id,
                        name = gamer.name,
                        email = gamer.email,
                        username = gamer.username,
                        country = gamer.country,
                        city = gamer.city,
                        gender = gamer.gender,
                        description = gamer.description,
                        profileImageUrl = gamer.profileImageUrl,
                        steamId = gamer.steamId,
                        xboxGamertag = gamer.xboxGamertag,
                        psnId = gamer.psnId,
                        isProfilePublic = gamer.isProfilePublic
                    )
                    customSections = gamer.customSections
                    screenReady = RequestState.Success(Unit)
                } else if (result.isError()) {
                    screenReady = RequestState.Error(result.getErrorMessage())
                }
            }
        }
    }

    private fun loadGamingStats(userId: String? = null) {
        launchWithKey("load_stats") {
            val flow = if (userId != null) {
                gameListRepository.getGameListForUser(userId)
            } else {
                gameListRepository.getGameList()
            }

            flow.collectLatest { result ->
                if (result.isSuccess()) {
                    val games = result.getSuccessData()
                    
                    // Update library games for profile display
                    _libraryGames.value = games
                    
                    val totalGames = games.size
                    val finishedGames = games.count { it.status == GameStatus.FINISHED }
                    val playingGames = games.count { it.status == GameStatus.PLAYING }
                    val droppedGames = games.count { it.status == GameStatus.DROPPED }
                    val wantToPlayGames = games.count { it.status == GameStatus.WANT }
                    val onHoldGames = games.count { it.status == GameStatus.ON_HOLD }
                    val hoursPlayed = games.sumOf { it.hoursPlayed }
                    
                    val ratedGames = games.filter { it.rating != null && it.rating > 0 }
                    val avgRating = if (ratedGames.isNotEmpty()) {
                        ratedGames.mapNotNull { it.rating }.average().toFloat()
                    } else 0f
                    
                    val totalGamesForCompletion = totalGames - wantToPlayGames
                    val completionRate = if (totalGamesForCompletion > 0) {
                        (finishedGames.toFloat() / totalGamesForCompletion) * 100
                    } else 0f

                    statsState = ProfileStatsState(
                        totalGames = totalGames,
                        finishedGames = finishedGames,
                        playingGames = playingGames,
                        droppedGames = droppedGames,
                        wantToPlayGames = wantToPlayGames,
                        onHoldGames = onHoldGames,
                        hoursPlayed = hoursPlayed,
                        avgRating = avgRating,
                        completionRate = completionRate
                    )
                }
            }
        }
    }

    /**
     * Loads featured badges for the specified user.
     * Requirements: 9.1, 9.3
     */
    private fun loadFeaturedBadges(userId: String?) {
        if (userId == null) {
            _featuredBadges.value = emptyList()
            return
        }
        
        launchWithKey("load_badges") {
            featuredBadgesRepository.getFeaturedBadges(userId).collectLatest { badges ->
                _featuredBadges.value = badges
            }
        }
    }

    fun addCustomSection(title: String, type: ProfileSectionType, gameIds: List<Int>) {
        val newSection = ProfileSection(
            id = UUID.randomUUID().toString(),
            title = title,
            type = type,
            gameIds = gameIds,
            order = customSections.size
        )
        pendingSectionTitle = title
        pendingSectionActionMessage = "Section \"$title\" added"
        pendingSectionShowUndo = false
        customSections = customSections + newSection
        saveCustomSections()
    }

    fun updateCustomSection(sectionId: String, title: String, type: ProfileSectionType, gameIds: List<Int>) {
        pendingSectionTitle = title
        pendingSectionActionMessage = "Section \"$title\" updated"
        pendingSectionShowUndo = false
        customSections = customSections.map { section ->
            if (section.id == sectionId) {
                section.copy(title = title, type = type, gameIds = gameIds)
            } else {
                section
            }
        }
        saveCustomSections()
    }

    fun deleteCustomSection(sectionId: String) {
        val toDelete = customSections.find { it.id == sectionId } ?: return
        lastDeletedSection = toDelete
        lastDeletedIndex = customSections.indexOf(toDelete).takeIf { it >= 0 }
        customSections = customSections.filter { it.id != sectionId }
        pendingSectionTitle = toDelete.title
        pendingSectionActionMessage = "Section deleted"
        pendingSectionShowUndo = true
        sectionSnackbarState = SectionSnackbarState(
            title = toDelete.title,
            message = "Section deleted",
            showUndo = true
        )
    }

    private fun saveCustomSections() {
        launchWithKey("save_sections") {
            val result = gamerRepository.updateGamer(customSections = customSections)
            if (result.isError()) {
                loadProfileData(null)
                clearPendingSectionSnackbar()
            } else if (result.isSuccess()) {
                val title = pendingSectionTitle
                val message = pendingSectionActionMessage
                val showUndo = pendingSectionShowUndo ?: false
                if (title != null && message != null) {
                    sectionSnackbarState = SectionSnackbarState(
                        title = title,
                        message = message,
                        showUndo = showUndo
                    )
                }
                clearPendingSectionSnackbar()
                // Clear deletion caches after persistence
                if (showUndo) {
                    lastDeletedSection = null
                    lastDeletedIndex = null
                }
            }
        }
    }

    fun dismissSectionSnackbar() {
        sectionSnackbarState = null
    }

    private fun clearPendingSectionSnackbar() {
        pendingSectionTitle = null
        pendingSectionActionMessage = null
        pendingSectionShowUndo = null
    }

    fun undoDeleteSection() {
        val deleted = lastDeletedSection ?: return
        val insertIndex = lastDeletedIndex ?: customSections.size
        val mutable = customSections.toMutableList()
        mutable.add(insertIndex.coerceAtMost(mutable.size), deleted)
        customSections = mutable
        clearPendingSectionSnackbar()
        lastDeletedSection = null
        lastDeletedIndex = null
        sectionSnackbarState = null
        saveCustomSections()
    }

    fun onSectionSnackbarDismiss() {
        if (lastDeletedSection != null && (pendingSectionShowUndo == true || sectionSnackbarState?.showUndo == true)) {
            // finalize deletion
            saveCustomSections()
            lastDeletedSection = null
            lastDeletedIndex = null
            clearPendingSectionSnackbar()
        }
        sectionSnackbarState = null
    }

    fun showUnsavedSectionSnackbar(draft: SectionDraft) {
        unsavedSectionDraft = draft
        showUnsavedSectionSnackbar = true
    }

    fun dismissUnsavedSectionSnackbar() {
        showUnsavedSectionSnackbar = false
        unsavedSectionDraft = null
    }

    fun reopenUnsavedSectionDraft() {
        reopenSectionDraft = unsavedSectionDraft
        showUnsavedSectionSnackbar = false
        unsavedSectionDraft = null
    }

    fun consumeReopenSectionDraft() {
        reopenSectionDraft = null
    }

    fun saveUnsavedSection() {
        val draft = unsavedSectionDraft ?: return
        if (draft.id != null) {
            updateCustomSection(draft.id, draft.title, draft.type, draft.gameIds)
        } else {
            addCustomSection(draft.title, draft.type, draft.gameIds)
        }
        dismissUnsavedSectionSnackbar()
    }

    fun refreshData(userId: String? = null) {
        screenReady = RequestState.Loading
        loadProfile(userId)
    }

    /**
     * Determines if the roast button should be visible.
     * The roast button is shown only when viewing a public profile of another user.
     * 
     * Requirements: 14.1, 14.2, 14.3
     */
    fun shouldShowRoastButton(): Boolean {
        // Don't show on own profile (Requirement 14.1)
        if (isCurrentUser) return false
        
        // Don't show on private profiles (Requirement 14.2)
        if (!profileState.isProfilePublic) return false
        
        // Show on public profiles of other users (Requirement 14.3)
        return true
    }

    // ==================== Friendship Operations ====================

    /**
     * Loads the friendship status with real-time updates.
     * Requirements: 8.1-8.4, 14.6, 14.7
     */
    private fun loadFriendshipStatus(currentUserId: String, targetUserId: String) {
        launchWithKey("friendship_status") {
            friendsRepository.getFriendshipStatusRealtime(currentUserId, targetUserId)
                .collectLatest { status ->
                    _friendshipState.value = _friendshipState.value.copy(
                        status = status,
                        isLoading = false
                    )
                    
                    // If status is REQUEST_RECEIVED, fetch the pending request
                    if (status == FriendshipStatus.REQUEST_RECEIVED) {
                        loadPendingRequest(currentUserId, targetUserId)
                    } else {
                        _friendshipState.value = _friendshipState.value.copy(pendingRequest = null)
                    }
                }
        }
    }

    /**
     * Loads the pending friend request from the target user.
     * Used when status is REQUEST_RECEIVED to enable accepting.
     */
    private suspend fun loadPendingRequest(currentUserId: String, targetUserId: String) {
        try {
            val request = friendsRepository.checkReciprocalRequest(currentUserId, targetUserId)
            _friendshipState.value = _friendshipState.value.copy(pendingRequest = request)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading pending request: ${e.message}", e)
        }
    }

    /**
     * Sends a friend request to the profile user.
     * Requirements: 8.5, 8.6, 8.7, 13.1, 13.2
     */
    fun sendFriendRequest() {
        val currentUserId = gamerRepository.getCurrentUserId() ?: return
        val targetId = targetUserId ?: return
        
        // Check network connectivity - Requirements: 13.1, 13.4
        if (!isOnline()) {
            _friendshipState.value = _friendshipState.value.copy(error = NETWORK_ERROR_MESSAGE)
            return
        }
        
        viewModelScope.launch {
            _friendshipState.value = _friendshipState.value.copy(
                isLoading = true,
                error = null,
                successMessage = null
            )
            
            try {
                // Check for reciprocal request first
                val reciprocalRequest = friendsRepository.checkReciprocalRequest(currentUserId, targetId)
                if (reciprocalRequest != null) {
                    // Target user already sent us a request, accept it instead
                    _friendshipState.value = _friendshipState.value.copy(
                        isLoading = false,
                        pendingRequest = reciprocalRequest,
                        status = FriendshipStatus.REQUEST_RECEIVED
                    )
                    return@launch
                }
                
                // Check for declined request cooldown - Requirements: 3.26
                val cooldownHours = friendsRepository.getDeclinedRequestCooldown(currentUserId, targetId)
                if (cooldownHours != null && cooldownHours > 0) {
                    _friendshipState.value = _friendshipState.value.copy(
                        isLoading = false,
                        limitReachedType = LimitType.COOLDOWN,
                        cooldownHours = cooldownHours
                    )
                    return@launch
                }
                
                // Validate limits - Requirements: 3.21, 3.22
                val validation = friendsRepository.canSendRequest(currentUserId)
                if (!validation.canSend) {
                    val limitType = when {
                        validation.dailyLimitReached -> LimitType.DAILY_LIMIT
                        validation.pendingLimitReached -> LimitType.PENDING_LIMIT
                        validation.friendsLimitReached -> LimitType.FRIENDS_LIMIT
                        else -> null
                    }
                    _friendshipState.value = _friendshipState.value.copy(
                        isLoading = false,
                        limitReachedType = limitType
                    )
                    return@launch
                }
                
                // Get current user info
                val currentUserInfo = getCurrentUserInfo()
                if (currentUserInfo == null) {
                    _friendshipState.value = _friendshipState.value.copy(
                        isLoading = false,
                        error = "Failed to get user information"
                    )
                    return@launch
                }
                
                // Send the request
                val result = friendsRepository.sendFriendRequest(
                    fromUserId = currentUserId,
                    fromUsername = currentUserInfo.first,
                    fromProfileImageUrl = currentUserInfo.second,
                    toUserId = targetId,
                    toUsername = profileState.username,
                    toProfileImageUrl = profileState.profileImageUrl
                )
                
                when (result) {
                    is RequestState.Success -> {
                        _friendshipState.value = _friendshipState.value.copy(
                            isLoading = false,
                            status = FriendshipStatus.REQUEST_SENT,
                            successMessage = null
                        )
                        friendActionSnackbarState = FriendActionSnackbarState(
                            username = profileState.username,
                            message = "Friend request sent"
                        )
                    }
                    is RequestState.Error -> {
                        _friendshipState.value = _friendshipState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                    else -> {
                        _friendshipState.value = _friendshipState.value.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending friend request: ${e.message}", e)
                _friendshipState.value = _friendshipState.value.copy(
                    isLoading = false,
                    error = "Failed to send friend request"
                )
            }
        }
    }

    /**
     * Accepts a friend request from the profile user.
     * Requirements: 8.8, 8.9, 13.1, 13.2
     */
    fun acceptFriendRequest() {
        val request = _friendshipState.value.pendingRequest ?: return
        val currentUserId = gamerRepository.getCurrentUserId() ?: return
        
        // Check network connectivity - Requirements: 13.1, 13.4
        if (!isOnline()) {
            _friendshipState.value = _friendshipState.value.copy(error = NETWORK_ERROR_MESSAGE)
            return
        }
        
        viewModelScope.launch {
            _friendshipState.value = _friendshipState.value.copy(
                isLoading = true,
                error = null,
                successMessage = null
            )
            
            try {
                // Validate friends limit - Requirements: 6.11
                val validation = friendsRepository.canSendRequest(currentUserId)
                if (validation.friendsLimitReached) {
                    _friendshipState.value = _friendshipState.value.copy(
                        isLoading = false,
                        limitReachedType = LimitType.FRIENDS_LIMIT
                    )
                    return@launch
                }
                
                val result = friendsRepository.acceptFriendRequest(request)
                
                when (result) {
                    is RequestState.Success -> {
                        _friendshipState.value = _friendshipState.value.copy(
                            isLoading = false,
                            status = FriendshipStatus.FRIENDS,
                            pendingRequest = null,
                            successMessage = null
                        )
                        friendActionSnackbarState = FriendActionSnackbarState(
                            username = profileState.username,
                            message = "Friend added"
                        )
                    }
                    is RequestState.Error -> {
                        _friendshipState.value = _friendshipState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                    else -> {
                        _friendshipState.value = _friendshipState.value.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error accepting friend request: ${e.message}", e)
                _friendshipState.value = _friendshipState.value.copy(
                    isLoading = false,
                    error = "Failed to accept friend request"
                )
            }
        }
    }

    /**
     * Shows the unfriend confirmation dialog.
     * Requirements: 9.1
     */
    fun showUnfriendDialog() {
        _friendshipState.value = _friendshipState.value.copy(showUnfriendDialog = true)
    }

    /**
     * Dismisses the unfriend confirmation dialog.
     * Requirements: 9.4
     */
    fun dismissUnfriendDialog() {
        _friendshipState.value = _friendshipState.value.copy(showUnfriendDialog = false)
    }

    /**
     * Removes the profile user from friends.
     * Requirements: 9.2, 9.3, 9.5, 13.1, 13.2
     */
    fun unfriend() {
        val currentUserId = gamerRepository.getCurrentUserId() ?: return
        val targetId = targetUserId ?: return
        
        // Check network connectivity - Requirements: 13.1, 13.4
        if (!isOnline()) {
            _friendshipState.value = _friendshipState.value.copy(
                showUnfriendDialog = false,
                error = NETWORK_ERROR_MESSAGE
            )
            return
        }
        
        viewModelScope.launch {
            _friendshipState.value = _friendshipState.value.copy(
                isLoading = true,
                showUnfriendDialog = false,
                error = null,
                successMessage = null
            )
            
            try {
                val result = friendsRepository.removeFriend(currentUserId, targetId)
                
                when (result) {
                    is RequestState.Success -> {
                        _friendshipState.value = _friendshipState.value.copy(
                            isLoading = false,
                            status = FriendshipStatus.NOT_FRIENDS,
                            successMessage = null
                        )
                        friendActionSnackbarState = FriendActionSnackbarState(
                            username = profileState.username,
                            message = "Removed from friends"
                        )
                    }
                    is RequestState.Error -> {
                        _friendshipState.value = _friendshipState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                    else -> {
                        _friendshipState.value = _friendshipState.value.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error unfriending: ${e.message}", e)
                _friendshipState.value = _friendshipState.value.copy(
                    isLoading = false,
                    error = "Failed to remove friend"
                )
            }
        }
    }

    /**
     * Handles the friend action button tap based on current status.
     * Requirements: 8.5, 8.8, 9.1
     */
    fun onFriendActionButtonClick() {
        when (_friendshipState.value.status) {
            FriendshipStatus.NOT_FRIENDS -> sendFriendRequest()
            FriendshipStatus.REQUEST_RECEIVED -> acceptFriendRequest()
            FriendshipStatus.FRIENDS -> showUnfriendDialog()
            FriendshipStatus.REQUEST_SENT -> {
                // Button is disabled in this state, no action needed
            }
        }
    }

    /**
     * Clears the friendship error message.
     */
    fun clearFriendshipError() {
        _friendshipState.value = _friendshipState.value.copy(error = null)
    }

    /**
     * Clears the friendship success message.
     */
    fun clearFriendshipSuccess() {
        _friendshipState.value = _friendshipState.value.copy(successMessage = null)
    }

    fun dismissFriendActionSnackbar() {
        friendActionSnackbarState = null
    }
    
    /**
     * Dismisses the limit reached dialog.
     * Requirements: 3.21, 3.22, 3.26, 6.11
     */
    fun dismissLimitDialog() {
        _friendshipState.value = _friendshipState.value.copy(
            limitReachedType = null,
            cooldownHours = null
        )
    }

    /**
     * Determines if the friend action button should be visible.
     * Requirements: 8.11
     */
    fun shouldShowFriendActionButton(): Boolean {
        // Don't show on own profile
        return !isCurrentUser
    }

    /**
     * Gets the current user's username and profile image URL.
     */
    private suspend fun getCurrentUserInfo(): Pair<String, String?>? {
        return try {
            val state = gamerRepository.readCustomerFlow().first { it.isSuccess() || it.isError() }
            if (state is RequestState.Success) {
                Pair(state.data.username, state.data.profileImageUrl)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user info: ${e.message}", e)
            null
        }
    }
}
