package com.example.arcadia.domain.usecase.friend

import com.example.arcadia.domain.model.friend.FriendRequestStatus
import com.example.arcadia.domain.model.friend.FriendshipStatus
import com.example.arcadia.domain.model.friend.NotAuthenticatedException
import com.example.arcadia.domain.model.friend.UserSearchResult
import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * Use case for searching users with friendship status.
 * 
 * This fixes the issue where search results didn't include
 * the correct friendship status, causing incorrect action buttons.
 */
class SearchUsersUseCase(
    private val friendsRepository: FriendsRepository,
    private val gamerRepository: GamerRepository
) {
    companion object {
        private const val MIN_QUERY_LENGTH = 2
    }
    
    /**
     * Searches for users by username and enriches results with friendship status.
     * 
     * @param query The search query (minimum 2 characters)
     * @return RequestState with list of UserSearchResult including friendship status
     */
    suspend operator fun invoke(query: String): RequestState<List<UserSearchResult>> {
        if (query.length < MIN_QUERY_LENGTH) {
            return RequestState.Success(emptyList())
        }
        
        val currentUserId = gamerRepository.getCurrentUserId()
            ?: return RequestState.Error("User not authenticated")
        
        return try {
            // Get search results
            val searchResult = friendsRepository.searchUsers(query, currentUserId)
            
            if (searchResult !is RequestState.Success) {
                return searchResult
            }
            
            val users = searchResult.data
            if (users.isEmpty()) {
                return RequestState.Success(emptyList())
            }
            
            // Enrich with friendship status
            val enrichedUsers = enrichWithFriendshipStatus(currentUserId, users)
            RequestState.Success(enrichedUsers)
        } catch (e: Exception) {
            RequestState.Error("Search failed: ${e.message}")
        }
    }
    
    /**
     * Enriches search results with friendship status for each user.
     * Uses parallel queries for efficiency.
     */
    private suspend fun enrichWithFriendshipStatus(
        currentUserId: String,
        users: List<UserSearchResult>
    ): List<UserSearchResult> = coroutineScope {
        // Fetch all friendship data in parallel
        val friendsDeferred = async {
            try {
                friendsRepository.getFriendsRealtime(currentUserId)
                    .first { it is RequestState.Success || it is RequestState.Error }
                    .let { state ->
                        if (state is RequestState.Success) state.data.map { it.userId }.toSet()
                        else emptySet()
                    }
            } catch (e: Exception) {
                emptySet()
            }
        }
        
        val outgoingDeferred = async {
            try {
                friendsRepository.getOutgoingRequestsRealtime(currentUserId)
                    .first { it is RequestState.Success || it is RequestState.Error }
                    .let { state ->
                        if (state is RequestState.Success) {
                            state.data
                                .filter { it.status == FriendRequestStatus.PENDING }
                                .map { it.toUserId }
                                .toSet()
                        } else emptySet()
                    }
            } catch (e: Exception) {
                emptySet()
            }
        }
        
        val incomingDeferred = async {
            try {
                friendsRepository.getIncomingRequestsRealtime(currentUserId)
                    .first { it is RequestState.Success || it is RequestState.Error }
                    .let { state ->
                        if (state is RequestState.Success) {
                            state.data
                                .filter { it.status == FriendRequestStatus.PENDING }
                                .map { it.fromUserId }
                                .toSet()
                        } else emptySet()
                    }
            } catch (e: Exception) {
                emptySet()
            }
        }
        
        val friendIds = friendsDeferred.await()
        val outgoingRequestIds = outgoingDeferred.await()
        val incomingRequestIds = incomingDeferred.await()
        
        // Map users with their friendship status
        users.map { user ->
            val status = when {
                friendIds.contains(user.userId) -> FriendshipStatus.FRIENDS
                outgoingRequestIds.contains(user.userId) -> FriendshipStatus.REQUEST_SENT
                incomingRequestIds.contains(user.userId) -> FriendshipStatus.REQUEST_RECEIVED
                else -> FriendshipStatus.NOT_FRIENDS
            }
            user.copy(friendshipStatus = status)
        }
    }
}
