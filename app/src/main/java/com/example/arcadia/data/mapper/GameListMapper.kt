package com.example.arcadia.data.mapper

import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import io.appwrite.models.Row

object GameListMapper {

    /**
     * Maps an Appwrite Row to a GameListEntry domain model.
     */
    @Suppress("UNCHECKED_CAST")
    fun toGameListEntry(row: Row<Map<String, Any>>): GameListEntry {
        val data = row.data
        
        return GameListEntry(
            id = row.id,
            rawgId = (data["rawgId"] as? Number)?.toInt() ?: 0,
            name = data["name"] as? String ?: "",
            backgroundImage = data["backgroundImage"] as? String,
            genres = (data["genres"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            platforms = (data["platforms"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            developers = (data["developers"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            publishers = (data["publishers"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            addedAt = (data["addedAt"] as? Number)?.toLong() ?: 0L,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L,
            status = GameStatus.fromString(data["status"] as? String ?: "WANT"),
            rating = (data["rating"] as? Number)?.toFloat()?.let { (it * 10).toInt() / 10f },
            review = data["review"] as? String ?: "",
            hoursPlayed = (data["hoursPlayed"] as? Number)?.toInt() ?: 0,
            aspects = (data["aspects"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            releaseDate = data["releaseDate"] as? String,
            importance = (data["importance"] as? Number)?.toInt() ?: 0
        )
    }

    /**
     * Maps a GameListEntry to a Map for Appwrite document creation/update.
     */
    fun toAppwriteData(entry: GameListEntry, userId: String): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "rawgId" to entry.rawgId,
            "name" to entry.name,
            "backgroundImage" to entry.backgroundImage,
            "genres" to entry.genres,
            "platforms" to entry.platforms,
            "developers" to entry.developers,
            "publishers" to entry.publishers,
            "addedAt" to entry.addedAt,
            "updatedAt" to entry.updatedAt,
            "status" to entry.status.name,
            // Store rating in the same 1-decimal bucket used for sorting
            "rating" to entry.rating?.let { (it * 10).toInt() / 10f },
            "review" to entry.review,
            "hoursPlayed" to entry.hoursPlayed,
            "aspects" to entry.aspects,
            "releaseDate" to entry.releaseDate,
            "importance" to entry.importance
        )
    }
}
