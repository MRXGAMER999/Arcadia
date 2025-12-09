package com.example.arcadia.data.repository

import android.util.Log
import com.example.arcadia.data.remote.OneSignalNotificationService
import com.example.arcadia.domain.model.Gamer
import com.example.arcadia.domain.model.friend.Friend
import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.domain.model.friend.FriendRequestStatus
import com.example.arcadia.domain.model.friend.FriendshipStatus
import com.example.arcadia.domain.model.friend.RequestValidation
import com.example.arcadia.domain.model.friend.UserSearchResult
import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.util.RequestState
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Implementation of FriendsRepository using Firebase Firestore.
 * Handles all friend-related operations including CRUD, real-time listeners, search, and validation.
 */
class FriendsRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val notificationService: OneSignalNotificationService
) : FriendsRepository {

    companion object {
        private const val TAG = "FriendsRepositoryImpl"
        private const val USERS_COLLECTION = "users"
        private const val FRIENDS_SUBCOLLECTION = "friends"
        private const val FRIEND_REQUESTS_COLLECTION = "friendRequests"
        
        // Limits
        private const val DAILY_REQUEST_LIMIT = 20
        private const val PENDING_REQUEST_LIMIT = 100
        private const val FRIENDS_LIMIT = 500
        private const val PAGINATION_LIMIT = 20
        private const val SEARCH_RESULTS_LIMIT = 20
        private const val DECLINE_COOLDOWN_HOURS = 24
        private const val DECLINED_REQUEST_RETENTION_DAYS = 30
    }

    // ==================== Friends List Operations ====================

    override fun getFriendsRealtime(userId: String): Flow<RequestState<List<Friend>>> = callbackFlow {
        trySend(RequestState.Loading)
        
        val listenerRegistration = firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(FRIENDS_SUBCOLLECTION)
            .orderBy("username", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting friends: ${error.message}", error)
                    trySend(RequestState.Error("Failed to load friends: ${error.message}"))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val friends = snapshot.documents.mapNotNull { doc ->
                        try {
                            Friend(
                                userId = doc.getString("userId") ?: doc.id,
                                username = doc.getString("username") ?: "",
                                profileImageUrl = doc.getString("profileImageUrl"),
                                addedAt = doc.getTimestamp("addedAt")?.toDate()?.time ?: 0L
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing friend document: ${e.message}", e)
                            null
                        }
                    }.sortedBy { it.username.lowercase() }
                    
                    trySend(RequestState.Success(friends))
                }
            }
        
        awaitClose {
            listenerRegistration.remove()
        }
    }

    override fun getFriendsPaginated(
        userId: String,
        limit: Int,
        lastUsername: String?
    ): Flow<RequestState<List<Friend>>> = flow {
        emit(RequestState.Loading)
        
        try {
            var query = firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection(FRIENDS_SUBCOLLECTION)
                .orderBy("username", Query.Direction.ASCENDING)
                .limit(limit.toLong())
            
            if (lastUsername != null) {
                query = query.startAfter(lastUsername)
            }
            
            val snapshot = query.get().await()
            
            val friends = snapshot.documents.mapNotNull { doc ->
                try {
                    Friend(
                        userId = doc.getString("userId") ?: doc.id,
                        username = doc.getString("username") ?: "",
                        profileImageUrl = doc.getString("profileImageUrl"),
                        addedAt = doc.getTimestamp("addedAt")?.toDate()?.time ?: 0L
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing friend document: ${e.message}", e)
                    null
                }
            }
            
            emit(RequestState.Success(friends))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paginated friends: ${e.message}", e)
            emit(RequestState.Error("Failed to load friends: ${e.message}"))
        }
    }

    override suspend fun removeFriend(currentUserId: String, friendUserId: String): RequestState<Unit> {
        return try {
            val batch = firestore.batch()
            
            // Remove from current user's friends subcollection
            val currentUserFriendRef = firestore
                .collection(USERS_COLLECTION)
                .document(currentUserId)
                .collection(FRIENDS_SUBCOLLECTION)
                .document(friendUserId)
            batch.delete(currentUserFriendRef)
            
            // Remove from friend's friends subcollection
            val friendUserFriendRef = firestore
                .collection(USERS_COLLECTION)
                .document(friendUserId)
                .collection(FRIENDS_SUBCOLLECTION)
                .document(currentUserId)
            batch.delete(friendUserFriendRef)
            
            batch.commit().await()
            
            Log.d(TAG, "Successfully removed friend: $friendUserId from user: $currentUserId")
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing friend: ${e.message}", e)
            RequestState.Error("Failed to remove friend: ${e.message}")
        }
    }

    override suspend fun isFriend(userId: String, friendUserId: String): Boolean {
        return try {
            val friendDoc = firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection(FRIENDS_SUBCOLLECTION)
                .document(friendUserId)
                .get()
                .await()
            
            friendDoc.exists()
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
            // Validate and reset daily limit if needed
            validateAndResetDailyLimit(fromUserId)
            
            // Create the friend request document
            val requestData = hashMapOf(
                "fromUserId" to fromUserId,
                "fromUsername" to fromUsername,
                "fromProfileImageUrl" to fromProfileImageUrl,
                "toUserId" to toUserId,
                "toUsername" to toUsername,
                "toProfileImageUrl" to toProfileImageUrl,
                "status" to FriendRequestStatus.PENDING.name.lowercase(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            // Add the request to Firestore
            firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .add(requestData)
                .await()
            
            // Increment the daily request counter
            firestore.collection(USERS_COLLECTION)
                .document(fromUserId)
                .update("friendRequestsSentToday", FieldValue.increment(1))
                .await()
            
            Log.d(TAG, "Successfully sent friend request from $fromUserId to $toUserId")
            
            // Send push notification to recipient
            try {
                val recipientDoc = firestore.collection(USERS_COLLECTION).document(toUserId).get().await()
                val recipientPlayerId = recipientDoc.getString("oneSignalPlayerId")
                
                if (!recipientPlayerId.isNullOrBlank()) {
                    notificationService.sendFriendRequestNotification(recipientPlayerId, fromUsername)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send notification: ${e.message}")
                // Don't fail the request if notification fails
            }
            
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request: ${e.message}", e)
            RequestState.Error("Failed to send friend request: ${e.message}")
        }
    }

    override suspend fun acceptFriendRequest(request: FriendRequest): RequestState<Unit> {
        return try {
            val batch = firestore.batch()
            
            // 1. Update request status to "accepted"
            val requestRef = firestore.collection(FRIEND_REQUESTS_COLLECTION).document(request.id)
            batch.update(requestRef, mapOf(
                "status" to FriendRequestStatus.ACCEPTED.name.lowercase(),
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            
            // 2. Add to sender's friends subcollection
            val senderFriendRef = firestore
                .collection(USERS_COLLECTION)
                .document(request.fromUserId)
                .collection(FRIENDS_SUBCOLLECTION)
                .document(request.toUserId)
            batch.set(senderFriendRef, mapOf(
                "userId" to request.toUserId,
                "username" to request.toUsername,
                "profileImageUrl" to request.toProfileImageUrl,
                "addedAt" to FieldValue.serverTimestamp()
            ))
            
            // 3. Add to recipient's friends subcollection
            val recipientFriendRef = firestore
                .collection(USERS_COLLECTION)
                .document(request.toUserId)
                .collection(FRIENDS_SUBCOLLECTION)
                .document(request.fromUserId)
            batch.set(recipientFriendRef, mapOf(
                "userId" to request.fromUserId,
                "username" to request.fromUsername,
                "profileImageUrl" to request.fromProfileImageUrl,
                "addedAt" to FieldValue.serverTimestamp()
            ))
            
            batch.commit().await()
            
            Log.d(TAG, "Successfully accepted friend request: ${request.id}")
            
            // Send push notification to original sender
            try {
                val senderDoc = firestore.collection(USERS_COLLECTION).document(request.fromUserId).get().await()
                val senderPlayerId = senderDoc.getString("oneSignalPlayerId")
                
                if (!senderPlayerId.isNullOrBlank()) {
                    notificationService.sendFriendAcceptedNotification(senderPlayerId, request.toUsername)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send notification: ${e.message}")
                // Don't fail the request if notification fails
            }
            
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request: ${e.message}", e)
            RequestState.Error("Failed to accept friend request: ${e.message}")
        }
    }

    override suspend fun declineFriendRequest(requestId: String): RequestState<Unit> {
        return try {
            firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .document(requestId)
                .update(mapOf(
                    "status" to FriendRequestStatus.DECLINED.name.lowercase(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                .await()
            
            Log.d(TAG, "Successfully declined friend request: $requestId")
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error declining friend request: ${e.message}", e)
            RequestState.Error("Failed to decline friend request: ${e.message}")
        }
    }

    override suspend fun cancelFriendRequest(requestId: String): RequestState<Unit> {
        return try {
            firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .document(requestId)
                .delete()
                .await()
            
            Log.d(TAG, "Successfully cancelled friend request: $requestId")
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling friend request: ${e.message}", e)
            RequestState.Error("Failed to cancel friend request: ${e.message}")
        }
    }

    /**
     * Validates and resets the daily limit counter if the date has changed.
     */
    private suspend fun validateAndResetDailyLimit(userId: String) {
        try {
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            val lastResetDate = userDoc.getString("friendRequestsLastResetDate")
            val todayDate = getCurrentDateUTC()
            
            if (lastResetDate != todayDate) {
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .update(mapOf(
                        "friendRequestsSentToday" to 0,
                        "friendRequestsLastResetDate" to todayDate
                    ))
                    .await()
                
                Log.d(TAG, "Reset daily limit for user: $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating daily limit: ${e.message}", e)
        }
    }

    /**
     * Gets the current date in UTC timezone formatted as YYYY-MM-DD.
     */
    private fun getCurrentDateUTC(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date())
    }


    // ==================== Request Queries and Listeners ====================

    override fun getIncomingRequestsRealtime(userId: String): Flow<RequestState<List<FriendRequest>>> = callbackFlow {
        trySend(RequestState.Loading)
        
        val listenerRegistration = firestore
            .collection(FRIEND_REQUESTS_COLLECTION)
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", FriendRequestStatus.PENDING.name.lowercase())
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting incoming requests: ${error.message}", error)
                    trySend(RequestState.Error("Failed to load incoming requests: ${error.message}"))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val requests = snapshot.documents.mapNotNull { doc ->
                        parseFriendRequest(doc)
                    }
                    trySend(RequestState.Success(requests))
                }
            }
        
        awaitClose {
            listenerRegistration.remove()
        }
    }

    override fun getOutgoingRequestsRealtime(userId: String): Flow<RequestState<List<FriendRequest>>> = callbackFlow {
        trySend(RequestState.Loading)
        
        val listenerRegistration = firestore
            .collection(FRIEND_REQUESTS_COLLECTION)
            .whereEqualTo("fromUserId", userId)
            .whereEqualTo("status", FriendRequestStatus.PENDING.name.lowercase())
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting outgoing requests: ${error.message}", error)
                    trySend(RequestState.Error("Failed to load outgoing requests: ${error.message}"))
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val requests = snapshot.documents.mapNotNull { doc ->
                        parseFriendRequest(doc)
                    }
                    trySend(RequestState.Success(requests))
                }
            }
        
        awaitClose {
            listenerRegistration.remove()
        }
    }

    override fun getPendingRequestCountRealtime(userId: String): Flow<Int> = callbackFlow {
        val listenerRegistration = firestore
            .collection(FRIEND_REQUESTS_COLLECTION)
            .whereEqualTo("toUserId", userId)
            .whereEqualTo("status", FriendRequestStatus.PENDING.name.lowercase())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting pending request count: ${error.message}", error)
                    trySend(0)
                    return@addSnapshotListener
                }
                
                val count = snapshot?.size() ?: 0
                trySend(count)
            }
        
        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Parses a Firestore document into a FriendRequest object.
     */
    private fun parseFriendRequest(doc: com.google.firebase.firestore.DocumentSnapshot): FriendRequest? {
        return try {
            val statusString = doc.getString("status") ?: return null
            val status = when (statusString.lowercase()) {
                "pending" -> FriendRequestStatus.PENDING
                "accepted" -> FriendRequestStatus.ACCEPTED
                "declined" -> FriendRequestStatus.DECLINED
                else -> FriendRequestStatus.PENDING
            }
            
            FriendRequest(
                id = doc.id,
                fromUserId = doc.getString("fromUserId") ?: "",
                fromUsername = doc.getString("fromUsername") ?: "",
                fromProfileImageUrl = doc.getString("fromProfileImageUrl"),
                toUserId = doc.getString("toUserId") ?: "",
                toUsername = doc.getString("toUsername") ?: "",
                toProfileImageUrl = doc.getString("toProfileImageUrl"),
                status = status,
                createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing friend request: ${e.message}", e)
            null
        }
    }


    // ==================== Friendship Status Operations ====================

    override fun getFriendshipStatusRealtime(
        currentUserId: String,
        targetUserId: String
    ): Flow<FriendshipStatus> = callbackFlow {
        // First check if they are friends
        val friendsListener = firestore
            .collection(USERS_COLLECTION)
            .document(currentUserId)
            .collection(FRIENDS_SUBCOLLECTION)
            .document(targetUserId)
            .addSnapshotListener { friendSnapshot, friendError ->
                if (friendError != null) {
                    Log.e(TAG, "Error checking friendship: ${friendError.message}", friendError)
                    return@addSnapshotListener
                }
                
                if (friendSnapshot != null && friendSnapshot.exists()) {
                    trySend(FriendshipStatus.FRIENDS)
                    return@addSnapshotListener
                }
                
                // Not friends, check for pending requests
                // Check if current user sent a request to target
                firestore.collection(FRIEND_REQUESTS_COLLECTION)
                    .whereEqualTo("fromUserId", currentUserId)
                    .whereEqualTo("toUserId", targetUserId)
                    .whereEqualTo("status", FriendRequestStatus.PENDING.name.lowercase())
                    .limit(1)
                    .get()
                    .addOnSuccessListener { sentSnapshot ->
                        if (!sentSnapshot.isEmpty) {
                            trySend(FriendshipStatus.REQUEST_SENT)
                            return@addOnSuccessListener
                        }
                        
                        // Check if target user sent a request to current user
                        firestore.collection(FRIEND_REQUESTS_COLLECTION)
                            .whereEqualTo("fromUserId", targetUserId)
                            .whereEqualTo("toUserId", currentUserId)
                            .whereEqualTo("status", FriendRequestStatus.PENDING.name.lowercase())
                            .limit(1)
                            .get()
                            .addOnSuccessListener { receivedSnapshot ->
                                if (!receivedSnapshot.isEmpty) {
                                    trySend(FriendshipStatus.REQUEST_RECEIVED)
                                } else {
                                    trySend(FriendshipStatus.NOT_FRIENDS)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error checking received requests: ${e.message}", e)
                                trySend(FriendshipStatus.NOT_FRIENDS)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error checking sent requests: ${e.message}", e)
                        trySend(FriendshipStatus.NOT_FRIENDS)
                    }
            }
        
        awaitClose {
            friendsListener.remove()
        }
    }

    override suspend fun checkReciprocalRequest(
        currentUserId: String,
        targetUserId: String
    ): FriendRequest? {
        return try {
            val snapshot = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", targetUserId)
                .whereEqualTo("toUserId", currentUserId)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name.lowercase())
                .limit(1)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                null
            } else {
                parseFriendRequest(snapshot.documents.first())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking reciprocal request: ${e.message}", e)
            null
        }
    }


    // ==================== Search Operations ====================

    override suspend fun searchUsers(
        query: String,
        currentUserId: String
    ): RequestState<List<UserSearchResult>> {
        if (query.length < 2) {
            return RequestState.Success(emptyList())
        }
        
        return try {
            val lowerQuery = query.lowercase()
            
            // Firestore doesn't support case-insensitive partial matching natively,
            // so we fetch users and filter client-side.
            // For production, consider using Algolia or Firebase Extensions for full-text search.
            val snapshot = firestore.collection(USERS_COLLECTION)
                .orderBy("username")
                .get()
                .await()
            
            val matchingUsers = snapshot.documents
                .mapNotNull { doc ->
                    val userId = doc.id
                    val username = doc.getString("username") ?: ""
                    val profileImageUrl = doc.getString("profileImageUrl")
                    
                    // Filter: exclude current user and match query
                    if (userId != currentUserId && username.lowercase().contains(lowerQuery)) {
                        Triple(userId, username, profileImageUrl)
                    } else {
                        null
                    }
                }
                .sortedWith(compareBy(
                    // Priority 1: Exact match
                    { !it.second.lowercase().equals(lowerQuery) },
                    // Priority 2: Starts with query
                    { !it.second.lowercase().startsWith(lowerQuery) },
                    // Priority 3: Alphabetical
                    { it.second.lowercase() }
                ))
                .take(SEARCH_RESULTS_LIMIT)
            
            // Get friendship status for each result
            val results = matchingUsers.map { (userId, username, profileImageUrl) ->
                val friendshipStatus = getFriendshipStatusForSearch(currentUserId, userId)
                val isRecentlyDeclined = checkRecentlyDeclined(currentUserId, userId)
                
                UserSearchResult(
                    userId = userId,
                    username = username,
                    profileImageUrl = profileImageUrl,
                    friendshipStatus = friendshipStatus,
                    isRecentlyDeclined = isRecentlyDeclined
                )
            }
            
            RequestState.Success(results)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users: ${e.message}", e)
            RequestState.Error("Failed to search users: ${e.message}")
        }
    }

    /**
     * Gets the friendship status for a user in search results.
     * This is a one-time check, not a real-time listener.
     */
    private suspend fun getFriendshipStatusForSearch(
        currentUserId: String,
        targetUserId: String
    ): FriendshipStatus {
        return try {
            // Check if they are friends
            val isFriends = isFriend(currentUserId, targetUserId)
            if (isFriends) {
                return FriendshipStatus.FRIENDS
            }
            
            // Check if current user sent a request
            val sentRequest = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", currentUserId)
                .whereEqualTo("toUserId", targetUserId)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name.lowercase())
                .limit(1)
                .get()
                .await()
            
            if (!sentRequest.isEmpty) {
                return FriendshipStatus.REQUEST_SENT
            }
            
            // Check if target user sent a request
            val receivedRequest = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", targetUserId)
                .whereEqualTo("toUserId", currentUserId)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name.lowercase())
                .limit(1)
                .get()
                .await()
            
            if (!receivedRequest.isEmpty) {
                return FriendshipStatus.REQUEST_RECEIVED
            }
            
            FriendshipStatus.NOT_FRIENDS
        } catch (e: Exception) {
            Log.e(TAG, "Error getting friendship status for search: ${e.message}", e)
            FriendshipStatus.NOT_FRIENDS
        }
    }

    /**
     * Checks if the current user was recently declined by the target user (within 24 hours).
     */
    private suspend fun checkRecentlyDeclined(
        currentUserId: String,
        targetUserId: String
    ): Boolean {
        return try {
            val cooldownHours = getDeclinedRequestCooldown(currentUserId, targetUserId)
            cooldownHours != null && cooldownHours > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error checking recently declined: ${e.message}", e)
            false
        }
    }


    // ==================== Validation Operations ====================

    override suspend fun canSendRequest(userId: String): RequestValidation {
        return try {
            // Validate and reset daily limit if needed
            validateAndResetDailyLimit(userId)
            
            val userDoc = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            
            // Check daily limit
            val requestsSentToday = userDoc.getLong("friendRequestsSentToday")?.toInt() ?: 0
            val dailyLimitReached = requestsSentToday >= DAILY_REQUEST_LIMIT
            
            // Check pending requests limit
            val pendingRequestsSnapshot = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", userId)
                .whereEqualTo("status", FriendRequestStatus.PENDING.name.lowercase())
                .get()
                .await()
            val pendingLimitReached = pendingRequestsSnapshot.size() >= PENDING_REQUEST_LIMIT
            
            // Check friends limit
            val friendsSnapshot = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(FRIENDS_SUBCOLLECTION)
                .get()
                .await()
            val friendsLimitReached = friendsSnapshot.size() >= FRIENDS_LIMIT
            
            val canSend = !dailyLimitReached && !pendingLimitReached && !friendsLimitReached
            
            RequestValidation(
                canSend = canSend,
                dailyLimitReached = dailyLimitReached,
                pendingLimitReached = pendingLimitReached,
                friendsLimitReached = friendsLimitReached
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error validating request limits: ${e.message}", e)
            // Return a validation that allows sending on error (fail open)
            RequestValidation(canSend = true)
        }
    }

    override suspend fun getDeclinedRequestCooldown(fromUserId: String, toUserId: String): Int? {
        return try {
            val snapshot = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", fromUserId)
                .whereEqualTo("toUserId", toUserId)
                .whereEqualTo("status", FriendRequestStatus.DECLINED.name.lowercase())
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                return null
            }
            
            val request = snapshot.documents.first()
            val updatedAt = request.getTimestamp("updatedAt")?.toDate()?.time ?: return null
            
            val hoursSinceDecline = (System.currentTimeMillis() - updatedAt) / (1000 * 60 * 60)
            val remainingHours = DECLINE_COOLDOWN_HOURS - hoursSinceDecline.toInt()
            
            if (remainingHours > 0) remainingHours else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting declined request cooldown: ${e.message}", e)
            null
        }
    }


    // ==================== Profile Data Sync Operations ====================

    override suspend fun updateFriendProfileData(
        userId: String,
        friendUserId: String,
        newUsername: String?,
        newProfileImageUrl: String?
    ): RequestState<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>()
            
            newUsername?.let { updates["username"] = it }
            newProfileImageUrl?.let { updates["profileImageUrl"] = it }
            
            if (updates.isEmpty()) {
                return RequestState.Success(Unit)
            }
            
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(FRIENDS_SUBCOLLECTION)
                .document(friendUserId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Successfully updated friend profile data for $friendUserId")
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating friend profile data: ${e.message}", e)
            RequestState.Error("Failed to update friend profile data: ${e.message}")
        }
    }

    // ==================== OneSignal Operations ====================

    override suspend fun updateOneSignalPlayerId(userId: String, playerId: String): RequestState<Unit> {
        return try {
            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update("oneSignalPlayerId", playerId)
                .await()
            
            Log.d(TAG, "Successfully updated OneSignal player ID for user: $userId")
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating OneSignal player ID: ${e.message}", e)
            RequestState.Error("Failed to update OneSignal player ID: ${e.message}")
        }
    }

    override suspend fun getUserPlayerIds(userIds: List<String>): Map<String, String?> {
        return try {
            if (userIds.isEmpty()) {
                return emptyMap()
            }
            
            // Firestore 'in' queries are limited to 10 items
            val results = mutableMapOf<String, String?>()
            
            userIds.chunked(10).forEach { chunk ->
                val snapshot = firestore.collection(USERS_COLLECTION)
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                    .get()
                    .await()
                
                snapshot.documents.forEach { doc ->
                    results[doc.id] = doc.getString("oneSignalPlayerId")
                }
            }
            
            results
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user player IDs: ${e.message}", e)
            emptyMap()
        }
    }

    // ==================== Cleanup Operations ====================

    override suspend fun cleanupOldDeclinedRequests(userId: String, maxDeletes: Int) {
        try {
            val thirtyDaysAgo = System.currentTimeMillis() - (DECLINED_REQUEST_RETENTION_DAYS * 24 * 60 * 60 * 1000L)
            val thirtyDaysAgoDate = Date(thirtyDaysAgo)
            
            // Find old declined requests where the user is either sender or recipient
            val oldRequestsAsSender = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", userId)
                .whereEqualTo("status", FriendRequestStatus.DECLINED.name.lowercase())
                .whereLessThan("updatedAt", com.google.firebase.Timestamp(thirtyDaysAgoDate))
                .limit(maxDeletes.toLong())
                .get()
                .await()
            
            val oldRequestsAsRecipient = firestore.collection(FRIEND_REQUESTS_COLLECTION)
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("status", FriendRequestStatus.DECLINED.name.lowercase())
                .whereLessThan("updatedAt", com.google.firebase.Timestamp(thirtyDaysAgoDate))
                .limit(maxDeletes.toLong())
                .get()
                .await()
            
            val allOldRequests = (oldRequestsAsSender.documents + oldRequestsAsRecipient.documents)
                .distinctBy { it.id }
                .take(maxDeletes)
            
            if (allOldRequests.isEmpty()) {
                Log.d(TAG, "No old declined requests to clean up for user: $userId")
                return
            }
            
            val batch = firestore.batch()
            allOldRequests.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            batch.commit().await()
            
            Log.d(TAG, "Successfully cleaned up ${allOldRequests.size} old declined requests for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old declined requests: ${e.message}", e)
        }
    }
}
