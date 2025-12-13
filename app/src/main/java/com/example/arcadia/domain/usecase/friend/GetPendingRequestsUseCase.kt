package com.example.arcadia.domain.usecase.friend

import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Use case for getting pending (incoming) friend requests.
 */
class GetPendingRequestsUseCase(
    private val friendsRepository: FriendsRepository,
    private val gamerRepository: GamerRepository
) {
    /**
     * Gets realtime updates of incoming friend requests.
     * 
     * @return Flow emitting RequestState with list of incoming requests
     */
    operator fun invoke(): Flow<RequestState<List<FriendRequest>>> {
        val userId = gamerRepository.getCurrentUserId()
            ?: return flowOf(RequestState.Error("User not authenticated"))
        
        return friendsRepository.getIncomingRequestsRealtime(userId)
    }
}
