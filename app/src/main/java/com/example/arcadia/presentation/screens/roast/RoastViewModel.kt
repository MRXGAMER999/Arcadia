package com.example.arcadia.presentation.screens.roast

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.arcadia.domain.model.ai.Badge
import com.example.arcadia.domain.model.ai.RoastInsights
import com.example.arcadia.domain.model.ai.RoastStats
import com.example.arcadia.domain.model.ai.RoastValidation
import com.example.arcadia.domain.repository.AIRepository
import com.example.arcadia.domain.repository.FeaturedBadgesRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.domain.repository.RoastRepository
import com.example.arcadia.domain.usecase.ExtractRoastStatsUseCase
import com.example.arcadia.presentation.base.BaseViewModel
import com.example.arcadia.presentation.screens.analytics.AnalyticsState

/**
 * UI state for the Roast Screen.
 * 
 * Requirements: 2.1, 2.2, 2.3, 13.1
 */
data class RoastScreenState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val roast: RoastInsights? = null,
    val generatedAt: Long? = null,
    val badges: List<Badge> = emptyList(),
    val selectedBadges: List<Badge> = emptyList(),
    val error: String? = null,
    val isRegenerateDialogVisible: Boolean = false,
    val targetUserId: String? = null,
    val hasInsufficientStats: Boolean = false,
    val isSavingBadges: Boolean = false,
    val badgesSaved: Boolean = false,
    val badgeSaveError: String? = null
) {
    companion object {
        const val MAX_FEATURED_BADGES = 3
    }
}


/**
 * ViewModel for the Roast Screen.
 * Handles roast generation, caching, badge generation, and badge selection.
 * 
 * Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 8.2, 8.3, 8.4, 10.4, 12.2, 12.3, 13.1, 13.2, 13.4
 */
class RoastViewModel(
    private val aiRepository: AIRepository,
    private val roastRepository: RoastRepository,
    private val gameListRepository: com.example.arcadia.domain.repository.GameListRepository,
    private val calculateGamingStatsUseCase: com.example.arcadia.domain.usecase.CalculateGamingStatsUseCase,
    private val determineGamingPersonalityUseCase: com.example.arcadia.domain.usecase.DetermineGamingPersonalityUseCase,
    private val extractRoastStatsUseCase: ExtractRoastStatsUseCase,
    private val featuredBadgesRepository: FeaturedBadgesRepository,
    private val gamerRepository: GamerRepository,
    private val targetUserId: String? = null
) : BaseViewModel() {

    var state by mutableStateOf(RoastScreenState(targetUserId = targetUserId))
        private set

    // Fun loading messages that rotate during roast generation
    private val loadingMessages = listOf(
        "Analyzing your gaming sins...",
        "Consulting the roast gods...",
        "Calculating your life choices...",
        "Preparing maximum savagery...",
        "Reviewing your backlog shame...",
        "Generating personalized burns...",
        "Measuring your completion rate tears...",
        "Crafting artisanal roasts..."
    )

    private var currentStats: RoastStats? = null

    init {
        if (targetUserId == null) {
            // Self roast - load cached roast first
            loadCachedRoast()
        }
        // Load stats for roast generation
        loadStats()
    }

    /**
     * Loads the cached roast from local storage.
     * Only applicable for self-roasts (targetUserId == null).
     * 
     * Requirements: 6.3, 13.4
     */
    fun loadCachedRoast() {
        if (targetUserId != null) return // Don't load cache for friend roasts

        launchWithKey("load_cached_roast") {
            roastRepository.getLastRoastWithTimestamp().collect { roastWithTimestamp ->
                if (roastWithTimestamp != null) {
                    state = state.copy(
                        roast = roastWithTimestamp.roast,
                        generatedAt = roastWithTimestamp.generatedAt,
                        error = null
                    )
                }
            }
        }
    }

    /**
     * Loads gaming stats for the current or target user.
     * 
     * Requirements: 4.1, 10.3
     */
    private fun loadStats() {
        launchWithKey("load_stats") {
            val gamesFlow = if (targetUserId != null) {
                gameListRepository.getGameListForUser(targetUserId)
            } else {
                gameListRepository.getGameList()
            }

            gamesFlow.collect { result ->
                when (result) {
                    is com.example.arcadia.util.RequestState.Success -> {
                        val games = result.data
                        val gamingStats = calculateGamingStatsUseCase(games)
                        val personality = determineGamingPersonalityUseCase(gamingStats)

                        // Create AnalyticsState to use with ExtractRoastStatsUseCase
                        val analyticsState = AnalyticsState(
                            totalGames = gamingStats.totalGames,
                            completedGames = gamingStats.completedGames,
                            droppedGames = gamingStats.droppedGames,
                            hoursPlayed = gamingStats.totalHours,
                            completionRate = gamingStats.completionRate,
                            topGenres = gamingStats.topGenres,
                            averageRating = gamingStats.averageRating,
                            gamingPersonality = personality
                        )

                        currentStats = extractRoastStatsUseCase(analyticsState)

                        // Check if stats are sufficient
                        val hasInsufficientStats = currentStats?.let { 
                            RoastValidation.hasInsufficientStats(it) 
                        } ?: true

                        state = state.copy(hasInsufficientStats = hasInsufficientStats)
                    }
                    is com.example.arcadia.util.RequestState.Error -> {
                        state = state.copy(
                            error = result.message,
                            isLoading = false
                        )
                    }
                    is com.example.arcadia.util.RequestState.Loading -> {
                        // Stats are loading, wait
                    }
                    is com.example.arcadia.util.RequestState.Idle -> {
                        // Initial state, wait
                    }
                }
            }
        }
    }

    /**
     * Generates a new roast using AI.
     * For self-roasts, saves to local storage.
     * For friend roasts, does not save locally.
     * 
     * Requirements: 2.1, 2.2, 4.1, 10.4
     */
    fun generateRoast() {
        val stats = currentStats
        if (stats == null) {
            state = state.copy(error = "Unable to load gaming stats. Please try again.")
            return
        }

        if (RoastValidation.hasInsufficientStats(stats)) {
            state = state.copy(
                hasInsufficientStats = true,
                error = "Add more games to your library first! You need at least ${RoastValidation.MIN_GAMES} games and ${RoastValidation.MIN_HOURS} hours played."
            )
            return
        }

        launchWithKey("generate_roast") {
            state = state.copy(
                isLoading = true,
                loadingMessage = loadingMessages.random(),
                error = null
            )

            // Start rotating loading messages
            val messageJob = launchWithKey("loading_messages") {
                while (true) {
                    kotlinx.coroutines.delay(2000)
                    state = state.copy(loadingMessage = loadingMessages.random())
                }
            }

            try {
                // Generate roast
                val roastResult = aiRepository.generateRoast(stats)
                
                roastResult.onSuccess { roast ->
                    val generatedAt = System.currentTimeMillis()
                    
                    // Save to local storage only for self-roasts (Requirement 10.4)
                    if (targetUserId == null) {
                        roastRepository.saveRoast(roast)
                    }

                    state = state.copy(
                        isLoading = false,
                        roast = roast,
                        generatedAt = generatedAt,
                        error = null
                    )

                    // Generate badges after roast
                    generateBadges(stats)
                }.onFailure { error ->
                    state = state.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to generate roast. Please try again."
                    )
                }
            } finally {
                cancelJob("loading_messages")
            }
        }
    }

    /**
     * Generates badges based on gaming stats.
     * 
     * Requirements: 7.1, 7.2
     */
    private suspend fun generateBadges(stats: RoastStats) {
        val badgesResult = aiRepository.generateBadges(stats)
        
        badgesResult.onSuccess { badges ->
            state = state.copy(badges = badges)
        }.onFailure {
            // Badge generation failure is non-critical, just log
        }
    }

    /**
     * Shows the regenerate confirmation dialog.
     * 
     * Requirements: 3.1
     */
    fun showRegenerateDialog() {
        state = state.copy(isRegenerateDialogVisible = true)
    }

    /**
     * Hides the regenerate confirmation dialog.
     * 
     * Requirements: 3.2
     */
    fun hideRegenerateDialog() {
        state = state.copy(isRegenerateDialogVisible = false)
    }

    /**
     * Confirms regeneration and generates a new roast.
     * 
     * Requirements: 3.3
     */
    fun confirmRegenerate() {
        hideRegenerateDialog()
        generateRoast()
    }

    /**
     * Handles the regenerate button click.
     * Shows confirmation dialog if a roast exists, otherwise generates directly.
     * 
     * Requirements: 3.1, 3.2
     */
    fun regenerateRoast() {
        if (state.roast != null) {
            showRegenerateDialog()
        } else {
            generateRoast()
        }
    }

    /**
     * Selects or deselects a badge for featuring.
     * Enforces maximum of 3 featured badges.
     * 
     * Requirements: 8.1, 8.2
     */
    fun selectBadge(badge: Badge) {
        val currentSelected = state.selectedBadges.toMutableList()
        
        if (currentSelected.contains(badge)) {
            // Deselect
            currentSelected.remove(badge)
        } else {
            // Select (if under max)
            if (currentSelected.size < RoastScreenState.MAX_FEATURED_BADGES) {
                currentSelected.add(badge)
            }
        }
        
        state = state.copy(selectedBadges = currentSelected)
    }

    /**
     * Saves the selected badges to the user's profile as featured badges.
     * Only available for self-roasts (not friend roasts).
     * 
     * Requirements: 8.2, 8.3, 8.4
     */
    fun saveFeaturedBadges() {
        // Only allow saving for self-roasts
        if (targetUserId != null) return
        
        val userId = gamerRepository.getCurrentUserId()
        if (userId == null) {
            state = state.copy(badgeSaveError = "Unable to save badges. Please sign in.")
            return
        }
        
        if (state.selectedBadges.isEmpty()) {
            state = state.copy(badgeSaveError = "Please select at least one badge to feature.")
            return
        }
        
        launchWithKey("save_badges") {
            state = state.copy(isSavingBadges = true, badgeSaveError = null)
            
            val result = featuredBadgesRepository.saveFeaturedBadges(userId, state.selectedBadges)
            
            result.onSuccess {
                state = state.copy(
                    isSavingBadges = false,
                    badgesSaved = true,
                    badgeSaveError = null
                )
            }.onFailure { error ->
                state = state.copy(
                    isSavingBadges = false,
                    badgeSaveError = error.message ?: "Failed to save badges. Please try again."
                )
            }
        }
    }

    /**
     * Retries roast generation after an error.
     * 
     * Requirements: 13.2
     */
    fun retry() {
        state = state.copy(error = null)
        generateRoast()
    }
}
