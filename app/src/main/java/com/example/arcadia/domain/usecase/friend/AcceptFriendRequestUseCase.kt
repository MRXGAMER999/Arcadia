package com.example.arcadia.domain.usecase.friend

import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.domain.model.friend.LimitReachedException
import com.example.arcadia.domain.model.friend.NotAuthenticatedException
import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.presentation.screens.friends.components.LimitType
import com.example.arcadia.util.RequestState

/**
 * Use case for accepting a friend request.
 * Validates friends limit before accepting.
 */
class AcceptFriendRequestUseCase(
    private val friendsRepository: FriendsRepository,
    private val gamerRepository: GamerRepository
) {
    /**
     * Accepts a friend request.
     * 
     * @param request The friend request to accept
     * @return Result.success(Unit) on success, or Result.failure with specific exception
     */
    suspend operator fun invoke(request: FriendRequest): Result<Unit> {
        val currentUserId = gamerRepository.getCurrentUserId()
            ?: return Result.failure(NotAuthenticatedException())
        
        // Validate friends limit
        val validation = friendsRepository.canSendRequest(currentUserId)
        if (validation.friendsLimitReached) {
            return Result.failure(LimitReachedException(LimitType.FRIENDS_LIMIT, validation))
        }
        
        return when (val result = friendsRepository.acceptFriendRequest(request)) {
            is RequestState.Success -> Result.success(Unit)
            is RequestState.Error -> Result.failure(Exception(result.message))
            else -> Result.failure(Exception("Unexpected state"))
        }
    }
    
    /**
     * Accepts a friend request from a specific user.
     * Looks up the pending request first.
     * 
     * @param fromUserId The ID of the user who sent the request
     * @return Result.success(Unit) on success, or Result.failure with specific exception
     */
    suspend fun fromUser(fromUserId: String): Result<Unit> {
        val currentUserId = gamerRepository.getCurrentUserId()
            ?: return Result.failure(NotAuthenticatedException())
        
        // Find the pending request
        val request = friendsRepository.checkReciprocalRequest(currentUserId, fromUserId)
            ?: return Result.failure(Exception("Friend request not found"))
        
        return invoke(request)
    }
}
