package com.example.arcadia.domain.model

import androidx.compose.runtime.Immutable

/**
 * Represents a game in the user's game list with tracking information
 */
@Immutable
data class GameListEntry(
    val id: String = "", // Firestore document ID
    val rawgId: Int = 0, // Game ID from RAWG API
    val name: String = "",
    val backgroundImage: String? = null,
    val genres: List<String> = emptyList(),
    val platforms: List<String> = emptyList(), // List of platform names
    val addedAt: Long = 0L, // Timestamp when game was added
    val updatedAt: Long = 0L, // Timestamp when entry was last updated
    val status: GameStatus = GameStatus.WANT,
    val rating: Float? = null, // User's rating (0.0 - 10.0), null if not rated
    val review: String = "", // User's review/notes
    val hoursPlayed: Int = 0, // Hours played
    val aspects: List<String> = emptyList(), // List of best aspects tags
    val releaseDate: String? = null // Release date from RAWG API (YYYY-MM-DD format)
)

/**
 * Status of a game in the user's list
 */
enum class GameStatus(val displayName: String) {
    PLAYING("Playing"),
    FINISHED("Finished"),
    DROPPED("Dropped"),
    WANT("Want to Play"),
    ON_HOLD("On Hold");
    
    companion object {
        fun fromString(value: String): GameStatus {
            return entries.find { it.name == value } ?: WANT
        }
    }
}
