package com.example.arcadia.data.repository

import android.util.Log
import com.example.arcadia.data.remote.RawgApiService
import com.example.arcadia.data.remote.dto.GameDto
import com.example.arcadia.data.remote.mapper.toGame
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.util.CacheKeys
import com.example.arcadia.util.DateUtils
import com.example.arcadia.util.NetworkCacheManager
import com.example.arcadia.util.RequestDeduplicator
import com.example.arcadia.util.RequestState
import com.example.arcadia.util.safeGameListApiFlow
import com.example.arcadia.util.safeRequestState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Optimized GameRepository implementation with:
 * - In-memory caching for instant repeated loads
 * - Request deduplication to prevent duplicate simultaneous requests
 * - Parallel fetching for faster data loading
 */
class GameRepositoryImpl(
    private val apiService: RawgApiService,
    private val cacheManager: NetworkCacheManager,
    private val deduplicator: RequestDeduplicator
) : GameRepository {
    
    private val TAG = "GameRepository"
    
    override fun getPopularGames(page: Int, pageSize: Int): Flow<RequestState<List<Game>>> = flow {
        emit(RequestState.Loading)
        
        val cacheKey = CacheKeys.popularGames(page, pageSize)
        
        // Check cache first
        cacheManager.get<List<Game>>(cacheKey)?.let { cached ->
            Log.d(TAG, "Cache hit for popular games (page $page)")
            emit(RequestState.Success(cached))
            return@flow
        }
        
        // Deduplicate and fetch
        try {
            val games = deduplicator.dedupe(cacheKey) {
                val dates = DateUtils.getCurrentYearRange()
                apiService.getGames(
                    page = page,
                    pageSize = pageSize,
                    dates = dates,
                    ordering = "-rating,-added"
                ).results.map { it.toGame() }
            }
            
            cacheManager.put(cacheKey, games)
            emit(RequestState.Success(games))
        } catch (e: CancellationException) {
            Log.d(TAG, "getPopularGames cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching popular games: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch popular games: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getUpcomingGames(page: Int, pageSize: Int): Flow<RequestState<List<Game>>> = flow {
        emit(RequestState.Loading)
        
        val cacheKey = CacheKeys.upcomingGames(page, pageSize)
        
        // Check cache first
        cacheManager.get<List<Game>>(cacheKey)?.let { cached ->
            Log.d(TAG, "Cache hit for upcoming games (page $page)")
            emit(RequestState.Success(cached))
            return@flow
        }
        
        try {
            val games = deduplicator.dedupe(cacheKey) {
                val dates = DateUtils.getUpcomingRange()
                apiService.getGames(
                    page = page,
                    pageSize = pageSize,
                    dates = dates,
                    ordering = "released"
                ).results.map { it.toGame() }
            }
            
            cacheManager.put(cacheKey, games)
            emit(RequestState.Success(games))
        } catch (e: CancellationException) {
            Log.d(TAG, "getUpcomingGames cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching upcoming games: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch upcoming games: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getNewReleases(page: Int, pageSize: Int): Flow<RequestState<List<Game>>> = flow {
        emit(RequestState.Loading)
        
        val cacheKey = CacheKeys.newReleases(page, pageSize)
        
        // Check cache first
        cacheManager.get<List<Game>>(cacheKey)?.let { cached ->
            Log.d(TAG, "Cache hit for new releases (page $page)")
            emit(RequestState.Success(cached))
            return@flow
        }
        
        try {
            val games = deduplicator.dedupe(cacheKey) {
                val dates = DateUtils.getRecentRange(60)
                apiService.getGames(
                    page = page,
                    pageSize = pageSize,
                    dates = dates,
                    ordering = "-released,-rating"
                ).results.map { it.toGame() }
            }
            
            cacheManager.put(cacheKey, games)
            emit(RequestState.Success(games))
        } catch (e: CancellationException) {
            Log.d(TAG, "getNewReleases cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching new releases: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch new releases: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getGamesByGenre(genreId: Int, page: Int, pageSize: Int): Flow<RequestState<List<Game>>> = flow {
        emit(RequestState.Loading)
        
        val cacheKey = CacheKeys.gamesByGenre(genreId, page, pageSize)
        
        // Check cache first
        cacheManager.get<List<Game>>(cacheKey)?.let { cached ->
            Log.d(TAG, "Cache hit for games by genre $genreId (page $page)")
            emit(RequestState.Success(cached))
            return@flow
        }
        
        try {
            val games = deduplicator.dedupe(cacheKey) {
                apiService.getGames(
                    page = page,
                    pageSize = pageSize,
                    genres = genreId.toString(),
                    ordering = "-rating"
                ).results.map { it.toGame() }
            }
            
            cacheManager.put(cacheKey, games)
            emit(RequestState.Success(games))
        } catch (e: CancellationException) {
            Log.d(TAG, "getGamesByGenre cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching games by genre: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch games by genre: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getRecommendedGames(tags: String, page: Int, pageSize: Int): Flow<RequestState<List<Game>>> = flow {
        emit(RequestState.Loading)
        
        val cacheKey = CacheKeys.recommendedGames(page, pageSize)
        
        // Check cache first (shorter duration for recommendations)
        cacheManager.get<List<Game>>(cacheKey)?.let { cached ->
            Log.d(TAG, "Cache hit for recommended games (page $page)")
            emit(RequestState.Success(cached))
            return@flow
        }
        
        try {
            val games = deduplicator.dedupe(cacheKey) {
                val dates = DateUtils.getYearsBackRange(5)
                apiService.getGames(
                    page = page,
                    pageSize = pageSize,
                    tags = tags,
                    dates = dates,
                    ordering = "-rating,-added"
                ).results.map { it.toGame() }
            }
            
            // Shorter cache for recommendations (2 minutes)
            cacheManager.putShort(cacheKey, games)
            emit(RequestState.Success(games))
        } catch (e: CancellationException) {
            Log.d(TAG, "getRecommendedGames cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recommended games: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch recommended games: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun searchGames(query: String, page: Int, pageSize: Int): Flow<RequestState<List<Game>>> = flow {
        emit(RequestState.Loading)
        
        val cacheKey = CacheKeys.searchGames(query, page, pageSize)
        
        // Check cache first
        cacheManager.get<List<Game>>(cacheKey)?.let { cached ->
            Log.d(TAG, "Cache hit for search '$query' (page $page)")
            emit(RequestState.Success(cached))
            return@flow
        }
        
        try {
            val games = deduplicator.dedupe(cacheKey) {
                apiService.getGames(
                    page = page,
                    pageSize = pageSize,
                    search = query,
                    ordering = "-rating,-added"
                ).results.map { it.toGame() }
            }
            
            // Shorter cache for search results (2 minutes)
            cacheManager.putShort(cacheKey, games)
            emit(RequestState.Success(games))
        } catch (e: CancellationException) {
            Log.d(TAG, "searchGames cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error searching games: ${e.message}", e)
            emit(RequestState.Error("Failed to search games: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getGameDetails(gameId: Int): Flow<RequestState<Game>> = flow {
        emit(RequestState.Loading)
        
        val cacheKey = CacheKeys.gameDetails(gameId)
        
        // Check cache first (longer duration for game details)
        cacheManager.get<Game>(cacheKey)?.let { cached ->
            Log.d(TAG, "Cache hit for game details $gameId")
            emit(RequestState.Success(cached))
            return@flow
        }
        
        try {
            val game = deduplicator.dedupe(cacheKey) {
                apiService.getGameDetails(gameId).toGame()
            }
            
            // Longer cache for game details (15 minutes)
            cacheManager.putLong(cacheKey, game)
            emit(RequestState.Success(game))
        } catch (e: CancellationException) {
            Log.d(TAG, "getGameDetails cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching game details: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch game details: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getGameDetailsWithMedia(gameId: Int): Flow<RequestState<Game>> = flow {
        emit(RequestState.Loading)
        
        val cacheKey = CacheKeys.gameDetailsWithMedia(gameId)
        
        // Check cache first
        cacheManager.get<Game>(cacheKey)?.let { cached ->
            Log.d(TAG, "Cache hit for game details with media $gameId")
            emit(RequestState.Success(cached))
            return@flow
        }
        
        try {
            val game = deduplicator.dedupe(cacheKey) {
                // Fetch all data in parallel for faster loading
                coroutineScope {
                    val gameDeferred = async { apiService.getGameDetails(gameId) }
                    val videosDeferred = async { 
                        try { apiService.getGameVideos(gameId) } catch (e: Exception) { null } 
                    }
                    val screenshotsDeferred = async { 
                        try { apiService.getGameScreenshots(gameId) } catch (e: Exception) { null } 
                    }
                    
                    val gameDto = gameDeferred.await()
                    var game = gameDto.toGame()
                    
                    // Add trailer
                    videosDeferred.await()?.let { movieResponse ->
                        val trailerUrl = movieResponse.results.firstOrNull()?.data?.qualityMax
                            ?: movieResponse.results.firstOrNull()?.data?.quality480
                        if (trailerUrl != null) {
                            game = game.copy(trailerUrl = trailerUrl)
                        }
                    }
                    
                    // Add screenshots
                    screenshotsDeferred.await()?.let { screenshotResponse ->
                        val screenshots = screenshotResponse.results.map { it.image }
                        game = game.copy(screenshots = screenshots)
                    }
                    
                    game
                }
            }
            
            // Longer cache for game details with media (15 minutes)
            cacheManager.putLong(cacheKey, game)
            emit(RequestState.Success(game))
        } catch (e: CancellationException) {
            Log.d(TAG, "getGameDetailsWithMedia cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching game details with media: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch game details: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getGamesByStudios(
        developerSlugs: String?,
        publisherSlugs: String?,
        page: Int,
        pageSize: Int
    ): Flow<RequestState<List<Game>>> = flow {
        emit(RequestState.Loading)
        
        val slugsHash = "${developerSlugs.orEmpty()}_${publisherSlugs.orEmpty()}".hashCode()
        val cacheKey = CacheKeys.studioGames("$slugsHash", page)
        
        // Check cache first
        cacheManager.get<List<Game>>(cacheKey)?.let { cached ->
            Log.d(TAG, "Cache hit for studio games (page $page)")
            emit(RequestState.Success(cached))
            return@flow
        }
        
        try {
            val games = deduplicator.dedupe(cacheKey) {
                // Fetch multiple pages in parallel for more results
                val pagesToFetch = listOf(1, 2, 3)
                val fetchPageSize = 40 // Max per page
                
                coroutineScope {
                    val deferredResults = mutableListOf<kotlinx.coroutines.Deferred<List<GameDto>>>()
                    
                    // Parallel fetch by developers (multiple pages)
                    if (!developerSlugs.isNullOrBlank()) {
                        pagesToFetch.forEach { pageNum ->
                            deferredResults.add(async {
                                try {
                                    apiService.getGames(
                                        page = pageNum,
                                        pageSize = fetchPageSize,
                                        developers = developerSlugs,
                                        ordering = "-rating,-added"
                                    ).results.also {
                                        Log.d(TAG, "Fetched ${it.size} games by developers (page $pageNum)")
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error fetching developers page $pageNum: ${e.message}")
                                    emptyList()
                                }
                            })
                        }
                    }
                    
                    // Parallel fetch by publishers (multiple pages)
                    if (!publisherSlugs.isNullOrBlank()) {
                        pagesToFetch.forEach { pageNum ->
                            deferredResults.add(async {
                                try {
                                    apiService.getGames(
                                        page = pageNum,
                                        pageSize = fetchPageSize,
                                        publishers = publisherSlugs,
                                        ordering = "-rating,-added"
                                    ).results.also {
                                        Log.d(TAG, "Fetched ${it.size} games by publishers (page $pageNum)")
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Error fetching publishers page $pageNum: ${e.message}")
                                    emptyList()
                                }
                            })
                        }
                    }
                    
                    // Wait for all parallel requests to complete
                    val allGameDtos = deferredResults.awaitAll().flatten()
                    
                    // Deduplicate by game ID and convert to Game objects
                    val seenIds = mutableSetOf<Int>()
                    val uniqueGames = allGameDtos.mapNotNull { dto ->
                        if (dto.id !in seenIds) {
                            seenIds.add(dto.id)
                            dto.toGame()
                        } else {
                            null
                        }
                    }
                    
                    // Sort by rating (best first)
                    uniqueGames.sortedByDescending { it.rating }
                }
            }
            
            Log.d(TAG, "Total unique games for studios: ${games.size}")
            cacheManager.put(cacheKey, games)
            emit(RequestState.Success(games))
        } catch (e: CancellationException) {
            Log.d(TAG, "getGamesByStudios cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching games by studios: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch games by studios: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getFilteredGames(
        developerSlugs: String?,
        genres: String?,
        startDate: String?,
        endDate: String?,
        ordering: String?,
        page: Int,
        pageSize: Int
    ): Flow<RequestState<List<Game>>> = flow {
        emit(RequestState.Loading)
        
        val filterHash = "$developerSlugs$genres$startDate$endDate$ordering".hashCode()
        val cacheKey = CacheKeys.filteredGames(filterHash, page)
        
        // Check cache first
        cacheManager.get<List<Game>>(cacheKey)?.let { cached ->
            Log.d(TAG, "Cache hit for filtered games (page $page)")
            emit(RequestState.Success(cached))
            return@flow
        }
        
        try {
            val games = deduplicator.dedupe(cacheKey) {
                val dates = if (startDate != null && endDate != null) {
                    DateUtils.formatDateRange(startDate, endDate)
                } else null
                
                Log.d(TAG, "Filtering games: studios=$developerSlugs, genres=$genres, dates=$dates, ordering=$ordering")
                
                // If filtering by studio (developerSlugs), search both Developers AND Publishers
                // This fixes the issue where selecting a publisher returns 0 results
                if (!developerSlugs.isNullOrBlank()) {
                    coroutineScope {
                        val devDeferred = async {
                            try {
                                apiService.getGames(
                                    page = page,
                                    pageSize = pageSize,
                                    developers = developerSlugs,
                                    genres = genres,
                                    dates = dates,
                                    ordering = ordering ?: "-rating,-added"
                                ).results
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to fetch by developers: ${e.message}")
                                emptyList()
                            }
                        }
                        
                        val pubDeferred = async {
                            try {
                                apiService.getGames(
                                    page = page,
                                    pageSize = pageSize,
                                    publishers = developerSlugs,
                                    genres = genres,
                                    dates = dates,
                                    ordering = ordering ?: "-rating,-added"
                                ).results
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to fetch by publishers: ${e.message}")
                                emptyList()
                            }
                        }
                        
                        val devGames = devDeferred.await()
                        val pubGames = pubDeferred.await()
                        
                        Log.d(TAG, "Merged results: ${devGames.size} dev games + ${pubGames.size} pub games")
                        
                        val merged = (devGames + pubGames)
                            .distinctBy { it.id }
                            .map { it.toGame() }
                            
                        // Re-sort merged list based on ordering
                        sortMergedGames(merged, ordering)
                    }
                } else {
                    // Standard single request if no studio filter
                    apiService.getGames(
                        page = page,
                        pageSize = pageSize,
                        developers = null,
                        genres = genres,
                        dates = dates,
                        ordering = ordering ?: "-rating,-added"
                    ).results.map { it.toGame() }.also {
                        Log.d(TAG, "Filtered games result: ${it.size} games")
                    }
                }
            }
            
            cacheManager.put(cacheKey, games)
            emit(RequestState.Success(games))
        } catch (e: CancellationException) {
            Log.d(TAG, "getFilteredGames cancelled")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching filtered games: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch filtered games: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    private fun sortMergedGames(games: List<Game>, ordering: String?): List<Game> {
        if (ordering == null) return games.sortedByDescending { it.rating }
        
        return when {
            ordering.contains("rating") -> {
                if (ordering.startsWith("-")) games.sortedByDescending { it.rating }
                else games.sortedBy { it.rating }
            }
            ordering.contains("released") -> {
                if (ordering.startsWith("-")) games.sortedByDescending { it.released }
                else games.sortedBy { it.released }
            }
            ordering.contains("name") -> {
                if (ordering.startsWith("-")) games.sortedByDescending { it.name }
                else games.sortedBy { it.name }
            }
            // For 'added' (popularity) or others, we preserve the API's relative ordering
            // as best as we can (usually API sorts strictly, so merging maintains rough order)
            else -> games 
        }
    }
    
    /**
     * Invalidate all cached game data. Call this when data might be stale.
     */
    fun invalidateCache() {
        cacheManager.removeByPrefix(CacheKeys.GAMES_PREFIX)
        cacheManager.removeByPrefix(CacheKeys.GAME_DETAILS_PREFIX)
        Log.d(TAG, "Game cache invalidated")
    }
    
    /**
     * Invalidate cache for a specific game.
     */
    fun invalidateGameCache(gameId: Int) {
        cacheManager.remove(CacheKeys.gameDetails(gameId))
        cacheManager.remove(CacheKeys.gameDetailsWithMedia(gameId))
        Log.d(TAG, "Cache invalidated for game $gameId")
    }
}
