package com.example.arcadia.presentation.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.repository.AIRepository
import com.example.arcadia.presentation.base.BaseViewModel
import com.example.arcadia.domain.model.DiscoveryFilterState
import com.example.arcadia.domain.model.DiscoverySortOrder
import com.example.arcadia.domain.model.DiscoverySortType
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.ReleaseTimeframe
import com.example.arcadia.domain.model.StudioFilterState
import com.example.arcadia.domain.model.StudioFilterType
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.SortOrder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.util.PreferencesManager
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Data class representing the discovery/filter state for the UI.
 */
data class DiscoveryState(
    val games: RequestState<List<Game>> = RequestState.Idle,
    val isRefreshing: Boolean = false
)

/**
 * ViewModel responsible for discovery and filtering functionality.
 * Handles:
 * - Discovery filters (genre, developer, timeframe, sort)
 * - Studio filters (legacy)
 * - Filtered game loading and pagination
 */
class DiscoveryViewModel(
    private val gameRepository: GameRepository,
    private val aiRepository: AIRepository,
    private val gameListRepository: GameListRepository,
    private val preferencesManager: PreferencesManager
) : BaseViewModel() {

    companion object {
        private const val TAG = "DiscoveryViewModel"
        private const val FILTER_DEBOUNCE_MS = 150L
        private const val DEVELOPER_SEARCH_DEBOUNCE_MS = 300L
        private const val DEFAULT_PAGE_SIZE = 40
        private const val MIN_GAMES_THRESHOLD = 15
        private const val AI_RECOMMENDATION_BATCH = 12
    }

    // Discovery state exposed to UI
    private val _discoveryState = MutableStateFlow(DiscoveryState())
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    // Studio filter state (legacy - kept for compatibility)
    var studioFilterState by mutableStateOf(StudioFilterState())
        private set

    // Discovery filter state
    var discoveryFilterState by mutableStateOf(DiscoveryFilterState())
        private set

    // Track if discovery filter dialog is shown
    var showDiscoveryFilterDialog by mutableStateOf(false)
        private set

    // Games in library (updated from HomeViewModel)
    private var gamesInLibrary: Set<Int> = emptySet()

    // Cache for discovery results
    private var lastDiscoveryResults: List<Game> = emptyList()
    private var discoveryFilterPage = 1
    private var isLoadingMoreDiscovery = false

    // Cache for studio filter
    private var studioFilteredGames: MutableList<Game> = mutableListOf()
    private var studioFilterSlugs: String = ""

    init {
        loadSavedDiscoveryPreferences()
        // Auto-apply AI recommendations if it's the default
        if (discoveryFilterState.sortType == DiscoverySortType.AI_RECOMMENDATION) {
            applyDiscoveryFilters()
        }
    }

    /**
     * Update the games in library set (called from HomeViewModel).
     */
    fun updateGamesInLibrary(gameIds: Set<Int>) {
        gamesInLibrary = gameIds
        // Re-filter if discovery filters are active
        if (discoveryFilterState.hasActiveFilters) {
            refilterDiscoveryResults()
        }
    }

    /**
     * Check if the user's library is empty.
     * Used to show appropriate empty state for AI recommendations.
     */
    fun isLibraryEmpty(): Boolean = gamesInLibrary.isEmpty()

    /**
     * Get current filtered games as RequestState for compatibility.
     */
    fun getFilteredGames(): RequestState<List<Game>> = _discoveryState.value.games

    // ==================== Preferences ====================

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

    // ==================== Discovery Filter Methods ====================

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
        
        // Immediately show local suggestions (no delay, synchronous)
        if (query.length >= 2) {
            val localSuggestions = aiRepository.getLocalStudioSuggestions(query, 5)
            if (localSuggestions.isNotEmpty()) {
                discoveryFilterState = discoveryFilterState.copy(
                    searchResults = localSuggestions.map { it.name },
                    expandedStudios = localSuggestions.map { it.name }.toSet(),
                    isLoadingDevelopers = true // Still loading AI results
                )
            }
        }
        
        if (query.length < 2) {
            discoveryFilterState = discoveryFilterState.copy(
                searchResults = emptyList(),
                expandedStudios = emptySet(),
                isLoadingDevelopers = false
            )
            return
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
                android.util.Log.e(TAG, "Developer search failed", e)
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
     */
    fun selectDeveloperWithStudios(developer: String) {
        launchWithKey("select_developer") {
            try {
                val subStudios = aiRepository.getExpandedStudios(developer)
                val currentSelected = discoveryFilterState.selectedDevelopers.toMutableMap()
                currentSelected[developer] = subStudios - developer

                discoveryFilterState = discoveryFilterState.copy(
                    selectedDevelopers = currentSelected,
                    developerSearchQuery = "",
                    searchResults = emptyList()
                )

                android.util.Log.d(TAG, "Selected $developer with ${subStudios.size - 1} sub-studios")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to get sub-studios for $developer", e)
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
     * @param onComplete Callback when filters are applied (for HomeViewModel to update state)
     */
    fun applyDiscoveryFilters(onComplete: ((RequestState<List<Game>>) -> Unit)? = null) {
        dismissDiscoveryFilterDialog()
        saveDiscoveryPreferences()

        if (!discoveryFilterState.hasActiveFilters) {
            lastDiscoveryResults = emptyList()
            discoveryFilterPage = 1
            clearStudioFilter()
            _discoveryState.value = _discoveryState.value.copy(games = RequestState.Idle)
            onComplete?.invoke(RequestState.Idle)
            return
        }

        launchWithKey("discovery_filter") {
            _discoveryState.value = _discoveryState.value.copy(games = RequestState.Loading)
            onComplete?.invoke(RequestState.Loading)

            try {
                // Handle AI Recommendations separately
                if (discoveryFilterState.sortType == DiscoverySortType.AI_RECOMMENDATION) {
                    fetchAIRecommendations(isLoadMore = false, onComplete = onComplete)
                    return@launchWithKey
                }

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

                android.util.Log.d(TAG, "Applying discovery filters: developers=$developerSlugs, genres=$genreSlugs, dates=$startDate..$endDate, ordering=$ordering")

                discoveryFilterPage = 1

                gameRepository.getFilteredGames(
                    developerSlugs = developerSlugs,
                    genres = genreSlugs,
                    startDate = startDate,
                    endDate = endDate,
                    ordering = ordering,
                    page = 1,
                    pageSize = DEFAULT_PAGE_SIZE
                ).collect { state ->
                    when (state) {
                        is RequestState.Success -> {
                            lastDiscoveryResults = state.data
                            val filtered = state.data.filter { it.id !in gamesInLibrary }
                            android.util.Log.d(TAG, "Discovery filter: ${state.data.size} games, ${filtered.size} after library filter")
                            val resultState = RequestState.Success(filtered)
                            _discoveryState.value = _discoveryState.value.copy(games = resultState)
                            onComplete?.invoke(resultState)
                        }
                        is RequestState.Error -> {
                            android.util.Log.e(TAG, "Discovery filter error: ${state.message}")
                            _discoveryState.value = _discoveryState.value.copy(games = state)
                            onComplete?.invoke(state)
                        }
                        is RequestState.Loading -> {
                            _discoveryState.value = _discoveryState.value.copy(games = state)
                            onComplete?.invoke(state)
                        }
                        else -> {}
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.d(TAG, "Discovery filter cancelled")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Discovery filter exception", e)
                val errorState = RequestState.Error("Failed to apply filters: ${e.message}")
                _discoveryState.value = _discoveryState.value.copy(games = errorState)
                onComplete?.invoke(errorState)
            }
        }
    }

    /**
     * Re-filter cached discovery results without making a new API call.
     */
    private fun refilterDiscoveryResults() {
        if (lastDiscoveryResults.isEmpty()) {
            // No cached results - need to fetch fresh data
            applyDiscoveryFilters()
            return
        }

        val filtered = lastDiscoveryResults.filter { it.id !in gamesInLibrary }
        android.util.Log.d(TAG, "Refiltered discovery: ${lastDiscoveryResults.size} -> ${filtered.size} games (library has ${gamesInLibrary.size} games)")
        _discoveryState.value = _discoveryState.value.copy(games = RequestState.Success(filtered))

        // Auto-load more if we're running low on games after filtering
        if (filtered.size < MIN_GAMES_THRESHOLD && !isLoadingMoreDiscovery) {
            android.util.Log.d(TAG, "Low on games after refiltering (${filtered.size} < $MIN_GAMES_THRESHOLD), loading more...")
            loadMoreDiscoveryResults()
        }
    }

    /**
     * Load more discovery results for pagination.
     */
    fun loadMoreDiscoveryResults() {
        if (isLoadingMoreDiscovery || !discoveryFilterState.hasActiveFilters) return

        launchWithKey("load_more_discovery") {
            isLoadingMoreDiscovery = true
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
                    pageSize = DEFAULT_PAGE_SIZE
                ).collect { state ->
                    if (state is RequestState.Success && state.data.isNotEmpty()) {
                        lastDiscoveryResults = lastDiscoveryResults + state.data
                        val filtered = lastDiscoveryResults.filter { it.id !in gamesInLibrary }
                        android.util.Log.d(TAG, "Loaded more discovery: +${state.data.size} games, total filtered: ${filtered.size}")
                        _discoveryState.value = _discoveryState.value.copy(games = RequestState.Success(filtered))
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
     * Refresh discovery games (pull-to-refresh).
     */
    suspend fun refreshDiscoveryGames() {
        try {
            if (discoveryFilterState.sortType == DiscoverySortType.AI_RECOMMENDATION) {
                // Force refresh to get new recommendations
                fetchAIRecommendations(isLoadMore = false, forceRefresh = true)
                return
            }

            discoveryFilterPage++
            android.util.Log.d(TAG, "Refresh: Fetching discovery games page $discoveryFilterPage")

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
                pageSize = DEFAULT_PAGE_SIZE
            ).collect { state ->
                if (state is RequestState.Success && state.data.isNotEmpty()) {
                    android.util.Log.d(TAG, "Refresh: Got ${state.data.size} new discovery games")
                    lastDiscoveryResults = lastDiscoveryResults + state.data
                    val filtered = lastDiscoveryResults.filter { it.id !in gamesInLibrary }
                    _discoveryState.value = _discoveryState.value.copy(games = RequestState.Success(filtered))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching more discovery games on refresh", e)
        }
    }

    /**
     * Clear all discovery filters.
     */
    fun clearDiscoveryFilters() {
        cancelJob("discovery_filter")
        cancelJob("developer_search")

        // Reset to default state but with POPULARITY sort type instead of AI
        discoveryFilterState = DiscoveryFilterState(
            sortType = com.example.arcadia.domain.model.DiscoverySortType.POPULARITY
        )
        clearStudioFilter()

        lastDiscoveryResults = emptyList()
        discoveryFilterPage = 1

        preferencesManager.clearDiscoveryFilters()

        _discoveryState.value = _discoveryState.value.copy(games = RequestState.Idle)
    }

    /**
     * Check if discovery filter is active.
     */
    fun isDiscoveryFilterActive(): Boolean = discoveryFilterState.hasActiveFilters

    // ==================== Studio Filter Methods (Legacy) ====================

    /**
     * Set studio filter with debouncing.
     */
    fun setStudioFilter(studioName: String) {
        studioFilterState = studioFilterState.copy(
            isLoadingExpansion = true,
            selectedParentStudio = studioName,
            error = null
        )

        launchWithDebounce(
            key = "studio_filter",
            delay = FILTER_DEBOUNCE_MS
        ) {
            try {
                val expandedStudios = aiRepository.getExpandedStudios(studioName)

                studioFilterState = studioFilterState.copy(
                    expandedStudios = expandedStudios,
                    selectedStudios = expandedStudios.toMutableSet(),
                    isLoadingExpansion = false
                )

                fetchGamesForStudioFilter(studioName)
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.d(TAG, "Studio filter cancelled for: $studioName")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Studio filter setup failed", e)
                studioFilterState = studioFilterState.copy(
                    isLoadingExpansion = false,
                    error = e.message
                )
            }
        }
    }

    private fun fetchGamesForStudioFilter(parentStudio: String) {
        launchWithKey("studio_games_fetch") {
            _discoveryState.value = _discoveryState.value.copy(games = RequestState.Loading)

            studioFilteredGames.clear()
            studioFilterState = studioFilterState.copy(
                currentPage = 1,
                hasMorePages = true,
                isLoadingMore = false
            )

            try {
                val slugs = aiRepository.getStudioSlugs(parentStudio)
                studioFilterSlugs = slugs

                if (slugs.isBlank()) {
                    android.util.Log.w(TAG, "No slugs found for: $parentStudio")
                    _discoveryState.value = _discoveryState.value.copy(games = RequestState.Success(emptyList()))
                    studioFilterState = studioFilterState.copy(hasMorePages = false)
                    return@launchWithKey
                }

                val developerSlugs = when (studioFilterState.filterType) {
                    StudioFilterType.DEVELOPER, StudioFilterType.BOTH -> slugs
                    StudioFilterType.PUBLISHER -> null
                }
                val publisherSlugs = when (studioFilterState.filterType) {
                    StudioFilterType.PUBLISHER, StudioFilterType.BOTH -> slugs
                    StudioFilterType.DEVELOPER -> null
                }

                gameRepository.getGamesByStudios(
                    developerSlugs = developerSlugs,
                    publisherSlugs = publisherSlugs,
                    page = 1,
                    pageSize = DEFAULT_PAGE_SIZE
                ).collect { state ->
                    when (state) {
                        is RequestState.Success -> {
                            val filtered = state.data.filter { it.id !in gamesInLibrary }
                            studioFilteredGames.addAll(filtered)
                            _discoveryState.value = _discoveryState.value.copy(
                                games = RequestState.Success(studioFilteredGames.toList())
                            )
                            studioFilterState = studioFilterState.copy(hasMorePages = state.data.size >= DEFAULT_PAGE_SIZE)
                        }
                        is RequestState.Error -> {
                            _discoveryState.value = _discoveryState.value.copy(games = state)
                        }
                        is RequestState.Loading -> {
                            _discoveryState.value = _discoveryState.value.copy(games = state)
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Studio filter exception", e)
                _discoveryState.value = _discoveryState.value.copy(
                    games = RequestState.Error("Failed to filter by studio: ${e.message}")
                )
            }
        }
    }

    /**
     * Load more games for studio filter pagination.
     */
    fun loadMoreStudioGames() {
        if (!isStudioFilterActive() || studioFilterState.isLoadingMore || !studioFilterState.hasMorePages) {
            return
        }

        launchWithKey("load_more_studio_games") {
            studioFilterState = studioFilterState.copy(isLoadingMore = true)

            try {
                val nextPage = studioFilterState.currentPage + 1

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
                    pageSize = DEFAULT_PAGE_SIZE
                ).collect { state ->
                    when (state) {
                        is RequestState.Success -> {
                            val existingIds = studioFilteredGames.map { it.id }.toSet()
                            val newGames = state.data.filter {
                                it.id !in gamesInLibrary && it.id !in existingIds
                            }

                            studioFilteredGames.addAll(newGames)
                            _discoveryState.value = _discoveryState.value.copy(
                                games = RequestState.Success(studioFilteredGames.toList())
                            )

                            studioFilterState = studioFilterState.copy(
                                currentPage = nextPage,
                                hasMorePages = state.data.size >= DEFAULT_PAGE_SIZE,
                                isLoadingMore = false
                            )
                        }
                        is RequestState.Error -> {
                            studioFilterState = studioFilterState.copy(isLoadingMore = false)
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Load more studio games exception", e)
                studioFilterState = studioFilterState.copy(isLoadingMore = false)
            }
        }
    }

    fun toggleStudioSelection(studio: String) {
        val current = studioFilterState.selectedStudios.toMutableSet()
        if (studio in current) current.remove(studio) else current.add(studio)
        studioFilterState = studioFilterState.copy(selectedStudios = current)
        studioFilterState.selectedParentStudio?.let { fetchGamesForStudioFilter(it) }
    }

    fun selectAllStudios() {
        studioFilterState = studioFilterState.copy(selectedStudios = studioFilterState.expandedStudios)
        studioFilterState.selectedParentStudio?.let { fetchGamesForStudioFilter(it) }
    }

    fun deselectAllStudios() {
        studioFilterState = studioFilterState.copy(selectedStudios = emptySet())
        _discoveryState.value = _discoveryState.value.copy(games = RequestState.Success(emptyList()))
    }

    fun setStudioFilterType(filterType: StudioFilterType) {
        studioFilterState = studioFilterState.copy(filterType = filterType)
        studioFilterState.selectedParentStudio?.let { fetchGamesForStudioFilter(it) }
    }

    fun clearStudioFilter() {
        cancelJob("studio_filter")
        cancelJob("studio_games_fetch")
        studioFilterState = StudioFilterState()
        studioFilteredGames.clear()
    }

    fun isStudioFilterActive(): Boolean = studioFilterState.selectedParentStudio != null

    // ==================== Helper Methods ====================

    private fun calculateDateRange(timeframe: ReleaseTimeframe): Pair<String?, String?> {
        if (timeframe == ReleaseTimeframe.ALL) return null to null

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

    private fun getApiOrdering(sortType: DiscoverySortType, sortOrder: DiscoverySortOrder): String? {
        val prefix = if (sortOrder == DiscoverySortOrder.ASCENDING) "" else "-"

        return when (sortType) {
            DiscoverySortType.AI_RECOMMENDATION -> null
            DiscoverySortType.RATING -> "${prefix}rating"
            DiscoverySortType.RELEASE_DATE -> "${prefix}released"
            DiscoverySortType.NAME -> "${prefix}name"
            DiscoverySortType.POPULARITY -> "${prefix}added"
        }
    }

    /**
     * Fetch AI-powered game recommendations based on user's library.
     * Simplified implementation with better error handling.
     */
    private suspend fun fetchAIRecommendations(
        isLoadMore: Boolean,
        forceRefresh: Boolean = false,
        onComplete: ((RequestState<List<Game>>) -> Unit)? = null
    ) {
        // Prevent concurrent load more operations
        if (isLoadMore && isLoadingMoreDiscovery) {
            android.util.Log.d(TAG, "Already loading more, skipping duplicate request")
            return
        }
        
        try {
            if (isLoadMore) {
                isLoadingMoreDiscovery = true
            }
            
            // 1. Get user's library to use for recommendations AND filtering
            // Use filter to skip Loading state and add timeout to prevent indefinite blocking
            val libraryState = kotlinx.coroutines.withTimeoutOrNull(15_000L) {
                gameListRepository.getGameList(SortOrder.NEWEST_FIRST)
                    .filter { it is RequestState.Success || it is RequestState.Error }
                    .first()
            }
            
            if (libraryState == null) {
                val error = RequestState.Error("Timed out loading library. Please try again.")
                _discoveryState.value = _discoveryState.value.copy(games = error)
                onComplete?.invoke(error)
                return
            }
            
            if (libraryState !is RequestState.Success || libraryState.data.isEmpty()) {
                val error = RequestState.Error("Add games to your library to get AI recommendations.")
                _discoveryState.value = _discoveryState.value.copy(games = error)
                onComplete?.invoke(error)
                return
            }

            // Update gamesInLibrary to ensure we have the latest library state
            val currentLibraryIds = libraryState.data.map { it.rawgId }.toSet()
            gamesInLibrary = currentLibraryIds
            
            // Limit library size for AI analysis to prevent token limit issues
            // Use top 50 most recent/relevant games for analysis
            val limitedLibrary = libraryState.data.take(50)
            android.util.Log.d(TAG, "Using ${limitedLibrary.size} of ${libraryState.data.size} library games for AI analysis")

            // 2. Use fixed batch size and send prior recs to avoid duplicates without growing tokens
            val count = AI_RECOMMENDATION_BATCH
            val excludeGames = lastDiscoveryResults.map { it.name }.takeLast(50)
            
            // 3. Get recommendations from AI (already sorted by confidence)
            // Use limited library to avoid token limits with large libraries
            val aiResult = aiRepository.getLibraryBasedRecommendations(
                games = limitedLibrary, 
                count = count,
                forceRefresh = forceRefresh,
                excludeGames = excludeGames
            )
            
            aiResult.fold(
                onSuccess = { suggestions ->
                    // Get games sorted by confidence
                    val sortedGameNames = suggestions.getGamesSortedByConfidence()
                    
                    // Filter out games we already have displayed if loading more
                    val newSuggestions = if (isLoadMore) {
                        val existingNames = lastDiscoveryResults.map { it.name.lowercase() }.toSet()
                        sortedGameNames.filter { it.lowercase() !in existingNames }
                    } else {
                        sortedGameNames
                    }

                    if (newSuggestions.isEmpty() && isLoadMore) {
                        android.util.Log.d(TAG, "No new games to load")
                        return@fold
                    }

                    // Fetch games in parallel
                    val games = fetchGamesForNames(newSuggestions)

                    val finalGames = if (isLoadMore) {
                        (lastDiscoveryResults + games).distinctBy { it.id }
                    } else {
                        games.distinctBy { it.id }
                    }
                    
                    lastDiscoveryResults = finalGames
                    
                    // Filter out library games using the freshly updated library IDs
                    val filtered = finalGames.filter { it.id !in gamesInLibrary }
                    
                    android.util.Log.d(TAG, "AI recommendations: ${finalGames.size} total, ${filtered.size} after filtering out library games")
                    
                    val resultState = RequestState.Success(filtered)
                    _discoveryState.value = _discoveryState.value.copy(games = resultState)
                    onComplete?.invoke(resultState)
                    
                    // Reset pagination on fresh load only (load more increments before calling)
                    if (!isLoadMore) {
                        discoveryFilterPage = 1
                    }
                },
                onFailure = { e ->
                    val error = RequestState.Error(e.message ?: "Failed to get AI recommendations")
                    if (!isLoadMore) {
                        _discoveryState.value = _discoveryState.value.copy(games = error)
                        onComplete?.invoke(error)
                    }
                }
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error fetching AI recommendations", e)
            val error = RequestState.Error("An error occurred: ${e.message}")
            if (!isLoadMore) {
                _discoveryState.value = _discoveryState.value.copy(games = error)
                onComplete?.invoke(error)
            }
        } finally {
            if (isLoadMore) {
                isLoadingMoreDiscovery = false
            }
        }
    }
    
    /**
     * Fetch Game objects for a list of game names.
     * Uses parallel requests for faster loading with thread-safe deduplication.
     */
    private suspend fun fetchGamesForNames(gameNames: List<String>): List<Game> {
        // Use thread-safe collections for concurrent access
        val seenIds = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
        
        // Process in batches for better performance
        val batchSize = 4
        val allResults = mutableListOf<Game>()
        
        for (batch in gameNames.chunked(batchSize)) {
            coroutineScope {
                val batchResults = batch.map { gameName ->
                    async {
                        try {
                            // Use filter to skip Loading state and add timeout to prevent hanging
                            val searchResult = kotlinx.coroutines.withTimeoutOrNull(10_000L) {
                                gameRepository.searchGames(gameName, page = 1, pageSize = 1)
                                    .filter { it !is RequestState.Loading }
                                    .first()
                            }
                            if (searchResult is RequestState.Success && searchResult.data.isNotEmpty()) {
                                searchResult.data.first()
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "Failed to fetch game: $gameName", e)
                            null
                        }
                    }
                }.awaitAll()
                
                // Thread-safe deduplication: only add if not already seen
                batchResults.filterNotNull().forEach { game ->
                    if (seenIds.add(game.id)) {
                        // Successfully added to set means it wasn't there before
                        synchronized(allResults) {
                            allResults.add(game)
                        }
                    }
                }
            }
        }
        
        return allResults
    }
}
