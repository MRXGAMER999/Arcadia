package com.example.arcadia.domain.model.friend

import kotlinx.serialization.Serializable

/**
 * Represents a friend in the user's friends list.
 * Data is cached in the user's friends subcollection for efficient display.
 */
@Serializable
data class Friend(
    val userId: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val addedAt: Long = 0L
)
