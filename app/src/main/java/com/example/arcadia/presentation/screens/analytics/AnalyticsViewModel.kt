package com.example.arcadia.presentation.screens.analytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.AIError
import com.example.arcadia.presentation.base.BaseViewModel
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.model.ai.GameInsights
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.AIRepository
import com.example.arcadia.domain.repository.SortOrder
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar

data class AnalyticsState(
    val isLoading: Boolean = true,
    val totalGames: Int = 0,
    val completedGames: Int = 0,
    val droppedGames: Int = 0,
    val hoursPlayed: Int = 0,
    val completionRate: Float = 0f,
    val topGenres: List<Pair<String, Int>> = emptyList(),
    val topPlatforms: List<Pair<String, Int>> = emptyList(),
    val averageRating: Float = 0f,
    val gamingPersonality: GamingPersonality = GamingPersonality.Explorer,
    val genreRatingAnalysis: List<GenreRatingStats> = emptyList(),
    val gamesAddedByYear: List<Pair<String, Int>> = emptyList(),
    val recentTrend: String = "",
    val aiInsights: GameInsights? = null,
    val isLoadingInsights: Boolean = false,
    val insightsError: String? = null,
    val isInsightsErrorRetryable: Boolean = false,
    // Streaming support
    val streamingText: String? = null,
    val isStreaming: Boolean = false
)

data class GenreRatingStats(
    val genre: String,
    val avgRating: Float,
    val count: Int
)

enum class GamingPersonality(val title: String, val description: String) {
    Completionist("The Completionist", "You finish almost everything you start. Dedication is your middle name."),
    Explorer("The Explorer", "You love trying new things across many genres. Variety is the spice of life."),
    Specialist("The Specialist", "You know what you like and stick to it. A master of your favorite genres."),
    Socialite("The Socialite", "You prefer multiplayer experiences and shared worlds."), // Hard to detect without multiplayer tags
    Casual("The Casual Gamer", "You play for fun when you can. No pressure, just vibes."),
    Hardcore("The Hardcore", "High hours, tough genres, high completion. You breathe gaming.")
}

class AnalyticsViewModel(
    private val gameListRepository: GameListRepository,
    private val aiRepository: AIRepository
) : BaseViewModel() {

    var state by mutableStateOf(AnalyticsState())
        private set
    
    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        launchWithKey("load_analytics") {
            state = state.copy(isLoading = true)
            // Fetch all games
            gameListRepository.getGameList(SortOrder.TITLE_A_Z).collect { result ->
                if (result is RequestState.Success) {
                    calculateStats(result.data)
                    // Load AI insights with streaming after basic stats are calculated
                    loadAIInsightsStreaming(result.data)
                } else if (result is RequestState.Error) {
                    state = state.copy(isLoading = false)
                }
            }
        }
    }

    /**
     * Load AI insights using streaming for better UX.
     * Shows partial results as they are generated.
     */
    fun loadAIInsightsStreaming(games: List<GameListEntry>) {
        if (games.isEmpty()) {
            state = state.copy(
                isLoadingInsights = false,
                insightsError = null,
                isStreaming = false
            )
            return
        }
        
        launchWithKey("ai_insights") {
            state = state.copy(
                isLoadingInsights = true, 
                insightsError = null,
                isStreaming = true,
                streamingText = null
            )

            aiRepository.analyzeGamingProfileStreaming(games).collect { streamingInsights ->
                if (streamingInsights.isComplete) {
                    // Streaming complete
                    state = state.copy(
                        aiInsights = streamingInsights.parsedInsights,
                        isLoadingInsights = false,
                        isStreaming = false,
                        streamingText = null,
                        insightsError = if (streamingInsights.parsedInsights == null && 
                                           streamingInsights.partialText.startsWith("Error:")) {
                            streamingInsights.partialText.removePrefix("Error: ")
                        } else null
                    )
                } else {
                    // Update with partial text for live preview
                    state = state.copy(
                        streamingText = streamingInsights.partialText
                    )
                }
            }
        }
    }

    /**
     * Legacy non-streaming method - kept for fallback
     */
    fun loadAIInsights(games: List<GameListEntry>) {
        if (games.isEmpty()) {
            state = state.copy(
                isLoadingInsights = false,
                insightsError = null
            )
            return
        }

        launchWithKey("ai_insights") {
            state = state.copy(isLoadingInsights = true, insightsError = null)

            val result = aiRepository.analyzeGamingProfile(games)

            result.onSuccess { insights ->
                state = state.copy(
                    aiInsights = insights,
                    isLoadingInsights = false,
                    insightsError = null,
                    isInsightsErrorRetryable = false
                )
            }.onFailure { error ->
                val aiError = if (error is AIError) error else AIError.from(error)
                state = state.copy(
                    isLoadingInsights = false,
                    insightsError = aiError.message ?: "Failed to generate insights",
                    isInsightsErrorRetryable = with(AIError.Companion) { aiError.isRetryable() }
                )
            }
        }
    }

    fun retryLoadInsights() {
        launchWithKey("retry_insights") {
            gameListRepository.getGameList(SortOrder.TITLE_A_Z).collect { result ->
                if (result is RequestState.Success) {
                    loadAIInsightsStreaming(result.data)
                }
            }
        }
    }

    private fun calculateStats(games: List<GameListEntry>) {
        if (games.isEmpty()) {
            state = state.copy(isLoading = false)
            return
        }

        val totalGames = games.size
        val completed = games.count { it.status == GameStatus.FINISHED }
        val dropped = games.count { it.status == GameStatus.DROPPED }
        val hours = games.sumOf { it.hoursPlayed }
        
        // Completion Rate: Finished / (Finished + Playing + Dropped + OnHold) -> Excluding "Want"
        val playedGamesCount = games.count { it.status != GameStatus.WANT }
        val completionRate = if (playedGamesCount > 0) {
            (completed.toFloat() / playedGamesCount.toFloat()) * 100f
        } else 0f

        // Top Genres
        val genreCounts = games.flatMap { it.genres }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(6)

        // Top Platforms
        val platformCounts = games.flatMap { it.platforms }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        // Avg Rating
        val ratedGames = games.filter { it.rating != null && it.rating!! > 0 }
        val avgRating = if (ratedGames.isNotEmpty()) {
            ratedGames.map { it.rating!! }.average().toFloat()
        } else 0f

        // Genre Rating Analysis (Min 2 games to be significant)
        val genreRatings = games.flatMap { game -> 
                game.genres.map { genre -> genre to game.rating } 
            }
            .filter { it.second != null && it.second!! > 0 }
            .groupBy { it.first }
            .map { (genre, ratings) ->
                GenreRatingStats(
                    genre = genre,
                    avgRating = ratings.mapNotNull { it.second }.average().toFloat(),
                    count = ratings.size
                )
            }
            .filter { it.count >= 2 }
            .sortedByDescending { it.avgRating }
            .take(5)

        // Games Added By Year
        val calendar = Calendar.getInstance()
        val gamesByYear = games.groupBy { 
            calendar.timeInMillis = it.addedAt
            calendar.get(Calendar.YEAR).toString()
        }.mapValues { it.value.size }.toList().sortedBy { it.first }

        // Determine Personality
        val personality = determinePersonality(completed, playedGamesCount, genreCounts.size, hours)

        // Trend
        val trend = if (genreCounts.isNotEmpty()) {
             "You've been playing a lot of ${genreCounts.first().first} games recently."
        } else "Start playing to see trends!"

        state = state.copy(
            isLoading = false,
            totalGames = totalGames,
            completedGames = completed,
            droppedGames = dropped,
            hoursPlayed = hours,
            completionRate = completionRate,
            topGenres = genreCounts,
            topPlatforms = platformCounts,
            averageRating = avgRating,
            gamingPersonality = personality,
            genreRatingAnalysis = genreRatings,
            gamesAddedByYear = gamesByYear,
            recentTrend = trend
        )
    }

    private fun determinePersonality(
        completed: Int,
        playedCount: Int,
        uniqueGenres: Int,
        totalHours: Int
    ): GamingPersonality {
        if (playedCount == 0) return GamingPersonality.Casual

        val completionRatio = completed.toFloat() / playedCount.toFloat()
        
        return when {
            completionRatio > 0.8f && totalHours > 50 -> GamingPersonality.Completionist
            uniqueGenres > 8 -> GamingPersonality.Explorer
            totalHours > 200 -> GamingPersonality.Hardcore
            uniqueGenres <= 3 && playedCount > 5 -> GamingPersonality.Specialist
            else -> GamingPersonality.Casual
        }
    }
}
