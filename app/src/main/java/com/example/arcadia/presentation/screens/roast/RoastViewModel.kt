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
import com.example.arcadia.presentation.screens.roast.util.MotionPreferences
import com.example.arcadia.presentation.screens.roast.util.RuneTextGenerator
import com.example.arcadia.data.mapper.RoastResponseMapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

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
    val badgeSaveError: String? = null,

    // Animation state fields
    val streamingText: String = "",
    val isStreaming: Boolean = false,
    val revealPhase: RevealPhase = RevealPhase.HIDDEN,
    val isShaking: Boolean = false,
    val reduceMotion: Boolean = false
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
    private val loadingMessages = MysticalLoadingMessages.messages

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
                        error = null,
                        revealPhase = RevealPhase.COMPLETE
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

        // Use streaming generation
        generateRoastStreaming()
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
        // Shake then generate
        launchWithKey("shake_trigger") {
            if (!state.reduceMotion) {
                state = state.copy(isShaking = true)
                delay(500) // Shake duration
                state = state.copy(isShaking = false)
            }
            generateRoast()
        }
    }

    /**
     * Handles the regenerate button click.
     * Shows confirmation dialog if a roast exists, otherwise generates directly.
     * 
     * Requirements: 3.1, 3.2, 7.2
     */
    fun regenerateRoast() {
        if (state.roast != null) {
            showRegenerateDialog()
        } else {
            // Shake then generate
            launchWithKey("shake_trigger") {
                if (!state.reduceMotion) {
                    state = state.copy(isShaking = true)
                    delay(500) // Shake duration
                    state = state.copy(isShaking = false)
                }
                generateRoast()
            }
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

    /**
     * Checks for reduced motion preferences.
     * Should be called from the UI.
     * 
     * Requirements: 12.1, 12.2, 12.3
     */
    fun checkMotionPreferences(context: android.content.Context) {
        val isReduced = MotionPreferences.isReduceMotionEnabled(context)
        if (state.reduceMotion != isReduced) {
            state = state.copy(reduceMotion = isReduced)
        }
    }

    /**
     * Generates a roast using streaming.
     * 
     * Requirements: 3.1, 3.4
     */
    private fun generateRoastStreaming() {
        val stats = currentStats ?: return
        
        launchWithKey("generate_roast_streaming") {
            state = state.copy(
                isLoading = true,
                isStreaming = true,
                streamingText = "",
                revealPhase = RevealPhase.STREAMING,
                loadingMessage = loadingMessages.random(),
                error = null
            )

            // Start rotating loading messages
            val messageJob = launchWithKey("loading_messages") {
                while (true) {
                    delay(RevealTiming.MESSAGE_ROTATION_INTERVAL_MS)
                    state = state.copy(loadingMessage = loadingMessages.random())
                }
            }

            val fullTextBuilder = StringBuilder()

            try {
                aiRepository.generateRoastStreaming(stats)
                    .onEach { chunk ->
                        fullTextBuilder.append(chunk)
                        state = state.copy(streamingText = fullTextBuilder.toString())
                    }
                    .collect()

                // Streaming complete
                val fullText = fullTextBuilder.toString()
                val parseResult = RoastResponseMapper.parseRoastResponse(fullText)
                
                parseResult.fold(
                    onSuccess = { roast ->
                        val generatedAt = System.currentTimeMillis()
                        
                        if (targetUserId == null) {
                            roastRepository.saveRoast(roast)
                        }

                        // Update roast data but keep streaming/loading true until reveal starts
                        // This prevents a flash of the loading state or blank screen
                        state = state.copy(
                            roast = roast,
                            generatedAt = generatedAt,
                            error = null
                        )
                        
                        // Start reveal sequence
                        startRevealSequence()
                        
                        // Generate badges
                        generateBadges(stats)
                    },
                    onFailure = { error ->
                        state = state.copy(
                            isLoading = false,
                            isStreaming = false,
                            error = "The oracle spoke in riddles. (Parsing error)"
                        )
                    }
                )

            } catch (e: Exception) {
                state = state.copy(
                    isLoading = false,
                    isStreaming = false,
                    error = e.message ?: "The oracle was silent."
                )
            } finally {
                cancelJob("loading_messages")
            }
        }
    }

    /**
     * Orchestrates the reveal animation sequence.
     * 
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7
     */
    private fun startRevealSequence() {
        launchWithKey("reveal_sequence") {
            // If reduce motion is on, skip animations
            if (state.reduceMotion) {
                state = state.copy(
                    revealPhase = RevealPhase.COMPLETE,
                    isLoading = false,
                    isStreaming = false
                )
                return@launchWithKey
            }

            // Phase 1: Title (Immediate)
            // Set isLoading = false so UI switches to ResultCard
            state = state.copy(
                revealPhase = RevealPhase.REVEALING_TITLE,
                isLoading = false,
                isStreaming = false
            )
            
            // Phase 2: Headline
            delay(RevealTiming.HEADLINE_DELAY_MS)
            state = state.copy(revealPhase = RevealPhase.REVEALING_HEADLINE)
            
            // Phase 3: Could Have List
            delay(RevealTiming.COULD_HAVE_DELAY_MS - RevealTiming.HEADLINE_DELAY_MS)
            state = state.copy(revealPhase = RevealPhase.REVEALING_COULD_HAVE)
            
            // Phase 4: Prediction
            delay(RevealTiming.PREDICTION_DELAY_MS - RevealTiming.COULD_HAVE_DELAY_MS)
            state = state.copy(revealPhase = RevealPhase.REVEALING_PREDICTION)
            
            // Phase 5: Wholesome
            delay(RevealTiming.WHOLESOME_DELAY_MS - RevealTiming.PREDICTION_DELAY_MS)
            state = state.copy(revealPhase = RevealPhase.REVEALING_WHOLESOME)
            
            // Phase 6: Complete (Buttons)
            delay(RevealTiming.BUTTONS_DELAY_MS - RevealTiming.WHOLESOME_DELAY_MS)
            state = state.copy(
                revealPhase = RevealPhase.COMPLETE
            )
        }
    }
}
