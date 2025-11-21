package com.example.arcadia.presentation.screens.searchScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SearchScreenState(
    val query: String = "",
    val results: RequestState<List<Game>> = RequestState.Idle,
    val gamesInLibrary: Set<Int> = emptySet()
)

class SearchViewModel(
    private val gameRepository: GameRepository,
    private val gameListRepository: GameListRepository
) : ViewModel() {
    
    var screenState by mutableStateOf(SearchScreenState())
        private set
    
    private var searchJob: Job? = null
    
    init {
        // Observe user games library to keep track of which games are already added
        loadGamesInLibrary()
    }
    
    private fun loadGamesInLibrary() {
        viewModelScope.launch {
            gameListRepository.getGameList().collectLatest { state ->
                if (state is RequestState.Success) {
                    val gameIds = state.data.map { it.rawgId }.toSet()
                    screenState = screenState.copy(gamesInLibrary = gameIds)
                }
            }
        }
    }
    
    fun updateQuery(newQuery: String) {
        screenState = screenState.copy(query = newQuery)
        
        // Cancel previous search job
        searchJob?.cancel()
        
        // If query is empty, reset to idle
        if (newQuery.isBlank()) {
            screenState = screenState.copy(results = RequestState.Idle)
            return
        }
        
        // Debounce: wait 500ms before triggering search
        searchJob = viewModelScope.launch {
            delay(500)
            searchGames(newQuery)
        }
    }
    
    private fun searchGames(query: String) {
        viewModelScope.launch {
            gameRepository.searchGames(query, page = 1, pageSize = 40).collectLatest { state ->
                screenState = screenState.copy(results = state)
            }
        }
    }
    
    fun toggleGameInLibrary(
        game: Game,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val gameId = game.id
            
            if (gameId in screenState.gamesInLibrary) {
                // Game is already in library, do nothing or show message
                onError("Game is already in your library")
            } else {
                // Add game to library (Game List with default status WANT)
                when (val result = gameListRepository.addGameToList(game, GameStatus.WANT)) {
                    is RequestState.Success -> onSuccess()
                    is RequestState.Error -> onError(result.message)
                    else -> {}
                }
            }
        }
    }
    
    fun isGameInLibrary(gameId: Int): Boolean {
        return gameId in screenState.gamesInLibrary
    }
}
