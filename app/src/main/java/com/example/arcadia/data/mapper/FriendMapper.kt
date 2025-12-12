package com.example.arcadia.data.mapper

import com.example.arcadia.domain.model.friend.Friend
import com.example.arcadia.domain.model.friend.FriendRequest
import com.example.arcadia.domain.model.friend.FriendRequestStatus
import com.example.arcadia.domain.model.friend.UserSearchResult
import io.appwrite.models.Row
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object FriendMapper {

    fun toFriend(doc: Row<Map<String, Any>>): Friend? {
        return try {
            Friend(
                userId = doc.data["friendUserId"] as? String ?: "",
                username = doc.data["friendUsername"] as? String ?: "",
                profileImageUrl = doc.data["friendProfileImageUrl"] as? String,
                addedAt = parseTimestamp(doc.data["addedAt"])
            )
        } catch (e: Exception) {
            null
        }
    }

    fun toFriendRequest(doc: Row<Map<String, Any>>): FriendRequest? {
        return try {
            val statusStr = doc.data["status"] as? String ?: "pending"
            val status = try {
                FriendRequestStatus.valueOf(statusStr.uppercase())
            } catch (e: Exception) {
                FriendRequestStatus.PENDING
            }

            FriendRequest(
                id = doc.id,
                fromUserId = doc.data["fromUserId"] as? String ?: "",
                fromUsername = doc.data["fromUsername"] as? String ?: "",
                fromProfileImageUrl = doc.data["fromProfileImageUrl"] as? String,
                toUserId = doc.data["toUserId"] as? String ?: "",
                toUsername = doc.data["toUsername"] as? String ?: "",
                toProfileImageUrl = doc.data["toProfileImageUrl"] as? String,
                status = status,
                createdAt = parseTimestamp(doc.data["createdAt"]),
                updatedAt = parseTimestamp(doc.data["updatedAt"])
            )
        } catch (e: Exception) {
            null
        }
    }

    fun toUserSearchResult(doc: Row<Map<String, Any>>): UserSearchResult? {
        return try {
            UserSearchResult(
                userId = doc.id,
                username = doc.data["username"] as? String ?: "",
                profileImageUrl = doc.data["profileImageUrl"] as? String
                // bio is not in UserSearchResult
            )
        } catch (e: Exception) {
            null
        }
    }

    fun parseTimestamp(value: Any?): Long {
        return when (value) {
            is Number -> value.toLong()
            is String -> {
                try {
                    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
                    val cleanValue = value.replace("Z", "").replace("+00:00", "")
                    formatter.parse(cleanValue)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
            else -> 0L
        }
    }
}
