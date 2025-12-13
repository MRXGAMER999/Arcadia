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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * FriendsRepository implementation.
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
            .map { rows ->
                val friends = rows.mapNotNull { FriendMapper.toFriend(it) }
                    .sortedBy { it.username.lowercase() }
                RequestState.Success(friends) as RequestState<List<Friend>>
            }
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
            
            // Get current count before sending to ensure we can update it
            val userDoc = remoteDataSource.getUser(fromUserId)
            val currentCount = (userDoc.data["friendRequestsSentToday"] as? Number)?.toInt() ?: 0
            
            // Send the friend request and get the request ID
            val requestId = remoteDataSource.sendFriendRequest(
                fromUserId, fromUsername, fromProfileImageUrl,
                toUserId, toUsername, toProfileImageUrl
            )
            
            // Increment daily limit
            try {
                remoteDataSource.updateUser(fromUserId, mapOf("friendRequestsSentToday" to (currentCount + 1)))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update daily request counter: ${e.message}", e)
            }
            
            // Send notification with requestId for action buttons
            try {
                val recipientDoc = remoteDataSource.getUser(toUserId)
                val recipientPlayerId = recipientDoc.data["oneSignalPlayerId"] as? String
                if (!recipientPlayerId.isNullOrBlank()) {
                    notificationService.sendFriendRequestNotification(
                        recipientPlayerId = recipientPlayerId,
                        senderUsername = fromUsername,
                        requestId = requestId,
                        fromUserId = fromUserId
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send notification: ${e.message}")
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
            
            // Send notification
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

    override fun getIncomingRequestsRealtime(userId: String): Flow<RequestState<List<FriendRequest>>> {
        return remoteDataSource.observeIncomingFriendRequests(userId)
            .map { rows ->
                val requests = rows.mapNotNull { FriendMapper.toFriendRequest(it) }
                RequestState.Success(requests) as RequestState<List<FriendRequest>>
            }
            .onStart { emit(RequestState.Loading) }
            .catch { e ->
                Log.e(TAG, "Error getting incoming requests: ${e.message}", e)
                emit(RequestState.Error("Failed to load incoming requests: ${e.message}"))
            }
    }

    override fun getOutgoingRequestsRealtime(userId: String): Flow<RequestState<List<FriendRequest>>> {
        return remoteDataSource.observeOutgoingFriendRequests(userId)
            .map { rows ->
                val requests = rows.mapNotNull { FriendMapper.toFriendRequest(it) }
                RequestState.Success(requests) as RequestState<List<FriendRequest>>
            }
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
                .filter { it.userId != currentUserId }
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
        return if (request?.status == FriendRequestStatus.PENDING) request else null
    }

    override suspend fun canSendRequest(userId: String): RequestValidation {
        return try {
            validateAndResetDailyLimit(userId)
            
            val userDoc = remoteDataSource.getUser(userId)
            val dailySent = (userDoc.data["friendRequestsSentToday"] as? Number)?.toInt() ?: 0
            
            // Get counts from flows (first emission)
            val friendsCount = remoteDataSource.observeFriends(userId).first().size
            val pendingCount = remoteDataSource.observeOutgoingFriendRequests(userId).first().size
            
            RequestValidation(
                canSend = dailySent < DAILY_REQUEST_LIMIT && 
                         pendingCount < PENDING_LIMIT && 
                         friendsCount < FRIENDS_LIMIT,
                dailyLimitReached = dailySent >= DAILY_REQUEST_LIMIT,
                pendingLimitReached = pendingCount >= PENDING_LIMIT,
                friendsLimitReached = friendsCount >= FRIENDS_LIMIT
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking request validation: ${e.message}", e)
            RequestValidation(canSend = false)
        }
    }

    override suspend fun getDeclinedRequestCooldown(fromUserId: String, toUserId: String): Int? {
        return try {
            val declinedRequests = remoteDataSource.getDeclinedRequests(toUserId, fromUserId)
            if (declinedRequests.isEmpty()) return null
            
            val latestDeclined = declinedRequests.maxByOrNull { 
                (it.data["updatedAt"] as? String)?.let { date ->
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(date)?.time
                } ?: 0L
            } ?: return null
            
            val declinedTime = (latestDeclined.data["updatedAt"] as? String)?.let { date ->
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(date)?.time
            } ?: return null
            
            val hoursSinceDecline = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - declinedTime)
            val remaining = DECLINE_COOLDOWN_HOURS - hoursSinceDecline.toInt()
            
            if (remaining > 0) remaining else null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking declined cooldown: ${e.message}", e)
            null
        }
    }

    override suspend fun updateFriendProfileData(
        userId: String,
        friendUserId: String,
        newUsername: String?,
        newProfileImageUrl: String?
    ): RequestState<Unit> {
        return try {
            remoteDataSource.updateFriendshipProfile(userId, friendUserId, newUsername, newProfileImageUrl)
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating friend profile data: ${e.message}", e)
            RequestState.Error("Failed to update friend profile: ${e.message}")
        }
    }

    override suspend fun updateOneSignalPlayerId(userId: String, playerId: String): RequestState<Unit> {
        return try {
            remoteDataSource.updateUser(userId, mapOf("oneSignalPlayerId" to playerId))
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating OneSignal player ID: ${e.message}", e)
            RequestState.Error("Failed to update player ID: ${e.message}")
        }
    }

    override suspend fun getUserPlayerIds(userIds: List<String>): Map<String, String?> {
        return try {
            if (userIds.isEmpty()) return emptyMap()
            val users = remoteDataSource.getUsers(userIds)
            users.associate { row ->
                val id = row.data["\$id"] as? String ?: ""
                val playerId = row.data["oneSignalPlayerId"] as? String
                id to playerId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user player IDs: ${e.message}", e)
            emptyMap()
        }
    }

    override suspend fun cleanupOldDeclinedRequests(userId: String, maxDeletes: Int) {
        try {
            val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
            remoteDataSource.deleteOldDeclinedRequests(userId, thirtyDaysAgo, maxDeletes)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old declined requests: ${e.message}", e)
        }
    }
}
