package com.example.arcadia.presentation.screens.myGames

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.presentation.base.UndoableViewModel
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.SortOrder
import com.example.arcadia.domain.usecase.AddGameToLibraryUseCase
import com.example.arcadia.domain.usecase.FilterGamesUseCase
import com.example.arcadia.domain.usecase.RemoveGameFromLibraryUseCase
import com.example.arcadia.domain.usecase.SortGamesUseCase
import com.example.arcadia.presentation.components.MediaLayout
import com.example.arcadia.presentation.components.QuickSettingsState
import com.example.arcadia.presentation.components.SortType
import com.example.arcadia.util.RequestState

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
    val originalGameBeforeEdit: GameListEntry? = null, // For reopening the sheet with unsaved data
    // Reorder mode - only enabled when sorting by rating and user has 5+ games with 10/10 rating
    val isReorderModeEnabled: Boolean = false,
    val canReorder: Boolean = false, // Whether reordering is possible (5+ 10/10 games + rating sort)
)

/**
 * ViewModel for My Games screen.
 * Uses FilterGamesUseCase, SortGamesUseCase, and RemoveGameFromLibraryUseCase 
 * to avoid duplicating business logic.
 */
class MyGamesViewModel(
    gameListRepository: GameListRepository,
    private val preferencesManager: com.example.arcadia.util.PreferencesManager,
    addGameToLibraryUseCase: AddGameToLibraryUseCase,
    removeGameFromLibraryUseCase: RemoveGameFromLibraryUseCase,
    private val filterGamesUseCase: FilterGamesUseCase,
    private val sortGamesUseCase: SortGamesUseCase
) : UndoableViewModel(gameListRepository, addGameToLibraryUseCase, removeGameFromLibraryUseCase) {
    
    companion object {
        private const val NOTIFICATION_DURATION_MS = 2000L
        private const val UNSAVED_CHANGES_TIMEOUT_MS = 5000L
    }
    
    var screenState by mutableStateOf(MyGamesScreenState())
        private set
    
    // Job for unsaved changes snackbar auto-dismiss
    private var unsavedChangesJob: kotlinx.coroutines.Job? = null

    init {
        // Load saved filter preferences
        val savedGenres = preferencesManager.getSelectedGenres()
        val savedStatuses = preferencesManager.getSelectedStatuses()
        
        // Load other settings
        val savedMediaLayout = preferencesManager.getMediaLayout()?.let { 
            try { MediaLayout.valueOf(it) } catch (e: Exception) { null } 
        }
        val savedSortType = preferencesManager.getSortType()?.let {
            try { SortType.valueOf(it) } catch (e: Exception) { null }
        }
        val savedSortOrder = preferencesManager.getSortOrder()?.let {
            try { com.example.arcadia.presentation.components.SortOrder.valueOf(it) } catch (e: Exception) { null }
        }
        val savedShowDateAdded = preferencesManager.getShowDateAdded()
        val savedShowReleaseDate = preferencesManager.getShowReleaseDate()
        
        screenState = screenState.copy(
            quickSettingsState = screenState.quickSettingsState.copy(
                selectedGenres = savedGenres,
                selectedStatuses = savedStatuses,
                mediaLayout = savedMediaLayout ?: screenState.quickSettingsState.mediaLayout,
                sortType = savedSortType ?: screenState.quickSettingsState.sortType,
                sortOrder = savedSortOrder ?: screenState.quickSettingsState.sortOrder,
                showDateAdded = savedShowDateAdded,
                showReleaseDate = savedShowReleaseDate
            )
        )
        
        // Also update the main sortOrder based on the loaded quick settings
        val initialSortOrder = mapToRepositorySortOrder(
            screenState.quickSettingsState.sortType,
            screenState.quickSettingsState.sortOrder
        )
        screenState = screenState.copy(sortOrder = initialSortOrder)
        
        loadGames()
    }
    
    fun getAvailableGenres(): List<String> {
        return screenState.allGames
            .flatMap { it.genres }
            .distinct()
            .sorted()
    }
    
    fun getAvailableStatuses(): List<GameStatus> {
        return GameStatus.entries.toList()
    }
    
    fun getFilteredGamesCount(): Int {
        val filteredGames = filterGamesUseCase(
            games = screenState.allGames,
            genres = screenState.quickSettingsState.selectedGenres,
            statuses = screenState.quickSettingsState.selectedStatuses
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
     * Uses FilterGamesUseCase and SortGamesUseCase for business logic.
     * Also persists filter preferences and checks if reordering is possible.
     */
    private fun applyCurrentFilters() {
        val filteredGames = filterGamesUseCase(
            games = screenState.allGames,
            genres = screenState.quickSettingsState.selectedGenres,
            statuses = screenState.quickSettingsState.selectedStatuses
        )
        val sortedGames = sortGamesUseCase(filteredGames, screenState.sortOrder)
        
        // Check if reordering is possible (2+ games with the same rating AND sorting by rating)
        val isRatingSorted = screenState.sortOrder == SortOrder.RATING_HIGH || screenState.sortOrder == SortOrder.RATING_LOW
        // Group games by rating and check if any rating has 2+ games
        val hasReorderableGames = sortedGames
            .filter { it.rating != null }
            .groupBy { it.rating }
            .any { (_, games) -> games.size >= 2 }
        val canReorder = isRatingSorted && hasReorderableGames
        
        android.util.Log.d("MyGamesVM", "applyCurrentFilters: isRatingSorted=$isRatingSorted, hasReorderableGames=$hasReorderableGames, canReorder=$canReorder")
        
        screenState = screenState.copy(
            games = RequestState.Success(sortedGames),
            canReorder = canReorder,
            // Disable reorder mode if no longer valid
            isReorderModeEnabled = if (!canReorder) false else screenState.isReorderModeEnabled
        )
        
        // Persist filter preferences
        with(screenState.quickSettingsState) {
            preferencesManager.saveSelectedGenres(selectedGenres)
            preferencesManager.saveSelectedStatuses(selectedStatuses)
            preferencesManager.saveMediaLayout(mediaLayout.name)
            preferencesManager.saveSortType(sortType.name)
            preferencesManager.saveSortOrder(sortOrder.name)
            preferencesManager.saveShowDateAdded(showDateAdded)
            preferencesManager.saveShowReleaseDate(showReleaseDate)
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
    
    // Track the index of the removed game for restoration at the same position
    private var removedGameIndex: Int = -1
    
    fun removeGameWithUndo(game: GameListEntry) {
        // Get the most up-to-date version of the game from allGames to preserve any recent changes
        // (e.g., rating updates that might not have been reflected in the UI yet)
        val currentGame = screenState.allGames.find { it.id == game.id } ?: game
        
        // Remember the index before removal for restoration
        removedGameIndex = screenState.allGames.indexOfFirst { it.id == currentGame.id }
        
        removeGameWithUndo(
            game = currentGame,
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
            // Restore at the original index if valid, otherwise append to the end
            val restoredAllGames = if (removedGameIndex >= 0 && removedGameIndex <= screenState.allGames.size) {
                screenState.allGames.toMutableList().apply {
                    add(removedGameIndex, restoredGame)
                }
            } else {
                screenState.allGames + restoredGame
            }
            
            // Reset the index tracker
            removedGameIndex = -1
            
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
     * Show snackbar when user dismisses rating sheet with unsaved changes.
     * Uses a dedicated job for proper cancellation.
     */
    fun showUnsavedChangesSnackbar(unsavedGame: GameListEntry) {
        // Cancel any existing job
        unsavedChangesJob?.cancel()
        
        // Show the snackbar
        screenState = screenState.copy(
            showUnsavedChangesSnackbar = true,
            unsavedChangesGame = unsavedGame
        )
        
        // Auto-dismiss after timeout
        unsavedChangesJob = launchWithKey("unsaved_changes_snackbar") {
            kotlinx.coroutines.delay(UNSAVED_CHANGES_TIMEOUT_MS)
            if (screenState.showUnsavedChangesSnackbar) {
                dismissUnsavedChangesSnackbar()
            }
        }
    }
    
    /**
     * Save the unsaved changes when user taps "Save" on the snackbar
     */
    fun saveUnsavedChanges() {
        unsavedChangesJob?.cancel()
        screenState.unsavedChangesGame?.let { game ->
            updateGameEntry(game)
        }
        screenState = screenState.copy(
            showUnsavedChangesSnackbar = false,
            unsavedChangesGame = null,
            originalGameBeforeEdit = null
        )
    }
    
    /**
     * Dismiss the unsaved changes snackbar
     */
    fun dismissUnsavedChangesSnackbar() {
        unsavedChangesJob?.cancel()
        screenState = screenState.copy(
            showUnsavedChangesSnackbar = false,
            unsavedChangesGame = null,
            originalGameBeforeEdit = null
        )
    }
    
    /**
     * Reopen the GameRatingSheet with the unsaved changes
     */
    fun reopenWithUnsavedChanges() {
        unsavedChangesJob?.cancel()
        screenState.unsavedChangesGame?.let { unsavedGame ->
            screenState = screenState.copy(
                selectedGameToEdit = unsavedGame,
                showUnsavedChangesSnackbar = false,
                unsavedChangesGame = null
            )
        }
    }
    
    fun selectGameToEdit(game: GameListEntry?) {
        // Cancel unsaved changes snackbar when opening a new edit sheet
        if (game != null) {
            unsavedChangesJob?.cancel()
            screenState = screenState.copy(
                selectedGameToEdit = game,
                originalGameBeforeEdit = game,
                showUnsavedChangesSnackbar = false,
                unsavedChangesGame = null
            )
        } else {
            screenState = screenState.copy(
                selectedGameToEdit = null
                // Keep originalGameBeforeEdit for potential unsaved changes handling
            )
        }
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
     * Toggle reorder mode on/off.
     * Only works when canReorder is true (5+ games with 10/10 rating and rating sort active).
     */
    fun toggleReorderMode() {
        if (screenState.canReorder) {
            screenState = screenState.copy(isReorderModeEnabled = !screenState.isReorderModeEnabled)
        }
    }
    
    /**
     * Exit reorder mode.
     */
    fun exitReorderMode() {
        screenState = screenState.copy(isReorderModeEnabled = false)
    }
    
    /**
     * Handle reordering of games with the same rating.
     * This method is called when a user drags a game to a new position within games that have the same rating.
     * It recalculates importance values and saves them to Firebase.
     * 
     * @param fromIndex The original index of the game being moved
     * @param toIndex The new index where the game should be placed
     */
    fun onGameReorder(fromIndex: Int, toIndex: Int) {
        val currentGames = (screenState.games as? RequestState.Success)?.data ?: return
        if (fromIndex == toIndex || fromIndex < 0 || toIndex < 0) return
        if (fromIndex >= currentGames.size || toIndex >= currentGames.size) return
        
        val movedGame = currentGames[fromIndex]
        
        // Only allow reordering rated games
        if (movedGame.rating == null) return
        
        val targetRating = movedGame.rating
        
        // Create new list with reordered games
        val mutableGames = currentGames.toMutableList()
        mutableGames.removeAt(fromIndex)
        val adjustedToIndex = toIndex.coerceIn(0, mutableGames.size)
        mutableGames.add(adjustedToIndex, movedGame)
        
        // Recalculate importance for ALL games with the SAME rating based on their new positions
        // This ensures games stay in the order the user set when sorted by rating
        val gamesWithSameRating = mutableGames.filter { it.rating == targetRating }
        val importanceUpdates = mutableMapOf<String, Int>()
        
        val updatedGames = mutableGames.map { game ->
            if (game.rating == targetRating) {
                // Calculate new importance: higher index in same-rating group = lower importance
                val positionInGroup = gamesWithSameRating.indexOf(game)
                val newImportance = (gamesWithSameRating.size - positionInGroup) * 1000
                importanceUpdates[game.id] = newImportance
                game.copy(importance = newImportance)
            } else {
                game
            }
        }
        
        // Update UI immediately (optimistic update)
        screenState = screenState.copy(
            games = RequestState.Success(updatedGames),
            allGames = screenState.allGames.map { existing ->
                updatedGames.find { it.id == existing.id } ?: existing
            }
        )
        
        // Save to Firebase
        saveImportanceUpdates(importanceUpdates)
    }
    
    /**
     * Save importance updates to Firebase.
     */
    private fun saveImportanceUpdates(updates: Map<String, Int>) {
        launchWithKey("save_importance") {
            when (val result = gameListRepository.updateGamesImportance(updates)) {
                is RequestState.Success -> {
                    // Success - UI already updated
                }
                is RequestState.Error -> {
                    // Revert on error by reloading
                    loadGames()
                    showTemporaryNotification(
                        setNotification = {
                            screenState = screenState.copy(
                                showSuccessNotification = true,
                                notificationMessage = "Failed to save order"
                            )
                        },
                        clearNotification = {
                            screenState = screenState.copy(showSuccessNotification = false)
                        },
                        duration = NOTIFICATION_DURATION_MS
                    )
                }
                else -> {}
            }
        }
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
