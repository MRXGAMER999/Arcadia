package com.example.arcadia.domain.model.friend

import com.example.arcadia.presentation.screens.friends.components.LimitType

/**
 * Sealed class representing outcomes of friend actions.
 * Provides type-safe error handling for use cases.
 */
sealed class FriendActionResult {
    data object Success : FriendActionResult()
    data class Error(val message: String) : FriendActionResult()
}

/**
 * Exception thrown when a reciprocal request exists.
 * The target user has already sent a request to the current user.
 */
class ReciprocalRequestException(
    val request: FriendRequest
) : Exception("User has already sent you a friend request")

/**
 * Exception thrown when the user is in a cooldown period.
 * Occurs after being declined by the target user.
 */
class CooldownException(
    val hoursRemaining: Int
) : Exception("Please wait $hoursRemaining hours before sending another request")

/**
 * Exception thrown when a limit has been reached.
 */
class LimitReachedException(
    val limitType: LimitType,
    val validation: RequestValidation? = null
) : Exception(
    when (limitType) {
        LimitType.DAILY_LIMIT -> "Daily friend request limit reached"
        LimitType.PENDING_LIMIT -> "Too many pending requests"
        LimitType.FRIENDS_LIMIT -> "Maximum friends limit reached"
        LimitType.COOLDOWN -> "Cooldown period active"
    }
)

/**
 * Exception thrown when the user is not authenticated.
 */
class NotAuthenticatedException : Exception("User not authenticated")

/**
 * Exception thrown when user info cannot be retrieved.
 */
class UserInfoException : Exception("Failed to get user information")
