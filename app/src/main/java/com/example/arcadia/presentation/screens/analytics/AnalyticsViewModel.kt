package com.example.arcadia.presentation.screens.analytics

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.arcadia.domain.model.AIError
import com.example.arcadia.presentation.base.BaseViewModel
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.ai.GameInsights
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.AIRepository
import com.example.arcadia.domain.repository.SortOrder
import com.example.arcadia.domain.usecase.CalculateGamingStatsUseCase
import com.example.arcadia.domain.usecase.DetermineGamingPersonalityUseCase
import com.example.arcadia.domain.usecase.GamingPersonality
import com.example.arcadia.domain.usecase.GenreRatingStats
import com.example.arcadia.util.RequestState

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

/**
 * ViewModel for the Analytics screen.
 * Uses CalculateGamingStatsUseCase and DetermineGamingPersonalityUseCase 
 * to avoid duplicating business logic.
 */
class AnalyticsViewModel(
    private val gameListRepository: GameListRepository,
    private val aiRepository: AIRepository,
    private val calculateGamingStatsUseCase: CalculateGamingStatsUseCase,
    private val determineGamingPersonalityUseCase: DetermineGamingPersonalityUseCase
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

    /**
     * Calculates statistics using CalculateGamingStatsUseCase and 
     * DetermineGamingPersonalityUseCase to avoid duplicating business logic.
     */
    private fun calculateStats(games: List<GameListEntry>) {
        // Delegate to use case for stats calculation
        val stats = calculateGamingStatsUseCase(games)
        
        // Delegate to use case for personality determination
        val personality = determineGamingPersonalityUseCase(stats)

        state = state.copy(
            isLoading = false,
            totalGames = stats.totalGames,
            completedGames = stats.completedGames,
            droppedGames = stats.droppedGames,
            hoursPlayed = stats.totalHours,
            completionRate = stats.completionRate,
            topGenres = stats.topGenres,
            topPlatforms = stats.topPlatforms,
            averageRating = stats.averageRating,
            gamingPersonality = personality,
            genreRatingAnalysis = stats.genreRatingAnalysis,
            gamesAddedByYear = stats.gamesAddedByYear,
            recentTrend = stats.recentTrend
        )
    }
}
