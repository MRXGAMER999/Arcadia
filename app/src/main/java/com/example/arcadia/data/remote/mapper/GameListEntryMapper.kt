package com.example.arcadia.data.remote.mapper

import com.example.arcadia.data.remote.dto.GameListEntryDto
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import kotlin.math.roundToInt

/**
 * Round a float to one decimal place.
 * Example: 8.976004600524902 -> 9.0, 8.45 -> 8.5
 */
private fun Float.roundToOneDecimal(): Float {
    return (this * 10).roundToInt() / 10f
}

/**
 * Convert DTO to domain model
 */
fun GameListEntryDto.toGameListEntry(documentId: String): GameListEntry {
    return GameListEntry(
        id = documentId,
        rawgId = rawgId,
        name = name,
        backgroundImage = backgroundImage,
        genres = genres,
        platforms = platforms,
        developers = developers,
        publishers = publishers,
        addedAt = addedAt,
        updatedAt = updatedAt,
        status = GameStatus.fromString(status),
        rating = rating?.roundToOneDecimal(),
        review = review,
        hoursPlayed = hoursPlayed,
        aspects = aspects,
        releaseDate = releaseDate,
        importance = importance
    )
}

/**
 * Convert domain model to DTO
 */
fun GameListEntry.toDto(): GameListEntryDto {
    return GameListEntryDto(
        rawgId = rawgId,
        name = name,
        backgroundImage = backgroundImage,
        genres = genres,
        platforms = platforms,
        developers = developers,
        publishers = publishers,
        addedAt = addedAt,
        updatedAt = updatedAt,
        status = status.name,
        rating = rating?.roundToOneDecimal(),
        review = review,
        hoursPlayed = hoursPlayed,
        aspects = aspects,
        releaseDate = releaseDate,
        importance = importance
    )
}

/**
 * Convert Game to GameListEntry (for initial add)
 */
fun Game.toGameListEntry(status: GameStatus = GameStatus.FINISHED): GameListEntry {
    val currentTime = System.currentTimeMillis()
    return GameListEntry(
        rawgId = id,
        name = name,
        backgroundImage = backgroundImage,
        genres = genres,
        platforms = platforms,
        developers = developers,
        publishers = publishers,
        addedAt = currentTime,
        updatedAt = currentTime,
        status = status,
        rating = null,
        review = "",
        hoursPlayed = 0,
        aspects = emptyList(),
        releaseDate = released,
        importance = 0
    )
}
