package com.example.arcadia.domain.repository

import androidx.paging.PagingData
import com.example.arcadia.domain.model.Game
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for paged game data.
 * 
 * This interface provides Paging 3 compatible data sources for:
 * - AI-powered recommendations (with offline support via RemoteMediator)
 * - Recommendation feedback tracking for AI improvement
 * - Future: Filtered game lists, search results, etc.
 */
interface PagedGameRepository {
    
    /**
     * Get AI-powered game recommendations as a paged flow.
     * 
     * Uses RemoteMediator pattern:
     * - Data is cached in Room for offline support
     * - Automatically refreshes when library changes significantly (3+ games)
     * - Supports instant app restart (loads from cache)
     * - Progressive loading (high confidence games first)
     * 
     * @return Flow of PagingData containing recommended games
     */
    fun getAIRecommendations(): Flow<PagingData<Game>>
    
    /**
     * Force refresh AI recommendations.
     * Clears cache and fetches fresh data from AI.
     */
    suspend fun refreshAIRecommendations()
    
    /**
     * Clear all cached AI recommendations.
     */
    suspend fun clearAIRecommendationsCache()
    
    /**
     * Get the count of cached AI recommendations.
     */
    suspend fun getCachedRecommendationsCount(): Int
    
    /**
     * Record that user clicked on a recommendation.
     * Used for feedback loop to improve future AI prompts.
     * 
     * @param gameId The RAWG game ID that was clicked
     */
    suspend fun recordRecommendationClick(gameId: Int)
    
    /**
     * Record that user added a recommended game to their library.
     * Strong positive signal for AI improvement.
     * 
     * @param gameId The RAWG game ID that was added
     */
    suspend fun recordRecommendationAddedToLibrary(gameId: Int)
}
