package com.example.arcadia.domain.repository

import com.example.arcadia.domain.model.friend.Friend
import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.domain.model.friend.FriendshipStatus
import com.example.arcadia.domain.model.friend.RequestValidation
import com.example.arcadia.domain.model.friend.UserSearchResult
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing friends and friend requests.
 * Provides methods for CRUD operations, real-time listeners, search, and validation.
 */
interface FriendsRepository {
    
    // ==================== Friends List Operations ====================
    
    /**
     * Get real-time updates of the user's friends list.
     * @param userId The user whose friends list to observe
     * @return Flow emitting the friends list sorted alphabetically by username
     */
    fun getFriendsRealtime(userId: String): Flow<RequestState<List<Friend>>>
    
    /**
     * Get paginated friends list for efficient loading.
     * @param userId The user whose friends list to retrieve
     * @param limit Maximum number of friends to return (default 20)
     * @param lastUsername Username to start after for pagination (null for first page)
     * @return Flow emitting the paginated friends list
     */
    fun getFriendsPaginated(
        userId: String, 
        limit: Int, 
        lastUsername: String?
    ): Flow<RequestState<List<Friend>>>
    
    /**
     * Remove a friend from both users' friends lists.
     * Uses batch write for atomicity.
     * @param currentUserId The current user's ID
     * @param friendUserId The friend to remove
     * @return Result of the operation
     */
    suspend fun removeFriend(currentUserId: String, friendUserId: String): RequestState<Unit>
    
    /**
     * Check if two users are friends.
     * @param userId The first user's ID
     * @param friendUserId The second user's ID
     * @return True if they are friends, false otherwise
     */
    suspend fun isFriend(userId: String, friendUserId: String): Boolean
    
    // ==================== Friend Request Operations ====================
    
    /**
     * Get real-time updates of incoming friend requests.
     * @param userId The user whose incoming requests to observe
     * @return Flow emitting pending incoming requests sorted by newest first
     */
    fun getIncomingRequestsRealtime(userId: String): Flow<RequestState<List<FriendRequest>>>
    
    /**
     * Get real-time updates of outgoing friend requests.
     * @param userId The user whose outgoing requests to observe
     * @return Flow emitting pending outgoing requests sorted by newest first
     */
    fun getOutgoingRequestsRealtime(userId: String): Flow<RequestState<List<FriendRequest>>>
    
    /**
     * Get real-time count of pending incoming requests for badge display.
     * @param userId The user whose pending request count to observe
     * @return Flow emitting the count of pending incoming requests
     */
    fun getPendingRequestCountRealtime(userId: String): Flow<RequestState<Int>>
    
    /**
     * Send a friend request to another user.
     * Validates limits and creates request document in Appwrite.
     * @param fromUserId Sender's user ID
     * @param fromUsername Sender's username
     * @param fromProfileImageUrl Sender's profile image URL (nullable)
     * @param toUserId Recipient's user ID
     * @param toUsername Recipient's username
     * @param toProfileImageUrl Recipient's profile image URL (nullable)
     * @return Result of the operation
     */
    suspend fun sendFriendRequest(
        fromUserId: String,
        fromUsername: String,
        fromProfileImageUrl: String?,
        toUserId: String,
        toUsername: String,
        toProfileImageUrl: String?
    ): RequestState<Unit>
    
    /**
     * Accept a friend request.
     * Uses batch write to update request status and add both users to each other's friends.
     * @param request The friend request to accept
     * @return Result of the operation
     */
    suspend fun acceptFriendRequest(request: FriendRequest): RequestState<Unit>
    
    /**
     * Decline a friend request.
     * Updates request status to "declined" (not deleted for cooldown tracking).
     * @param requestId The ID of the request to decline
     * @return Result of the operation
     */
    suspend fun declineFriendRequest(requestId: String): RequestState<Unit>
    
    /**
     * Cancel a sent friend request.
     * Deletes the request document from Appwrite.
     * @param requestId The ID of the request to cancel
     * @return Result of the operation
     */
    suspend fun cancelFriendRequest(requestId: String): RequestState<Unit>
    
    // ==================== Friendship Status Operations ====================
    
    /**
     * Get real-time friendship status between two users.
     * @param currentUserId The current user's ID
     * @param targetUserId The target user's ID
     * @return Flow emitting the current friendship status
     */
    fun getFriendshipStatusRealtime(
        currentUserId: String, 
        targetUserId: String
    ): Flow<FriendshipStatus>
    
    /**
     * Check if the target user has sent a pending request to the current user.
     * Used to detect reciprocal requests before sending.
     * @param currentUserId The current user's ID
     * @param targetUserId The target user's ID
     * @return The pending request if exists, null otherwise
     */
    suspend fun checkReciprocalRequest(
        currentUserId: String, 
        targetUserId: String
    ): FriendRequest?
    
    // ==================== Search Operations ====================
    
    /**
     * Search for users by username.
     * Performs case-insensitive partial matching, excludes current user,
     * and includes friendship status for each result.
     * @param query The search query (minimum 2 characters)
     * @param currentUserId The current user's ID (to exclude from results)
     * @return Result containing list of matching users with friendship status
     */
    suspend fun searchUsers(
        query: String, 
        currentUserId: String
    ): RequestState<List<UserSearchResult>>
    
    // ==================== Validation Operations ====================
    
    /**
     * Validate if a user can send a friend request.
     * Checks daily limit (20), pending limit (100), and friends limit (500).
     * @param userId The user to validate
     * @return Validation result with flags for each limit type
     */
    suspend fun canSendRequest(userId: String): RequestValidation
    
    /**
     * Get remaining cooldown hours for a declined request.
     * Users must wait 24 hours after being declined before sending again.
     * @param fromUserId The sender's user ID
     * @param toUserId The recipient's user ID
     * @return Hours remaining in cooldown, or null if no cooldown applies
     */
    suspend fun getDeclinedRequestCooldown(fromUserId: String, toUserId: String): Int?
    
    // ==================== Profile Data Sync Operations ====================
    
    /**
     * Update cached friend profile data in the friends subcollection.
     * Called when viewing a friend's profile and detecting stale data.
     * @param userId The user whose friends subcollection to update
     * @param friendUserId The friend whose data to update
     * @param newUsername Updated username (null to skip)
     * @param newProfileImageUrl Updated profile image URL (null to skip)
     * @return Result of the operation
     */
    suspend fun updateFriendProfileData(
        userId: String,
        friendUserId: String,
        newUsername: String?,
        newProfileImageUrl: String?
    ): RequestState<Unit>
    
    // ==================== OneSignal Operations ====================
    
    /**
     * Update the user's OneSignal player ID for push notifications.
     * @param userId The user's ID
     * @param playerId The OneSignal player ID
     * @return Result of the operation
     */
    suspend fun updateOneSignalPlayerId(userId: String, playerId: String): RequestState<Unit>
    
    /**
     * Get OneSignal player IDs for multiple users.
     * Used for sending push notifications.
     * @param userIds List of user IDs
     * @return Map of userId to playerId (null if not set)
     */
    suspend fun getUserPlayerIds(userIds: List<String>): Map<String, String?>
    
    // ==================== Cleanup Operations ====================
    
    /**
     * Clean up old declined requests to prevent data accumulation.
     * Deletes declined requests older than 30 days.
     * @param userId The user whose declined requests to clean up
     * @param maxDeletes Maximum number of requests to delete per session (default 10)
     */
    suspend fun cleanupOldDeclinedRequests(userId: String, maxDeletes: Int = 10)
}
