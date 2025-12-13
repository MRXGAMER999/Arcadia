package com.example.arcadia.domain.usecase.friend

import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.util.RequestState

/**
 * Use case for cancelling a sent friend request.
 */
class CancelFriendRequestUseCase(
    private val friendsRepository: FriendsRepository
) {
    /**
     * Cancels a sent friend request.
     * The request is deleted from the database.
     * 
     * @param requestId The ID of the request to cancel
     * @return Result.success(Unit) on success, or Result.failure with exception
     */
    suspend operator fun invoke(requestId: String): Result<Unit> {
        return when (val result = friendsRepository.cancelFriendRequest(requestId)) {
            is RequestState.Success -> Result.success(Unit)
            is RequestState.Error -> Result.failure(Exception(result.message))
            else -> Result.failure(Exception("Unexpected state"))
        }
    }
}
