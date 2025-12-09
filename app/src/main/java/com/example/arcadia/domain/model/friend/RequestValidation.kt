package com.example.arcadia.domain.model.friend

import kotlinx.serialization.Serializable

/**
 * Result of validating whether a user can send a friend request.
 * Contains flags for each limit type that may prevent sending.
 */
@Serializable
data class RequestValidation(
    /** Whether the user can send a friend request */
    val canSend: Boolean = true,
    
    /** Whether the daily limit (20 requests/day) has been reached */
    val dailyLimitReached: Boolean = false,
    
    /** Whether the pending requests limit (100) has been reached */
    val pendingLimitReached: Boolean = false,
    
    /** Whether the friends limit (500) has been reached */
    val friendsLimitReached: Boolean = false
)
