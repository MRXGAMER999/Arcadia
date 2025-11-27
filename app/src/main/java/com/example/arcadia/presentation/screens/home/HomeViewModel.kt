package com.example.arcadia.presentation.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.arcadia.domain.repository.AIRepository
import com.example.arcadia.presentation.base.LibraryAwareViewModel
import com.example.arcadia.domain.model.DiscoveryFilterState
import com.example.arcadia.domain.model.DiscoverySortOrder
import com.example.arcadia.domain.model.DiscoverySortType
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.ReleaseTimeframe
import com.example.arcadia.domain.model.StudioFilterState
import com.example.arcadia.domain.model.StudioFilterType
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.domain.usecase.AddGameToLibraryUseCase
import com.example.arcadia.domain.usecase.ParallelGameFilter // Kept for DI, may be used for future local filtering
import com.example.arcadia.util.PreferencesManager
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HomeScreenState(
    val popularGames: RequestState<List<Game>> = RequestState.Idle,
    val upcomingGames: RequestState<List<Game>> = RequestState.Idle,
    val recommendedGames: RequestState<List<Game>> = RequestState.Idle,
    val newReleases: RequestState<List<Game>> = RequestState.Idle,
    // gamesInLibrary is now in LibraryAwareViewModel
    // gameToAddWithStatus is now in LibraryAwareViewModel
    val isRefreshing: Boolean = false, // Pull-to-refresh state
    val isLoadingMore: Boolean = false, // Pagination loading state
    // Snackbar state is now in LibraryAwareViewModel
)

class HomeViewModel(
    private val gameRepository: GameRepository,
    gameListRepository: GameListRepository,
    private val aiRepository: AIRepository,
    private val preferencesManager: PreferencesManager,
    addGameToLibraryUseCase: AddGameToLibraryUseCase,
    @Suppress("unused") private val parallelGameFilter: ParallelGameFilter // Kept for potential future local filtering
) : LibraryAwareViewModel(gameListRepository, addGameToLibraryUseCase) {
    
    companion object {
        private const val TAG = "HomeViewModel"
        
        // Timing constants
        private const val ANIMATION_DURATION_MS = 600L
        private const val NOTIFICATION_DISPLAY_MS = 1500L
        private const val ERROR_RESET_DELAY_MS = 3000L
        private const val DUPLICATE_CHECK_DELAY_MS = 2000L
        private const val FILTER_DEBOUNCE_MS = 150L
        private const val DEVELOPER_SEARCH_DEBOUNCE_MS = 300L
        
        // Pagination constants - OPTIMIZED for faster initial load
        private const val INITIAL_PAGE_SIZE = 6     // Smaller initial load for faster first paint
        private const val DEFAULT_PAGE_SIZE = 10    // Standard page size after initial load
        private const val RECOMMENDATION_PAGE_SIZE = 15  // Reduced from 30 for faster loading
        private const val MIN_RECOMMENDATIONS_COUNT = 10 // Reduced from 20
        private const val AI_RECOMMENDATION_COUNT = 12   // Reduced from 40 for instant load
        private const val AI_LOAD_MORE_COUNT = 8         // Reduced from 15
        
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
    
    // Cache for AI recommendations to persist across app restarts
    private var cachedAIRecommendations: List<Game> = emptyList()
    private var lastLibraryHash: Int = 0

    init {
        // Load saved discovery filter preferences
        loadSavedDiscoveryPreferences()
        // Start real-time flows only once
        // Library tracking is automatic!
        // Load initial data - but skip recommendations if filters are active
        loadInitialData()
        // Run one-time migrations in background
        runMigrationsIfNeeded()
    }
    
    /**
     * Run one-time data migrations in the background.
     * This won't block the UI and runs silently.
     */
    private fun runMigrationsIfNeeded() {
        launchWithKey("migrations") {
            // Migration V1: Enrich old library entries with developer/publisher data
            if (preferencesManager.needsDevPubMigration()) {
                android.util.Log.d(TAG, "Starting dev/pub migration for old library entries...")
                try {
                    val result = gameListRepository.migrateLibraryWithDevPub { rawgId ->
                        // Fetch game details from RAWG API
                        var game: com.example.arcadia.domain.model.Game? = null
                        gameRepository.getGameDetails(rawgId).collect { state ->
                            if (state is RequestState.Success) {
                                game = state.data
                            }
                        }
                        game
                    }
                    if (result is RequestState.Success) {
                        preferencesManager.markDevPubMigrationComplete()
                        android.util.Log.d(TAG, "Dev/pub migration completed: ${result.data} entries updated")
                    } else if (result is RequestState.Error) {
                        android.util.Log.e(TAG, "Dev/pub migration failed: ${result.message}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Dev/pub migration failed, will retry next launch: ${e.message}")
                    // Don't mark complete - will retry on next app launch
                }
            }
        }
    }
    
    /**
     * Load initial data with parallel fetching for faster startup.
     * Uses smaller page sizes for initial load to get data on screen faster.
     */
    private fun loadInitialData() {
        // Launch all initial loads in parallel for faster startup
        launchWithKey("initial_data_load") {
            // Load horizontal sections with smaller initial page size
            loadPopularGames(INITIAL_PAGE_SIZE)
            loadUpcomingGames(INITIAL_PAGE_SIZE)
            loadNewReleases(INITIAL_PAGE_SIZE)
            
            // Load recommendations (or apply filters) separately
            if (discoveryFilterState.hasActiveFilters) {
                applyDiscoveryFilters()
            } else {
                loadRecommendedGames()
            }
            
            // After initial fast load, prefetch more data in background
            prefetchAdditionalData()
        }
    }
    
    /**
     * Prefetch additional data in background after initial load completes.
     * This loads more items for each section without blocking the UI.
     */
    private fun prefetchAdditionalData() {
        launchWithKey("prefetch_data") {
            // Small delay to let UI render first
            delay(500)
            
            // Prefetch full page sizes in background using parallel coroutines
            coroutineScope {
                launch { 
                    gameRepository.getPopularGames(1, DEFAULT_PAGE_SIZE).collect { /* prefetch */ }
                }
                launch { 
                    gameRepository.getUpcomingGames(1, DEFAULT_PAGE_SIZE).collect { /* prefetch */ }
                }
                launch { 
                    gameRepository.getNewReleases(1, DEFAULT_PAGE_SIZE).collect { /* prefetch */ }
                }
            }
            android.util.Log.d(TAG, "Background prefetch completed")
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
    
    /**
     * Reload all data with full page sizes. Called on manual refresh.
     */
    fun loadAllData() {
        // Reload one-shot data with full page sizes (no duplicate flows)
        loadPopularGames(DEFAULT_PAGE_SIZE)
        loadUpcomingGames(DEFAULT_PAGE_SIZE)
        loadNewReleases(DEFAULT_PAGE_SIZE)
        
        // If discovery filters are active, apply them instead of loading default recommendations
        if (discoveryFilterState.hasActiveFilters) {
            applyDiscoveryFilters()
        } else {
            loadRecommendedGames()
        }
    }
    
    override fun onLibraryUpdated(games: List<GameListEntry>) {
        // Reapply appropriate filter when game list changes (local filtering only)
        if (discoveryFilterState.hasActiveFilters) {
            // Re-filter cached discovery results locally
            refilterDiscoveryResults()
        } else {
            applyRecommendationFilter()
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
        
        val filtered = lastDiscoveryResults.filter { !isGameInLibrary(it.id) }
        android.util.Log.d(TAG, "Refiltered discovery: ${lastDiscoveryResults.size} -> ${filtered.size} games")
        screenState = screenState.copy(recommendedGames = RequestState.Success(filtered))
        
        // Auto-fetch more if list is getting too short
        if (filtered.size < MIN_RECOMMENDATIONS_COUNT && !isLoadingMoreDiscovery) {
            loadMoreDiscoveryResults()
        }
    }
    
    /**
     * Load popular games with configurable page size.
     * Uses smaller page size for initial load, larger for refresh.
     */
    private fun loadPopularGames(pageSize: Int = DEFAULT_PAGE_SIZE) {
        launchWithKey("popular_games") {
            gameRepository.getPopularGames(page = 1, pageSize = pageSize).collect { state ->
                screenState = screenState.copy(popularGames = state)
            }
        }
    }
    
    /**
     * Load upcoming games with configurable page size.
     * Uses smaller page size for initial load, larger for refresh.
     */
    private fun loadUpcomingGames(pageSize: Int = DEFAULT_PAGE_SIZE) {
        launchWithKey("upcoming_games") {
            // Load most anticipated upcoming games (ordered by added count)
            val today = LocalDate.now()
            val oneYearFromNow = today.plusYears(1)
            
            gameRepository.getFilteredGames(
                startDate = today.format(DateTimeFormatter.ISO_DATE),
                endDate = oneYearFromNow.format(DateTimeFormatter.ISO_DATE),
                ordering = "-added", // Most added = most anticipated
                page = 1,
                pageSize = pageSize
            ).collect { state ->
                screenState = screenState.copy(upcomingGames = state)
            }
        }
    }
    
    /**
     * Load new releases with configurable page size.
     * Uses smaller page size for initial load, larger for refresh.
     */
    private fun loadNewReleases(pageSize: Int = DEFAULT_PAGE_SIZE) {
        launchWithKey("new_releases") {
            gameRepository.getNewReleases(page = 1, pageSize = pageSize).collect { state ->
                screenState = screenState.copy(newReleases = state)
            }
        }
    }
    
    private fun loadRecommendedGames() {
        launchWithKey("recommended_games") {
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
        launchWithKey("ensure_min_recommendations") {
            val current = screenState.recommendedGames
            if (current is RequestState.Success && current.data.size < minCount && !isLoadingMoreRecommendations) {
                loadMoreRecommendations()
            }
        }
    }
    
    fun loadMoreRecommendations() {
        if (isLoadingMoreRecommendations) return
        
        launchWithKey("load_more_recommendations") {
            isLoadingMoreRecommendations = true
            screenState = screenState.copy(isLoadingMore = true)
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
                    screenState = screenState.copy(isLoadingMore = false)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.d(TAG, "loadMoreRecommendations cancelled")
                isLoadingMoreRecommendations = false
                screenState = screenState.copy(isLoadingMore = false)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading more recommendations", e)
                isLoadingMoreRecommendations = false
                screenState = screenState.copy(isLoadingMore = false)
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

        // Exclude games in library
        val excludeIds = gamesInLibrary.value

        // Always filter with the latest data to include newly loaded games
        val filtered = lastRecommended.filter { it.id !in excludeIds }
        
        // Only skip if nothing has changed (same exclude set AND same filtered result size)
        if (excludeIds == lastExcludeIds && filtered.size == lastFilteredRecommendations.size && lastFilteredRecommendations.isNotEmpty()) {
            android.util.Log.d("HomeViewModel", "Filter skipped: no changes (excludeIds=${excludeIds.size}, games=${filtered.size})")
            return
        }

        lastExcludeIds = excludeIds
        lastFilteredRecommendations = filtered
        android.util.Log.d("HomeViewModel", "Filter applied: ${lastRecommended.size} -> ${filtered.size} games (excluded ${excludeIds.size})")
        screenState = screenState.copy(recommendedGames = RequestState.Success(filtered))
        
        // Auto-backfill if filtered results are too few
        ensureMinimumRecommendations()
    }

    fun retry() {
        loadAllData()
    }
    
    /**
     * Refresh Home tab data - used for pull-to-refresh on Home screen.
     * Loads more recommended games and refreshes horizontal sections.
     */
    fun refreshHome() {
        launchWithKey("refresh_home") {
            screenState = screenState.copy(isRefreshing = true)
            
            try {
                // Load more recommended games directly (bypass guard)
                fetchMoreRecommendedGames()
                
                // Also refresh the horizontal sections with fresh data
                loadPopularGames()
                loadUpcomingGames()
                loadNewReleases()
                
                // Give a small delay for the refresh indicator to be visible
                delay(800)
            } finally {
                screenState = screenState.copy(isRefreshing = false)
            }
        }
    }
    
    /**
     * Refresh Discover tab data - used for pull-to-refresh on Discover screen.
     * Loads more discovery games based on current filters.
     */
    fun refreshDiscover() {
        launchWithKey("refresh_discover") {
            screenState = screenState.copy(isRefreshing = true)
            
            try {
                // Load more discovery games (works with or without filters)
                fetchMoreDiscoveryGames()
                
                // Brief delay for visual feedback
                delay(300)
            } finally {
                screenState = screenState.copy(isRefreshing = false)
            }
        }
    }
    
    /**
     * Directly fetch more recommended games for pull-to-refresh.
     * This bypasses the isLoadingMoreRecommendations guard.
     */
    private suspend fun fetchMoreRecommendedGames() {
        try {
            recommendationPage++
            android.util.Log.d(TAG, "Refresh: Fetching recommended games page $recommendationPage")
            gameRepository.getRecommendedGames(
                tags = DEFAULT_RECOMMENDATION_TAGS,
                page = recommendationPage,
                pageSize = RECOMMENDATION_PAGE_SIZE
            ).collect { state ->
                if (state is RequestState.Success) {
                    android.util.Log.d(TAG, "Refresh: Got ${state.data.size} new games")
                    lastRecommended = lastRecommended + state.data
                    applyRecommendationFilter()
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            android.util.Log.d(TAG, "fetchMoreRecommendedGames cancelled")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching more recommendations on refresh", e)
        }
    }
    
    /**
     * Directly fetch more discovery games for pull-to-refresh.
     * This bypasses the isLoadingMoreDiscovery guard.
     * If no filters are active, it fetches games with default ordering.
     */
    private suspend fun fetchMoreDiscoveryGames() {
        try {
            if (discoveryFilterState.sortType == DiscoverySortType.AI_RECOMMENDATION) {
                // Force refresh to get new recommendations
                fetchAIRecommendations(isLoadMore = false, forceRefresh = true)
                return
            }

            discoveryFilterPage++
            android.util.Log.d(TAG, "Refresh: Fetching discovery games page $discoveryFilterPage (hasFilters=${discoveryFilterState.hasActiveFilters})")
            
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
                    android.util.Log.d(TAG, "Refresh: Got ${state.data.size} new discovery games")
                    lastDiscoveryResults = lastDiscoveryResults + state.data
                    val filtered = lastDiscoveryResults.filter { !isGameInLibrary(it.id) }
                    screenState = screenState.copy(
                        recommendedGames = RequestState.Success(filtered)
                    )
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            android.util.Log.d(TAG, "fetchMoreDiscoveryGames cancelled")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching more discovery games on refresh", e)
        }
    }
    
    // ==================== Studio Filter Methods ====================

    // Separate job for fetching games after filter is set
    // private var studioGamesJob: Job? = null // Removed in favor of launchWithKey

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
        launchWithDebounce(
            key = "studio_filter_setup",
            delay = FILTER_DEBOUNCE_MS
        ) {
            try {
                // Get expanded studios for UI display
                val expandedStudios = aiRepository.getExpandedStudios(studioName)

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
        launchWithKey("studio_games_fetch") {
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
                val slugs = aiRepository.getStudioSlugs(parentStudio)
                studioFilterSlugs = slugs
                
                // Check if we have valid slugs
                if (slugs.isBlank()) {
                    android.util.Log.w("HomeViewModel", "No slugs found for: $parentStudio")
                    screenState = screenState.copy(
                        recommendedGames = RequestState.Success(emptyList())
                    )
                    studioFilterState = studioFilterState.copy(hasMorePages = false)
                    return@launchWithKey
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
                            val filtered = state.data.filter { !isGameInLibrary(it.id) }
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
        
        launchWithKey("load_more_studio_games") {
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
                                !isGameInLibrary(it.id) && it.id !in existingIds 
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
        cancelJob("studio_filter_setup")
        cancelJob("studio_games_fetch")
        
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

    // private var discoveryFilterJob: Job? = null // Removed in favor of launchWithKey
    // private var developerSearchJob: Job? = null // Removed in favor of launchWithKey

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
     * Uses instant local suggestions first, then enriches with AI results.
     */
    fun searchDevelopers(query: String) {
        // Cancel any previous search job
        cancelJob("developer_search")
        
        if (query.length < 2) {
            discoveryFilterState = discoveryFilterState.copy(
                searchResults = emptyList(),
                expandedStudios = emptySet(),
                isLoadingDevelopers = false
            )
            return
        }
        
        // Immediately show local suggestions (no delay, synchronous)
        val localSuggestions = aiRepository.getLocalStudioSuggestions(query, 5)
        if (localSuggestions.isNotEmpty()) {
            discoveryFilterState = discoveryFilterState.copy(
                searchResults = localSuggestions.map { it.name },
                expandedStudios = localSuggestions.map { it.name }.toSet(),
                isLoadingDevelopers = true // Still loading AI results
            )
        }
        
        launchWithDebounce(
            key = "developer_search",
            delay = DEVELOPER_SEARCH_DEBOUNCE_MS
        ) {
            discoveryFilterState = discoveryFilterState.copy(isLoadingDevelopers = true)
            
            try {
                // Search for studios with AI enhancement
                val searchResult = aiRepository.searchStudios(
                    query = query,
                    includePublishers = true,
                    includeDevelopers = true,
                    limit = 10
                )
                
                discoveryFilterState = discoveryFilterState.copy(
                    searchResults = searchResult.matches.map { it.name },
                    expandedStudios = searchResult.matches.map { it.name }.toSet(),
                    isLoadingDevelopers = false
                )
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Developer search failed", e)
                // Keep any local results we already have
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
        launchWithKey("select_developer") {
            try {
                // Get sub-studios for the selected developer
                val subStudios = aiRepository.getExpandedStudios(developer)
                
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

        launchWithKey("discovery_filter") {
            screenState = screenState.copy(recommendedGames = RequestState.Loading)

            try {
                // Handle AI Recommendations separately
                if (discoveryFilterState.sortType == DiscoverySortType.AI_RECOMMENDATION) {
                    fetchAIRecommendations(isLoadMore = false)
                    return@launchWithKey
                }

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
                            val filtered = state.data.filter { !isGameInLibrary(it.id) }
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
    fun loadMoreDiscoveryResults() {
        if (isLoadingMoreDiscovery || !discoveryFilterState.hasActiveFilters) return
        
        launchWithKey("load_more_discovery") {
            isLoadingMoreDiscovery = true
            screenState = screenState.copy(isLoadingMore = true)
            try {
                if (discoveryFilterState.sortType == DiscoverySortType.AI_RECOMMENDATION) {
                    // Increment page before fetching to get more games
                    discoveryFilterPage++
                    fetchAIRecommendations(isLoadMore = true)
                    return@launchWithKey
                }

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
                        val filtered = lastDiscoveryResults.filter { it.id !in gamesInLibrary.value }
                        android.util.Log.d(TAG, "Loaded more discovery: +${state.data.size} games, total filtered: ${filtered.size}")
                        screenState = screenState.copy(
                            recommendedGames = RequestState.Success(filtered)
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.d(TAG, "loadMoreDiscoveryResults cancelled")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading more discovery results", e)
            } finally {
                isLoadingMoreDiscovery = false
                screenState = screenState.copy(isLoadingMore = false)
            }
        }
    }

    /**
     * Clear all discovery filters.
     */
    fun clearDiscoveryFilters() {
        // Cancel any ongoing filter jobs first
        cancelJob("discovery_filter")
        cancelJob("developer_search")
        
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
            DiscoverySortType.AI_RECOMMENDATION -> null // Use default relevance or handle AI separately
            DiscoverySortType.RATING -> "${prefix}rating"
            DiscoverySortType.RELEASE_DATE -> "${prefix}released"
            DiscoverySortType.NAME -> "${prefix}name"
            DiscoverySortType.POPULARITY -> "${prefix}added"
        }
    }

    /**
     * Fetch AI-powered game recommendations based on user's library.
     * Simplified implementation with better error handling and race condition prevention.
     */
    private suspend fun fetchAIRecommendations(
        isLoadMore: Boolean,
        forceRefresh: Boolean = false
    ) {
        try {
            // Set loading state only for initial load (load more sets it before calling)
            if (!isLoadMore && screenState.recommendedGames !is RequestState.Success) {
                screenState = screenState.copy(recommendedGames = RequestState.Loading)
            }
            
            // 1. Get user's library
            val libraryData = fetchLibraryData()
            
            // Fallback for new users with empty library
            if (libraryData.isEmpty()) {
                android.util.Log.d(TAG, "Library is empty, falling back to popular games")
                loadRecommendedGames()
                return
            }

            // 2. Check cache (only if not force refresh and not loading more)
            val currentLibraryHash = computeLibraryHash(libraryData)
            if (!forceRefresh && !isLoadMore && cachedAIRecommendations.isNotEmpty() && currentLibraryHash == lastLibraryHash) {
                android.util.Log.d(TAG, "Using cached AI recommendations (${cachedAIRecommendations.size} games)")
                val filtered = cachedAIRecommendations.filter { !isGameInLibrary(it.id) }
                screenState = screenState.copy(recommendedGames = RequestState.Success(filtered))
                lastDiscoveryResults = cachedAIRecommendations
                return
            }

            android.util.Log.d(TAG, "Fetching AI recommendations for ${libraryData.size} library games")

            // 3. Calculate request count
            val count = if (isLoadMore) {
                AI_RECOMMENDATION_COUNT + (discoveryFilterPage * AI_LOAD_MORE_COUNT)
            } else {
                AI_RECOMMENDATION_COUNT
            }
            
            // 4. Get AI recommendations (uses sorted by confidence)
            val aiResult = aiRepository.getLibraryBasedRecommendations(
                games = libraryData, 
                count = count,
                forceRefresh = forceRefresh
            )
            
            aiResult.fold(
                onSuccess = { suggestions ->
                    handleAIRecommendationsSuccess(suggestions, isLoadMore, currentLibraryHash)
                },
                onFailure = { e ->
                    handleAIRecommendationsError(e, isLoadMore)
                }
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            android.util.Log.d(TAG, "fetchAIRecommendations cancelled")
            if (isLoadMore) screenState = screenState.copy(isLoadingMore = false)
            throw e
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in fetchAIRecommendations", e)
            handleAIRecommendationsError(e, isLoadMore)
        }
    }
    
    /**
     * Fetch the user's library data safely.
     */
    private suspend fun fetchLibraryData(): List<GameListEntry> {
        return try {
            gameListRepository.getGameList(com.example.arcadia.domain.repository.SortOrder.NEWEST_FIRST)
                .first { state -> state is RequestState.Success || state is RequestState.Error }
                .let { state ->
                    when (state) {
                        is RequestState.Success -> state.data
                        else -> emptyList()
                    }
                }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching library", e)
            emptyList()
        }
    }
    
    /**
     * Compute a hash of the library for cache invalidation.
     * Includes ratings and status to invalidate when user updates library.
     */
    private fun computeLibraryHash(games: List<GameListEntry>): Int {
        return games.map { "${it.rawgId}_${it.rating}_${it.status}" }.sorted().hashCode()
    }
    
    /**
     * Handle successful AI recommendations response.
     */
    private suspend fun handleAIRecommendationsSuccess(
        suggestions: com.example.arcadia.domain.model.ai.AIGameSuggestions,
        isLoadMore: Boolean,
        currentLibraryHash: Int
    ) {
        android.util.Log.d(TAG, "AI suggested ${suggestions.games.size} games (sorted by confidence)")
        
        // Get game names sorted by confidence
        val sortedGameNames = suggestions.getGamesSortedByConfidence()
        
        // Filter out games we already have if loading more
        val newSuggestions = if (isLoadMore) {
            val existingNames = lastDiscoveryResults.map { it.name.lowercase() }.toSet()
            sortedGameNames.filter { it.lowercase() !in existingNames }
        } else {
            sortedGameNames
        }

        if (newSuggestions.isEmpty()) {
            if (isLoadMore) {
                android.util.Log.d(TAG, "No new games to load")
                screenState = screenState.copy(isLoadingMore = false)
            }
            return
        }

        // Initialize state for fresh load
        if (!isLoadMore) {
            screenState = screenState.copy(recommendedGames = RequestState.Success(emptyList()))
        }

        // Fetch Game objects in parallel
        val existingIds = if (isLoadMore) lastDiscoveryResults.map { it.id }.toSet() else emptySet()
        val games = fetchGamesInParallel(newSuggestions, existingIds)

        // Combine results
        val finalGames = if (isLoadMore) {
            (lastDiscoveryResults + games).distinctBy { it.id }
        } else {
            games.distinctBy { it.id }
        }
        
        lastDiscoveryResults = finalGames
        
        // Update cache on fresh load
        if (!isLoadMore) {
            cachedAIRecommendations = lastDiscoveryResults
            lastLibraryHash = currentLibraryHash
        }
        
        // Filter out library games and update state
        val filtered = lastDiscoveryResults.filter { !isGameInLibrary(it.id) }
        
        screenState = screenState.copy(
            recommendedGames = RequestState.Success(filtered),
            isLoadingMore = false
        )
        
        // Reset pagination on fresh load only (load more increments before calling)
        if (!isLoadMore) {
            discoveryFilterPage = 1
        }
    }
    
    /**
     * Handle AI recommendations error.
     */
    private fun handleAIRecommendationsError(e: Throwable, isLoadMore: Boolean) {
        val errorMessage = e.message ?: "Failed to get AI recommendations"
        android.util.Log.e(TAG, "AI recommendations error: $errorMessage")
        
        if (!isLoadMore) {
            screenState = screenState.copy(recommendedGames = RequestState.Error(errorMessage))
        } else {
            screenState = screenState.copy(isLoadingMore = false)
        }
    }
    
    /**
     * Fetch multiple games in parallel with progressive UI updates.
     * Simplified implementation with better error handling.
     */
    private suspend fun fetchGamesInParallel(
        gameNames: List<String>,
        existingIds: Set<Int>
    ): List<Game> = coroutineScope {
        val seenIds = java.util.Collections.synchronizedSet(existingIds.toMutableSet())
        val allGames = java.util.Collections.synchronizedList(mutableListOf<Game>())
        
        // Process in batches for progressive UI updates
        val batchSize = 4
        val batches = gameNames.chunked(batchSize)
        
        for (batch in batches) {
            // Launch parallel searches for this batch
            val results = batch.map { gameName ->
                async {
                    searchGameByName(gameName)
                }
            }.awaitAll()
            
            // Filter and add unique games
            val newGames = results.filterNotNull().filter { game ->
                synchronized(seenIds) {
                    if (game.id !in seenIds) {
                        seenIds.add(game.id)
                        true
                    } else {
                        false
                    }
                }
            }
            
            allGames.addAll(newGames)
            
            // Progressive UI update
            if (newGames.isNotEmpty() && screenState.recommendedGames is RequestState.Success) {
                val currentList = (screenState.recommendedGames as RequestState.Success).data
                val updatedList = (currentList + newGames).distinctBy { it.id }
                screenState = screenState.copy(recommendedGames = RequestState.Success(updatedList))
            }
        }
        
        allGames.toList()
    }
    
    /**
     * Search for a game by name, returning the first match or null.
     */
    private suspend fun searchGameByName(gameName: String): Game? {
        return try {
            val result = gameRepository.searchGames(gameName, page = 1, pageSize = 1)
                .first { it is RequestState.Success || it is RequestState.Error }
            
            if (result is RequestState.Success && result.data.isNotEmpty()) {
                result.data.first()
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch game: $gameName", e)
            null
        }
    }
}

