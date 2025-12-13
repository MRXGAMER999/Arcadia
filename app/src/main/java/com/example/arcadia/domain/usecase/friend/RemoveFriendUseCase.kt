package com.example.arcadia.domain.usecase.friend

import com.example.arcadia.domain.model.friend.NotAuthenticatedException
import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.util.RequestState

/**
 * Use case for removing a friend.
 */
class RemoveFriendUseCase(
    private val friendsRepository: FriendsRepository,
    private val gamerRepository: GamerRepository
) {
    /**
     * Removes a friend from the current user's friends list.
     * This is a mutual operation - both users are removed from each other's lists.
     * 
     * @param friendUserId The ID of the friend to remove
     * @return Result.success(Unit) on success, or Result.failure with exception
     */
    suspend operator fun invoke(friendUserId: String): Result<Unit> {
        val currentUserId = gamerRepository.getCurrentUserId()
            ?: return Result.failure(NotAuthenticatedException())
        
        return when (val result = friendsRepository.removeFriend(currentUserId, friendUserId)) {
            is RequestState.Success -> Result.success(Unit)
            is RequestState.Error -> Result.failure(Exception(result.message))
            else -> Result.failure(Exception("Unexpected state"))
        }
    }
}
