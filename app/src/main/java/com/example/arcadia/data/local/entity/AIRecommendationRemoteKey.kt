package com.example.arcadia.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the state of AI recommendation remote keys for Paging 3 RemoteMediator.
 * 
 * This entity helps RemoteMediator know:
 * - When the cache was last refreshed
 * - What library hash was used (for invalidation)
 * - Library size for smarter invalidation (only refresh if 3+ games added)
 * - Whether there are more recommendations available
 * - Library fingerprint for cross-user cache potential
 */
@Entity(tableName = "ai_recommendation_remote_keys")
data class AIRecommendationRemoteKey(
    @PrimaryKey
    val id: Int = 0, // Single row for AI recommendations state
    val lastRefreshTime: Long = 0,
    val libraryHash: Int = 0,
    val librarySize: Int = 0, // For smarter invalidation
    val libraryFingerprint: String = "", // Top 10 games hash for similar-library caching
    val totalRecommendationsRequested: Int = 0,
    val totalRecommendationsCached: Int = 0,
    val highConfidenceCached: Int = 0, // Games with confidence >= 70
    val mediumConfidenceCached: Int = 0, // Games with confidence 40-69
    val isEndReached: Boolean = false
)
