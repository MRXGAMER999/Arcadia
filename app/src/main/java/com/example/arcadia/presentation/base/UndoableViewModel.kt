package com.example.arcadia.presentation.base

import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.usecase.AddGameToLibraryUseCase
import com.example.arcadia.domain.usecase.RemoveGameFromLibraryUseCase
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel for screens that support undo operations for game removal.
 * Uses RemoveGameFromLibraryUseCase for proper business logic encapsulation.
 * Provides common functionality for:
 * - Optimistic UI updates
 * - Undo countdown with visual progress
 * - Automatic deletion after timeout
 * - Error handling with rollback
 */
abstract class UndoableViewModel(
    gameListRepository: GameListRepository,
    addGameToLibraryUseCase: AddGameToLibraryUseCase,
    private val removeGameFromLibraryUseCase: RemoveGameFromLibraryUseCase
) : LibraryAwareViewModel(gameListRepository, addGameToLibraryUseCase) {

    // Undo state
    data class UndoState<T>(
        val item: T? = null,
        val showSnackbar: Boolean = false,
        val timeRemaining: Float = 1f, // 0f to 1f for progress bar
        val operation: UndoOperation? = null
    )

    private val _undoState = MutableStateFlow(UndoState<GameListEntry>())
    val undoState: StateFlow<UndoState<GameListEntry>> = _undoState.asStateFlow()

    companion object {
        const val UNDO_TIMEOUT_MS = 5000L
    }

    /**
     * Remove a game with undo functionality.
     * Optimistically removes from UI and schedules actual deletion.
     * 
     * @param game The game to remove
     * @param onOptimisticRemove Called immediately for optimistic UI update
     * @param onActualRemove Called when actual deletion completes
     * @param onError Called if deletion fails
     */
    protected fun removeGameWithUndo(
        game: GameListEntry,
        onOptimisticRemove: (GameListEntry) -> Unit,
        onActualRemove: (GameListEntry) -> Unit = {},
        onError: (GameListEntry, String) -> Unit = { _, _ -> }
    ) {
        // Cancel any existing undo operation
        _undoState.value.operation?.executeNow()

        // Optimistically remove from UI
        onOptimisticRemove(game)

        // Schedule actual deletion with progress updates
        val operation = scheduleUndoOperation(
            timeoutMs = UNDO_TIMEOUT_MS,
            updateProgress = { progress ->
                _undoState.value = _undoState.value.copy(timeRemaining = progress)
            },
            onTimeout = {
                // Execute actual deletion
                executeRemoval(game, onActualRemove, onError)
            }
        )

        // Show undo snackbar
        _undoState.value = UndoState(
            item = game,
            showSnackbar = true,
            timeRemaining = 1f,
            operation = operation
        )
    }

    /**
     * Executes the actual removal from Firebase using RemoveGameFromLibraryUseCase.
     */
    private suspend fun executeRemoval(
        game: GameListEntry,
        onSuccess: (GameListEntry) -> Unit,
        onError: (GameListEntry, String) -> Unit
    ) {
        when (val result = removeGameFromLibraryUseCase(game)) {
            is RequestState.Success -> {
                _undoState.value = UndoState()
                onSuccess(game)
            }
            is RequestState.Error -> {
                _undoState.value = UndoState()
                onError(game, result.message)
            }
            else -> {
                _undoState.value = UndoState()
            }
        }
    }

    /**
     * Undo the removal - cancel the scheduled deletion.
     */
    fun undoRemoval(
        onRestore: (GameListEntry) -> Unit
    ) {
        val game = _undoState.value.item ?: return
        
        // Cancel the scheduled deletion
        _undoState.value.operation?.cancel()

        // Restore the game
        onRestore(game)

        // Clear undo state
        _undoState.value = UndoState()

        // Show success notification
        showTemporaryNotification(
            setNotification = { /* UI handles this */ },
            clearNotification = {},
            duration = 2000L
        )
    }

    /**
     * Dismiss the undo snackbar and execute deletion immediately.
     */
    fun dismissUndoSnackbar(
        onActualRemove: (GameListEntry) -> Unit = {},
        onError: (GameListEntry, String) -> Unit = { _, _ -> }
    ) {
        val game = _undoState.value.item ?: return

        // Execute immediately
        _undoState.value.operation?.executeNow()

        viewModelScope.launch {
            executeRemoval(game, onActualRemove, onError)
        }
    }
}
