package com.example.arcadia.presentation.screens.myGames

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.presentation.base.UndoableViewModel
import com.example.arcadia.presentation.base.SortCriterion
import com.example.arcadia.util.FilterHelper
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.SortOrder
import com.example.arcadia.presentation.components.MediaLayout
import com.example.arcadia.presentation.components.QuickSettingsState
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class MyGamesScreenState(
    val games: RequestState<List<GameListEntry>> = RequestState.Idle,
    val allGames: List<GameListEntry> = emptyList(),
    val sortOrder: SortOrder = SortOrder.RATING_HIGH, // Default: sort by rating (highest first)
    val selectedGameToEdit: GameListEntry? = null,
    val showSuccessNotification: Boolean = false,
    val notificationMessage: String = "",
    // Undo state is now in UndoableViewModel
    val showQuickRateDialog: Boolean = false,
    val gameToQuickRate: GameListEntry? = null,
    val showQuickSettingsDialog: Boolean = false,
    val quickSettingsState: QuickSettingsState = QuickSettingsState(),
    // Unsaved changes snackbar
    val showUnsavedChangesSnackbar: Boolean = false,
    val unsavedChangesGame: GameListEntry? = null,
)

class MyGamesViewModel(
    private val gameListRepository: GameListRepository,
    private val preferencesManager: com.example.arcadia.util.PreferencesManager
) : UndoableViewModel(gameListRepository) {
    
    companion object {
        private const val NOTIFICATION_DURATION_MS = 2000L
        private const val UNDO_DELAY_MS = 5000L
    }
    
    var screenState by mutableStateOf(MyGamesScreenState())
        private set
    
    // private var gamesJob: Job? = null // Removed in favor of launchWithKey

    init {
        // Load saved filter preferences
        val savedGenres = preferencesManager.getSelectedGenres()
        val savedStatuses = preferencesManager.getSelectedStatuses()
        
        screenState = screenState.copy(
            quickSettingsState = screenState.quickSettingsState.copy(
                selectedGenres = savedGenres,
                selectedStatuses = savedStatuses
            )
        )
        
        loadGames()
    }
    
    private fun mapToSortCriterion(sortOrder: SortOrder): SortCriterion {
        return when (sortOrder) {
            SortOrder.TITLE_A_Z -> SortCriterion.Title
            SortOrder.TITLE_Z_A -> SortCriterion.TitleDesc
            SortOrder.NEWEST_FIRST -> SortCriterion.DateAddedDesc
            SortOrder.OLDEST_FIRST -> SortCriterion.DateAdded
            SortOrder.RATING_HIGH -> SortCriterion.RatingDesc
            SortOrder.RATING_LOW -> SortCriterion.Rating
            SortOrder.RELEASE_NEW -> SortCriterion.ReleaseDateDesc
            SortOrder.RELEASE_OLD -> SortCriterion.ReleaseDate
        }
    }
    
    fun getAvailableGenres(): List<String> {
        return FilterHelper.extractGenres(screenState.allGames)
    }
    
    fun getAvailableStatuses(): List<GameStatus> {
        return FilterHelper.getAllStatuses()
    }
    
    fun getFilteredGamesCount(): Int {
        val filteredGames = FilterHelper.applyFilters(
            games = screenState.allGames,
            selectedGenres = screenState.quickSettingsState.selectedGenres,
            selectedStatuses = screenState.quickSettingsState.selectedStatuses
        )
        return filteredGames.size
    }
    
    fun getTotalGamesCount(): Int {
        return screenState.allGames.size
    }
    
    fun saveFilterPreset(name: String) {
        val preset = com.example.arcadia.util.FilterPreset(
            name = name,
            genres = screenState.quickSettingsState.selectedGenres,
            statuses = screenState.quickSettingsState.selectedStatuses
        )
        preferencesManager.saveFilterPreset(preset)
    }
    
    fun getFilterPresets(): List<com.example.arcadia.util.FilterPreset> {
        return preferencesManager.getFilterPresets()
    }
    
    fun loadFilterPreset(preset: com.example.arcadia.util.FilterPreset) {
        screenState = screenState.copy(
            quickSettingsState = screenState.quickSettingsState.copy(
                selectedGenres = preset.genres,
                selectedStatuses = preset.statuses
            )
        )
        applyCurrentFilters()
    }
    
    fun deleteFilterPreset(presetName: String) {
        preferencesManager.deleteFilterPreset(presetName)
    }
    
    fun applyFilters(genres: Set<String>, statuses: Set<GameStatus>) {
        updateFilters(genres = genres, statuses = statuses)
    }
    
    fun clearAllFilters() {
        updateFilters(genres = emptySet(), statuses = emptySet())
    }
    
    fun clearGenreFilters() {
        updateFilters(genres = emptySet())
    }
    
    fun clearStatusFilters() {
        updateFilters(statuses = emptySet())
    }

    /**
     * Updates filter settings and applies them to the game list.
     * Automatically saves preferences and updates the UI.
     */
    private fun updateFilters(
        genres: Set<String>? = null,
        statuses: Set<GameStatus>? = null
    ) {
        val currentFilters = screenState.quickSettingsState

        screenState = screenState.copy(
            quickSettingsState = currentFilters.copy(
                selectedGenres = genres ?: currentFilters.selectedGenres,
                selectedStatuses = statuses ?: currentFilters.selectedStatuses
            )
        )

        applyCurrentFilters()
    }
    
    /**
     * Applies current filter and sort settings to allGames and updates the displayed list.
     * Also persists filter preferences.
     */
    private fun applyCurrentFilters() {
        val filteredGames = FilterHelper.applyFilters(
            games = screenState.allGames,
            selectedGenres = screenState.quickSettingsState.selectedGenres,
            selectedStatuses = screenState.quickSettingsState.selectedStatuses
        )
        val sortedGames = FilterHelper.sortGames(filteredGames, mapToSortCriterion(screenState.sortOrder))
        screenState = screenState.copy(games = RequestState.Success(sortedGames))
        
        // Persist filter preferences
        with(screenState.quickSettingsState) {
            preferencesManager.saveSelectedGenres(selectedGenres)
            preferencesManager.saveSelectedStatuses(selectedStatuses)
        }
    }
    
    fun toggleSortOrder() {
        val newSortOrder = when (screenState.sortOrder) {
            SortOrder.NEWEST_FIRST -> SortOrder.OLDEST_FIRST
            SortOrder.OLDEST_FIRST -> SortOrder.NEWEST_FIRST
            SortOrder.TITLE_A_Z -> SortOrder.TITLE_Z_A
            SortOrder.TITLE_Z_A -> SortOrder.TITLE_A_Z
            SortOrder.RATING_HIGH -> SortOrder.RATING_LOW
            SortOrder.RATING_LOW -> SortOrder.RATING_HIGH
            SortOrder.RELEASE_NEW -> SortOrder.RELEASE_OLD
            SortOrder.RELEASE_OLD -> SortOrder.RELEASE_NEW
        }
        screenState = screenState.copy(sortOrder = newSortOrder)
        loadGames()
    }
    
    fun setSortOrder(sortOrder: SortOrder) {
        if (screenState.sortOrder != sortOrder) {
            screenState = screenState.copy(sortOrder = sortOrder)
            loadGames()
        }
    }
    
    private fun loadGames() {
        launchWithKey("load_games") {
            val sortOrder = screenState.sortOrder

            // Fetch all games without genre filtering (client-side filtering will be applied)
            gameListRepository.getGameList(sortOrder)
                .collect { state ->
                    when (state) {
                        is RequestState.Success -> {
                            // Store unfiltered games
                            val allGames = state.data
                            screenState = screenState.copy(allGames = allGames)
                            
                            // Apply client-side filtering with current state (not captured)
                            // This ensures we always use the latest filter settings even when
                            // Firestore updates trigger the snapshot listener
                            applyCurrentFilters()
                        }
                        is RequestState.Error -> {
                            // Keep existing allGames and filter state
                            screenState = screenState.copy(games = state)
                        }
                        is RequestState.Loading -> {
                            screenState = screenState.copy(games = state)
                        }
                        else -> {
                            screenState = screenState.copy(games = state)
                        }
                    }
                }
        }
    }
    
    fun removeGameWithUndo(game: GameListEntry) {
        removeGameWithUndo(
            game = game,
            onOptimisticRemove = { removedGame ->
                val updatedAllGames = screenState.allGames.filter { it.id != removedGame.id }
                screenState = screenState.copy(allGames = updatedAllGames)
                applyCurrentFilters()
            },
            onActualRemove = {
                // Success - already removed from UI
            },
            onError = { restoredGame, error ->
                // Restore on error
                val restoredAllGames = screenState.allGames + restoredGame
                
                showTemporaryNotification(
                    setNotification = {
                        screenState = screenState.copy(
                            allGames = restoredAllGames,
                            showSuccessNotification = true,
                            notificationMessage = "Failed to remove: $error"
                        )
                        applyCurrentFilters()
                    },
                    clearNotification = {
                        screenState = screenState.copy(showSuccessNotification = false)
                    },
                    duration = NOTIFICATION_DURATION_MS
                )
            }
        )
    }
    
    fun undoDeletion() {
        undoRemoval { restoredGame ->
            val restoredAllGames = screenState.allGames + restoredGame
            
            showTemporaryNotification(
                setNotification = {
                    screenState = screenState.copy(
                        allGames = restoredAllGames,
                        showSuccessNotification = true,
                        notificationMessage = "Game restored"
                    )
                    applyCurrentFilters()
                },
                clearNotification = {
                    screenState = screenState.copy(showSuccessNotification = false)
                },
                duration = NOTIFICATION_DURATION_MS
            )
        }
    }
    
    fun dismissUndoSnackbar() {
        dismissUndoSnackbar(
            onActualRemove = {
                // Success
            },
            onError = { restoredGame, error ->
                // Restore on error
                val restoredAllGames = screenState.allGames + restoredGame
                
                showTemporaryNotification(
                    setNotification = {
                        screenState = screenState.copy(
                            allGames = restoredAllGames,
                            showSuccessNotification = true,
                            notificationMessage = "Failed to remove: $error"
                        )
                        applyCurrentFilters()
                    },
                    clearNotification = {
                        screenState = screenState.copy(showSuccessNotification = false)
                    },
                    duration = NOTIFICATION_DURATION_MS
                )
            }
        )
    }
    
    /**
     * Show snackbar when user dismisses rating sheet with unsaved changes
     */
    fun showUnsavedChangesSnackbar(unsavedGame: GameListEntry) {
        showTemporaryNotification(
            setNotification = {
                screenState = screenState.copy(
                    showUnsavedChangesSnackbar = true,
                    unsavedChangesGame = unsavedGame
                )
            },
            clearNotification = {
                if (screenState.showUnsavedChangesSnackbar) {
                    dismissUnsavedChangesSnackbar()
                }
            },
            duration = UNDO_DELAY_MS
        )
    }
    
    /**
     * Save the unsaved changes when user taps "Save" on the snackbar
     */
    fun saveUnsavedChanges() {
        screenState.unsavedChangesGame?.let { game ->
            updateGameEntry(game)
        }
        dismissUnsavedChangesSnackbar()
    }
    
    /**
     * Dismiss the unsaved changes snackbar
     */
    fun dismissUnsavedChangesSnackbar() {
        screenState = screenState.copy(
            showUnsavedChangesSnackbar = false,
            unsavedChangesGame = null
        )
    }
    
    fun selectGameToEdit(game: GameListEntry?) {
        screenState = screenState.copy(selectedGameToEdit = game)
    }

    fun updateGameEntry(entry: GameListEntry) {
        launchWithKey("update_game_entry") {
            when (val result = gameListRepository.updateGameEntry(entry)) {
                is RequestState.Success -> {
                    // Update the allGames list with the new entry
                    val updatedAllGames = screenState.allGames.map { game ->
                        if (game.id == entry.id) entry else game
                    }
                    
                    showTemporaryNotification(
                        setNotification = {
                            screenState = screenState.copy(
                                allGames = updatedAllGames,
                                selectedGameToEdit = null,
                                showSuccessNotification = true,
                                notificationMessage = "Game updated successfully"
                            )
                            // Re-apply filters with the updated game data
                            applyCurrentFilters()
                        },
                        clearNotification = {
                            screenState = screenState.copy(showSuccessNotification = false)
                        },
                        duration = NOTIFICATION_DURATION_MS
                    )
                }
                is RequestState.Error -> {
                    // Handle error (maybe show snackbar)
                }
                else -> {}
            }
        }
    }
    
    fun dismissNotification() {
        screenState = screenState.copy(showSuccessNotification = false)
    }
    
    fun showQuickRateDialog(game: GameListEntry) {
        screenState = screenState.copy(
            showQuickRateDialog = true,
            gameToQuickRate = game
        )
    }
    
    fun dismissQuickRateDialog() {
        screenState = screenState.copy(
            showQuickRateDialog = false,
            gameToQuickRate = null
        )
    }
    
    fun quickRateGame(rating: Float) {
        screenState.gameToQuickRate?.let { game ->
            val updatedGame = game.copy(
                rating = rating,
                updatedAt = System.currentTimeMillis()
            )
            updateGameEntry(updatedGame)
            dismissQuickRateDialog()
        }
    }
    
    fun retry() {
        loadGames()
    }
    
    fun showQuickSettingsDialog() {
        screenState = screenState.copy(showQuickSettingsDialog = true)
    }
    
    fun dismissQuickSettingsDialog() {
        screenState = screenState.copy(showQuickSettingsDialog = false)
    }
    
    fun updateQuickSettings(settings: QuickSettingsState) {
        screenState = screenState.copy(quickSettingsState = settings)
        applyCurrentFilters()
    }
    
    fun applyQuickSettings() {
        applyCurrentFilters()
        
        // Map QuickSettingsState to repository SortOrder
        val newSortOrder = mapToRepositorySortOrder(
            screenState.quickSettingsState.sortType,
            screenState.quickSettingsState.sortOrder
        )

        if (screenState.sortOrder != newSortOrder) {
            screenState = screenState.copy(sortOrder = newSortOrder)
            loadGames()
        }
        
        dismissQuickSettingsDialog()
    }

    /**
     * Maps QuickSettings UI sort type and order to repository SortOrder enum.
     */
    private fun mapToRepositorySortOrder(
        sortType: com.example.arcadia.presentation.components.SortType,
        sortOrder: com.example.arcadia.presentation.components.SortOrder
    ): SortOrder {
        val isAscending = sortOrder == com.example.arcadia.presentation.components.SortOrder.ASCENDING

        return when (sortType) {
            com.example.arcadia.presentation.components.SortType.TITLE ->
                if (isAscending) SortOrder.TITLE_A_Z else SortOrder.TITLE_Z_A

            com.example.arcadia.presentation.components.SortType.RATING ->
                if (isAscending) SortOrder.RATING_LOW else SortOrder.RATING_HIGH

            com.example.arcadia.presentation.components.SortType.ADDED ->
                if (isAscending) SortOrder.OLDEST_FIRST else SortOrder.NEWEST_FIRST

            com.example.arcadia.presentation.components.SortType.DATE ->
                if (isAscending) SortOrder.RELEASE_OLD else SortOrder.RELEASE_NEW
        }
    }
    
    /**
     * Check if a game is pending deletion (to prevent race conditions).
     */
    fun isGamePendingDeletion(gameId: String): Boolean {
        return undoState.value.item?.id == gameId
    }
    
    /**
     * Cleanup when ViewModel is cleared to prevent memory leaks.
     */
    override fun onCleared() {
        super.onCleared()
        // Cancel games loading job
        // gamesJob?.cancel() // Handled by BaseViewModel
    }
}
