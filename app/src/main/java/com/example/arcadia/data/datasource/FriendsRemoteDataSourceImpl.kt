package com.example.arcadia.data.datasource

import android.util.Log
import com.example.arcadia.BuildConfig
import com.example.arcadia.domain.model.friend.FriendRequestStatus
import com.example.arcadia.util.AppwriteConstants
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.models.Row
import io.appwrite.services.Realtime
import io.appwrite.services.TablesDB
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FriendsRemoteDataSourceImpl(
    tablesDbLazy: Lazy<TablesDB>,
    realtimeLazy: Lazy<Realtime>
) : FriendsRemoteDataSource {

    private val tablesDb by tablesDbLazy
    private val realtime by realtimeLazy

    companion object {
        private const val TAG = "FriendsRemoteDataSource"
        private val DATABASE_ID = BuildConfig.APPWRITE_DATABASE_ID
        private val USERS_COLLECTION_ID = AppwriteConstants.USERS_COLLECTION_ID
        private val FRIEND_REQUESTS_COLLECTION_ID = AppwriteConstants.FRIEND_REQUESTS_COLLECTION_ID
        private val FRIENDSHIPS_COLLECTION_ID = AppwriteConstants.FRIENDSHIPS_COLLECTION_ID
        
        private const val FRIENDS_LIMIT = 500
    }

    override fun observeFriends(userId: String): Flow<List<Row<Map<String, Any>>>> = callbackFlow {
        // Initial fetch
        try {
            val initialDocs = fetchFriends(userId)
            val result = trySend(initialDocs)
            if (result.isFailure) {
                Log.w(TAG, "Failed to send initial friends data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching initial friends: ${e.message}", e)
            // Don't close, let realtime try
        }

        val subscription = realtime.subscribe(
            "databases.$DATABASE_ID.tables.$FRIENDSHIPS_COLLECTION_ID.rows"
        ) {
            launch {
                try {
                    val docs = fetchFriends(userId)
                    val result = trySend(docs)
                    if (result.isFailure) {
                        Log.w(TAG, "Failed to send updated friends data, buffer may be full")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing friends: ${e.message}", e)
                }
            }
        }

        awaitClose { subscription.close() }
    }.buffer(Channel.CONFLATED)

    private suspend fun fetchFriends(userId: String): List<Row<Map<String, Any>>> {
        val docs = tablesDb.listRows(
            databaseId = DATABASE_ID,
            tableId = FRIENDSHIPS_COLLECTION_ID,
            queries = listOf(
                Query.equal("userId", userId),
                Query.orderAsc("friendUsername"),
                Query.limit(FRIENDS_LIMIT)
            )
        )
        return docs.rows
    }

    override suspend fun removeFriend(currentUserId: String, friendUserId: String) {
        // Delete from current user
        val currentUserDocs = tablesDb.listRows(
            databaseId = DATABASE_ID,
            tableId = FRIENDSHIPS_COLLECTION_ID,
            queries = listOf(
                Query.equal("userId", currentUserId),
                Query.equal("friendUserId", friendUserId)
            )
        )
        currentUserDocs.rows.forEach { doc ->
            tablesDb.deleteRow(DATABASE_ID, FRIENDSHIPS_COLLECTION_ID, doc.id)
        }

        // Delete from friend
        val friendUserDocs = tablesDb.listRows(
            databaseId = DATABASE_ID,
            tableId = FRIENDSHIPS_COLLECTION_ID,
            queries = listOf(
                Query.equal("userId", friendUserId),
                Query.equal("friendUserId", currentUserId)
            )
        )
        friendUserDocs.rows.forEach { doc ->
            tablesDb.deleteRow(DATABASE_ID, FRIENDSHIPS_COLLECTION_ID, doc.id)
        }
    }

    override suspend fun isFriend(userId: String, friendUserId: String): Boolean {
        val docs = tablesDb.listRows(
            databaseId = DATABASE_ID,
            tableId = FRIENDSHIPS_COLLECTION_ID,
            queries = listOf(
                Query.equal("userId", userId),
                Query.equal("friendUserId", friendUserId),
                Query.limit(1)
            )
        )
        return docs.rows.isNotEmpty()
    }

    override fun observeIncomingFriendRequests(userId: String): Flow<List<Row<Map<String, Any>>>> = callbackFlow {
        try {
            val result = trySend(fetchIncomingRequests(userId))
            if (result.isFailure) {
                Log.w(TAG, "Failed to send initial incoming requests")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching initial incoming requests: ${e.message}", e)
        }

        val subscription = realtime.subscribe(
            "databases.$DATABASE_ID.tables.$FRIEND_REQUESTS_COLLECTION_ID.rows"
        ) {
            launch {
                try {
                    val result = trySend(fetchIncomingRequests(userId))
                    if (result.isFailure) {
                        Log.w(TAG, "Failed to send updated incoming requests, buffer may be full")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing incoming requests: ${e.message}", e)
                }
            }
        }
        awaitClose { subscription.close() }
    }.buffer(Channel.CONFLATED)

    private suspend fun fetchIncomingRequests(userId: String): List<Row<Map<String, Any>>> {
        return tablesDb.listRows(
            databaseId = DATABASE_ID,
            tableId = FRIEND_REQUESTS_COLLECTION_ID,
            queries = listOf(
                Query.equal("toUserId", userId),
                Query.equal("status", FriendRequestStatus.PENDING.name.lowercase()),
                Query.orderDesc("createdAt")
            )
        ).rows
    }

    override fun observeOutgoingFriendRequests(userId: String): Flow<List<Row<Map<String, Any>>>> = callbackFlow {
        try {
            val result = trySend(fetchOutgoingRequests(userId))
            if (result.isFailure) {
                Log.w(TAG, "Failed to send initial outgoing requests")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching initial outgoing requests: ${e.message}", e)
        }

        val subscription = realtime.subscribe(
            "databases.$DATABASE_ID.tables.$FRIEND_REQUESTS_COLLECTION_ID.rows"
        ) {
            launch {
                try {
                    val result = trySend(fetchOutgoingRequests(userId))
                    if (result.isFailure) {
                        Log.w(TAG, "Failed to send updated outgoing requests, buffer may be full")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing outgoing requests: ${e.message}", e)
                }
            }
        }
        awaitClose { subscription.close() }
    }.buffer(Channel.CONFLATED)

    private suspend fun fetchOutgoingRequests(userId: String): List<Row<Map<String, Any>>> {
        return tablesDb.listRows(
            databaseId = DATABASE_ID,
            tableId = FRIEND_REQUESTS_COLLECTION_ID,
            queries = listOf(
                Query.equal("fromUserId", userId),
                Query.equal("status", FriendRequestStatus.PENDING.name.lowercase()),
                Query.orderDesc("createdAt")
            )
        ).rows
    }

    override suspend fun sendFriendRequest(
        fromUserId: String,
        fromUsername: String,
        fromProfileImageUrl: String?,
        toUserId: String,
        toUsername: String,
        toProfileImageUrl: String?
    ) {
        val currentTime = System.currentTimeMillis()
        val requestData = mapOf(
            "fromUserId" to fromUserId,
            "fromUsername" to fromUsername,
            "fromProfileImageUrl" to fromProfileImageUrl,
            "toUserId" to toUserId,
            "toUsername" to toUsername,
            "toProfileImageUrl" to toProfileImageUrl,
            "status" to FriendRequestStatus.PENDING.name.lowercase(),
            "createdAt" to currentTime,
            "updatedAt" to currentTime
        )
        tablesDb.createRow(
            databaseId = DATABASE_ID,
            tableId = FRIEND_REQUESTS_COLLECTION_ID,
            rowId = ID.unique(),
            data = requestData
        )
    }

    override suspend fun acceptFriendRequest(
        requestId: String,
        fromUserId: String,
        toUserId: String,
        toUsername: String,
        toProfileImageUrl: String?,
        fromUsername: String,
        fromProfileImageUrl: String?
    ) {
        val currentTime = System.currentTimeMillis()
        val currentTimeIso = toIso8601(currentTime)

        tablesDb.updateRow(
            databaseId = DATABASE_ID,
            tableId = FRIEND_REQUESTS_COLLECTION_ID,
            rowId = requestId,
            data = mapOf(
                "status" to FriendRequestStatus.ACCEPTED.name.lowercase(),
                "updatedAt" to currentTime
            )
        )

        tablesDb.createRow(
            databaseId = DATABASE_ID,
            tableId = FRIENDSHIPS_COLLECTION_ID,
            rowId = ID.unique(),
            data = mapOf(
                "userId" to fromUserId,
                "friendUserId" to toUserId,
                "friendUsername" to toUsername,
                "friendProfileImageUrl" to toProfileImageUrl,
                "addedAt" to currentTimeIso
            )
        )

        tablesDb.createRow(
            databaseId = DATABASE_ID,
            tableId = FRIENDSHIPS_COLLECTION_ID,
            rowId = ID.unique(),
            data = mapOf(
                "userId" to toUserId,
                "friendUserId" to fromUserId,
                "friendUsername" to fromUsername,
                "friendProfileImageUrl" to fromProfileImageUrl,
                "addedAt" to currentTimeIso
            )
        )
    }

    override suspend fun declineFriendRequest(requestId: String) {
        tablesDb.updateRow(
            databaseId = DATABASE_ID,
            tableId = FRIEND_REQUESTS_COLLECTION_ID,
            rowId = requestId,
            data = mapOf(
                "status" to FriendRequestStatus.DECLINED.name.lowercase(),
                "updatedAt" to System.currentTimeMillis()
            )
        )
    }

    override suspend fun cancelFriendRequest(requestId: String) {
        tablesDb.deleteRow(
            databaseId = DATABASE_ID,
            tableId = FRIEND_REQUESTS_COLLECTION_ID,
            rowId = requestId
        )
    }

    override suspend fun searchUsers(query: String, currentUserId: String): List<Row<Map<String, Any>>> {
        return tablesDb.listRows(
            databaseId = DATABASE_ID,
            tableId = USERS_COLLECTION_ID,
            queries = listOf(
                Query.search("username", query),
                Query.limit(20)
            )
        ).rows
    }

    override suspend fun getUser(userId: String): Row<Map<String, Any>> {
        return tablesDb.getRow(
            databaseId = DATABASE_ID,
            tableId = USERS_COLLECTION_ID,
            rowId = userId
        )
    }

    override suspend fun updateUser(userId: String, data: Map<String, Any>) {
        tablesDb.updateRow(
            databaseId = DATABASE_ID,
            tableId = USERS_COLLECTION_ID,
            rowId = userId,
            data = data
        )
    }

    override suspend fun getUsers(userIds: List<String>): List<Row<Map<String, Any>>> {
        if (userIds.isEmpty()) return emptyList()
        return tablesDb.listRows(
            databaseId = DATABASE_ID,
            tableId = USERS_COLLECTION_ID,
            queries = listOf(
                Query.equal("\$id", userIds),
                Query.limit(500)
            )
        ).rows
    }

    override fun observeUsersByIds(userIds: List<String>): Flow<List<Row<Map<String, Any>>>> = callbackFlow {
        if (userIds.isEmpty()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        // Initial fetch
        try {
            val initialUsers = getUsers(userIds)
            val result = trySend(initialUsers)
            if (result.isFailure) {
                Log.w(TAG, "Failed to send initial users data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching initial users: ${e.message}", e)
            trySend(emptyList())
        }

        // Subscribe to users table changes
        val subscription = realtime.subscribe(
            "databases.$DATABASE_ID.tables.$USERS_COLLECTION_ID.rows"
        ) {
            launch {
                try {
                    val users = getUsers(userIds)
                    val result = trySend(users)
                    if (result.isFailure) {
                        Log.w(TAG, "Failed to send updated users data, buffer may be full")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing users: ${e.message}", e)
                }
            }
        }

        awaitClose { subscription.close() }
    }.buffer(Channel.CONFLATED)

    override fun observePendingRequestCount(userId: String): Flow<Int> = callbackFlow {
        val query = listOf(
            Query.equal("toUserId", userId),
            Query.equal("status", FriendRequestStatus.PENDING.name.lowercase())
        )
        
        try {
            val count = tablesDb.listRows(DATABASE_ID, FRIEND_REQUESTS_COLLECTION_ID, query).total.toInt()
            val result = trySend(count)
            if (result.isFailure) {
                Log.w(TAG, "Failed to send initial pending count")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching pending count: ${e.message}", e)
        }

        val subscription = realtime.subscribe(
            "databases.$DATABASE_ID.tables.$FRIEND_REQUESTS_COLLECTION_ID.rows"
        ) {
            launch {
                try {
                    val count = tablesDb.listRows(DATABASE_ID, FRIEND_REQUESTS_COLLECTION_ID, query).total.toInt()
                    val result = trySend(count)
                    if (result.isFailure) {
                        Log.w(TAG, "Failed to send updated pending count, buffer may be full")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing pending count: ${e.message}", e)
                }
            }
        }
        awaitClose { subscription.close() }
    }.buffer(Channel.CONFLATED)

    override suspend fun getFriendship(userId: String, friendUserId: String): Row<Map<String, Any>>? {
        val docs = tablesDb.listRows(
            databaseId = DATABASE_ID,
            tableId = FRIENDSHIPS_COLLECTION_ID,
            queries = listOf(
                Query.equal("userId", userId),
                Query.equal("friendUserId", friendUserId),
                Query.limit(1)
            )
        )
        return docs.rows.firstOrNull()
    }

    override suspend fun getFriendRequest(fromUserId: String, toUserId: String): Row<Map<String, Any>>? {
        val docs = tablesDb.listRows(
            databaseId = DATABASE_ID,
            tableId = FRIEND_REQUESTS_COLLECTION_ID,
            queries = listOf(
                Query.equal("fromUserId", fromUserId),
                Query.equal("toUserId", toUserId),
                Query.equal("status", FriendRequestStatus.PENDING.name.lowercase()),
                Query.orderDesc("createdAt"),
                Query.limit(1)
            )
        )
        return docs.rows.firstOrNull()
    }

    override suspend fun getDeclinedRequests(toUserId: String, fromUserId: String): List<Row<Map<String, Any>>> {
        return tablesDb.listRows(
            databaseId = DATABASE_ID,
            tableId = FRIEND_REQUESTS_COLLECTION_ID,
            queries = listOf(
                Query.equal("toUserId", toUserId),
                Query.equal("fromUserId", fromUserId),
                Query.equal("status", FriendRequestStatus.DECLINED.name.lowercase())
            )
        ).rows
    }

    override suspend fun updateFriendshipProfile(userId: String, friendUserId: String, username: String?, profileImageUrl: String?) {
        val friendship = getFriendship(userId, friendUserId) ?: return
        val updates = mutableMapOf<String, Any?>()
        if (username != null) updates["friendUsername"] = username
        if (profileImageUrl != null) updates["friendProfileImageUrl"] = profileImageUrl
        
        if (updates.isNotEmpty()) {
            tablesDb.updateRow(
                databaseId = DATABASE_ID,
                tableId = FRIENDSHIPS_COLLECTION_ID,
                rowId = friendship.id,
                data = updates
            )
        }
    }

    override suspend fun deleteOldDeclinedRequests(userId: String, olderThan: Long, maxDeletes: Int) {
        val docs = tablesDb.listRows(
            databaseId = DATABASE_ID,
            tableId = FRIEND_REQUESTS_COLLECTION_ID,
            queries = listOf(
                Query.equal("toUserId", userId),
                Query.equal("status", FriendRequestStatus.DECLINED.name.lowercase()),
                Query.lessThan("updatedAt", olderThan),
                Query.limit(maxDeletes)
            )
        )
        docs.rows.forEach { doc ->
            tablesDb.deleteRow(DATABASE_ID, FRIEND_REQUESTS_COLLECTION_ID, doc.id)
        }
    }

    private fun toIso8601(timestamp: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(timestamp))
    }
}
