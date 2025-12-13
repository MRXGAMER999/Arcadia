package com.example.arcadia.presentation.screens.friends.state

import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.domain.model.friend.UserSearchResult
import com.example.arcadia.presentation.screens.friends.components.LimitType

/**
 * UI state for user search in Add Friends flow.
 */
data class UserSearchUiState(
    val query: String = "",
    val results: List<UserSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val hint: SearchHint = SearchHint.MinCharacters,
    
    /** ID of user currently being actioned (for button loading) */
    val actioningUserId: String? = null,
    
    /** One-time events */
    val event: SearchEvent? = null
)

/**
 * Search hint states.
 */
sealed interface SearchHint {
    data object MinCharacters : SearchHint
    data object NoResults : SearchHint
    data object SearchFailed : SearchHint
    data object None : SearchHint
}

/**
 * One-time events for search.
 */
sealed interface SearchEvent {
    data class ShowSnackbar(val message: String, val username: String) : SearchEvent
    data class ShowError(val message: String) : SearchEvent
    data class ShowLimitDialog(val limitType: LimitType, val cooldownHours: Int? = null) : SearchEvent
    data class ShowReciprocalDialog(val request: FriendRequest, val user: UserSearchResult) : SearchEvent
    data class NavigateToProfile(val userId: String) : SearchEvent
}
