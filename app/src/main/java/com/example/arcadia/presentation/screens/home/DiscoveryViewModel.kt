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
import com.example.arcadia.domain.model.ReleaseTimeframe
import com.example.arcadia.domain.model.StudioFilterState
import com.example.arcadia.domain.model.StudioFilterType
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
    private val studioExpansionRepository: StudioExpansionRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "DiscoveryViewModel"
        private const val FILTER_DEBOUNCE_MS = 150L
        private const val DEVELOPER_SEARCH_DEBOUNCE_MS = 300L
        private const val DEFAULT_PAGE_SIZE = 40
        private const val MIN_GAMES_THRESHOLD = 15
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

    // Job management
    private var discoveryFilterJob: Job? = null
    private var developerSearchJob: Job? = null
    private var filterJob: Job? = null
    private var studioGamesJob: Job? = null

    init {
        loadSavedDiscoveryPreferences()
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
     */
    fun searchDevelopers(query: String) {
        developerSearchJob?.cancel()
        developerSearchJob = viewModelScope.launch {
            discoveryFilterState = discoveryFilterState.copy(isLoadingDevelopers = true)
            delay(DEVELOPER_SEARCH_DEBOUNCE_MS)

            try {
                val developers = studioExpansionRepository.getExpandedStudios(query)
                discoveryFilterState = discoveryFilterState.copy(
                    searchResults = developers.toList(),
                    expandedStudios = developers,
                    isLoadingDevelopers = false
                )
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Developer search failed", e)
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
        viewModelScope.launch {
            try {
                val subStudios = studioExpansionRepository.getExpandedStudios(developer)
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

        discoveryFilterJob?.cancel()
        discoveryFilterJob = viewModelScope.launch {
            _discoveryState.value = _discoveryState.value.copy(games = RequestState.Loading)
            onComplete?.invoke(RequestState.Loading)

            try {
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
            applyDiscoveryFilters()
            return
        }

        val filtered = lastDiscoveryResults.filter { it.id !in gamesInLibrary }
        android.util.Log.d(TAG, "Refiltered discovery: ${lastDiscoveryResults.size} -> ${filtered.size} games")
        _discoveryState.value = _discoveryState.value.copy(games = RequestState.Success(filtered))

        if (filtered.size < MIN_GAMES_THRESHOLD && !isLoadingMoreDiscovery) {
            loadMoreDiscoveryResults()
        }
    }

    /**
     * Load more discovery results for pagination.
     */
    fun loadMoreDiscoveryResults() {
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
        discoveryFilterJob?.cancel()
        developerSearchJob?.cancel()

        discoveryFilterState = DiscoveryFilterState()
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

        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(FILTER_DEBOUNCE_MS)

            try {
                val expandedStudios = studioExpansionRepository.getExpandedStudios(studioName)

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
        studioGamesJob?.cancel()
        studioGamesJob = viewModelScope.launch {
            _discoveryState.value = _discoveryState.value.copy(games = RequestState.Loading)

            studioFilteredGames.clear()
            studioFilterState = studioFilterState.copy(
                currentPage = 1,
                hasMorePages = true,
                isLoadingMore = false
            )

            try {
                val slugs = studioExpansionRepository.getStudioSlugs(parentStudio)
                studioFilterSlugs = slugs

                if (slugs.isBlank()) {
                    android.util.Log.w(TAG, "No slugs found for: $parentStudio")
                    _discoveryState.value = _discoveryState.value.copy(games = RequestState.Success(emptyList()))
                    studioFilterState = studioFilterState.copy(hasMorePages = false)
                    return@launch
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

        viewModelScope.launch {
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
        filterJob?.cancel()
        studioGamesJob?.cancel()
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
            DiscoverySortType.RELEVANCE -> null
            DiscoverySortType.RATING -> "${prefix}rating"
            DiscoverySortType.RELEASE_DATE -> "${prefix}released"
            DiscoverySortType.NAME -> "${prefix}name"
            DiscoverySortType.POPULARITY -> "${prefix}added"
        }
    }
}
