package com.example.arcadia.presentation.screens.detailsScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.domain.repository.UserGamesRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class AddToLibraryState {
    data object Idle : AddToLibraryState()
    data object Loading : AddToLibraryState()
    data class Success(val message: String) : AddToLibraryState()
    data class Error(val message: String) : AddToLibraryState()
}

data class DetailsUiState(
    val gameState: RequestState<Game> = RequestState.Idle,
    val isInLibrary: Boolean = false,
    val isInGameList: Boolean = false,
    val addToLibraryState: AddToLibraryState = AddToLibraryState.Idle,
    val addToListInProgress: Boolean = false,
    val errorMessage: String? = null
)

class DetailsScreenViewModel(
    private val gameRepository: GameRepository,
    private val userGamesRepository: UserGamesRepository,
    private val gameListRepository: GameListRepository
) : ViewModel() {

    var uiState by mutableStateOf(DetailsUiState())
        private set

    private var currentGameId: Int? = null

    fun loadGameDetails(gameId: Int) {
        currentGameId = gameId
        uiState = uiState.copy(gameState = RequestState.Loading, errorMessage = null)

        // Fetch details with media (trailer + screenshots)
        viewModelScope.launch {
            gameRepository.getGameDetailsWithMedia(gameId).collectLatest { state ->
                uiState = uiState.copy(gameState = state)
            }
        }

        // Check membership flags concurrently
        viewModelScope.launch {
            val inLibrary = userGamesRepository.isGameInLibrary(gameId)
            val inList = gameListRepository.isGameInList(gameId)
            uiState = uiState.copy(isInLibrary = inLibrary, isInGameList = inList)
        }
    }

    fun addToLibrary() {
        // Prevent duplicate requests
        if (uiState.addToLibraryState is AddToLibraryState.Loading) {
            return
        }

        // Skip if already in library
        if (uiState.isInLibrary) {
            uiState = uiState.copy(
                addToLibraryState = AddToLibraryState.Error("Game is already in your library")
            )
            // Reset state after delay
            viewModelScope.launch {
                delay(2000)
                uiState = uiState.copy(addToLibraryState = AddToLibraryState.Idle)
            }
            return
        }

        val game = (uiState.gameState as? RequestState.Success)?.data
        if (game == null) {
            uiState = uiState.copy(
                addToLibraryState = AddToLibraryState.Error("Game data not available")
            )
            return
        }

        // Set loading state
        uiState = uiState.copy(
            addToLibraryState = AddToLibraryState.Loading,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                // Double-check if game is in library (race condition protection)
                val alreadyInLibrary = userGamesRepository.isGameInLibrary(game.id)
                if (alreadyInLibrary) {
                    uiState = uiState.copy(
                        isInLibrary = true,
                        addToLibraryState = AddToLibraryState.Idle
                    )
                    return@launch
                }

                when (val result = userGamesRepository.addGame(game)) {
                    is RequestState.Success -> {
                        uiState = uiState.copy(
                            isInLibrary = true,
                            addToLibraryState = AddToLibraryState.Success("Added to library!")
                        )
                        // Reset success state after delay
                        delay(2000)
                        uiState = uiState.copy(addToLibraryState = AddToLibraryState.Idle)
                    }
                    is RequestState.Error -> {
                        uiState = uiState.copy(
                            addToLibraryState = AddToLibraryState.Error(result.message)
                        )
                        // Reset error state after delay
                        delay(3000)
                        uiState = uiState.copy(addToLibraryState = AddToLibraryState.Idle)
                    }
                    else -> {
                        uiState = uiState.copy(
                            addToLibraryState = AddToLibraryState.Error("Unexpected error occurred")
                        )
                        delay(3000)
                        uiState = uiState.copy(addToLibraryState = AddToLibraryState.Idle)
                    }
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    addToLibraryState = AddToLibraryState.Error(
                        e.localizedMessage ?: "An error occurred"
                    )
                )
                delay(3000)
                uiState = uiState.copy(addToLibraryState = AddToLibraryState.Idle)
            }
        }
    }

    fun addToGameList(
        initialStatus: GameStatus = GameStatus.WANT,
        onDone: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val game = (uiState.gameState as? RequestState.Success)?.data ?: return
        if (uiState.isInGameList) {
            onDone(false, "Game is already in your list")
            return
        }
        uiState = uiState.copy(addToListInProgress = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = gameListRepository.addGameToList(game, initialStatus)) {
                is RequestState.Success -> {
                    uiState = uiState.copy(isInGameList = true, addToListInProgress = false)
                    onDone(true, null)
                }
                is RequestState.Error -> {
                    uiState = uiState.copy(addToListInProgress = false, errorMessage = result.message)
                    onDone(false, result.message)
                }
                else -> {}
            }
        }
    }

    fun retry() {
        currentGameId?.let { loadGameDetails(it) }
    }
}