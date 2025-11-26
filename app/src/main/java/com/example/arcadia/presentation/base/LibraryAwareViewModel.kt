package com.example.arcadia.presentation.base

import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.usecase.AddGameToLibraryUseCase
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel for screens that need to track which games are in the user's library.
 * Uses AddGameToLibraryUseCase for adding games to properly encapsulate business logic.
 * Provides common functionality for:
 * - Tracking games in library
 * - Adding games with status selection
 * - Undo add functionality
 * - Snackbar notifications
 * - Unsaved changes handling
 */
abstract class LibraryAwareViewModel(
    protected val gameListRepository: GameListRepository,
    private val addGameToLibraryUseCase: AddGameToLibraryUseCase
) : BaseViewModel() {

    // Library state
    private val _gamesInLibrary = MutableStateFlow<Set<Int>>(emptySet())
    val gamesInLibrary: StateFlow<Set<Int>> = _gamesInLibrary.asStateFlow()

    // Snackbar state for "Game added" with undo
    data class SnackbarState(
        val show: Boolean = false,
        val gameName: String = "",
        val entryId: String? = null
    )

    private val _snackbarState = MutableStateFlow(SnackbarState())
    val snackbarState: StateFlow<SnackbarState> = _snackbarState.asStateFlow()

    // Status picker / Add game sheet state
    // This holds the current game being added AND any unsaved data
    data class AddGameSheetState(
        val isOpen: Boolean = false,
        val originalGame: Game? = null,
        val unsavedEntry: GameListEntry? = null // Holds unsaved changes when reopening
    )
    
    private val _addGameSheetState = MutableStateFlow(AddGameSheetState())
    val addGameSheetState: StateFlow<AddGameSheetState> = _addGameSheetState.asStateFlow()
    
    // Unsaved changes snackbar state (separate from "game added" snackbar)
    data class UnsavedChangesState(
        val show: Boolean = false,
        val unsavedEntry: GameListEntry? = null,
        val originalGame: Game? = null
    )
    
    private val _unsavedChangesState = MutableStateFlow(UnsavedChangesState())
    val unsavedAddGameState: StateFlow<UnsavedChangesState> = _unsavedChangesState.asStateFlow()
    
    // Job for auto-dismiss timer
    private var unsavedChangesTimerJob: Job? = null
    private var addedGameTimerJob: Job? = null

    init {
        observeLibrary()
    }

    /**
     * Observes the user's game library and updates the gamesInLibrary set.
     */
    private fun observeLibrary() {
        launchWithKey("observe_library") {
            gameListRepository.getGameList().collect { state ->
                if (state is RequestState.Success) {
                    val games = state.data as List<GameListEntry>
                    val gameIds = games.map { it.rawgId }.toSet()
                    _gamesInLibrary.value = gameIds
                    onLibraryUpdated(games)
                }
            }
        }
    }

    /**
     * Override this to react to library updates (e.g., reapply filters).
     */
    protected open fun onLibraryUpdated(games: List<GameListEntry>) {}

    /**
     * Check if a game is in the library.
     */
    fun isGameInLibrary(gameId: Int): Boolean {
        return gameId in _gamesInLibrary.value
    }

    /**
     * Show the add game sheet for a game.
     */
    fun showStatusPicker(game: Game) {
        if (isGameInLibrary(game.id)) {
            return // Already in library
        }
        // Cancel any unsaved changes snackbar when opening a new sheet
        cancelUnsavedChangesSnackbar()
        
        _addGameSheetState.value = AddGameSheetState(
            isOpen = true,
            originalGame = game,
            unsavedEntry = null
        )
    }

    /**
     * Dismiss the add game sheet without saving.
     */
    fun dismissStatusPicker() {
        _addGameSheetState.value = AddGameSheetState()
    }

    /**
     * Called when sheet is dismissed with unsaved changes.
     * Shows the unsaved changes snackbar.
     */
    fun handleSheetDismissedWithUnsavedChanges(unsavedEntry: GameListEntry, originalGame: Game) {
        // First close the sheet
        _addGameSheetState.value = AddGameSheetState()
        
        // Then show the unsaved changes snackbar
        _unsavedChangesState.value = UnsavedChangesState(
            show = true,
            unsavedEntry = unsavedEntry,
            originalGame = originalGame
        )
        
        // Auto-dismiss after 5 seconds
        unsavedChangesTimerJob?.cancel()
        unsavedChangesTimerJob = viewModelScope.launch {
            kotlinx.coroutines.delay(5000L)
            if (_unsavedChangesState.value.show) {
                _unsavedChangesState.value = UnsavedChangesState()
            }
        }
    }

    /**
     * Reopen the sheet with unsaved changes.
     */
    fun reopenAddGameWithUnsavedChanges() {
        val state = _unsavedChangesState.value
        if (state.originalGame != null && state.unsavedEntry != null) {
            // Cancel the timer
            unsavedChangesTimerJob?.cancel()
            
            // Reopen the sheet with the unsaved data
            _addGameSheetState.value = AddGameSheetState(
                isOpen = true,
                originalGame = state.originalGame,
                unsavedEntry = state.unsavedEntry
            )
            
            // Clear the unsaved changes state
            _unsavedChangesState.value = UnsavedChangesState()
        }
    }

    /**
     * Save the unsaved changes directly from the snackbar.
     */
    fun saveUnsavedAddGameChanges() {
        val state = _unsavedChangesState.value
        state.unsavedEntry?.let { entry ->
            unsavedChangesTimerJob?.cancel()
            _unsavedChangesState.value = UnsavedChangesState()
            addGameWithEntry(entry)
        }
    }

    /**
     * Dismiss the unsaved changes snackbar without saving.
     */
    fun dismissUnsavedAddGameChanges() {
        unsavedChangesTimerJob?.cancel()
        _unsavedChangesState.value = UnsavedChangesState()
    }
    
    private fun cancelUnsavedChangesSnackbar() {
        unsavedChangesTimerJob?.cancel()
        _unsavedChangesState.value = UnsavedChangesState()
    }

    /**
     * Add a game to the library with the selected status.
     * Uses AddGameToLibraryUseCase for proper business logic encapsulation.
     * Shows a snackbar with undo functionality.
     */
    fun addGameWithStatus(
        game: Game,
        status: GameStatus,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // Close sheet first
        _addGameSheetState.value = AddGameSheetState()
        // Clear any unsaved changes state
        cancelUnsavedChangesSnackbar()

        viewModelScope.launch {
            when (val result = addGameToLibraryUseCase(game, status)) {
                is RequestState.Success -> {
                    val entryId = result.data
                    showAddedGameSnackbar(game.name, entryId)
                    onSuccess()
                }
                is RequestState.Error -> onError(result.message)
                else -> {}
            }
        }
    }

    /**
     * Add a game to the library with a full GameListEntry (includes rating, aspects, etc.).
     * Shows a snackbar with undo functionality.
     */
    fun addGameWithEntry(
        entry: GameListEntry,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // Close sheet first
        _addGameSheetState.value = AddGameSheetState()
        // Clear any unsaved changes state
        cancelUnsavedChangesSnackbar()

        viewModelScope.launch {
            when (val result = gameListRepository.addGameListEntry(entry)) {
                is RequestState.Success -> {
                    val entryId = result.data
                    showAddedGameSnackbar(entry.name, entryId)
                    onSuccess()
                }
                is RequestState.Error -> onError(result.message)
                else -> {}
            }
        }
    }
    
    private fun showAddedGameSnackbar(gameName: String, entryId: String) {
        // Cancel any existing timer
        addedGameTimerJob?.cancel()
        
        _snackbarState.value = SnackbarState(
            show = true,
            gameName = gameName,
            entryId = entryId
        )
        
        // Auto-hide after 5 seconds
        addedGameTimerJob = viewModelScope.launch {
            kotlinx.coroutines.delay(5000L)
            if (_snackbarState.value.show) {
                _snackbarState.value = SnackbarState()
            }
        }
    }

    /**
     * Undo adding a game - removes it from the library.
     */
    fun undoAddGame() {
        val entryId = _snackbarState.value.entryId ?: return
        addedGameTimerJob?.cancel()
        _snackbarState.value = SnackbarState()

        viewModelScope.launch {
            gameListRepository.removeGameFromList(entryId)
        }
    }

    /**
     * Dismiss the snackbar without undoing.
     */
    fun dismissSnackbar() {
        addedGameTimerJob?.cancel()
        _snackbarState.value = SnackbarState()
    }

    /**
     * Toggle game in library: Add if not present, remove if present.
     */
    fun toggleGameInLibrary(
        game: Game,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (isGameInLibrary(game.id)) {
            // Remove
            launchWithKey("toggle_library_${game.id}") {
                val entryId = gameListRepository.getEntryIdByRawgId(game.id)
                if (entryId != null) {
                    when (val result = gameListRepository.removeGameFromList(entryId)) {
                        is RequestState.Success -> onSuccess()
                        is RequestState.Error -> onError(result.message)
                        else -> {}
                    }
                } else {
                    onError("Could not find game in library")
                }
            }
        } else {
            // Add - Show status picker
            showStatusPicker(game)
        }
    }
    
}
