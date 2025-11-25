package com.example.arcadia.data.repository

import android.util.Log
import com.example.arcadia.data.remote.RawgApiService
import com.example.arcadia.data.remote.dto.GameDto
import com.example.arcadia.data.remote.mapper.toGame
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.util.DateUtils
import com.example.arcadia.util.RequestState
import com.example.arcadia.util.safeGameListApiFlow
import com.example.arcadia.util.safeRequestState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class GameRepositoryImpl(
    private val apiService: RawgApiService
) : GameRepository {
    
    private val TAG = "GameRepository"
    
    override fun getPopularGames(page: Int, pageSize: Int): Flow<RequestState<List<Game>>> =
        safeGameListApiFlow(TAG, "popular games") {
            val dates = DateUtils.getCurrentYearRange()
            apiService.getGames(
                page = page,
                pageSize = pageSize,
                dates = dates,
                ordering = "-rating,-added"
            ).results
        }
    
    override fun getUpcomingGames(page: Int, pageSize: Int): Flow<RequestState<List<Game>>> =
        safeGameListApiFlow(TAG, "upcoming games") {
            val dates = DateUtils.getUpcomingRange()
            apiService.getGames(
                page = page,
                pageSize = pageSize,
                dates = dates,
                ordering = "released"
            ).results
        }
    
    override fun getNewReleases(page: Int, pageSize: Int): Flow<RequestState<List<Game>>> =
        safeGameListApiFlow(TAG, "new releases") {
            val dates = DateUtils.getRecentRange(60)
            apiService.getGames(
                page = page,
                pageSize = pageSize,
                dates = dates,
                ordering = "-released,-rating"
            ).results
        }
    
    override fun getGamesByGenre(genreId: Int, page: Int, pageSize: Int): Flow<RequestState<List<Game>>> =
        safeGameListApiFlow(TAG, "games by genre") {
            apiService.getGames(
                page = page,
                pageSize = pageSize,
                genres = genreId.toString(),
                ordering = "-rating"
            ).results
        }
    
    override fun getRecommendedGames(tags: String, page: Int, pageSize: Int): Flow<RequestState<List<Game>>> =
        safeGameListApiFlow(TAG, "recommended games") {
            val dates = DateUtils.getYearsBackRange(5)
            apiService.getGames(
                page = page,
                pageSize = pageSize,
                tags = tags,
                dates = dates,
                ordering = "-rating,-added"
            ).results
        }
    
    override fun searchGames(query: String, page: Int, pageSize: Int): Flow<RequestState<List<Game>>> =
        safeGameListApiFlow(TAG, "search games") {
            apiService.getGames(
                page = page,
                pageSize = pageSize,
                search = query,
                ordering = "-rating,-added"
            ).results
        }

    override fun getGameDetails(gameId: Int): Flow<RequestState<Game>> = flow {
        emit(RequestState.Loading)
        emit(safeRequestState(TAG, "game details") {
            apiService.getGameDetails(gameId).toGame()
        })
    }.flowOn(Dispatchers.IO)

    override fun getGameDetailsWithMedia(gameId: Int): Flow<RequestState<Game>> = flow {
        try {
            emit(RequestState.Loading)
            
            // Fetch game details
            val gameDto = apiService.getGameDetails(gameId)
            var game = gameDto.toGame()
            
            // Fetch trailer
            try {
                val movieResponse = apiService.getGameVideos(gameId)
                Log.d(TAG, "Movies response for game $gameId: $movieResponse")
                
                val trailerUrl = movieResponse.results.firstOrNull()?.data?.qualityMax
                    ?: movieResponse.results.firstOrNull()?.data?.quality480
                
                if (trailerUrl != null) {
                    Log.d(TAG, "Found trailer URL for game $gameId: $trailerUrl")
                } else {
                    Log.d(TAG, "No trailer URL found for game $gameId")
                }
                
                game = game.copy(trailerUrl = trailerUrl)
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching game trailer: ${e.message}", e)
                // Continue without trailer
            }
            
            // Fetch screenshots
            try {
                val screenshotResponse = apiService.getGameScreenshots(gameId)
                Log.d(TAG, "Screenshots response for game $gameId: count=${screenshotResponse.count}, results size=${screenshotResponse.results.size}")
                val screenshots = screenshotResponse.results.map { it.image }
                game = game.copy(screenshots = screenshots)
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching game screenshots: ${e.message}", e)
                // Continue with default screenshots from game details
            }
            
            emit(RequestState.Success(game))
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
        try {
            emit(RequestState.Loading)
            
            // Fetch multiple pages in parallel for more results
            val pagesToFetch = listOf(1, 2, 3)
            val fetchPageSize = 40 // Max per page
            
            val allGameDtos = coroutineScope {
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
                deferredResults.awaitAll().flatten()
            }
            
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
            val sortedGames = uniqueGames.sortedByDescending { it.rating }
            
            Log.d(TAG, "Total unique games for studios: ${sortedGames.size} (from ${allGameDtos.size} total fetched)")
            emit(RequestState.Success(sortedGames))
            
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
    ): Flow<RequestState<List<Game>>> =
        safeGameListApiFlow(TAG, "filtered games") {
            val dates = if (startDate != null && endDate != null) {
                DateUtils.formatDateRange(startDate, endDate)
            } else null
            
            Log.d(TAG, "Filtering games: developers=$developerSlugs, genres=$genres, dates=$dates, ordering=$ordering")
            
            apiService.getGames(
                page = page,
                pageSize = pageSize,
                developers = developerSlugs,
                genres = genres,
                dates = dates,
                ordering = ordering ?: "-rating,-added"
            ).results.also {
                Log.d(TAG, "Filtered games result: ${it.size} games")
            }
        }
}
