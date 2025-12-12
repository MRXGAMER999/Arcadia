package com.example.arcadia.data.mapper

import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import io.appwrite.models.Row

object GameListMapper {

    private fun parseInt(value: Any?): Int {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: 0
            else -> 0
        }
    }

    private fun parseLong(value: Any?): Long {
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    private fun parseFloatOrNull(value: Any?): Float? {
        return when (value) {
            null -> null
            is Number -> value.toFloat()
            is String -> value.toFloatOrNull()
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseStringList(value: Any?): List<String> {
        return when (value) {
            is List<*> -> value.filterIsInstance<String>()
            is String -> {
                // Handles migration where arrays were stored as "a, b, c"
                value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
            else -> emptyList()
        }
    }

    private fun joinStringList(values: List<String>, maxLen: Int = 255): String {
        val joined = values.joinToString(", ")
        return if (joined.length > maxLen) joined.take(maxLen) else joined
    }

    /**
     * Maps an Appwrite Row to a GameListEntry domain model.
     */
    fun toGameListEntry(row: Row<Map<String, Any>>): GameListEntry {
        val data = row.data
        
        return GameListEntry(
            id = row.id,
            rawgId = parseInt(data["rawgId"]),
            name = data["name"] as? String ?: "",
            backgroundImage = data["backgroundImage"] as? String,
            genres = parseStringList(data["genres"]),
            platforms = parseStringList(data["platforms"]),
            developers = parseStringList(data["developers"]),
            publishers = parseStringList(data["publishers"]),
            addedAt = parseLong(data["addedAt"]),
            updatedAt = parseLong(data["updatedAt"]),
            status = GameStatus.fromString(data["status"] as? String ?: "WANT"),
            rating = parseFloatOrNull(data["rating"])?.let { (it * 10).toInt() / 10f },
            review = data["review"] as? String ?: "",
            hoursPlayed = parseInt(data["hoursPlayed"]),
            aspects = parseStringList(data["aspects"]),
            releaseDate = data["releaseDate"] as? String,
            importance = parseInt(data["importance"])
        )
    }

    /**
     * Maps a GameListEntry to a Map for Appwrite document creation/update.
     */
    fun toAppwriteData(entry: GameListEntry, userId: String): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            // Your current Appwrite table defines rawgId as string (per Console screenshot).
            "rawgId" to entry.rawgId.toString(),
            "name" to entry.name,
            "backgroundImage" to entry.backgroundImage,
            // Your current Appwrite table defines these as string columns (not arrays).
            "genres" to joinStringList(entry.genres, maxLen = 255),
            "platforms" to joinStringList(entry.platforms, maxLen = 255),
            "developers" to joinStringList(entry.developers, maxLen = 255),
            "publishers" to joinStringList(entry.publishers, maxLen = 255),
            "addedAt" to entry.addedAt,
            "updatedAt" to entry.updatedAt,
            "status" to entry.status.name,
            // Store rating in the same 1-decimal bucket used for sorting
            "rating" to entry.rating?.let { (it * 10).toInt() / 10f },
            "review" to entry.review,
            "hoursPlayed" to entry.hoursPlayed,
            "aspects" to joinStringList(entry.aspects, maxLen = 255),
            "releaseDate" to entry.releaseDate,
            "importance" to entry.importance
        )
    }
}
