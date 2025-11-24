package com.example.arcadia.presentation.screens.myGames

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.GameListEntry
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
    val sortOrder: SortOrder = SortOrder.RATING_HIGH,
    val selectedGameToEdit: GameListEntry? = null,
    val showSuccessNotification: Boolean = false,
    val notificationMessage: String = "",
    val deletedGame: GameListEntry? = null,
    val showUndoSnackbar: Boolean = false,
    val showQuickRateDialog: Boolean = false,
    val gameToQuickRate: GameListEntry? = null,
    val showQuickSettingsDialog: Boolean = false,
    val quickSettingsState: QuickSettingsState = QuickSettingsState()
)

class MyGamesViewModel(
    private val gameListRepository: GameListRepository,
    private val preferencesManager: com.example.arcadia.util.PreferencesManager
) : ViewModel() {
    
    var screenState by mutableStateOf(MyGamesScreenState())
        private set
    
    private var gamesJob: Job? = null
    private val pendingDeletions = mutableMapOf<String, Job>()

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
    
    private fun filterGames(games: List<GameListEntry>): List<GameListEntry> {
        val selectedGenres = screenState.quickSettingsState.selectedGenres
        val selectedStatuses = screenState.quickSettingsState.selectedStatuses
        
        return filterGamesWithState(games, selectedGenres, selectedStatuses)
    }
    
    private fun filterGamesWithState(
        games: List<GameListEntry>,
        selectedGenres: Set<String>,
        selectedStatuses: Set<GameStatus>
    ): List<GameListEntry> {
        // Early return if no filters
        if (selectedGenres.isEmpty() && selectedStatuses.isEmpty()) {
            return games
        }
        
        // Convert to HashSet for O(1) lookup
        val genreSet = selectedGenres.toHashSet()
        val statusSet = selectedStatuses.toHashSet()
        
        return games.filter { game ->
            // Genre filter: if no genres selected, pass all; otherwise check if game has at least one selected genre
            // Use case-insensitive comparison
            val passesGenreFilter = genreSet.isEmpty() || 
                game.genres.any { genre -> 
                    genreSet.any { selectedGenre -> 
                        selectedGenre.equals(genre, ignoreCase = true) 
                    }
                }
            
            // Status filter: if no statuses selected, pass all; otherwise check if game status matches
            val passesStatusFilter = statusSet.isEmpty() || 
                statusSet.contains(game.status)
            
            // Game must pass both filters
            passesGenreFilter && passesStatusFilter
        }
    }
    
    private fun sortGames(games: List<GameListEntry>, sortOrder: SortOrder): List<GameListEntry> {
        return when (sortOrder) {
            SortOrder.TITLE_A_Z -> games.sortedBy { it.name.lowercase() }
            SortOrder.TITLE_Z_A -> games.sortedByDescending { it.name.lowercase() }
            SortOrder.NEWEST_FIRST -> games.sortedByDescending { it.addedAt }
            SortOrder.OLDEST_FIRST -> games.sortedBy { it.addedAt }
            SortOrder.RATING_HIGH -> games.sortedByDescending { it.rating ?: -1f }
            SortOrder.RATING_LOW -> games.sortedBy { it.rating ?: Float.MAX_VALUE }
            SortOrder.RELEASE_NEW -> games.sortedByDescending { it.releaseDate ?: "" }
            SortOrder.RELEASE_OLD -> games.sortedBy { it.releaseDate ?: "9999" }
        }
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
        val filteredGames = filterGames(screenState.allGames)
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
        // Validate genres exist in available genres
        val validGenres = genres.filter { it in getAvailableGenres() }.toSet()
        
        screenState = screenState.copy(
            quickSettingsState = screenState.quickSettingsState.copy(
                selectedGenres = validGenres,
                selectedStatuses = statuses
            )
        )
        
        applyCurrentFilters()
    }
    
    fun clearAllFilters() {
        screenState = screenState.copy(
            quickSettingsState = screenState.quickSettingsState.copy(
                selectedGenres = emptySet(),
                selectedStatuses = emptySet()
            )
        )
        
        applyCurrentFilters()
    }
    
    fun clearGenreFilters() {
        screenState = screenState.copy(
            quickSettingsState = screenState.quickSettingsState.copy(
                selectedGenres = emptySet()
            )
        )
        applyCurrentFilters()
    }
    
    fun clearStatusFilters() {
        screenState = screenState.copy(
            quickSettingsState = screenState.quickSettingsState.copy(
                selectedStatuses = emptySet()
            )
        )
        applyCurrentFilters()
    }
    
    private fun applyCurrentFilters() {
        val filteredGames = filterGames(screenState.allGames)
        val sortedGames = sortGames(filteredGames, screenState.sortOrder)
        screenState = screenState.copy(games = RequestState.Success(sortedGames))
        
        // Save preferences
        preferencesManager.saveSelectedGenres(screenState.quickSettingsState.selectedGenres)
        preferencesManager.saveSelectedStatuses(screenState.quickSettingsState.selectedStatuses)
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
        gamesJob?.cancel()
        gamesJob = viewModelScope.launch {
            val sortOrder = screenState.sortOrder
            val selectedGenres = screenState.quickSettingsState.selectedGenres
            val selectedStatuses = screenState.quickSettingsState.selectedStatuses
            
            // Fetch all games without genre filtering (client-side filtering will be applied)
            gameListRepository.getGameList(sortOrder)
                .collect { state ->
                    when (state) {
                        is RequestState.Success -> {
                            // Store unfiltered games
                            val allGames = state.data
                            screenState = screenState.copy(allGames = allGames)
                            
                            // Apply client-side filtering with captured state
                            val filteredGames = filterGamesWithState(allGames, selectedGenres, selectedStatuses)
                            val sortedGames = sortGames(filteredGames, sortOrder)
                            screenState = screenState.copy(games = RequestState.Success(sortedGames))
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
        // Execute any existing pending deletion for this game immediately
        pendingDeletions[game.id]?.cancel()

        screenState = screenState.copy(
            deletedGame = game,
            showUndoSnackbar = true
        )
        
        // Start 5-second countdown before actual deletion
        val job = viewModelScope.launch {
            delay(5000) // 5 seconds to undo
            // Actually delete from Firebase
            gameListRepository.removeGameFromList(game.id)
            pendingDeletions.remove(game.id)

            // Only clear UI state if this is still the displayed deleted game
            if (screenState.deletedGame?.id == game.id) {
                screenState = screenState.copy(
                    deletedGame = null,
                    showUndoSnackbar = false
                )
            }
        }
        pendingDeletions[game.id] = job
    }
    
    fun undoDeletion() {
        val deletedGame = screenState.deletedGame
        if (deletedGame != null) {
            // Cancel the pending deletion
            pendingDeletions[deletedGame.id]?.cancel()
            pendingDeletions.remove(deletedGame.id)

            viewModelScope.launch {
                // Re-add the game using addGameListEntry
                gameListRepository.addGameListEntry(deletedGame)
                screenState = screenState.copy(
                    deletedGame = null,
                    showUndoSnackbar = false,
                    showSuccessNotification = true,
                    notificationMessage = "Game restored"
                )
                delay(2000)
                screenState = screenState.copy(showSuccessNotification = false)
            }
        }
    }
    
    fun dismissUndoSnackbar() {
        screenState = screenState.copy(showUndoSnackbar = false)
    }
    
    fun selectGameToEdit(game: GameListEntry?) {
        screenState = screenState.copy(selectedGameToEdit = game)
    }

    fun updateGameEntry(entry: GameListEntry) {
        viewModelScope.launch {
            when (val result = gameListRepository.updateGameEntry(entry)) {
                is RequestState.Success -> {
                    screenState = screenState.copy(
                        selectedGameToEdit = null,
                        showSuccessNotification = true,
                        notificationMessage = "Game updated successfully"
                    )
                    // Auto dismiss notification handled by UI or we can reset state after delay
                    delay(2000)
                    screenState = screenState.copy(showSuccessNotification = false)
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
        val newSortOrder = when (screenState.quickSettingsState.sortType) {
            com.example.arcadia.presentation.components.SortType.TITLE -> {
                if (screenState.quickSettingsState.sortOrder == com.example.arcadia.presentation.components.SortOrder.ASCENDING) {
                    SortOrder.TITLE_A_Z
                } else {
                    SortOrder.TITLE_Z_A
                }
            }
            com.example.arcadia.presentation.components.SortType.RATING -> {
                if (screenState.quickSettingsState.sortOrder == com.example.arcadia.presentation.components.SortOrder.ASCENDING) {
                    SortOrder.RATING_LOW
                } else {
                    SortOrder.RATING_HIGH
                }
            }
            com.example.arcadia.presentation.components.SortType.ADDED -> {
                if (screenState.quickSettingsState.sortOrder == com.example.arcadia.presentation.components.SortOrder.ASCENDING) {
                    SortOrder.OLDEST_FIRST
                } else {
                    SortOrder.NEWEST_FIRST
                }
            }
            com.example.arcadia.presentation.components.SortType.DATE -> {
                // DATE is for release date - for now use added date
                if (screenState.quickSettingsState.sortOrder == com.example.arcadia.presentation.components.SortOrder.ASCENDING) {
                    SortOrder.OLDEST_FIRST
                } else {
                    SortOrder.NEWEST_FIRST
                }
            }
        }
        
        if (screenState.sortOrder != newSortOrder) {
            screenState = screenState.copy(sortOrder = newSortOrder)
            loadGames()
        }
        
        dismissQuickSettingsDialog()
    }
}
