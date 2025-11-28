package com.example.arcadia.data.paging

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.example.arcadia.data.local.dao.CachedGamesDao
import com.example.arcadia.data.local.dao.RecommendationFeedbackDao
import com.example.arcadia.data.local.entity.AIRecommendationRemoteKey
import com.example.arcadia.data.local.entity.CachedGameEntity
import com.example.arcadia.data.local.entity.RecommendationFeedbackEntity
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.ai.GameRecommendation
import com.example.arcadia.domain.repository.AIRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlin.math.abs

/**
 * Enhanced RemoteMediator for AI-powered game recommendations.
 * 
 * Features:
 * 1. Progressive Loading: Fetches high-confidence games first for faster display
 * 2. Smarter Invalidation: Only refreshes when library changes by 3+ games
 * 3. Feedback Loop: Records shown recommendations for AI improvement
 * 4. Library Fingerprint: Top games hash for smarter caching
 * 
 * Flow:
 * 1. REFRESH: Fetch AI recommendations → Search RAWG for details → Save to Room
 * 2. APPEND: Not used (AI returns all recommendations at once)
 * 3. PREPEND: Not used
 * 
 * @param aiRepository Repository for getting AI recommendations
 * @param gameRepository Repository for fetching game details from RAWG
 * @param cachedGamesDao DAO for storing cached games
 * @param feedbackDao DAO for recording recommendation feedback (optional)
 * @param userLibraryProvider Function to get current user library
 * @param libraryHashProvider Function to compute library hash for invalidation
 * @param successfulRecommendationsProvider Function to get games user liked from past recommendations
 * @param recommendationCount Number of recommendations to request from AI
 */
@OptIn(ExperimentalPagingApi::class)
class AIRecommendationsRemoteMediator(
    private val aiRepository: AIRepository,
    private val gameRepository: GameRepository,
    private val cachedGamesDao: CachedGamesDao,
    private val feedbackDao: RecommendationFeedbackDao? = null,
    private val userLibraryProvider: suspend () -> List<GameListEntry>,
    private val libraryHashProvider: (List<GameListEntry>) -> Int,
    private val successfulRecommendationsProvider: (suspend () -> List<String>)? = null,
    private val recommendationCount: Int = 50
) : RemoteMediator<Int, CachedGameEntity>() {

    companion object {
        private const val TAG = "AIRecommendationsRM"
        
        /** Cache TTL: 2 hours - refresh if older than this */
        private const val CACHE_TTL_MS = 2 * 60 * 60 * 1000L
        
        /** Maximum concurrent RAWG API calls to avoid rate limiting */
        private const val MAX_CONCURRENT_FETCHES = 5
        
        /** Minimum library change to trigger refresh (smarter invalidation) */
        private const val MIN_LIBRARY_CHANGE_FOR_REFRESH = 3
        
        /** High confidence threshold for progressive loading */
        private const val HIGH_CONFIDENCE_THRESHOLD = 70
        
        /** Medium confidence threshold */
        private const val MEDIUM_CONFIDENCE_THRESHOLD = 40
    }

    /**
     * Called by Paging 3 to determine if we should refresh data.
     * 
     * Uses SMARTER INVALIDATION:
     * - Only refreshes if library changed by 3+ games (not every single change)
     * - Still respects TTL for staleness
     */
    override suspend fun initialize(): InitializeAction {
        val remoteKey = cachedGamesDao.getAIRemoteKey()
        
        if (remoteKey == null) {
            Log.d(TAG, "No remote key found, triggering initial refresh")
            return InitializeAction.LAUNCH_INITIAL_REFRESH
        }
        
        val currentLibrary = userLibraryProvider()
        val currentHash = libraryHashProvider(currentLibrary)
        val currentSize = currentLibrary.size
        val cacheAge = System.currentTimeMillis() - remoteKey.lastRefreshTime
        
        return when {
            // Cache is stale (TTL expired)
            cacheAge > CACHE_TTL_MS -> {
                Log.d(TAG, "Cache expired (${cacheAge / 1000}s old), refreshing")
                InitializeAction.LAUNCH_INITIAL_REFRESH
            }
            
            // SMARTER INVALIDATION: Only refresh if library changed significantly
            remoteKey.libraryHash != currentHash -> {
                val libraryDelta = abs(currentSize - remoteKey.librarySize)
                if (libraryDelta >= MIN_LIBRARY_CHANGE_FOR_REFRESH) {
                    Log.d(TAG, "Library changed significantly ($libraryDelta games), refreshing")
                    InitializeAction.LAUNCH_INITIAL_REFRESH
                } else {
                    Log.d(TAG, "Library changed slightly ($libraryDelta games), keeping cache")
                    InitializeAction.SKIP_INITIAL_REFRESH
                }
            }
            
            // Cache is valid
            else -> {
                Log.d(TAG, "Using cached recommendations (${remoteKey.totalRecommendationsCached} games)")
                InitializeAction.SKIP_INITIAL_REFRESH
            }
        }
    }

    /**
     * Main loading function called by Paging 3.
     */
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CachedGameEntity>
    ): MediatorResult {
        return when (loadType) {
            LoadType.REFRESH -> refreshRecommendationsProgressive()
            LoadType.PREPEND -> MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> MediatorResult.Success(endOfPaginationReached = true)
        }
    }

    /**
     * Enhanced refresh with PROGRESSIVE LOADING.
     * 
     * Strategy:
     * 1. Get AI recommendations with confidence scores
     * 2. Sort by confidence: High (70+), Medium (40-69), Low (<40)
     * 3. Fetch high-confidence games first → save to Room (fast initial display)
     * 4. Fetch medium/low confidence games
     * 5. Record all shown recommendations for feedback loop
     */
    private suspend fun refreshRecommendationsProgressive(): MediatorResult {
        return try {
            Log.d(TAG, "Starting AI recommendations refresh with progressive loading")
            
            // 1. Get current library
            val userLibrary = userLibraryProvider()
            val libraryHash = libraryHashProvider(userLibrary)
            val librarySize = userLibrary.size
            val libraryFingerprint = computeLibraryFingerprint(userLibrary)
            val libraryGameIds = userLibrary.map { it.rawgId }.toSet()
            
            Log.d(TAG, "User library: $librarySize games, fingerprint: $libraryFingerprint")
            
            // 2. Get AI recommendations
            val aiResult = aiRepository.getLibraryBasedRecommendations(
                games = userLibrary,
                count = recommendationCount,
                forceRefresh = true
            )
            
            val suggestions = aiResult.getOrElse { error ->
                Log.e(TAG, "AI recommendation failed: ${error.message}")
                return MediatorResult.Error(error)
            }
            
            val recommendations = suggestions.recommendations
            Log.d(TAG, "AI returned ${recommendations.size} recommendations")
            
            if (recommendations.isEmpty()) {
                saveEmptyState(libraryHash, librarySize, libraryFingerprint)
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            
            // 3. PROGRESSIVE LOADING: Sort by confidence tiers
            val highConfidence = recommendations.filter { it.confidence >= HIGH_CONFIDENCE_THRESHOLD }
            val mediumConfidence = recommendations.filter { 
                it.confidence in MEDIUM_CONFIDENCE_THRESHOLD until HIGH_CONFIDENCE_THRESHOLD 
            }
            val lowConfidence = recommendations.filter { it.confidence < MEDIUM_CONFIDENCE_THRESHOLD }
            
            Log.d(TAG, "Confidence breakdown: High=${highConfidence.size}, Medium=${mediumConfidence.size}, Low=${lowConfidence.size}")
            
            // Build confidence map
            val confidenceMap = recommendations.associate { it.name.lowercase() to it.confidence }
            
            // 4. Fetch HIGH confidence first for fast initial display
            val highConfidenceGames = fetchGamesInBatches(
                gameNames = highConfidence.map { it.name },
                confidenceMap = confidenceMap,
                startOrder = 0,
                libraryHash = libraryHash,
                libraryGameIds = libraryGameIds
            )
            
            // Save high-confidence immediately so UI can display
            if (highConfidenceGames.isNotEmpty()) {
                cachedGamesDao.refreshAIRecommendations(
                    games = highConfidenceGames,
                    remoteKey = AIRecommendationRemoteKey(
                        lastRefreshTime = System.currentTimeMillis(),
                        libraryHash = libraryHash,
                        librarySize = librarySize,
                        libraryFingerprint = libraryFingerprint,
                        totalRecommendationsRequested = recommendationCount,
                        totalRecommendationsCached = highConfidenceGames.size,
                        highConfidenceCached = highConfidenceGames.size,
                        mediumConfidenceCached = 0,
                        isEndReached = false // More coming
                    )
                )
                Log.d(TAG, "Saved ${highConfidenceGames.size} high-confidence games for immediate display")
            }
            
            // 5. Fetch MEDIUM + LOW confidence games
            val mediumGames = fetchGamesInBatches(
                gameNames = mediumConfidence.map { it.name },
                confidenceMap = confidenceMap,
                startOrder = highConfidenceGames.size,
                libraryHash = libraryHash,
                libraryGameIds = libraryGameIds
            )
            
            val lowGames = fetchGamesInBatches(
                gameNames = lowConfidence.map { it.name },
                confidenceMap = confidenceMap,
                startOrder = highConfidenceGames.size + mediumGames.size,
                libraryHash = libraryHash,
                libraryGameIds = libraryGameIds
            )
            
            // 6. Combine all and save final state
            val allGames = highConfidenceGames + mediumGames + lowGames
            
            cachedGamesDao.refreshAIRecommendations(
                games = allGames,
                remoteKey = AIRecommendationRemoteKey(
                    lastRefreshTime = System.currentTimeMillis(),
                    libraryHash = libraryHash,
                    librarySize = librarySize,
                    libraryFingerprint = libraryFingerprint,
                    totalRecommendationsRequested = recommendationCount,
                    totalRecommendationsCached = allGames.size,
                    highConfidenceCached = highConfidenceGames.size,
                    mediumConfidenceCached = mediumGames.size,
                    isEndReached = true
                )
            )
            
            // 7. FEEDBACK LOOP: Record shown recommendations
            recordShownRecommendations(allGames)
            
            Log.d(TAG, "Successfully cached ${allGames.size} AI recommendations (H:${highConfidenceGames.size}, M:${mediumGames.size}, L:${lowGames.size})")
            MediatorResult.Success(endOfPaginationReached = true)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing recommendations", e)
            MediatorResult.Error(e)
        }
    }

    /**
     * Fetch game details from RAWG in batches to avoid rate limiting.
     * Uses coroutines for parallel fetching within each batch.
     */
    private suspend fun fetchGamesInBatches(
        gameNames: List<String>,
        confidenceMap: Map<String, Int>,
        startOrder: Int,
        libraryHash: Int,
        libraryGameIds: Set<Int>
    ): List<CachedGameEntity> = coroutineScope {
        val results = mutableListOf<CachedGameEntity>()
        
        gameNames.chunked(MAX_CONCURRENT_FETCHES).forEachIndexed { batchIndex, batch ->
            val batchResults = batch.mapIndexed { indexInBatch, gameName ->
                async {
                    fetchSingleGame(
                        gameName = gameName,
                        confidence = confidenceMap[gameName.lowercase()] ?: 50,
                        order = startOrder + batchIndex * MAX_CONCURRENT_FETCHES + indexInBatch,
                        libraryHash = libraryHash
                    )
                }
            }.awaitAll().filterNotNull()
            
            // Filter out games already in library
            val filtered = batchResults.filter { it.id !in libraryGameIds }
            results.addAll(filtered)
            Log.d(TAG, "Batch ${batchIndex + 1}: fetched ${filtered.size}/${batch.size} games")
        }
        
        results
    }

    /**
     * Fetch a single game from RAWG by name.
     */
    private suspend fun fetchSingleGame(
        gameName: String,
        confidence: Int,
        order: Int,
        libraryHash: Int
    ): CachedGameEntity? {
        return try {
            val result = gameRepository.searchGames(gameName, page = 1, pageSize = 1).first()
            
            when (result) {
                is RequestState.Success -> {
                    result.data.firstOrNull()?.let { game ->
                        CachedGameEntity.fromGame(
                            game = game,
                            isAIRecommendation = true,
                            aiConfidence = confidence.toFloat(),
                            aiRecommendationOrder = order,
                            libraryHash = libraryHash
                        )
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch game '$gameName': ${e.message}")
            null
        }
    }
    
    /**
     * Compute a library fingerprint based on top-played games.
     * This allows potential cache sharing for users with similar libraries.
     */
    private fun computeLibraryFingerprint(library: List<GameListEntry>): String {
        return library
            .sortedByDescending { it.hoursPlayed }
            .take(10)
            .map { it.name.lowercase().take(10) }
            .sorted()
            .joinToString("|")
            .hashCode()
            .toString()
    }

    /**
     * Save empty state when AI returns no recommendations.
     */
    private suspend fun saveEmptyState(libraryHash: Int, librarySize: Int, fingerprint: String) {
        cachedGamesDao.refreshAIRecommendations(
            games = emptyList(),
            remoteKey = AIRecommendationRemoteKey(
                lastRefreshTime = System.currentTimeMillis(),
                libraryHash = libraryHash,
                librarySize = librarySize,
                libraryFingerprint = fingerprint,
                totalRecommendationsRequested = recommendationCount,
                totalRecommendationsCached = 0,
                highConfidenceCached = 0,
                mediumConfidenceCached = 0,
                isEndReached = true
            )
        )
    }

    /**
     * FEEDBACK LOOP: Record all shown recommendations for future prompt enhancement.
     */
    private suspend fun recordShownRecommendations(games: List<CachedGameEntity>) {
        feedbackDao?.let { dao ->
            try {
                val feedbacks = games.mapIndexed { index, game ->
                    RecommendationFeedbackEntity(
                        gameId = game.id,
                        gameName = game.name,
                        aiConfidence = game.aiConfidence ?: 50f,
                        positionWhenShown = index
                    )
                }
                dao.recordRecommendationsShown(feedbacks)
                Log.d(TAG, "Recorded ${feedbacks.size} recommendations for feedback tracking")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record feedback: ${e.message}")
            }
        }
    }
}
