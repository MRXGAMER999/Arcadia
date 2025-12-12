package com.example.arcadia.data.repository

import android.util.Log
import com.example.arcadia.data.datasource.FriendsRemoteDataSource
import com.example.arcadia.data.mapper.FriendMapper
import com.example.arcadia.data.remote.OneSignalNotificationService
import com.example.arcadia.domain.model.friend.Friend
import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.domain.model.friend.FriendRequestStatus
import com.example.arcadia.domain.model.friend.FriendshipStatus
import com.example.arcadia.domain.model.friend.RequestValidation
import com.example.arcadia.domain.model.friend.UserSearchResult
import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Refactored FriendsRepository implementation.
 * Delegates data operations to FriendsRemoteDataSource and mapping to FriendMapper.
 */
class FriendsRepositoryImpl(
    private val remoteDataSource: FriendsRemoteDataSource,
    private val notificationService: OneSignalNotificationService
) : FriendsRepository {

    companion object {
        private const val TAG = "FriendsRepositoryImpl"
        private const val DAILY_REQUEST_LIMIT = 20
        private const val DECLINE_COOLDOWN_HOURS = 24
        private const val FRIENDS_LIMIT = 500
        private const val PENDING_LIMIT = 100
    }

    // ==================== Friends List Operations ====================

    override fun getFriendsRealtime(userId: String): Flow<RequestState<List<Friend>>> {
        return remoteDataSource.observeFriends(userId)
            .map { friendshipRows ->
                // Extract friend user IDs and addedAt timestamps
                val friendships = friendshipRows.mapNotNull { row ->
                    val friendUserId = row.data["friendUserId"] as? String
                    val addedAt = FriendMapper.parseTimestamp(row.data["addedAt"])
                    if (friendUserId != null) friendUserId to addedAt else null
                }
                friendships
            }
            .flatMapLatest { friendships ->
                val friendIds = friendships.map { it.first }
                // Always use observeUsersByIds to maintain persistent subscription
                remoteDataSource.observeUsersByIds(friendIds)
                    .map { userRows ->
                        if (friendships.isEmpty()) {
                            emptyList()
                        } else {
                            val userMap = userRows.associate { 
                                it.id to (it.data["username"] as? String to it.data["profileImageUrl"] as? String)
                            }
                            // Build Friend objects with fresh user data
                            friendships.mapNotNull { (friendId, addedAt) ->
                                val (username, profileImageUrl) = userMap[friendId] ?: return@mapNotNull null
                                Friend(
                                    userId = friendId,
                                    username = username ?: "",
                                    profileImageUrl = profileImageUrl,
                                    addedAt = addedAt
                                )
                            }.sortedBy { it.username.lowercase() }
                        }
                    }
            }
            .map { friends -> RequestState.Success(friends) as RequestState<List<Friend>> }
            .onStart { emit(RequestState.Loading) }
            .catch { e ->
                Log.e(TAG, "Error getting friends: ${e.message}", e)
                emit(RequestState.Error("Failed to load friends: ${e.message}"))
            }
    }

    override suspend fun removeFriend(currentUserId: String, friendUserId: String): RequestState<Unit> {
        return try {
            remoteDataSource.removeFriend(currentUserId, friendUserId)
            Log.d(TAG, "Successfully removed friend: $friendUserId from user: $currentUserId")
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing friend: ${e.message}", e)
            RequestState.Error("Failed to remove friend: ${e.message}")
        }
    }

    override suspend fun isFriend(userId: String, friendUserId: String): Boolean {
        return try {
            remoteDataSource.isFriend(userId, friendUserId)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking friendship: ${e.message}", e)
            false
        }
    }

    // ==================== Friend Request Operations ====================

    override suspend fun sendFriendRequest(
        fromUserId: String,
        fromUsername: String,
        fromProfileImageUrl: String?,
        toUserId: String,
        toUsername: String,
        toProfileImageUrl: String?
    ): RequestState<Unit> {
        return try {
            validateAndResetDailyLimit(fromUserId)
            
            // Check for existing pending request in either direction
            val existingOutgoing = remoteDataSource.getFriendRequest(fromUserId, toUserId)
            if (existingOutgoing != null) {
                return RequestState.Error("You already have a pending request to this user")
            }
            
            val existingIncoming = remoteDataSource.getFriendRequest(toUserId, fromUserId)
            if (existingIncoming != null) {
                return RequestState.Error("This user has already sent you a request. Check your incoming requests.")
            }
            
            // Get current count before sending to ensure we can update it
            val userDoc = remoteDataSource.getUser(fromUserId)
            val currentCount = (userDoc.data["friendRequestsSentToday"] as? Number)?.toInt() ?: 0
            
            // Send the friend request
            remoteDataSource.sendFriendRequest(
                fromUserId, fromUsername, fromProfileImageUrl,
                toUserId, toUsername, toProfileImageUrl
            )
            
            // Increment daily limit - this is critical for rate limiting
            try {
                remoteDataSource.updateUser(fromUserId, mapOf("friendRequestsSentToday" to (currentCount + 1)))
            } catch (e: Exception) {
                // Log as error since this affects rate limiting
                Log.e(TAG, "Failed to update daily request counter - rate limiting may be affected: ${e.message}", e)
            }
            
            // Notification - failure is acceptable, just log it
            try {
                val recipientDoc = remoteDataSource.getUser(toUserId)
                val recipientPlayerId = recipientDoc.data["oneSignalPlayerId"] as? String
                if (!recipientPlayerId.isNullOrBlank()) {
                    notificationService.sendFriendRequestNotification(recipientPlayerId, fromUsername)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send notification (non-critical): ${e.message}")
            }
            
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request: ${e.message}", e)
            RequestState.Error("Failed to send friend request: ${e.message}")
        }
    }

    override suspend fun acceptFriendRequest(request: FriendRequest): RequestState<Unit> {
        return try {
            remoteDataSource.acceptFriendRequest(
                request.id,
                request.fromUserId,
                request.toUserId,
                request.toUsername,
                request.toProfileImageUrl,
                request.fromUsername,
                request.fromProfileImageUrl
            )
            
            // Notification
            try {
                val senderDoc = remoteDataSource.getUser(request.fromUserId)
                val senderPlayerId = senderDoc.data["oneSignalPlayerId"] as? String
                if (!senderPlayerId.isNullOrBlank()) {
                    notificationService.sendFriendAcceptedNotification(senderPlayerId, request.toUsername)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send notification: ${e.message}")
            }
            
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request: ${e.message}", e)
            RequestState.Error("Failed to accept friend request: ${e.message}")
        }
    }

    override suspend fun declineFriendRequest(requestId: String): RequestState<Unit> {
        return try {
            remoteDataSource.declineFriendRequest(requestId)
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error declining friend request: ${e.message}", e)
            RequestState.Error("Failed to decline friend request: ${e.message}")
        }
    }

    override suspend fun cancelFriendRequest(requestId: String): RequestState<Unit> {
        return try {
            remoteDataSource.cancelFriendRequest(requestId)
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling friend request: ${e.message}", e)
            RequestState.Error("Failed to cancel friend request: ${e.message}")
        }
    }

    private suspend fun validateAndResetDailyLimit(userId: String) {
        try {
            val userDoc = remoteDataSource.getUser(userId)
            val lastResetDate = userDoc.data["friendRequestsLastResetDate"] as? String
            val todayDate = getCurrentDateUTC()
            
            if (lastResetDate != todayDate) {
                remoteDataSource.updateUser(
                    userId,
                    mapOf(
                        "friendRequestsSentToday" to 0,
                        "friendRequestsLastResetDate" to todayDate
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating daily limit: ${e.message}", e)
        }
    }

    private fun getCurrentDateUTC(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date())
    }

    // ==================== Request Queries and Listeners ====================

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getIncomingRequestsRealtime(userId: String): Flow<RequestState<List<FriendRequest>>> {
        return remoteDataSource.observeIncomingFriendRequests(userId)
            .map { requestRows ->
                // Extract request data and fromUserIds
                val requests = requestRows.mapNotNull { FriendMapper.toFriendRequest(it) }
                requests
            }
            .flatMapLatest { requests ->
                val fromUserIds = requests.map { it.fromUserId }.distinct()
                // Always use observeUsersByIds to maintain persistent subscription
                remoteDataSource.observeUsersByIds(fromUserIds)
                    .map { userRows ->
                        if (requests.isEmpty()) {
                            emptyList()
                        } else {
                            val userMap = userRows.associate { 
                                it.id to (it.data["username"] as? String to it.data["profileImageUrl"] as? String)
                            }
                            // Enrich requests with fresh user data
                            requests.map { request ->
                                val userInfo = userMap[request.fromUserId]
                                if (userInfo != null) {
                                    val (username, profileImageUrl) = userInfo
                                    if (username != null) {
                                        request.copy(
                                            fromUsername = username,
                                            fromProfileImageUrl = profileImageUrl
                                        )
                                    } else {
                                        request // Fallback to stored data
                                    }
                                } else {
                                    request // Fallback to stored data
                                }
                            }
                        }
                    }
            }
            .map { requests -> RequestState.Success(requests) as RequestState<List<FriendRequest>> }
            .onStart { emit(RequestState.Loading) }
            .catch { e ->
                Log.e(TAG, "Error getting incoming requests: ${e.message}", e)
                emit(RequestState.Error("Failed to load incoming requests: ${e.message}"))
            }
    }

    override fun getOutgoingRequestsRealtime(userId: String): Flow<RequestState<List<FriendRequest>>> {
        return remoteDataSource.observeOutgoingFriendRequests(userId)
            .map { requestRows ->
                // Extract request data and toUserIds
                val requests = requestRows.mapNotNull { FriendMapper.toFriendRequest(it) }
                requests
            }
            .flatMapLatest { requests ->
                val toUserIds = requests.map { it.toUserId }.distinct()
                // Always use observeUsersByIds to maintain persistent subscription
                remoteDataSource.observeUsersByIds(toUserIds)
                    .map { userRows ->
                        if (requests.isEmpty()) {
                            emptyList()
                        } else {
                            val userMap = userRows.associate { 
                                it.id to (it.data["username"] as? String to it.data["profileImageUrl"] as? String)
                            }
                            // Enrich requests with fresh user data
                            requests.map { request ->
                                val userInfo = userMap[request.toUserId]
                                if (userInfo != null) {
                                    val (username, profileImageUrl) = userInfo
                                    if (username != null) {
                                        request.copy(
                                            toUsername = username,
                                            toProfileImageUrl = profileImageUrl
                                        )
                                    } else {
                                        request // Fallback to stored data
                                    }
                                } else {
                                    request // Fallback to stored data
                                }
                            }
                        }
                    }
            }
            .map { requests -> RequestState.Success(requests) as RequestState<List<FriendRequest>> }
            .onStart { emit(RequestState.Loading) }
            .catch { e ->
                Log.e(TAG, "Error getting outgoing requests: ${e.message}", e)
                emit(RequestState.Error("Failed to load outgoing requests: ${e.message}"))
            }
    }

    // ==================== Search ====================

    override suspend fun searchUsers(query: String, currentUserId: String): RequestState<List<UserSearchResult>> {
        return try {
            val rows = remoteDataSource.searchUsers(query, currentUserId)
            val results = rows.mapNotNull { FriendMapper.toUserSearchResult(it) }
                .filter { it.userId != currentUserId } // Filter out self
            RequestState.Success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users: ${e.message}", e)
            RequestState.Error("Failed to search users: ${e.message}")
        }
    }

    // ==================== Additional Methods ====================

    override fun getPendingRequestCountRealtime(userId: String): Flow<RequestState<Int>> {
        return remoteDataSource.observePendingRequestCount(userId)
            .map { RequestState.Success(it) as RequestState<Int> }
            .onStart { emit(RequestState.Loading) }
            .catch { e ->
                Log.e(TAG, "Error fetching pending request count: ${e.message}", e)
                emit(RequestState.Error("Failed to load pending requests: ${e.message}"))
            }
    }

    override fun getFriendshipStatusRealtime(
        currentUserId: String,
        targetUserId: String
    ): Flow<FriendshipStatus> {
        val friendsFlow = remoteDataSource.observeFriends(currentUserId)
            .map { rows -> rows.mapNotNull { FriendMapper.toFriend(it) } }
            .catch { e ->
                Log.w(TAG, "Error observing friends for status: ${e.message}")
                emit(emptyList())
            }

        val outgoingFlow = remoteDataSource.observeOutgoingFriendRequests(currentUserId)
            .map { rows -> rows.mapNotNull { FriendMapper.toFriendRequest(it) } }
            .catch { e ->
                Log.w(TAG, "Error observing outgoing requests for status: ${e.message}")
                emit(emptyList())
            }

        val incomingFlow = remoteDataSource.observeIncomingFriendRequests(currentUserId)
            .map { rows -> rows.mapNotNull { FriendMapper.toFriendRequest(it) } }
            .catch { e ->
                Log.w(TAG, "Error observing incoming requests for status: ${e.message}")
                emit(emptyList())
            }

        return combine(friendsFlow, outgoingFlow, incomingFlow) { friends, outgoing, incoming ->
            when {
                friends.any { it.userId == targetUserId } -> FriendshipStatus.FRIENDS
                outgoing.any { it.toUserId == targetUserId && it.status == FriendRequestStatus.PENDING } ->
                    FriendshipStatus.REQUEST_SENT
                incoming.any { it.fromUserId == targetUserId && it.status == FriendRequestStatus.PENDING } ->
                    FriendshipStatus.REQUEST_RECEIVED
                else -> FriendshipStatus.NOT_FRIENDS
            }
        }
    }

    override suspend fun checkReciprocalRequest(currentUserId: String, targetUserId: String): FriendRequest? {
        val row = remoteDataSource.getFriendRequest(targetUserId, currentUserId)
        val request = row?.let { FriendMapper.toFriendRequest(it) }
        // Only return if status is actually PENDING
        return if (request?.status == FriendRequestStatus.PENDING) request else null
    }

    override suspend fun canSendRequest(userId: String): RequestValidation {
        return try {
            // Reset daily counters if day changed (matches original behavior)
            validateAndResetDailyLimit(userId)

            val userDoc = remoteDataSource.getUser(userId)
            val sentToday = (userDoc.data["friendRequestsSentToday"] as? Number)?.toInt() ?: 0

            // Get current friends count to check if limit is reached
            val friendsCount = try {
                remoteDataSource.observeFriends(userId)
                    .map { rows -> rows.size }
                    .first()
            } catch (e: Exception) {
                Log.w(TAG, "canSendRequest friend count fallback: ${e.message}")
                0
            }

            val outgoingPendingCount = try {
                remoteDataSource.observeOutgoingFriendRequests(userId)
                    .map { rows -> rows.mapNotNull { FriendMapper.toFriendRequest(it) } }
                    .first()
                    .count { it.status == FriendRequestStatus.PENDING }
            } catch (e: Exception) {
                Log.w(TAG, "canSendRequest pending count fallback: ${e.message}")
                0
            }

            val dailyLimitReached = sentToday >= DAILY_REQUEST_LIMIT
            val friendsLimitReached = friendsCount >= FRIENDS_LIMIT
            val pendingLimitReached = outgoingPendingCount >= PENDING_LIMIT
            val canSend = !(dailyLimitReached || friendsLimitReached || pendingLimitReached)

            RequestValidation(
                canSend = canSend,
                dailyLimitReached = dailyLimitReached,
                pendingLimitReached = pendingLimitReached,
                friendsLimitReached = friendsLimitReached
            )
        } catch (e: Exception) {
            Log.w(TAG, "canSendRequest fallback to allow: ${e.message}")
            RequestValidation(canSend = true)
        }
    }

    override suspend fun getDeclinedRequestCooldown(fromUserId: String, toUserId: String): Int? {
        val declined = remoteDataSource.getDeclinedRequests(toUserId, fromUserId)
        val lastDeclined = declined.maxByOrNull { (it.data["updatedAt"] as? Number)?.toLong() ?: 0L }
        
        if (lastDeclined != null) {
            val updatedAt = (lastDeclined.data["updatedAt"] as? Number)?.toLong()
            // Guard against null or invalid timestamps
            if (updatedAt == null || updatedAt == 0L) {
                Log.w(TAG, "Invalid updatedAt timestamp for declined request")
                return null
            }
            
            val diff = System.currentTimeMillis() - updatedAt
            // Guard against negative diff (clock skew or future timestamp)
            if (diff < 0) {
                Log.w(TAG, "Negative time difference for declined request cooldown")
                return null
            }
            
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            if (hours < DECLINE_COOLDOWN_HOURS) {
                return (DECLINE_COOLDOWN_HOURS - hours).toInt()
            }
        }
        return null
    }

    override suspend fun updateFriendProfileData(userId: String, friendUserId: String, newUsername: String?, newProfileImageUrl: String?): RequestState<Unit> {
        return try {
            remoteDataSource.updateFriendshipProfile(userId, friendUserId, newUsername, newProfileImageUrl)
            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Error updating friend profile")
        }
    }

    override suspend fun updateOneSignalPlayerId(userId: String, playerId: String): RequestState<Unit> {
        return try {
            remoteDataSource.updateUser(userId, mapOf("oneSignalPlayerId" to playerId))
            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Error updating player ID")
        }
    }

    override suspend fun getUserPlayerIds(userIds: List<String>): Map<String, String?> {
        val users = remoteDataSource.getUsers(userIds)
        return users.associate { it.id to (it.data["oneSignalPlayerId"] as? String) }
    }

    override suspend fun cleanupOldDeclinedRequests(userId: String, maxDeletes: Int) {
        val olderThan = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        remoteDataSource.deleteOldDeclinedRequests(userId, olderThan, maxDeletes)
    }
}
