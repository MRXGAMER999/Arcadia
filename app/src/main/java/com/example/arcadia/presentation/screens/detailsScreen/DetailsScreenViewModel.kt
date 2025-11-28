package com.example.arcadia.presentation.screens.detailsScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.arcadia.data.remote.mapper.toGameListEntry
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.domain.usecase.AddGameToLibraryUseCase
import com.example.arcadia.domain.usecase.RemoveGameFromLibraryUseCase
import com.example.arcadia.presentation.base.UndoableViewModel
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class AddToLibraryState {
    data object Idle : AddToLibraryState()
    data object Loading : AddToLibraryState()
    data class Success(val message: String, val addedEntry: GameListEntry? = null) : AddToLibraryState()
    data class Error(val message: String) : AddToLibraryState()
}

data class DetailsUiState(
    val gameState: RequestState<Game> = RequestState.Idle,
    val addToLibraryState: AddToLibraryState = AddToLibraryState.Idle,
    val addToListInProgress: Boolean = false,
    val errorMessage: String? = null,
    val showRatingSheet: Boolean = false,
    val tempGameEntry: GameListEntry? = null, // For the rating sheet
    val originalLibraryEntry: GameListEntry? = null, // Original entry before any changes (for change detection)
    // Unsaved changes snackbar
    val showUnsavedChangesSnackbar: Boolean = false,
    val unsavedChangesGame: GameListEntry? = null
)

class DetailsScreenViewModel(
    private val gameRepository: GameRepository,
    gameListRepository: GameListRepository,
    addGameToLibraryUseCase: AddGameToLibraryUseCase,
    removeGameFromLibraryUseCase: RemoveGameFromLibraryUseCase
) : UndoableViewModel(gameListRepository, addGameToLibraryUseCase, removeGameFromLibraryUseCase) {

    companion object {
        private const val UNDO_DELAY_MS = 5000L
    }

    var uiState by mutableStateOf(DetailsUiState())
        private set

    private var currentGameId: Int? = null
    // private var detailsJob: kotlinx.coroutines.Job? = null // Removed in favor of launchWithKey

    fun loadGameDetails(gameId: Int) {
        currentGameId = gameId
        uiState = uiState.copy(gameState = RequestState.Loading, errorMessage = null)

        // Fetch details with media
        launchWithKey("load_details") {
            gameRepository.getGameDetailsWithMedia(gameId).collect { state ->
                uiState = uiState.copy(gameState = state)
            }
        }
        
        checkLibraryStatus(gameId)
    }
    
    private fun checkLibraryStatus(gameId: Int) {
        launchWithKey("check_library_entry") {
             if (isGameInLibrary(gameId)) {
                 val entryId = gameListRepository.getEntryIdByRawgId(gameId)
                 if (entryId != null) {
                     gameListRepository.getGameEntry(entryId).collect { entryState ->
                         if (entryState is RequestState.Success) {
                             uiState = uiState.copy(tempGameEntry = entryState.data)
                         }
                     }
                 }
             }
        }
    }
    
    override fun onLibraryUpdated(games: List<GameListEntry>) {
        val gameId = currentGameId ?: return
        val entry = games.find { it.rawgId == gameId }
        
        if (entry != null) {
            uiState = uiState.copy(tempGameEntry = entry)
        } else {
            if (uiState.tempGameEntry != null && !isGamePendingDeletion(uiState.tempGameEntry?.id)) {
                 uiState = uiState.copy(tempGameEntry = null)
            }
        }
    }
    
    private fun isGamePendingDeletion(entryId: String?): Boolean {
        return undoState.value.item?.id == entryId
    }

    fun onAddToLibraryClick() {
        val game = (uiState.gameState as? RequestState.Success)?.data ?: return
        val gameId = game.id
        
        if (isGameInLibrary(gameId)) {
            // Opening for edit - set original entry for change detection
            uiState = uiState.copy(
                showRatingSheet = true,
                originalLibraryEntry = uiState.tempGameEntry // Store current state as original
            )
        } else {
            val newEntry = game.toGameListEntry(status = GameStatus.FINISHED)
            uiState = uiState.copy(
                tempGameEntry = newEntry, 
                showRatingSheet = true,
                originalLibraryEntry = newEntry // For new games, original is the default entry
            )
        }
    }

    fun dismissRatingSheet() {
        uiState = uiState.copy(showRatingSheet = false)
    }

    fun showUnsavedChangesSnackbar(unsavedGame: GameListEntry) {
        showTemporaryNotification(
            setNotification = {
                uiState = uiState.copy(
                    showUnsavedChangesSnackbar = true,
                    unsavedChangesGame = unsavedGame
                )
            },
            clearNotification = {
                if (uiState.showUnsavedChangesSnackbar) {
                    dismissUnsavedChangesSnackbar()
                }
            },
            duration = UNDO_DELAY_MS
        )
    }

    fun saveUnsavedChanges() {
        uiState.unsavedChangesGame?.let { game ->
            saveGameEntry(game)
        }
        dismissUnsavedChangesSnackbar()
    }
    
    fun reopenWithUnsavedChanges() {
        val unsavedGame = uiState.unsavedChangesGame ?: return
        uiState = uiState.copy(
            showUnsavedChangesSnackbar = false,
            tempGameEntry = unsavedGame,
            showRatingSheet = true
        )
    }

    fun dismissUnsavedChangesSnackbar() {
        uiState = uiState.copy(
            showUnsavedChangesSnackbar = false,
            unsavedChangesGame = null
        )
    }

    fun removeGameFromLibrary(game: GameListEntry) {
        // Use the most up-to-date version from tempGameEntry to preserve any recent changes
        // (e.g., rating updates that might not have been reflected yet)
        val currentGame = uiState.tempGameEntry ?: game
        
        removeGameWithUndo(
            game = currentGame,
            onOptimisticRemove = { 
                uiState = uiState.copy(tempGameEntry = null)
            },
            onActualRemove = {
                showTemporaryNotification(
                    setNotification = {
                        uiState = uiState.copy(
                            addToLibraryState = AddToLibraryState.Success("Removed from library")
                        )
                    },
                    clearNotification = {
                        uiState = uiState.copy(addToLibraryState = AddToLibraryState.Idle)
                    },
                    duration = 2000
                )
            },
            onError = { restoredGame, error ->
                showTemporaryNotification(
                    setNotification = {
                        uiState = uiState.copy(
                            tempGameEntry = restoredGame,
                            addToLibraryState = AddToLibraryState.Error("Failed to remove: $error")
                        )
                    },
                    clearNotification = {
                        uiState = uiState.copy(addToLibraryState = AddToLibraryState.Idle)
                    },
                    duration = 3000
                )
            }
        )
    }

    fun undoRemoval() {
        undoRemoval { restoredGame ->
            showTemporaryNotification(
                setNotification = {
                    uiState = uiState.copy(
                        tempGameEntry = restoredGame,
                        addToLibraryState = AddToLibraryState.Success("Game restored")
                    )
                },
                clearNotification = {
                    uiState = uiState.copy(addToLibraryState = AddToLibraryState.Idle)
                },
                duration = 2000
            )
        }
    }

    fun dismissRemovalSnackbar() {
        dismissUndoSnackbar(
            onError = { restoredGame, error ->
                uiState = uiState.copy(
                    tempGameEntry = restoredGame,
                    addToLibraryState = AddToLibraryState.Error("Failed to remove: $error")
                )
            }
        )
    }

    fun saveGameEntry(entry: GameListEntry) {
        uiState = uiState.copy(
            showRatingSheet = false,
            addToListInProgress = true,
            addToLibraryState = AddToLibraryState.Loading
        )

        launchWithKey("save_entry") {
            val isUpdate = isGameInLibrary(entry.rawgId)
            
            val result = if (isUpdate) {
                gameListRepository.updateGameEntry(entry)
            } else {
                gameListRepository.addGameListEntry(entry)
            }

            when (result) {
                is RequestState.Success -> {
                    val savedEntry = if (isUpdate) {
                        entry
                    } else {
                        entry.copy(id = result.data as String)
                    }
                    
                    // For new additions, show snackbar with undo option (5 seconds)
                    // For updates, just show brief confirmation (no undo needed)
                    val snackbarDuration = if (isUpdate) 2000L else UNDO_DELAY_MS
                    
                    showTemporaryNotification(
                        setNotification = {
                            uiState = uiState.copy(
                                addToLibraryState = AddToLibraryState.Success(
                                    message = if (isUpdate) "Game updated!" else "Added to library!",
                                    addedEntry = if (isUpdate) null else savedEntry // Only track for undo on new adds
                                ),
                                addToListInProgress = false,
                                tempGameEntry = savedEntry
                            )
                        },
                        clearNotification = {
                            uiState = uiState.copy(addToLibraryState = AddToLibraryState.Idle)
                        },
                        duration = snackbarDuration
                    )
                }
                is RequestState.Error -> {
                    uiState = uiState.copy(
                        addToLibraryState = AddToLibraryState.Error(result.message),
                        addToListInProgress = false
                    )
                }
                else -> {}
            }
        }
    }

    fun undoAdd() {
        val successState = uiState.addToLibraryState as? AddToLibraryState.Success ?: return
        val addedEntry = successState.addedEntry ?: return
        
        // Remove the game that was just added
        removeGameWithUndo(
            game = addedEntry,
            onOptimisticRemove = {
                uiState = uiState.copy(
                    tempGameEntry = null,
                    addToLibraryState = AddToLibraryState.Idle
                )
            },
            onActualRemove = {
                // Silent removal - don't show another snackbar
            },
            onError = { restoredGame, error ->
                uiState = uiState.copy(
                    tempGameEntry = restoredGame,
                    addToLibraryState = AddToLibraryState.Error("Failed to undo: $error")
                )
            }
        )
    }
    
    fun dismissAddSnackbar() {
        uiState = uiState.copy(addToLibraryState = AddToLibraryState.Idle)
    }

    fun retry() {
        currentGameId?.let { loadGameDetails(it) }
    }
}
