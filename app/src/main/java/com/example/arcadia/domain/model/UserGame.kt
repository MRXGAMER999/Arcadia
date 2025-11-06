package com.example.arcadia.domain.model

data class UserGame(
    val id: String = "", // Firestore document ID
    val rawgId: Int = 0, // Game ID from RAWG API
    val name: String = "",
    val backgroundImage: String? = null,
    val addedAt: Long = 0L, // Timestamp in milliseconds
    val genres: List<String> = emptyList()
)

