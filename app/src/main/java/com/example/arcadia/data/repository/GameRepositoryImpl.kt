package com.example.arcadia.data.repository

import android.util.Log
import com.example.arcadia.data.remote.RawgApiService
import com.example.arcadia.data.remote.mapper.toGame
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GameRepositoryImpl(
    private val apiService: RawgApiService
) : GameRepository {
    
    private val TAG = "GameRepository"
    
    override fun getPopularGames(page: Int, pageSize: Int): Flow<RequestState<List<Game>>> = flow {
        try {
            emit(RequestState.Loading)
            
            // Get date range for current year
            val now = LocalDate.now()
            val startOfYear = now.withDayOfYear(1)
            val endOfYear = now.withMonth(12).withDayOfMonth(31)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val dates = "${startOfYear.format(formatter)},${endOfYear.format(formatter)}"
            
            val response = apiService.getGames(
                page = page,
                pageSize = pageSize,
                dates = dates,
                ordering = "-rating,-added" // Order by rating and number of people who added it
            )
            
            val games = response.results.map { it.toGame() }
            emit(RequestState.Success(games))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching popular games: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch popular games: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getUpcomingGames(page: Int, pageSize: Int): Flow<RequestState<List<Game>>> = flow {
        try {
            emit(RequestState.Loading)
            
            // Get date range for upcoming games (today to 1 year from now)
            val today = LocalDate.now()
            val nextYear = today.plusYears(1)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val dates = "${today.format(formatter)},${nextYear.format(formatter)}"
            
            val response = apiService.getGames(
                page = page,
                pageSize = pageSize,
                dates = dates,
                ordering = "released" // Order by release date (nearest first)
            )
            
            val games = response.results.map { it.toGame() }
            emit(RequestState.Success(games))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching upcoming games: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch upcoming games: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getNewReleases(page: Int, pageSize: Int): Flow<RequestState<List<Game>>> = flow {
        try {
            emit(RequestState.Loading)
            
            // Get date range for new releases (last 60 days)
            val today = LocalDate.now()
            val twoMonthsAgo = today.minusDays(60)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val dates = "${twoMonthsAgo.format(formatter)},${today.format(formatter)}"
            
            val response = apiService.getGames(
                page = page,
                pageSize = pageSize,
                dates = dates,
                ordering = "-released,-rating" // Order by release date (newest first) and rating
            )
            
            val games = response.results.map { it.toGame() }
            emit(RequestState.Success(games))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching new releases: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch new releases: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getGamesByGenre(genreId: Int, page: Int, pageSize: Int): Flow<RequestState<List<Game>>> = flow {
        try {
            emit(RequestState.Loading)
            
            val response = apiService.getGames(
                page = page,
                pageSize = pageSize,
                genres = genreId.toString(),
                ordering = "-rating"
            )
            
            val games = response.results.map { it.toGame() }
            emit(RequestState.Success(games))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching games by genre: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch games by genre: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getRecommendedGames(tags: String, page: Int, pageSize: Int): Flow<RequestState<List<Game>>> = flow {
        try {
            emit(RequestState.Loading)
            
            // Get date range for last 5 years
            val today = LocalDate.now()
            val fiveYearsAgo = today.minusYears(5)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val dates = "${fiveYearsAgo.format(formatter)},${today.format(formatter)}"
            
            val response = apiService.getGames(
                page = page,
                pageSize = pageSize,
                tags = tags,
                dates = dates,
                ordering = "-rating,-added"
            )
            
            val games = response.results.map { it.toGame() }
            emit(RequestState.Success(games))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recommended games: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch recommended games: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun searchGames(query: String, page: Int, pageSize: Int): Flow<RequestState<List<Game>>> = flow {
        try {
            emit(RequestState.Loading)
            
            val response = apiService.getGames(
                page = page,
                pageSize = pageSize,
                search = query,
                ordering = "-rating,-added"
            )
            
            val games = response.results.map { it.toGame() }
            emit(RequestState.Success(games))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error searching games: ${e.message}", e)
            emit(RequestState.Error("Failed to search games: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    override fun getGameDetails(gameId: Int): Flow<RequestState<Game>> = flow {
        try {
            emit(RequestState.Loading)
            val dto = apiService.getGameDetails(gameId)
            emit(RequestState.Success(dto.toGame()))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching game details: ${e.message}", e)
            emit(RequestState.Error("Failed to fetch game details: ${e.message}"))
        }
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
}
