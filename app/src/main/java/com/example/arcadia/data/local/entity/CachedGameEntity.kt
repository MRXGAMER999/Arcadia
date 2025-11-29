package com.example.arcadia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.arcadia.domain.model.Game

/**
 * Room entity for caching game data from RAWG API.
 * Used by Paging 3 RemoteMediator for offline support and instant app restarts.
 * 
 * This entity stores the full game details to avoid repeated API calls.
 */
@Entity(tableName = "cached_games")
data class CachedGameEntity(
    @PrimaryKey
    val id: Int,
    val slug: String,
    val name: String,
    val released: String?,
    val backgroundImage: String?,
    val rating: Double,
    val ratingTop: Int,
    val ratingsCount: Int,
    val metacritic: Int?,
    val playtime: Int,
    val platforms: String,  // JSON encoded list
    val genres: String,     // JSON encoded list
    val tags: String,       // JSON encoded list
    val screenshots: String, // JSON encoded list
    val trailerUrl: String?,
    val description: String?,
    val developers: String,  // JSON encoded list
    val publishers: String,  // JSON encoded list
    
    // AI recommendation metadata
    val isAIRecommendation: Boolean = false,
    val aiConfidence: Float? = null,
    val aiReason: String? = null,
    val aiTier: String? = null,
    val aiBadges: String = "", // JSON encoded list of AI-generated badges
    val aiRecommendationOrder: Int? = null, // For maintaining AI's suggested order
    
    // Cache metadata
    val cachedAt: Long = System.currentTimeMillis(),
    val libraryHashWhenCached: Int? = null // To know when to invalidate
) {
    /**
     * Convert Room entity to domain model.
     */
    fun toGame(): Game = Game(
        id = id,
        slug = slug,
        name = name,
        released = released,
        backgroundImage = backgroundImage,
        rating = rating,
        ratingTop = ratingTop,
        ratingsCount = ratingsCount,
        metacritic = metacritic,
        playtime = playtime,
        platforms = platforms.toStringList(),
        genres = genres.toStringList(),
        tags = tags.toStringList(),
        screenshots = screenshots.toStringList(),
        trailerUrl = trailerUrl,
        description = description,
        developers = developers.toStringList(),
        publishers = publishers.toStringList(),
        aiConfidence = aiConfidence,
        aiReason = aiReason,
        aiTier = aiTier,
        aiBadges = aiBadges.toStringList()
    )
    
    companion object {
        /**
         * Create Room entity from domain model.
         */
        fun fromGame(
            game: Game,
            isAIRecommendation: Boolean = false,
            aiConfidence: Float? = null,
            aiReason: String? = null,
            aiTier: String? = null,
            aiBadges: List<String> = emptyList(),
            aiRecommendationOrder: Int? = null,
            libraryHash: Int? = null
        ): CachedGameEntity = CachedGameEntity(
            id = game.id,
            slug = game.slug,
            name = game.name,
            released = game.released,
            backgroundImage = game.backgroundImage,
            rating = game.rating,
            ratingTop = game.ratingTop,
            ratingsCount = game.ratingsCount,
            metacritic = game.metacritic,
            playtime = game.playtime,
            platforms = game.platforms.toJsonString(),
            genres = game.genres.toJsonString(),
            tags = game.tags.toJsonString(),
            screenshots = game.screenshots.toJsonString(),
            trailerUrl = game.trailerUrl,
            description = game.description,
            developers = game.developers.toJsonString(),
            publishers = game.publishers.toJsonString(),
            isAIRecommendation = isAIRecommendation,
            aiConfidence = aiConfidence,
            aiReason = aiReason,
            aiTier = aiTier,
            aiBadges = aiBadges.toJsonString(),
            aiRecommendationOrder = aiRecommendationOrder,
            libraryHashWhenCached = libraryHash
        )
    }
}

/**
 * Simple JSON encoding for string lists (avoids full kotlinx.serialization dependency in entity).
 */
private fun List<String>.toJsonString(): String = 
    joinToString("|||") // Simple delimiter that won't appear in game data

private fun String.toStringList(): List<String> = 
    if (isBlank()) emptyList() else split("|||")
