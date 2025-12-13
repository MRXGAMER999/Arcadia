package com.example.arcadia.domain.usecase.friend

import com.example.arcadia.domain.model.friend.CooldownException
import com.example.arcadia.domain.model.friend.LimitReachedException
import com.example.arcadia.domain.model.friend.NotAuthenticatedException
import com.example.arcadia.domain.model.friend.ReciprocalRequestException
import com.example.arcadia.domain.model.friend.UserInfoException
import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.presentation.screens.friends.components.LimitType
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.first

/**
 * Use case for sending a friend request.
 * Encapsulates all validation logic:
 * - Authentication check
 * - Reciprocal request detection
 * - Cooldown period check
 * - Limit validation (daily, pending, friends)
 * 
 * This centralizes the friend request logic that was previously
 * duplicated in FriendsViewModel and ProfileViewModel.
 */
class SendFriendRequestUseCase(
    private val friendsRepository: FriendsRepository,
    private val gamerRepository: GamerRepository
) {
    /**
     * Sends a friend request to the target user.
     * 
     * @param targetUserId The ID of the user to send the request to
     * @param targetUsername The username of the target user
     * @param targetProfileImageUrl The profile image URL of the target user (nullable)
     * @return Result.success(Unit) on success, or Result.failure with specific exception
     * 
     * Possible exceptions:
     * - NotAuthenticatedException: User is not logged in
     * - ReciprocalRequestException: Target user already sent a request
     * - CooldownException: User was recently declined by target
     * - LimitReachedException: Daily/pending/friends limit reached
     * - UserInfoException: Failed to get current user info
     * - Exception: Generic failure from repository
     */
    suspend operator fun invoke(
        targetUserId: String,
        targetUsername: String,
        targetProfileImageUrl: String?
    ): Result<Unit> {
        // 1. Check authentication
        val currentUserId = gamerRepository.getCurrentUserId()
            ?: return Result.failure(NotAuthenticatedException())
        
        // 2. Prevent self-request
        if (targetUserId == currentUserId) {
            return Result.failure(IllegalArgumentException("Cannot send friend request to yourself"))
        }
        
        // 3. Check for reciprocal request
        val reciprocalRequest = friendsRepository.checkReciprocalRequest(currentUserId, targetUserId)
        if (reciprocalRequest != null) {
            return Result.failure(ReciprocalRequestException(reciprocalRequest))
        }
        
        // 4. Check cooldown period
        val cooldownHours = friendsRepository.getDeclinedRequestCooldown(currentUserId, targetUserId)
        if (cooldownHours != null && cooldownHours > 0) {
            return Result.failure(CooldownException(cooldownHours))
        }
        
        // 5. Validate limits
        val validation = friendsRepository.canSendRequest(currentUserId)
        if (!validation.canSend) {
            val limitType = when {
                validation.dailyLimitReached -> LimitType.DAILY_LIMIT
                validation.pendingLimitReached -> LimitType.PENDING_LIMIT
                validation.friendsLimitReached -> LimitType.FRIENDS_LIMIT
                else -> LimitType.DAILY_LIMIT // Fallback
            }
            return Result.failure(LimitReachedException(limitType, validation))
        }
        
        // 6. Get current user info
        val currentUserInfo = getCurrentUserInfo()
            ?: return Result.failure(UserInfoException())
        
        // 7. Send the request
        return when (val result = friendsRepository.sendFriendRequest(
            fromUserId = currentUserId,
            fromUsername = currentUserInfo.first,
            fromProfileImageUrl = currentUserInfo.second,
            toUserId = targetUserId,
            toUsername = targetUsername,
            toProfileImageUrl = targetProfileImageUrl
        )) {
            is RequestState.Success -> Result.success(Unit)
            is RequestState.Error -> Result.failure(Exception(result.message))
            else -> Result.failure(Exception("Unexpected state"))
        }
    }
    
    private suspend fun getCurrentUserInfo(): Pair<String, String?>? {
        return try {
            val state = gamerRepository.readCustomerFlow()
                .first { it.isSuccess() || it.isError() }
            
            if (state is RequestState.Success) {
                Pair(state.data.username, state.data.profileImageUrl)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
