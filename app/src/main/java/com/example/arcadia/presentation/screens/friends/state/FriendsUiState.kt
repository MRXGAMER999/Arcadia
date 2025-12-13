package com.example.arcadia.presentation.screens.friends.state

import androidx.paging.PagingData
import com.example.arcadia.domain.model.friend.Friend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * UI state for the Friends list screen.
 * Follows unidirectional data flow with immutable state.
 */
data class FriendsUiState(
    /** Paginated friends flow - collected by LazyPagingItems */
    val friendsPagingFlow: Flow<PagingData<Friend>> = emptyFlow(),
    
    /** Count of pending incoming requests for badge */
    val pendingRequestCount: Int = 0,
    
    /** Network connectivity status */
    val isOffline: Boolean = false,
    
    /** One-time events that should be consumed */
    val event: FriendsEvent? = null
)

/**
 * One-time events for the Friends screen.
 * Consumed after handling to prevent re-emission on recomposition.
 */
sealed interface FriendsEvent {
    data class ShowError(val message: String) : FriendsEvent
    data class NavigateToProfile(val userId: String) : FriendsEvent
    data object OpenAddFriendsSheet : FriendsEvent
}
