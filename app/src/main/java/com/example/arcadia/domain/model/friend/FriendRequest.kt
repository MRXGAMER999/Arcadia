package com.example.arcadia.domain.model.friend

import kotlinx.serialization.Serializable

/**
 * Represents a friend request between two users.
 * Stored in the friendRequests collection in Firestore.
 */
@Serializable
data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val fromUsername: String = "",
    val fromProfileImageUrl: String? = null,
    val toUserId: String = "",
    val toUsername: String = "",
    val toProfileImageUrl: String? = null,
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

/**
 * Status of a friend request.
 */
@Serializable
enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}
