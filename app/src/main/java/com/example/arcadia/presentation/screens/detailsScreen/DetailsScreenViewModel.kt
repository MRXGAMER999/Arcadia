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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


data class DetailsUiState(
    val gameState: RequestState<Game> = RequestState.Idle,
    val isInLibrary: Boolean = false,
    val isInGameList: Boolean = false,
    val addToLibraryInProgress: Boolean = false,
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

    fun addToLibrary(onDone: (Boolean, String?) -> Unit = { _, _ -> }) {
        val game = (uiState.gameState as? RequestState.Success)?.data ?: return
        // Skip if already in library
        if (uiState.isInLibrary) {
            onDone(false, "Game is already in your library")
            return
        }
        uiState = uiState.copy(addToLibraryInProgress = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = userGamesRepository.addGame(game)) {
                is RequestState.Success -> {
                    uiState = uiState.copy(isInLibrary = true, addToLibraryInProgress = false)
                    onDone(true, null)
                }
                is RequestState.Error -> {
                    uiState = uiState.copy(addToLibraryInProgress = false, errorMessage = result.message)
                    onDone(false, result.message)
                }
                else -> {}
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