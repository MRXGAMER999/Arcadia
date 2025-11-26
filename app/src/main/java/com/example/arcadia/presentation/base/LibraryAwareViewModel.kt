package com.example.arcadia.presentation.base

import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel for screens that need to track which games are in the user's library.
 * Provides common functionality for:
 * - Tracking games in library
 * - Adding games with status selection
 * - Undo add functionality
 * - Snackbar notifications
 */
abstract class LibraryAwareViewModel(
    protected val gameListRepository: GameListRepository
) : BaseViewModel() {

    // Library state
    private val _gamesInLibrary = MutableStateFlow<Set<Int>>(emptySet())
    val gamesInLibrary: StateFlow<Set<Int>> = _gamesInLibrary.asStateFlow()

    // Snackbar state
    data class SnackbarState(
        val show: Boolean = false,
        val gameName: String = "",
        val entryId: String? = null
    )

    private val _snackbarState = MutableStateFlow(SnackbarState())
    val snackbarState: StateFlow<SnackbarState> = _snackbarState.asStateFlow()

    // Status picker state
    private val _gameToAddWithStatus = MutableStateFlow<Game?>(null)
    val gameToAddWithStatus: StateFlow<Game?> = _gameToAddWithStatus.asStateFlow()

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
     * Show the status picker for adding a game to the library.
     */
    fun showStatusPicker(game: Game) {
        if (isGameInLibrary(game.id)) {
            return // Already in library
        }
        _gameToAddWithStatus.value = game
    }

    /**
     * Dismiss the status picker.
     */
    fun dismissStatusPicker() {
        _gameToAddWithStatus.value = null
    }

    /**
     * Add a game to the library with the selected status.
     * Shows a snackbar with undo functionality.
     */
    fun addGameWithStatus(
        game: Game,
        status: GameStatus,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        dismissStatusPicker()

        viewModelScope.launch {
            when (val result = gameListRepository.addGameToList(game, status)) {
                is RequestState.Success -> {
                    val entryId = result.data
                    _snackbarState.value = SnackbarState(
                        show = true,
                        gameName = game.name,
                        entryId = entryId
                    )
                    onSuccess()

                    // Auto-hide after 5 seconds
                    showTemporaryNotification(
                        setNotification = {},
                        clearNotification = { dismissSnackbar() },
                        duration = 5000L
                    )
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
        dismissStatusPicker()

        viewModelScope.launch {
            when (val result = gameListRepository.addGameListEntry(entry)) {
                is RequestState.Success -> {
                    val entryId = result.data
                    _snackbarState.value = SnackbarState(
                        show = true,
                        gameName = entry.name,
                        entryId = entryId
                    )
                    onSuccess()

                    // Auto-hide after 5 seconds
                    showTemporaryNotification(
                        setNotification = {},
                        clearNotification = { dismissSnackbar() },
                        duration = 5000L
                    )
                }
                is RequestState.Error -> onError(result.message)
                else -> {}
            }
        }
    }

    /**
     * Undo adding a game - removes it from the library.
     */
    fun undoAddGame() {
        val entryId = _snackbarState.value.entryId ?: return

        viewModelScope.launch {
            _snackbarState.value = SnackbarState()
            gameListRepository.removeGameFromList(entryId)
        }
    }

    /**
     * Dismiss the snackbar without undoing.
     */
    fun dismissSnackbar() {
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
