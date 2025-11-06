package com.example.arcadia.data.remote.mapper

import com.example.arcadia.data.remote.dto.GameListEntryDto
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus

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
        addedAt = addedAt,
        updatedAt = updatedAt,
        status = GameStatus.fromString(status),
        rating = rating,
        review = review,
        hoursPlayed = hoursPlayed
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
        addedAt = addedAt,
        updatedAt = updatedAt,
        status = status.name,
        rating = rating,
        review = review,
        hoursPlayed = hoursPlayed
    )
}

/**
 * Convert Game to GameListEntry (for initial add)
 */
fun Game.toGameListEntry(status: GameStatus = GameStatus.WANT): GameListEntry {
    val currentTime = System.currentTimeMillis()
    return GameListEntry(
        rawgId = id,
        name = name,
        backgroundImage = backgroundImage,
        genres = genres,
        addedAt = currentTime,
        updatedAt = currentTime,
        status = status,
        rating = null,
        review = "",
        hoursPlayed = 0
    )
}

