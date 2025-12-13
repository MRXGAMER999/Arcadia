package com.example.arcadia.domain.usecase.friend

import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.util.RequestState

/**
 * Use case for declining a friend request.
 */
class DeclineFriendRequestUseCase(
    private val friendsRepository: FriendsRepository
) {
    /**
     * Declines a friend request.
     * The request is marked as declined (not deleted) for cooldown tracking.
     * 
     * @param requestId The ID of the request to decline
     * @return Result.success(Unit) on success, or Result.failure with exception
     */
    suspend operator fun invoke(requestId: String): Result<Unit> {
        return when (val result = friendsRepository.declineFriendRequest(requestId)) {
            is RequestState.Success -> Result.success(Unit)
            is RequestState.Error -> Result.failure(Exception(result.message))
            else -> Result.failure(Exception("Unexpected state"))
        }
    }
}
