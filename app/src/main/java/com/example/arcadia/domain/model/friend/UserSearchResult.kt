package com.example.arcadia.domain.model.friend

import kotlinx.serialization.Serializable

/**
 * Represents a user in search results with their friendship status.
 * Used in the Add Friends search functionality.
 */
@Serializable
data class UserSearchResult(
    val userId: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val friendshipStatus: FriendshipStatus = FriendshipStatus.NOT_FRIENDS,
    val isRecentlyDeclined: Boolean = false
)
