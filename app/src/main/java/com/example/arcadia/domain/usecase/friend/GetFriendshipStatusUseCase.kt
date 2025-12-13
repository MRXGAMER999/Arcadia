package com.example.arcadia.domain.usecase.friend

import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.domain.model.friend.FriendshipStatus
import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.domain.repository.GamerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Use case for getting realtime friendship status with another user.
 * Used on profile screens to show the correct action button.
 */
class GetFriendshipStatusUseCase(
    private val friendsRepository: FriendsRepository,
    private val gamerRepository: GamerRepository
) {
    /**
     * Data class containing friendship status and pending request if applicable.
     */
    data class FriendshipStatusResult(
        val status: FriendshipStatus,
        val pendingRequest: FriendRequest?
    )
    
    /**
     * Gets realtime friendship status with the target user.
     * 
     * @param targetUserId The ID of the user to check status with
     * @return Flow emitting Result with FriendshipStatusResult
     */
    operator fun invoke(targetUserId: String): Flow<Result<FriendshipStatusResult>> {
        val currentUserId = gamerRepository.getCurrentUserId()
            ?: return flow { emit(Result.failure(Exception("User not authenticated"))) }
        
        return friendsRepository.getFriendshipStatusRealtime(currentUserId, targetUserId)
            .map { status ->
                // If status is REQUEST_RECEIVED, fetch the pending request
                val pendingRequest = if (status == FriendshipStatus.REQUEST_RECEIVED) {
                    try {
                        friendsRepository.checkReciprocalRequest(currentUserId, targetUserId)
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
                
                Result.success(FriendshipStatusResult(status, pendingRequest))
            }
    }
}
