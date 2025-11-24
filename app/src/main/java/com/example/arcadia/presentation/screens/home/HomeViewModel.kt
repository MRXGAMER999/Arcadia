package com.example.arcadia.presentation.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadia.data.repository.StudioExpansionRepository
import com.example.arcadia.domain.model.DiscoveryFilterState
import com.example.arcadia.domain.model.DiscoverySortOrder
import com.example.arcadia.domain.model.DiscoverySortType
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.model.ReleaseTimeframe
import com.example.arcadia.domain.model.StudioFilterState
import com.example.arcadia.domain.model.StudioFilterType
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.domain.usecase.ParallelGameFilter // Kept for DI, may be used for future local filtering
import com.example.arcadia.util.PreferencesManager
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

sealed class AddToLibraryState {
    data object Idle : AddToLibraryState()
    data class Loading(val gameId: Int) : AddToLibraryState()
    data class Success(val message: String, val gameId: Int) : AddToLibraryState()
    data class Error(val message: String, val gameId: Int) : AddToLibraryState()
}

data class HomeScreenState(
    val popularGames: RequestState<List<Game>> = RequestState.Idle,
    val upcomingGames: RequestState<List<Game>> = RequestState.Idle,
    val recommendedGames: RequestState<List<Game>> = RequestState.Idle,
    val newReleases: RequestState<List<Game>> = RequestState.Idle,
    val gamesInLibrary: Set<Int> = emptySet(), // Track rawgIds of games in library (now merged into gameListIds logic)
    val animatingGameIds: Set<Int> = emptySet(), // Games currently animating out
    val addToLibraryState: AddToLibraryState = AddToLibraryState.Idle
)

class HomeViewModel(
    private val gameRepository: GameRepository,
    private val gameListRepository: GameListRepository,
    private val studioExpansionRepository: StudioExpansionRepository,
    private val preferencesManager: PreferencesManager,
    @Suppress("unused") private val parallelGameFilter: ParallelGameFilter // Kept for potential future local filtering
) : ViewModel() {
    
    companion object {
        private const val TAG = "HomeViewModel"
        
        // Timing constants
        private const val ANIMATION_DURATION_MS = 600L
        private const val NOTIFICATION_DISPLAY_MS = 1500L
        private const val ERROR_RESET_DELAY_MS = 3000L
        private const val DUPLICATE_CHECK_DELAY_MS = 2000L
        private const val FILTER_DEBOUNCE_MS = 150L
        private const val DEVELOPER_SEARCH_DEBOUNCE_MS = 300L
        
        // Pagination constants
        private const val DEFAULT_PAGE_SIZE = 10
        private const val RECOMMENDATION_PAGE_SIZE = 40
        private const val MIN_RECOMMENDATIONS_COUNT = 15
        
        // API constants
        private const val DEFAULT_RECOMMENDATION_TAGS = "singleplayer,multiplayer"
    }
    
    var screenState by mutableStateOf(HomeScreenState())
        private set
    
    // Studio filter state (legacy - kept for compatibility)
    var studioFilterState by mutableStateOf(StudioFilterState())
        private set
    
    // Discovery filter state (new unified filter)
    var discoveryFilterState by mutableStateOf(DiscoveryFilterState())
        private set
    
    // Track if discovery filter dialog is shown
    var showDiscoveryFilterDialog by mutableStateOf(false)
        private set
    
    // Cache unfiltered recommendations for reapplying filters
    private var lastRecommended: List<Game> = emptyList()
    private var recommendationPage = 1
    private var isLoadingMoreRecommendations = false
    private var lastFilteredRecommendations: List<Game> = emptyList()
    private var lastExcludeIds: Set<Int> = emptySet()
    
    // Cache for discovery filter results
    private var lastDiscoveryResults: List<Game> = emptyList()
    private var discoveryFilterPage = 1
    private var isLoadingMoreDiscovery = false

    // Job management to prevent duplicate flows
    private var popularGamesJob: Job? = null
    private var upcomingGamesJob: Job? = null
    private var recommendedGamesJob: Job? = null
    private var newReleasesJob: Job? = null
    private var gameListJob: Job? = null
    private var filterJob: Job? = null

    // Thread-safe set to track games currently being added (prevents duplicate additions)
    private val gamesBeingAdded: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    init {
        // Load saved discovery filter preferences
        loadSavedDiscoveryPreferences()
        // Start real-time flows only once
        loadGameListIds()
        // Load initial data - but skip recommendations if filters are active
        loadInitialData()
    }
    
    private fun loadInitialData() {
        loadPopularGames()
        loadUpcomingGames()
        loadNewReleases()
        
        // If discovery filters are active, apply them instead of loading default recommendations
        if (discoveryFilterState.hasActiveFilters) {
            applyDiscoveryFilters()
        } else {
            loadRecommendedGames()
        }
    }
    
    private fun loadSavedDiscoveryPreferences() {
        discoveryFilterState = discoveryFilterState.copy(
            sortType = preferencesManager.getDiscoverySortType(),
            sortOrder = preferencesManager.getDiscoverySortOrder(),
            selectedGenres = preferencesManager.getDiscoveryGenres(),
            releaseTimeframe = preferencesManager.getDiscoveryTimeframe(),
            selectedDevelopers = preferencesManager.getDiscoveryDevelopers()
        )
    }
    
    private fun saveDiscoveryPreferences() {
        preferencesManager.saveDiscoverySortType(discoveryFilterState.sortType)
        preferencesManager.saveDiscoverySortOrder(discoveryFilterState.sortOrder)
        preferencesManager.saveDiscoveryGenres(discoveryFilterState.selectedGenres)
        preferencesManager.saveDiscoveryTimeframe(discoveryFilterState.releaseTimeframe)
        preferencesManager.saveDiscoveryDevelopers(discoveryFilterState.selectedDevelopers)
    }
    
    fun loadAllData() {
        // Reload one-shot data (no duplicate flows)
        loadPopularGames()
        loadUpcomingGames()
        loadNewReleases()
        
        // If discovery filters are active, apply them instead of loading default recommendations
        if (discoveryFilterState.hasActiveFilters) {
            applyDiscoveryFilters()
        } else {
            loadRecommendedGames()
        }
    }
    
    private fun loadGameListIds() {
        gameListJob?.cancel()
        gameListJob = viewModelScope.launch {
            gameListRepository.getGameList().collect { state ->
                if (state is RequestState.Success) {
                    val gameIds = state.data.map { it.rawgId }.toSet()
                    screenState = screenState.copy(gamesInLibrary = gameIds)
                    // Reapply appropriate filter when game list changes (local filtering only)
                    if (discoveryFilterState.hasActiveFilters) {
                        // Re-filter cached discovery results locally
                        refilterDiscoveryResults()
                    } else {
                        applyRecommendationFilter()
                    }
                }
            }
        }
    }
    
    /**
     * Re-filter cached discovery results without making a new API call.
     * Used when library changes to update the displayed list.
     */
    private fun refilterDiscoveryResults() {
        if (lastDiscoveryResults.isEmpty()) {
            // No cached results, need to fetch
            applyDiscoveryFilters()
            return
        }
        
        val filtered = lastDiscoveryResults.filter { it.id !in screenState.gamesInLibrary }
        android.util.Log.d(TAG, "Refiltered discovery: ${lastDiscoveryResults.size} -> ${filtered.size} games")
        screenState = screenState.copy(recommendedGames = RequestState.Success(filtered))
        
        // Auto-fetch more if list is getting too short
        if (filtered.size < MIN_RECOMMENDATIONS_COUNT && !isLoadingMoreDiscovery) {
            loadMoreDiscoveryResults()
        }
    }
    
    private fun loadPopularGames() {
        popularGamesJob?.cancel()
        popularGamesJob = viewModelScope.launch {
            gameRepository.getPopularGames(page = 1, pageSize = DEFAULT_PAGE_SIZE).collect { state ->
                screenState = screenState.copy(popularGames = state)
            }
        }
    }
    
    private fun loadUpcomingGames() {
        upcomingGamesJob?.cancel()
        upcomingGamesJob = viewModelScope.launch {
            gameRepository.getUpcomingGames(page = 1, pageSize = DEFAULT_PAGE_SIZE).collect { state ->
                screenState = screenState.copy(upcomingGames = state)
            }
        }
    }
    
    private fun loadRecommendedGames() {
        recommendedGamesJob?.cancel()
        recommendedGamesJob = viewModelScope.launch {
            gameRepository.getRecommendedGames(
                tags = DEFAULT_RECOMMENDATION_TAGS, 
                page = 1, 
                pageSize = RECOMMENDATION_PAGE_SIZE
            ).collect { state ->
                when (state) {
                    is RequestState.Success -> {
                        android.util.Log.d(TAG, "Recommendations loaded: ${state.data.size} games")
                        lastRecommended = state.data
                        recommendationPage = 1
                        applyRecommendationFilter()
                        ensureMinimumRecommendations()
                    }
                    is RequestState.Loading -> {
                        android.util.Log.d(TAG, "Loading recommendations...")
                        screenState = screenState.copy(recommendedGames = state)
                    }
                    is RequestState.Error -> {
                        android.util.Log.e(TAG, "Error loading recommendations: ${state.message}")
                        screenState = screenState.copy(recommendedGames = state)
                    }
                    else -> {
                        screenState = screenState.copy(recommendedGames = state)
                    }
                }
            }
        }
    }
    
    private fun ensureMinimumRecommendations(minCount: Int = MIN_RECOMMENDATIONS_COUNT) {
        viewModelScope.launch {
            val current = screenState.recommendedGames
            if (current is RequestState.Success && current.data.size < minCount && !isLoadingMoreRecommendations) {
                loadMoreRecommendations()
            }
        }
    }
    
    fun loadMoreRecommendations() {
        if (isLoadingMoreRecommendations) return
        
        viewModelScope.launch {
            isLoadingMoreRecommendations = true
            try {
                recommendationPage++
                gameRepository.getRecommendedGames(
                    tags = DEFAULT_RECOMMENDATION_TAGS, 
                    page = recommendationPage, 
                    pageSize = RECOMMENDATION_PAGE_SIZE
                ).collect { state ->
                    if (state is RequestState.Success) {
                        lastRecommended = lastRecommended + state.data
                        applyRecommendationFilter()
                    }
                    isLoadingMoreRecommendations = false
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading more recommendations", e)
                isLoadingMoreRecommendations = false
            }
        }
    }
    
    private fun applyRecommendationFilter() {
        // Skip if discovery filters are active - they handle their own filtering
        if (discoveryFilterState.hasActiveFilters) {
            android.util.Log.d("HomeViewModel", "Skipping recommendation filter - discovery filters are active")
            return
        }
        
        // If no cached recommendations, reload them
        if (lastRecommended.isEmpty()) {
            android.util.Log.d("HomeViewModel", "No cached recommendations, reloading...")
            loadRecommendedGames()
            return
        }

        // Exclude games in library but keep games that are currently animating out
        val excludeIds = screenState.gamesInLibrary - screenState.animatingGameIds

        // Only refilter if exclude set changed AND we have filtered at least once
        // This prevents skipping the initial filter when both sets are empty
        if (excludeIds == lastExcludeIds && lastFilteredRecommendations.isNotEmpty()) {
            android.util.Log.d("HomeViewModel", "Filter skipped: no changes (excludeIds=${excludeIds.size})")
            // Still update state to ensure UI shows current filtered list
            screenState = screenState.copy(recommendedGames = RequestState.Success(lastFilteredRecommendations))
            return
        }

        lastExcludeIds = excludeIds
        val filtered = lastRecommended.filter { it.id !in excludeIds }
        lastFilteredRecommendations = filtered
        android.util.Log.d("HomeViewModel", "Filter applied: ${lastRecommended.size} -> ${filtered.size} games (excluded ${excludeIds.size})")
        screenState = screenState.copy(recommendedGames = RequestState.Success(filtered))
        
        // Auto-backfill if filtered results are too few
        ensureMinimumRecommendations()
    }
    
    private fun loadNewReleases() {
        newReleasesJob?.cancel()
        newReleasesJob = viewModelScope.launch {
            gameRepository.getNewReleases(page = 1, pageSize = DEFAULT_PAGE_SIZE).collect { state ->
                screenState = screenState.copy(newReleases = state)
            }
        }
    }

    fun retry() {
        loadAllData()
    }
    
    fun isGameInLibrary(gameId: Int): Boolean {
        return screenState.gamesInLibrary.contains(gameId)
    }
    
    fun addGameToLibrary(game: Game) {
        viewModelScope.launch {
            // Thread-safe check: add returns false if already present
            if (!gamesBeingAdded.add(game.id)) {
                android.util.Log.d(TAG, "Game ${game.id} is already being added, skipping")
                return@launch
            }

            try {
                // Check if game is already in library
                if (isGameInLibrary(game.id)) {
                    screenState = screenState.copy(
                        addToLibraryState = AddToLibraryState.Error(
                            message = "${game.name} is already in your library",
                            gameId = game.id
                        )
                    )
                    delay(DUPLICATE_CHECK_DELAY_MS)
                    screenState = screenState.copy(addToLibraryState = AddToLibraryState.Idle)
                    return@launch
                }

                // Set loading state
                screenState = screenState.copy(
                    addToLibraryState = AddToLibraryState.Loading(gameId = game.id),
                    animatingGameIds = screenState.animatingGameIds + game.id
                )

                try {
                    // Add to library - local state is kept in sync via real-time flow
                    when (val result = gameListRepository.addGameToList(game, GameStatus.WANT)) {
                        is RequestState.Success -> {
                            screenState = screenState.copy(
                                addToLibraryState = AddToLibraryState.Success(
                                    message = "${game.name} added to library!",
                                    gameId = game.id
                                )
                            )

                            // Wait for animation to complete
                            delay(ANIMATION_DURATION_MS)

                            // Remove from animating set to allow filtering
                            screenState = screenState.copy(
                                animatingGameIds = screenState.animatingGameIds - game.id
                            )

                            // Reapply filter to remove the game from the list
                            applyRecommendationFilter()

                            // Auto-reset success state after showing notification
                            delay(NOTIFICATION_DISPLAY_MS)
                            screenState = screenState.copy(addToLibraryState = AddToLibraryState.Idle)
                        }
                        is RequestState.Error -> {
                            screenState = screenState.copy(
                                addToLibraryState = AddToLibraryState.Error(
                                    message = result.message,
                                    gameId = game.id
                                ),
                                animatingGameIds = screenState.animatingGameIds - game.id
                            )
                            delay(ERROR_RESET_DELAY_MS)
                            screenState = screenState.copy(addToLibraryState = AddToLibraryState.Idle)
                        }
                        else -> {
                            screenState = screenState.copy(
                                addToLibraryState = AddToLibraryState.Error(
                                    message = "Unexpected error occurred",
                                    gameId = game.id
                                ),
                                animatingGameIds = screenState.animatingGameIds - game.id
                            )
                            delay(ERROR_RESET_DELAY_MS)
                            screenState = screenState.copy(addToLibraryState = AddToLibraryState.Idle)
                        }
                    }
                } catch (e: Exception) {
                    screenState = screenState.copy(
                        addToLibraryState = AddToLibraryState.Error(
                            message = e.localizedMessage ?: "An error occurred",
                            gameId = game.id
                        ),
                        animatingGameIds = screenState.animatingGameIds - game.id
                    )
                    delay(ERROR_RESET_DELAY_MS)
                    screenState = screenState.copy(addToLibraryState = AddToLibraryState.Idle)
                }
            } finally {
                // Thread-safe removal from tracking set
                gamesBeingAdded.remove(game.id)
            }
        }
    }

    fun resetAddToLibraryState() {
        screenState = screenState.copy(addToLibraryState = AddToLibraryState.Idle)
    }

    // ==================== Studio Filter Methods ====================

    // Separate job for fetching games after filter is set
    private var studioGamesJob: Job? = null

    /**
     * Set studio filter with debouncing and optimistic UI.
     * Fetches games directly from RAWG API using studio slugs.
     */
    fun setStudioFilter(studioName: String) {
        // Optimistic UI: show loading immediately
        studioFilterState = studioFilterState.copy(
            isLoadingExpansion = true,
            selectedParentStudio = studioName,
            error = null
        )

        // Cancel previous filter job
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(FILTER_DEBOUNCE_MS) // Debounce rapid changes

            try {
                // Get expanded studios for UI display
                val expandedStudios = studioExpansionRepository.getExpandedStudios(studioName)

                studioFilterState = studioFilterState.copy(
                    expandedStudios = expandedStudios,
                    selectedStudios = expandedStudios.toMutableSet(),
                    isLoadingExpansion = false
                )

                // Fetch games from API using studio slugs
                fetchGamesForStudioFilter(studioName)
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Coroutine was cancelled (user typed again), ignore
                android.util.Log.d("HomeViewModel", "Studio filter cancelled for: $studioName")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Studio filter setup failed", e)
                studioFilterState = studioFilterState.copy(
                    isLoadingExpansion = false,
                    error = e.message
                )
            }
        }
    }

    // Cache for studio filtered games (for pagination)
    private var studioFilteredGames: MutableList<Game> = mutableListOf()
    private var studioFilterSlugs: String = ""

    /**
     * Fetch games for the current studio filter (initial load).
     * Uses the filterType to determine whether to query developers, publishers, or both.
     */
    private fun fetchGamesForStudioFilter(parentStudio: String) {
        studioGamesJob?.cancel()
        studioGamesJob = viewModelScope.launch {
            screenState = screenState.copy(recommendedGames = RequestState.Loading)
            
            // Reset pagination state
            studioFilteredGames.clear()
            studioFilterState = studioFilterState.copy(
                currentPage = 1,
                hasMorePages = true,
                isLoadingMore = false
            )
            
            try {
                // Get slugs for API filtering
                val slugs = studioExpansionRepository.getStudioSlugs(parentStudio)
                studioFilterSlugs = slugs
                
                // Check if we have valid slugs
                if (slugs.isBlank()) {
                    android.util.Log.w("HomeViewModel", "No slugs found for: $parentStudio")
                    screenState = screenState.copy(
                        recommendedGames = RequestState.Success(emptyList())
                    )
                    studioFilterState = studioFilterState.copy(hasMorePages = false)
                    return@launch
                }
                
                android.util.Log.d("HomeViewModel", "Fetching games for studios: $slugs (type: ${studioFilterState.filterType})")
                
                // Determine which slugs to use based on filter type
                val developerSlugs = when (studioFilterState.filterType) {
                    StudioFilterType.DEVELOPER, StudioFilterType.BOTH -> slugs
                    StudioFilterType.PUBLISHER -> null
                }
                val publisherSlugs = when (studioFilterState.filterType) {
                    StudioFilterType.PUBLISHER, StudioFilterType.BOTH -> slugs
                    StudioFilterType.DEVELOPER -> null
                }
                
                // Fetch games from API using developer/publisher slugs
                gameRepository.getGamesByStudios(
                    developerSlugs = developerSlugs,
                    publisherSlugs = publisherSlugs,
                    page = 1,
                    pageSize = 40
                ).collect { state ->
                    when (state) {
                        is RequestState.Success -> {
                            val filtered = state.data.filter { it.id !in screenState.gamesInLibrary }
                            studioFilteredGames.addAll(filtered)
                            android.util.Log.d("HomeViewModel", "Studio filter: ${state.data.size} games, ${filtered.size} after library filter")
                            screenState = screenState.copy(
                                recommendedGames = RequestState.Success(studioFilteredGames.toList())
                            )
                            // Check if we got fewer games than expected (no more pages)
                            studioFilterState = studioFilterState.copy(
                                hasMorePages = state.data.size >= 40 // If we got less than 40, probably no more
                            )
                        }
                        is RequestState.Error -> {
                            android.util.Log.e("HomeViewModel", "Studio filter error: ${state.message}")
                            screenState = screenState.copy(recommendedGames = state)
                        }
                        is RequestState.Loading -> {
                            screenState = screenState.copy(recommendedGames = state)
                        }
                        else -> {}
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.d("HomeViewModel", "Game fetch cancelled for: $parentStudio")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Studio filter exception", e)
                screenState = screenState.copy(
                    recommendedGames = RequestState.Error("Failed to filter by studio: ${e.message}")
                )
            }
        }
    }

    /**
     * Load more games for the current studio filter (pagination).
     * Called when user scrolls near the end of the list.
     */
    fun loadMoreStudioGames() {
        // Don't load if not in studio filter mode, already loading, or no more pages
        if (!isStudioFilterActive() || studioFilterState.isLoadingMore || !studioFilterState.hasMorePages) {
            return
        }
        
        val parentStudio = studioFilterState.selectedParentStudio ?: return
        
        viewModelScope.launch {
            studioFilterState = studioFilterState.copy(isLoadingMore = true)
            
            try {
                val nextPage = studioFilterState.currentPage + 1
                android.util.Log.d("HomeViewModel", "Loading more studio games, page: $nextPage")
                
                // Determine which slugs to use based on filter type
                val developerSlugs = when (studioFilterState.filterType) {
                    StudioFilterType.DEVELOPER, StudioFilterType.BOTH -> studioFilterSlugs
                    StudioFilterType.PUBLISHER -> null
                }
                val publisherSlugs = when (studioFilterState.filterType) {
                    StudioFilterType.PUBLISHER, StudioFilterType.BOTH -> studioFilterSlugs
                    StudioFilterType.DEVELOPER -> null
                }
                
                gameRepository.getGamesByStudios(
                    developerSlugs = developerSlugs,
                    publisherSlugs = publisherSlugs,
                    page = nextPage,
                    pageSize = 40
                ).collect { state ->
                    when (state) {
                        is RequestState.Success -> {
                            // Filter out games already in library and already in list
                            val existingIds = studioFilteredGames.map { it.id }.toSet()
                            val newGames = state.data.filter { 
                                it.id !in screenState.gamesInLibrary && it.id !in existingIds 
                            }
                            
                            studioFilteredGames.addAll(newGames)
                            android.util.Log.d("HomeViewModel", "Loaded ${newGames.size} more games (total: ${studioFilteredGames.size})")
                            
                            screenState = screenState.copy(
                                recommendedGames = RequestState.Success(studioFilteredGames.toList())
                            )
                            
                            studioFilterState = studioFilterState.copy(
                                currentPage = nextPage,
                                hasMorePages = state.data.size >= 40,
                                isLoadingMore = false
                            )
                        }
                        is RequestState.Error -> {
                            android.util.Log.e("HomeViewModel", "Error loading more: ${state.message}")
                            studioFilterState = studioFilterState.copy(isLoadingMore = false)
                        }
                        else -> {}
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.d("HomeViewModel", "Load more cancelled")
                studioFilterState = studioFilterState.copy(isLoadingMore = false)
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Load more exception", e)
                studioFilterState = studioFilterState.copy(isLoadingMore = false)
            }
        }
    }

    /**
     * Toggle individual subsidiary studio selection.
     */
    fun toggleStudioSelection(studio: String) {
        val current = studioFilterState.selectedStudios.toMutableSet()
        if (studio in current) {
            current.remove(studio)
        } else {
            current.add(studio)
        }
        studioFilterState = studioFilterState.copy(selectedStudios = current)
        studioFilterState.selectedParentStudio?.let { fetchGamesForStudioFilter(it) }
    }

    /**
     * Select all expanded studios.
     */
    fun selectAllStudios() {
        studioFilterState = studioFilterState.copy(
            selectedStudios = studioFilterState.expandedStudios
        )
        studioFilterState.selectedParentStudio?.let { fetchGamesForStudioFilter(it) }
    }

    /**
     * Deselect all studios.
     */
    fun deselectAllStudios() {
        studioFilterState = studioFilterState.copy(selectedStudios = emptySet())
        screenState = screenState.copy(recommendedGames = RequestState.Success(emptyList()))
    }

    /**
     * Set the filter type (developer, publisher, or both).
     */
    fun setStudioFilterType(filterType: StudioFilterType) {
        studioFilterState = studioFilterState.copy(filterType = filterType)
        studioFilterState.selectedParentStudio?.let { fetchGamesForStudioFilter(it) }
    }

    /**
     * Clear studio filter and restore unfiltered recommendations.
     */
    fun clearStudioFilter() {
        // Cancel any ongoing filter jobs
        filterJob?.cancel()
        studioGamesJob?.cancel()
        
        studioFilterState = StudioFilterState()
        applyRecommendationFilter() // Restore unfiltered recommendations
    }

    /**
     * Check if studio filter is active.
     */
    fun isStudioFilterActive(): Boolean {
        return studioFilterState.selectedParentStudio != null
    }

    // ==================== Discovery Filter Methods ====================

    private var discoveryFilterJob: Job? = null
    private var developerSearchJob: Job? = null

    /**
     * Show the discovery filter dialog.
     */
    fun showDiscoveryFilterDialog() {
        showDiscoveryFilterDialog = true
    }

    /**
     * Dismiss the discovery filter dialog.
     */
    fun dismissDiscoveryFilterDialog() {
        showDiscoveryFilterDialog = false
    }

    /**
     * Update the discovery filter state.
     */
    fun updateDiscoveryFilterState(newState: DiscoveryFilterState) {
        discoveryFilterState = newState
    }

    /**
     * Search for developers based on query.
     */
    fun searchDevelopers(query: String) {
        developerSearchJob?.cancel()
        developerSearchJob = viewModelScope.launch {
            discoveryFilterState = discoveryFilterState.copy(isLoadingDevelopers = true)
            delay(DEVELOPER_SEARCH_DEBOUNCE_MS) // Debounce
            
            try {
                // Get expanded studios/developers for the query
                val developers = studioExpansionRepository.getExpandedStudios(query)
                discoveryFilterState = discoveryFilterState.copy(
                    searchResults = developers.toList(),
                    expandedStudios = developers,
                    isLoadingDevelopers = false
                )
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Developer search failed", e)
                discoveryFilterState = discoveryFilterState.copy(
                    isLoadingDevelopers = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Select a developer with its associated sub-studios.
     * When a parent studio is selected (e.g., "Epic Games"), all sub-studios are included.
     */
    fun selectDeveloperWithStudios(developer: String) {
        viewModelScope.launch {
            try {
                // Get sub-studios for the selected developer
                val subStudios = studioExpansionRepository.getExpandedStudios(developer)
                
                val currentSelected = discoveryFilterState.selectedDevelopers.toMutableMap()
                currentSelected[developer] = subStudios - developer // Exclude the parent itself from sub-studios
                
                discoveryFilterState = discoveryFilterState.copy(
                    selectedDevelopers = currentSelected,
                    developerSearchQuery = "",
                    searchResults = emptyList()
                )
                
                android.util.Log.d("HomeViewModel", "Selected $developer with ${subStudios.size - 1} sub-studios")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to get sub-studios for $developer", e)
                // Still add the developer even if sub-studio fetch fails
                val currentSelected = discoveryFilterState.selectedDevelopers.toMutableMap()
                currentSelected[developer] = emptySet()
                
                discoveryFilterState = discoveryFilterState.copy(
                    selectedDevelopers = currentSelected,
                    developerSearchQuery = "",
                    searchResults = emptyList()
                )
            }
        }
    }

    /**
     * Apply discovery filters and fetch games.
     */
    fun applyDiscoveryFilters() {
        dismissDiscoveryFilterDialog()
        
        // Save preferences whenever filters are applied
        saveDiscoveryPreferences()
        
        if (!discoveryFilterState.hasActiveFilters) {
            // No filters active, show regular recommendations
            lastDiscoveryResults = emptyList()
            discoveryFilterPage = 1
            clearStudioFilter()
            applyRecommendationFilter()
            return
        }

        discoveryFilterJob?.cancel()
        discoveryFilterJob = viewModelScope.launch {
            screenState = screenState.copy(recommendedGames = RequestState.Loading)

            try {
                // Build filter parameters
                val developerSlugs = if (discoveryFilterState.selectedDevelopers.isNotEmpty()) {
                    discoveryFilterState.getAllDeveloperSlugs()
                } else null

                val genreSlugs = if (discoveryFilterState.selectedGenres.isNotEmpty()) {
                    discoveryFilterState.selectedGenres.joinToString(",") { 
                        it.lowercase().replace(" ", "-") 
                    }
                } else null

                // Calculate date range based on timeframe
                val (startDate, endDate) = calculateDateRange(discoveryFilterState.releaseTimeframe)

                // Determine ordering based on sort type and order
                val ordering = getApiOrdering(
                    discoveryFilterState.sortType, 
                    discoveryFilterState.sortOrder
                )

                android.util.Log.d("HomeViewModel", "Applying discovery filters: " +
                    "developers=$developerSlugs, genres=$genreSlugs, " +
                    "dates=$startDate..$endDate, ordering=$ordering")

                // Reset pagination for new filter
                discoveryFilterPage = 1

                // Fetch filtered games
                gameRepository.getFilteredGames(
                    developerSlugs = developerSlugs,
                    genres = genreSlugs,
                    startDate = startDate,
                    endDate = endDate,
                    ordering = ordering,
                    page = 1,
                    pageSize = 40
                ).collect { state ->
                    when (state) {
                        is RequestState.Success -> {
                            // Cache unfiltered results
                            lastDiscoveryResults = state.data
                            val filtered = state.data.filter { it.id !in screenState.gamesInLibrary }
                            android.util.Log.d("HomeViewModel", "Discovery filter: ${state.data.size} games, ${filtered.size} after library filter")
                            screenState = screenState.copy(
                                recommendedGames = RequestState.Success(filtered)
                            )
                        }
                        is RequestState.Error -> {
                            android.util.Log.e("HomeViewModel", "Discovery filter error: ${state.message}")
                            screenState = screenState.copy(recommendedGames = state)
                        }
                        is RequestState.Loading -> {
                            screenState = screenState.copy(recommendedGames = state)
                        }
                        else -> {}
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.d("HomeViewModel", "Discovery filter cancelled")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Discovery filter exception", e)
                screenState = screenState.copy(
                    recommendedGames = RequestState.Error("Failed to apply filters: ${e.message}")
                )
            }
        }
    }
    
    /**
     * Load more discovery results when the list is getting short.
     */
    private fun loadMoreDiscoveryResults() {
        if (isLoadingMoreDiscovery || !discoveryFilterState.hasActiveFilters) return
        
        viewModelScope.launch {
            isLoadingMoreDiscovery = true
            try {
                discoveryFilterPage++
                
                val developerSlugs = if (discoveryFilterState.selectedDevelopers.isNotEmpty()) {
                    discoveryFilterState.getAllDeveloperSlugs()
                } else null

                val genreSlugs = if (discoveryFilterState.selectedGenres.isNotEmpty()) {
                    discoveryFilterState.selectedGenres.joinToString(",") { 
                        it.lowercase().replace(" ", "-") 
                    }
                } else null

                val (startDate, endDate) = calculateDateRange(discoveryFilterState.releaseTimeframe)
                val ordering = getApiOrdering(discoveryFilterState.sortType, discoveryFilterState.sortOrder)

                gameRepository.getFilteredGames(
                    developerSlugs = developerSlugs,
                    genres = genreSlugs,
                    startDate = startDate,
                    endDate = endDate,
                    ordering = ordering,
                    page = discoveryFilterPage,
                    pageSize = 40
                ).collect { state ->
                    if (state is RequestState.Success && state.data.isNotEmpty()) {
                        // Add new results to cache
                        lastDiscoveryResults = lastDiscoveryResults + state.data
                        val filtered = lastDiscoveryResults.filter { it.id !in screenState.gamesInLibrary }
                        android.util.Log.d(TAG, "Loaded more discovery: +${state.data.size} games, total filtered: ${filtered.size}")
                        screenState = screenState.copy(
                            recommendedGames = RequestState.Success(filtered)
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading more discovery results", e)
            } finally {
                isLoadingMoreDiscovery = false
            }
        }
    }

    /**
     * Clear all discovery filters.
     */
    fun clearDiscoveryFilters() {
        // Cancel any ongoing filter jobs first
        discoveryFilterJob?.cancel()
        developerSearchJob?.cancel()
        
        discoveryFilterState = DiscoveryFilterState()
        clearStudioFilter()
        
        // Clear discovery cache
        lastDiscoveryResults = emptyList()
        discoveryFilterPage = 1
        
        // Clear saved preferences
        preferencesManager.clearDiscoveryFilters()
        
        // Force reload if we don't have cached data
        if (lastRecommended.isEmpty()) {
            loadRecommendedGames()
        } else {
            applyRecommendationFilter()
        }
    }

    /**
     * Check if discovery filter is active.
     */
    fun isDiscoveryFilterActive(): Boolean {
        return discoveryFilterState.hasActiveFilters
    }

    /**
     * Calculate the date range based on the release timeframe.
     */
    private fun calculateDateRange(timeframe: ReleaseTimeframe): Pair<String?, String?> {
        if (timeframe == ReleaseTimeframe.ALL) {
            return null to null
        }

        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val endDate = today.format(formatter)

        val startDate = when (timeframe) {
            ReleaseTimeframe.LAST_YEAR -> today.minusYears(1).format(formatter)
            ReleaseTimeframe.LAST_5_YEARS -> today.minusYears(5).format(formatter)
            ReleaseTimeframe.ALL -> null
        }

        return startDate to endDate
    }

    /**
     * Get the API ordering parameter based on sort type and order.
     */
    private fun getApiOrdering(sortType: DiscoverySortType, sortOrder: DiscoverySortOrder): String? {
        val prefix = if (sortOrder == DiscoverySortOrder.ASCENDING) "" else "-"
        
        return when (sortType) {
            DiscoverySortType.RELEVANCE -> null // Use default relevance
            DiscoverySortType.RATING -> "${prefix}rating"
            DiscoverySortType.RELEASE_DATE -> "${prefix}released"
            DiscoverySortType.NAME -> "${prefix}name"
            DiscoverySortType.POPULARITY -> "${prefix}added"
        }
    }
}
