package com.example.arcadia.presentation.screens.myGames

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.SortOrder
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
    val gameToQuickRate: GameListEntry? = null
)

class MyGamesViewModel(
    private val gameListRepository: GameListRepository
) : ViewModel() {
    
    var screenState by mutableStateOf(MyGamesScreenState())
        private set
    
    private var gamesJob: Job? = null
    
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
    
    private var undoJob: Job? = null
    
    fun removeGameWithUndo(game: GameListEntry) {
        screenState = screenState.copy(
            deletedGame = game,
            showUndoSnackbar = true
        )
        
        // Start 5-second countdown before actual deletion
        undoJob?.cancel()
        undoJob = viewModelScope.launch {
            delay(5000) // 5 seconds to undo
            if (screenState.deletedGame == game) {
                // Actually delete from Firebase
                gameListRepository.removeGameFromList(game.id)
                screenState = screenState.copy(
                    deletedGame = null,
                    showUndoSnackbar = false
                )
            }
        }
    }
    
    fun undoDeletion() {
        undoJob?.cancel()
        val deletedGame = screenState.deletedGame
        if (deletedGame != null) {
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
}
