package com.example.arcadia.data.datasource

import io.appwrite.models.Row
import kotlinx.coroutines.flow.Flow

interface FriendsRemoteDataSource {
    // Friends
    fun observeFriends(userId: String): Flow<List<Row<Map<String, Any>>>>
    suspend fun getFriends(userId: String, limit: Int, cursor: String? = null): List<Row<Map<String, Any>>>
    suspend fun removeFriend(currentUserId: String, friendUserId: String)
    suspend fun isFriend(userId: String, friendUserId: String): Boolean
    
    // Friend Requests
    fun observeIncomingFriendRequests(userId: String): Flow<List<Row<Map<String, Any>>>>
    fun observeOutgoingFriendRequests(userId: String): Flow<List<Row<Map<String, Any>>>>
    suspend fun sendFriendRequest(
        fromUserId: String,
        fromUsername: String,
        fromProfileImageUrl: String?,
        toUserId: String,
        toUsername: String,
        toProfileImageUrl: String?
    )
    suspend fun acceptFriendRequest(requestId: String, fromUserId: String, toUserId: String, toUsername: String, toProfileImageUrl: String?, fromUsername: String, fromProfileImageUrl: String?)
    suspend fun declineFriendRequest(requestId: String)
    suspend fun cancelFriendRequest(requestId: String)
    
    // Search & User
    suspend fun searchUsers(query: String, currentUserId: String): List<Row<Map<String, Any>>>
    suspend fun getUser(userId: String): Row<Map<String, Any>>
    suspend fun getUsers(userIds: List<String>): List<Row<Map<String, Any>>>
    suspend fun updateUser(userId: String, data: Map<String, Any>)
    
    // Additional methods for missing repository features
    fun observePendingRequestCount(userId: String): Flow<Int>
    suspend fun getFriendship(userId: String, friendUserId: String): Row<Map<String, Any>>?
    suspend fun getFriendRequest(fromUserId: String, toUserId: String): Row<Map<String, Any>>?
    suspend fun getDeclinedRequests(toUserId: String, fromUserId: String): List<Row<Map<String, Any>>>
    suspend fun updateFriendshipProfile(userId: String, friendUserId: String, username: String?, profileImageUrl: String?)
    suspend fun deleteOldDeclinedRequests(userId: String, olderThan: Long)
}
