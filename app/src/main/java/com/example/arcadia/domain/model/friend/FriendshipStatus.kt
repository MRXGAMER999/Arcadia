package com.example.arcadia.domain.model.friend

/**
 * Represents the relationship state between two users.
 * Used to determine which action button to display on profiles and search results.
 */
enum class FriendshipStatus {
    /** No relationship exists between the users */
    NOT_FRIENDS,
    
    /** Current user has sent a pending request to the target user */
    REQUEST_SENT,
    
    /** Target user has sent a pending request to the current user */
    REQUEST_RECEIVED,
    
    /** Users are friends (mutual connection exists) */
    FRIENDS
}
