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
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

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
    private val recommendationCount: Int = 25
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

    // Cache library during initialize() to avoid re-fetching in load()
    private var cachedInitializationLibrary: List<GameListEntry>? = null

    /**
     * Called by Paging 3 to determine if we should refresh data.
     * 
     * Uses SMARTER INVALIDATION:
     * - Only refreshes if library changed by 3+ games (not every single change)
     * - Still respects TTL for staleness
     * - Offline-first: shows cached data immediately, refreshes in background if needed
     */
    override suspend fun initialize(): InitializeAction {
        val remoteKey = cachedGamesDao.getAIRemoteKey()
        val cacheCount = cachedGamesDao.getAIRecommendationsCount()
        
        if (remoteKey == null || cacheCount == 0) {
            Log.d(TAG, "initialize: No cache found (key=$remoteKey, count=$cacheCount), triggering initial refresh")
            return InitializeAction.LAUNCH_INITIAL_REFRESH
        }
        
        // Check if cache is stale (TTL expired)
        val cacheAge = System.currentTimeMillis() - remoteKey.lastRefreshTime
        val isCacheStale = cacheAge > CACHE_TTL_MS
        
        // Check if library has changed significantly
        val currentLibrary = try {
            val lib = userLibraryProvider()
            cachedInitializationLibrary = lib // Cache for load()
            lib
        } catch (e: Exception) {
            Log.w(TAG, "initialize: Failed to get library for cache check, using cached data", e)
            return InitializeAction.SKIP_INITIAL_REFRESH
        }
        
        val currentLibraryHash = libraryHashProvider(currentLibrary)
        val currentLibrarySize = currentLibrary.size
        val librarySizeChange = kotlin.math.abs(currentLibrarySize - remoteKey.librarySize)
        val libraryHashChanged = currentLibraryHash != remoteKey.libraryHash
        val significantLibraryChange = librarySizeChange >= MIN_LIBRARY_CHANGE_FOR_REFRESH
        
        Log.d(TAG, "initialize: Cache check: age=${cacheAge/1000}s, stale=$isCacheStale, hashChanged=$libraryHashChanged, sizeChange=$librarySizeChange (prev=${remoteKey.librarySize}, curr=$currentLibrarySize)")
        
        // SAFEGUARD: If library suddenly appears empty (but we have substantial cache), 
        // assume it's a loading glitch (or fast switching) and keep cached data 
        // to prevent flashing "No Recommendations".
        // Real empty library (user deleted all games) can be handled by manual refresh.
        if (currentLibrarySize == 0 && remoteKey.librarySize > 5) {
             Log.w(TAG, "initialize: Library size dropped from ${remoteKey.librarySize} to 0. Assuming loading glitch, skipping refresh to preserve cache.")
             return InitializeAction.SKIP_INITIAL_REFRESH
        }
        
        // OFFLINE-FIRST STRATEGY with smart invalidation:
        // - Always show cached data immediately (SKIP_INITIAL_REFRESH)
        // - But trigger background refresh if cache is stale OR library changed significantly
        // This ensures instant app startup while keeping recommendations fresh
        return if (isCacheStale || (libraryHashChanged && significantLibraryChange)) {
            Log.d(TAG, "initialize: Cache needs refresh (stale=$isCacheStale, significantChange=$significantLibraryChange), triggering background refresh")
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            Log.d(TAG, "initialize: Found $cacheCount cached recommendations. Cache is fresh, skipping refresh.")
            InitializeAction.SKIP_INITIAL_REFRESH
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
            LoadType.APPEND -> appendMoreRecommendations()
        }
    }

    /**
     * Enhanced refresh with PROGRESSIVE LOADING and robust error handling.
     * 
     * Strategy:
     * 1. Get AI recommendations with confidence scores
     * 2. Sort by confidence: High (70+), Medium (40-69), Low (<40)
     * 3. Fetch high-confidence games first → save to Room (fast initial display)
     * 4. Fetch medium/low confidence games → append to Room
     * 5. Record all shown recommendations for feedback loop
     * 
     * Error Handling:
     * - If AI call fails, return error but don't clear cache
     * - If game fetching partially fails, save what we got
     * - Progressive saving ensures some data is always available
     */
    private suspend fun refreshRecommendationsProgressive(): MediatorResult {
        return try {
            Log.d(TAG, "Starting AI recommendations refresh with progressive loading")
            
            // 1. Get current library (use cached if available from initialize)
            val userLibrary = cachedInitializationLibrary ?: userLibraryProvider()
            cachedInitializationLibrary = null // Clear cache
            val libraryHash = libraryHashProvider(userLibrary)
            val librarySize = userLibrary.size
            val libraryFingerprint = computeLibraryFingerprint(userLibrary)
            
            // Limit library size for AI analysis to prevent token limit issues
            // Use top 50 most recent/relevant games for analysis
            val limitedLibrary = userLibrary.take(50)
            
            Log.d(TAG, "User library: $librarySize games (using ${limitedLibrary.size} for AI), fingerprint: $libraryFingerprint")
            
            // 2. Get AI recommendations
            val aiResult = aiRepository.getLibraryBasedRecommendations(
                games = limitedLibrary,
                count = recommendationCount,
                forceRefresh = true,
                allOwnedGames = userLibrary.map { it.name }.toSet()
            )
            
            val suggestions = aiResult.getOrElse { error ->
                Log.e(TAG, "AI recommendation failed: ${error.message}")
                // Don't clear existing cache on error - let user see stale data
                return MediatorResult.Error(error)
            }
            
            // CRITICAL FIX: Use recommendations if available, otherwise convert games list
            val recommendations = if (suggestions.recommendations.isNotEmpty()) {
                suggestions.recommendations
            } else if (suggestions.games.isNotEmpty()) {
                // Fallback: Convert plain game names to recommendations with default confidence
                Log.w(TAG, "⚠ AI returned ${suggestions.games.size} games but 0 recommendations. Converting to default recommendations.")
                suggestions.games.map { gameName ->
                    GameRecommendation(name = gameName, confidence = 50, reason = null)
                }
            } else {
                emptyList()
            }
            
            Log.d(TAG, "✓ AI returned ${recommendations.size} recommendations (from ${suggestions.games.size} games)")
            
            if (recommendations.isEmpty()) {
                Log.w(TAG, "⚠ AI returned 0 recommendations AND 0 games! This is unusual.")
                Log.d(TAG, "AI suggestions object: reasoning=${suggestions.reasoning?.take(100)}")
                saveEmptyState(libraryHash, librarySize, libraryFingerprint)
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            
            // Log if AI returned fewer games than requested (may indicate model limitations)
            if (recommendations.size < recommendationCount) {
                Log.w(TAG, "⚠ AI returned only ${recommendations.size}/${recommendationCount} games. This may be due to model token limits or conservative filtering.")
            }
            
            // Log first few recommendations to see metadata
            recommendations.take(3).forEachIndexed { idx, rec ->
                Log.d(TAG, "Sample rec[$idx]: name='${rec.name}', confidence=${rec.confidence}, reason='${rec.reason?.take(50)}'")
            }
            
            // 3. PROGRESSIVE LOADING: Sort by confidence tiers (but save ALL games)
            // Don't filter by confidence - all games are valid recommendations
            // Within each tier, apply SECONDARY SORT by year (newer first) for better quality ordering
            val highConfidence = recommendations
                .filter { it.confidence >= HIGH_CONFIDENCE_THRESHOLD }
                .sortedWith(compareByDescending<GameRecommendation> { it.confidence }
                    .thenByDescending { it.year ?: 0 }) // Newer games first within same confidence
                    
            val mediumConfidence = recommendations
                .filter { it.confidence in MEDIUM_CONFIDENCE_THRESHOLD until HIGH_CONFIDENCE_THRESHOLD }
                .sortedWith(compareByDescending<GameRecommendation> { it.confidence }
                    .thenByDescending { it.year ?: 0 })
                    
            val lowConfidence = recommendations
                .filter { it.confidence < MEDIUM_CONFIDENCE_THRESHOLD }
                .sortedWith(compareByDescending<GameRecommendation> { it.confidence }
                    .thenByDescending { it.year ?: 0 })
            
            Log.d(TAG, "Confidence breakdown: High=${highConfidence.size}, Medium=${mediumConfidence.size}, Low=${lowConfidence.size}")
            Log.d(TAG, "All recommendations will be cached regardless of confidence scores")
            
            // Build recommendation map for easy lookup by name
            val recommendationMap = recommendations.associateBy { it.name.lowercase() }
            
            // 4. Fetch HIGH confidence first for fast initial display
            val highConfidenceGames = fetchGamesInBatches(
                gameNames = highConfidence.map { it.name },
                recommendationMap = recommendationMap,
                startOrder = 0,
                libraryHash = libraryHash
            )
            
            // Save high-confidence immediately so UI can display something quickly
            // Even if medium/low fetching fails later, users see best recommendations
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
                Log.d(TAG, "✓ Saved ${highConfidenceGames.size} high-confidence games for immediate display")
            } else {
                Log.w(TAG, "⚠ No high-confidence games fetched, continuing with medium/low tier")
            }
            
            // 5. Fetch MEDIUM + LOW confidence games (even if high confidence was empty)
            val mediumGames = fetchGamesInBatches(
                gameNames = mediumConfidence.map { it.name },
                recommendationMap = recommendationMap,
                startOrder = highConfidenceGames.size,
                libraryHash = libraryHash
            )
            
            val lowGames = fetchGamesInBatches(
                gameNames = lowConfidence.map { it.name },
                recommendationMap = recommendationMap,
                startOrder = highConfidenceGames.size + mediumGames.size,
                libraryHash = libraryHash
            )
            
            // 6. Combine all and save final state
            val allGames = highConfidenceGames + mediumGames + lowGames
            
            // Always save what we fetched, even if it's less than expected
            if (allGames.isEmpty()) {
                Log.w(TAG, "⚠ No games were successfully fetched from RAWG. Saving empty state.")
                
                // FIX: If we had recommendations but fetched 0 games, it's likely a network/API error, not a valid empty state.
                // We should return Error so the UI shows the retry button.
                if (recommendations.isNotEmpty()) {
                     return MediatorResult.Error(Exception("Failed to fetch game details. Please check your connection."))
                }

                saveEmptyState(libraryHash, librarySize, libraryFingerprint)
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            
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
                    isEndReached = false // Allow fetching more on scroll
                )
            )
            
            // 7. FEEDBACK LOOP: Record shown recommendations
            recordShownRecommendations(allGames)
            
            Log.d(TAG, "✓ Successfully cached ${allGames.size}/${recommendationCount} AI recommendations (H:${highConfidenceGames.size}, M:${mediumGames.size}, L:${lowGames.size})")
            MediatorResult.Success(endOfPaginationReached = false) // More can be fetched on scroll
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error refreshing recommendations: ${e.message}", e)
            // Return error but existing cache remains intact for offline viewing
            MediatorResult.Error(e)
        }
    }

    /**
     * Fetch more recommendations when user scrolls to the end.
     * This allows continuous discovery of new games.
     */
    private suspend fun appendMoreRecommendations(): MediatorResult {
        return try {
            // Check if we should fetch more
            val remoteKey = cachedGamesDao.getAIRemoteKey()
            if (remoteKey?.isEndReached == true) {
                Log.d(TAG, "Already reached end of recommendations, no more to fetch")
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            
            val currentCount = cachedGamesDao.getAIRecommendationsCount()
            Log.d(TAG, "Appending more recommendations (current count: $currentCount)")
            
            // Get already-cached game names to exclude from new recommendations
            val alreadyRecommendedGames = cachedGamesDao.getAIRecommendationsFlow()
                .first()
                .map { it.name }
            Log.d(TAG, "Excluding ${alreadyRecommendedGames.size} already recommended games")
            
            // Get current library
            val userLibrary = userLibraryProvider()
            val libraryHash = libraryHashProvider(userLibrary)
            val librarySize = userLibrary.size
            val libraryFingerprint = computeLibraryFingerprint(userLibrary)
            
            // Request more recommendations, excluding already-recommended games
            val aiResult = aiRepository.getLibraryBasedRecommendations(
                games = userLibrary,
                count = recommendationCount,
                forceRefresh = true,
                excludeGames = alreadyRecommendedGames, // Tell AI to not recommend these again
                allOwnedGames = userLibrary.map { it.name }.toSet()
            )
            
            val suggestions = aiResult.getOrElse { error ->
                Log.e(TAG, "AI recommendation append failed: ${error.message}")
                // Don't mark as end reached on error - user can retry
                return MediatorResult.Error(error)
            }
            
            var recommendations = if (suggestions.recommendations.isNotEmpty()) {
                suggestions.recommendations
            } else if (suggestions.games.isNotEmpty()) {
                suggestions.games.map { gameName ->
                    GameRecommendation(name = gameName, confidence = 50, reason = null)
                }
            } else {
                // No more recommendations available
                Log.d(TAG, "AI returned 0 games on append - marking end reached")
                cachedGamesDao.insertRemoteKey(
                    remoteKey?.copy(isEndReached = true) ?: AIRecommendationRemoteKey(
                        lastRefreshTime = System.currentTimeMillis(),
                        libraryHash = libraryHash,
                        librarySize = librarySize,
                        libraryFingerprint = libraryFingerprint,
                        totalRecommendationsRequested = currentCount,
                        totalRecommendationsCached = currentCount,
                        highConfidenceCached = 0,
                        mediumConfidenceCached = 0,
                        isEndReached = true
                    )
                )
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            
            Log.d(TAG, "✓ AI returned ${recommendations.size} more recommendations")
            
            // Filter out any duplicates that AI might have returned anyway
            val alreadyRecommendedSet = alreadyRecommendedGames.map { it.lowercase() }.toSet()
            recommendations = recommendations.filter { rec ->
                rec.name.lowercase() !in alreadyRecommendedSet
            }
            Log.d(TAG, "After duplicate filter: ${recommendations.size} unique new games")
            
            if (recommendations.isEmpty()) {
                Log.d(TAG, "No unique games after filtering - marking end reached")
                // Always fetch fresh remoteKey to ensure we have correct current state
                val currentRemoteKey = cachedGamesDao.getAIRemoteKey()
                cachedGamesDao.insertRemoteKey(
                    currentRemoteKey?.copy(isEndReached = true) ?: AIRecommendationRemoteKey(
                        lastRefreshTime = System.currentTimeMillis(),
                        libraryHash = libraryHash,
                        librarySize = librarySize,
                        libraryFingerprint = libraryFingerprint,
                        totalRecommendationsRequested = currentCount,
                        totalRecommendationsCached = currentCount,
                        highConfidenceCached = 0,
                        mediumConfidenceCached = 0,
                        isEndReached = true
                    )
                )
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            
            // Sort by confidence (same as refresh logic)
            val highConfidence = recommendations
                .filter { it.confidence >= HIGH_CONFIDENCE_THRESHOLD }
                .sortedWith(compareByDescending<GameRecommendation> { it.confidence }
                    .thenByDescending { it.year ?: 0 })
                    
            val mediumConfidence = recommendations
                .filter { it.confidence in MEDIUM_CONFIDENCE_THRESHOLD until HIGH_CONFIDENCE_THRESHOLD }
                .sortedWith(compareByDescending<GameRecommendation> { it.confidence }
                    .thenByDescending { it.year ?: 0 })
                    
            val lowConfidence = recommendations
                .filter { it.confidence < MEDIUM_CONFIDENCE_THRESHOLD }
                .sortedWith(compareByDescending<GameRecommendation> { it.confidence }
                    .thenByDescending { it.year ?: 0 })
            
            val recommendationMap = recommendations.associateBy { it.name.lowercase() }
            
            // Fetch all in order
            val highGames = fetchGamesInBatches(
                gameNames = highConfidence.map { it.name },
                recommendationMap = recommendationMap,
                startOrder = currentCount,
                libraryHash = libraryHash
            )
            
            val mediumGames = fetchGamesInBatches(
                gameNames = mediumConfidence.map { it.name },
                recommendationMap = recommendationMap,
                startOrder = currentCount + highGames.size,
                libraryHash = libraryHash
            )
            
            val lowGames = fetchGamesInBatches(
                gameNames = lowConfidence.map { it.name },
                recommendationMap = recommendationMap,
                startOrder = currentCount + highGames.size + mediumGames.size,
                libraryHash = libraryHash
            )
            
            val newGames = highGames + mediumGames + lowGames
            
            if (newGames.isEmpty()) {
                Log.w(TAG, "No new games fetched from RAWG - marking end reached")
                // Always fetch fresh remoteKey to ensure we have correct current state
                val currentRemoteKey = cachedGamesDao.getAIRemoteKey()
                cachedGamesDao.insertRemoteKey(
                    currentRemoteKey?.copy(isEndReached = true) ?: AIRecommendationRemoteKey(
                        lastRefreshTime = System.currentTimeMillis(),
                        libraryHash = libraryHash,
                        librarySize = librarySize,
                        libraryFingerprint = libraryFingerprint,
                        totalRecommendationsRequested = currentCount,
                        totalRecommendationsCached = currentCount,
                        highConfidenceCached = 0,
                        mediumConfidenceCached = 0,
                        isEndReached = true
                    )
                )
                return MediatorResult.Success(endOfPaginationReached = true)
            }
            
            // Append to existing cache - fetch fresh remoteKey for accurate counts
            val currentRemoteKey = cachedGamesDao.getAIRemoteKey()
            cachedGamesDao.appendAIRecommendations(
                games = newGames,
                remoteKey = AIRecommendationRemoteKey(
                    lastRefreshTime = System.currentTimeMillis(),
                    libraryHash = libraryHash,
                    librarySize = librarySize,
                    libraryFingerprint = libraryFingerprint,
                    totalRecommendationsRequested = currentCount + recommendations.size,
                    totalRecommendationsCached = currentCount + newGames.size,
                    highConfidenceCached = (currentRemoteKey?.highConfidenceCached ?: 0) + highGames.size,
                    mediumConfidenceCached = (currentRemoteKey?.mediumConfidenceCached ?: 0) + mediumGames.size,
                    isEndReached = false // Can fetch more
                )
            )
            
            // Record new recommendations for feedback
            recordShownRecommendations(newGames)
            
            Log.d(TAG, "✓ Successfully appended ${newGames.size} AI recommendations (total now: ${currentCount + newGames.size})")
            MediatorResult.Success(endOfPaginationReached = false)
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error appending recommendations: ${e.message}", e)
            MediatorResult.Error(e)
        }
    }

    /**
     * Fetch game details from RAWG in batches to avoid rate limiting.
     * Uses concurrent Flow with flatMapMerge for efficient throughput.
     */
    private suspend fun fetchGamesInBatches(
        gameNames: List<String>,
        recommendationMap: Map<String, GameRecommendation>,
        startOrder: Int,
        libraryHash: Int
    ): List<CachedGameEntity> {
        // Fix: Use mapIndexed on the List, NOT on the Flow to avoid resolution errors
        return gameNames.mapIndexed { index, name -> IndexedValue(index, name) }
            .asFlow()
            .flatMapMerge(concurrency = MAX_CONCURRENT_FETCHES) { (index, gameName) ->
                flow {
                    val recommendation = recommendationMap[gameName.lowercase()] ?: GameRecommendation(gameName)
                    val result = fetchSingleGame(
                        gameName = gameName,
                        recommendation = recommendation,
                        order = startOrder + index,
                        libraryHash = libraryHash
                    )
                    if (result != null) emit(result)
                }
            }
            .toList()
            .sortedBy { it.aiRecommendationOrder }
    }

    /**
     * Fetch a single game from RAWG by name.
     * Preserves all AI recommendation metadata (confidence, reason, tier, badges).
     */
    private suspend fun fetchSingleGame(
        gameName: String,
        recommendation: GameRecommendation,
        order: Int,
        libraryHash: Int
    ): CachedGameEntity? {
        return try {
            // FIX: Filter out Loading state, otherwise first() returns Loading and we treat it as failure
            val result = gameRepository.searchGames(gameName, page = 1, pageSize = 1)
                .filter { it !is RequestState.Loading }
                .first()
            
            when (result) {
                is RequestState.Success -> {
                    result.data.firstOrNull()?.let { game ->
                        // Use recommendation's confidence (default 50 if not specified)
                        val confidence = recommendation.confidence
                        
                        // Derive tier from confidence score if not already set
                        val tier = when {
                            confidence >= 90 -> "PERFECT_MATCH"
                            confidence >= 80 -> "STRONG_MATCH"
                            confidence >= 60 -> "GOOD_MATCH"
                            confidence >= 40 -> "DECENT_MATCH"
                            else -> "OKAY_MATCH"
                        }
                        
                        val cachedEntity = CachedGameEntity.fromGame(
                            game = game,
                            isAIRecommendation = true,
                            aiConfidence = confidence.toFloat(),
                            aiReason = recommendation.reason,
                            aiTier = tier,
                            aiBadges = recommendation.badges,
                            aiRecommendationOrder = order,
                            libraryHash = libraryHash
                        )
                        
                        // Log what we're saving
                        if (order < 3) {
                            Log.d(TAG, "Saving game[${order}]: '${game.name}' conf=$confidence, badges=${recommendation.badges.take(2)}, reason='${recommendation.reason?.take(40)}', tier=$tier")
                        }
                        
                        cachedEntity
                    }
                }
                else -> {
                    Log.w(TAG, "No RAWG match for '$gameName'")
                    null
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
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
