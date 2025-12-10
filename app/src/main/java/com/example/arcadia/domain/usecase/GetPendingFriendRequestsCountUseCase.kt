package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.domain.repository.GamerRepository
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
    operator fun invoke(): Flow<Int> {
        val userId = gamerRepository.getCurrentUserId() ?: return flowOf(0)
        return friendsRepository.getPendingRequestCountRealtime(userId)
    }
}

