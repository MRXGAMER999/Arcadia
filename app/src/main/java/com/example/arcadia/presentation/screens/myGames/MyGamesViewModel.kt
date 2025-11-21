package com.example.arcadia.presentation.screens.myGames

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.GameListEntry
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
    val selectedGenre: String? = null,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
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
    private val gameListRepository: GameListRepository
) : ViewModel() {
    
    var screenState by mutableStateOf(MyGamesScreenState())
        private set
    
    private var gamesJob: Job? = null
    private val pendingDeletions = mutableMapOf<String, Job>()

    // Predefined genres matching the Figma design
    val availableGenres = listOf(
        "Action",
        "RPG",
        "Platformer",
        "Adventure",
        "Shooter",
        "Strategy"
    )
    
    init {
        loadGames()
    }
    
    fun selectGenre(genre: String?) {
        if (screenState.selectedGenre != genre) {
            screenState = screenState.copy(selectedGenre = genre)
            loadGames()
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
        gamesJob?.cancel()
        gamesJob = viewModelScope.launch {
            val selectedGenre = screenState.selectedGenre
            val sortOrder = screenState.sortOrder
            
            if (selectedGenre != null) {
                gameListRepository.getGameListByGenre(selectedGenre, sortOrder)
                    .collect { state ->
                        screenState = screenState.copy(games = state)
                    }
            } else {
                gameListRepository.getGameList(sortOrder)
                    .collect { state ->
                        screenState = screenState.copy(games = state)
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
    }
    
    fun applyQuickSettings() {
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
