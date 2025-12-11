package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Provides the realtime pending friend request count for the current user.
 * Encapsulates the lookup of the current user ID so callers only depend on this use case.
 */
class GetPendingFriendRequestsCountUseCase(
    private val gamerRepository: GamerRepository,
    private val friendsRepository: FriendsRepository
) {
    operator fun invoke(): Flow<RequestState<Int>> {
        val userId = gamerRepository.getCurrentUserId()
            ?: return flowOf(RequestState.Error("User not authenticated"))
        return friendsRepository.getPendingRequestCountRealtime(userId)
    }
}













