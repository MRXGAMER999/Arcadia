package com.example.arcadia.data.repository

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.arcadia.data.local.dao.CachedGamesDao
import com.example.arcadia.data.local.dao.RecommendationFeedbackDao
import com.example.arcadia.data.paging.AIRecommendationsRemoteMediator
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.repository.AIRepository
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.domain.repository.PagedGameRepository
import com.example.arcadia.domain.repository.SortOrder
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Enhanced implementation of PagedGameRepository using Paging 3 with RemoteMediator.
 * 
 * This repository provides:
 * - AI recommendations with offline caching
 * - Progressive loading via Paging 3 (high confidence first)
 * - Automatic refresh on significant library changes (3+ games)
 * - Instant app restart experience
 * - Feedback loop for AI improvement
 * 
 * Architecture:
 * ```
 * UI ← PagingData ← Pager ← PagingSource (Room) ← RemoteMediator ← AI + RAWG
 *                                                        ↓
 *                                              RecommendationFeedback
 * ```
 */
@OptIn(ExperimentalPagingApi::class)
class PagedGameRepositoryImpl(
    private val aiRepository: AIRepository,
    private val gameRepository: GameRepository,
    private val gameListRepository: GameListRepository,
    private val cachedGamesDao: CachedGamesDao,
    private val feedbackDao: RecommendationFeedbackDao? = null
) : PagedGameRepository {

    companion object {
        private const val TAG = "PagedGameRepository"
        
        /** Number of AI recommendations to request */
        const val AI_RECOMMENDATION_COUNT = 50
        
        /** Page size for Paging 3 */
        private const val PAGE_SIZE = 10
        
        /** Prefetch distance - load next page when this many items from end */
        private const val PREFETCH_DISTANCE = 5
    }

    /**
     * Get AI recommendations as a paged flow.
     * 
     * Uses enhanced Paging 3 RemoteMediator pattern:
     * 1. PagingSource reads from Room (fast, offline)
     * 2. RemoteMediator syncs from AI + RAWG → Room (high confidence first)
     * 3. Room updates trigger PagingSource invalidation
     * 4. Feedback is recorded for AI prompt improvement
     */
    override fun getAIRecommendations(): Flow<PagingData<Game>> {
        Log.d(TAG, "Creating AI recommendations Pager with enhanced features")
        
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false,
                initialLoadSize = PAGE_SIZE * 2 // Load more on first page for better UX
            ),
            remoteMediator = AIRecommendationsRemoteMediator(
                aiRepository = aiRepository,
                gameRepository = gameRepository,
                cachedGamesDao = cachedGamesDao,
                feedbackDao = feedbackDao,
                userLibraryProvider = { getUserLibrary() },
                libraryHashProvider = { computeLibraryHash(it) },
                successfulRecommendationsProvider = feedbackDao?.let { dao ->
                    { dao.getSuccessfulRecommendationNames(limit = 10) }
                },
                recommendationCount = AI_RECOMMENDATION_COUNT
            ),
            pagingSourceFactory = {
                cachedGamesDao.getAIRecommendationsPagingSource()
            }
        ).flow.map { pagingData ->
            // Convert CachedGameEntity to Game domain model
            pagingData.map { entity -> entity.toGame() }
        }
    }

    /**
     * Force refresh AI recommendations.
     * Clears existing cache to trigger RemoteMediator refresh.
     */
    override suspend fun refreshAIRecommendations() {
        Log.d(TAG, "Forcing AI recommendations refresh")
        cachedGamesDao.clearAIRecommendations()
        cachedGamesDao.clearRemoteKey()
    }

    /**
     * Clear all cached AI recommendations.
     */
    override suspend fun clearAIRecommendationsCache() {
        Log.d(TAG, "Clearing AI recommendations cache")
        cachedGamesDao.clearAIRecommendations()
        cachedGamesDao.clearRemoteKey()
    }

    /**
     * Get count of cached recommendations.
     */
    override suspend fun getCachedRecommendationsCount(): Int {
        return cachedGamesDao.getAIRecommendationsCount()
    }
    
    /**
     * Record that user clicked on a recommendation.
     * Used for feedback loop to improve future AI prompts.
     */
    override suspend fun recordRecommendationClick(gameId: Int) {
        feedbackDao?.recordClick(gameId)
    }
    
    /**
     * Record that user added a recommended game to their library.
     * Strong positive signal for AI improvement.
     */
    override suspend fun recordRecommendationAddedToLibrary(gameId: Int) {
        feedbackDao?.recordAddedToLibrary(gameId)
    }
    
    /**
     * Get analytics on recommendation effectiveness.
     */
    suspend fun getRecommendationAnalytics(): RecommendationAnalytics? {
        return feedbackDao?.let { dao ->
            RecommendationAnalytics(
                totalShown = dao.getTotalRecommendationsCount(),
                successfulCount = dao.getSuccessfulRecommendationsCount(),
                clickThroughRate = dao.getClickThroughRate() ?: 0f,
                conversionRate = dao.getConversionRate() ?: 0f,
                averageSuccessfulConfidence = dao.getAverageSuccessfulConfidence() ?: 0f
            )
        }
    }

    /**
     * Get current user library from Firebase.
     */
    private suspend fun getUserLibrary(): List<GameListEntry> {
        return try {
            val result = gameListRepository.getGameList(SortOrder.NEWEST_FIRST)
                .first { it is RequestState.Success || it is RequestState.Error }
            
            when (result) {
                is RequestState.Success -> result.data
                else -> emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user library", e)
            emptyList()
        }
    }

    /**
     * Compute hash of library for cache invalidation.
     * Includes game IDs, ratings, and status to detect meaningful changes.
     */
    private fun computeLibraryHash(games: List<GameListEntry>): Int {
        return games.map { "${it.rawgId}_${it.rating}_${it.status}" }
            .sorted()
            .hashCode()
    }
}

/**
 * Data class for recommendation analytics.
 */
data class RecommendationAnalytics(
    val totalShown: Int,
    val successfulCount: Int,
    val clickThroughRate: Float,
    val conversionRate: Float,
    val averageSuccessfulConfidence: Float
)
