package com.example.arcadia.data.remote.dto

/**
 * Firestore DTO for GameListEntry
 */
data class GameListEntryDto(
    val rawgId: Int = 0,
    val name: String = "",
    val backgroundImage: String? = null,
    val genres: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
    val developers: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val addedAt: Long = 0L,
    val updatedAt: Long = 0L,
    val status: String = "WANT", // Store enum as string
    val rating: Float? = null,
    val review: String = "",
    val hoursPlayed: Int = 0,
    val aspects: List<String> = emptyList(),
    val releaseDate: String? = null,
    val importance: Int = 0 // Custom ordering within same rating tier
)
