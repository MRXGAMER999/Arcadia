package com.example.arcadia.presentation.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.domain.repository.UserGamesRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class HomeScreenState(
    val popularGames: RequestState<List<Game>> = RequestState.Idle,
    val upcomingGames: RequestState<List<Game>> = RequestState.Idle,
    val recommendedGames: RequestState<List<Game>> = RequestState.Idle,
    val newReleases: RequestState<List<Game>> = RequestState.Idle,
    val gamesInLibrary: Set<Int> = emptySet(), // Track rawgIds of games in library
    val gameListIds: Set<Int> = emptySet() // Track rawgIds of games in game list (WANT, PLAYING, etc.)
)

class HomeViewModel(
    private val gameRepository: GameRepository,
    private val userGamesRepository: UserGamesRepository,
    private val gameListRepository: GameListRepository
) : ViewModel() {
    
    var screenState by mutableStateOf(HomeScreenState())
        private set
    
    // Cache unfiltered recommendations for reapplying filters
    private var lastRecommended: List<Game> = emptyList()
    
    init {
        loadAllData()
        loadGamesInLibrary()
        loadGameListIds()
    }
    
    fun loadAllData() {
        loadPopularGames()
        loadUpcomingGames()
        loadRecommendedGames()
        loadNewReleases()
        loadGamesInLibrary()
        loadGameListIds()
    }
    
    private fun loadGamesInLibrary() {
        viewModelScope.launch {
            userGamesRepository.getUserGames().collectLatest { state ->
                if (state is RequestState.Success) {
                    val gameIds = state.data.map { it.rawgId }.toSet()
                    screenState = screenState.copy(gamesInLibrary = gameIds)
                    // Reapply recommendation filter when library changes
                    applyRecommendationFilter()
                }
            }
        }
    }
    
    private fun loadGameListIds() {
        viewModelScope.launch {
            gameListRepository.getGameList().collectLatest { state ->
                if (state is RequestState.Success) {
                    val gameIds = state.data.map { it.rawgId }.toSet()
                    screenState = screenState.copy(gameListIds = gameIds)
                    // Reapply recommendation filter when game list changes
                    applyRecommendationFilter()
                }
            }
        }
    }
    
    private fun loadPopularGames() {
        viewModelScope.launch {
            gameRepository.getPopularGames(page = 1, pageSize = 10).collectLatest { state ->
                screenState = screenState.copy(popularGames = state)
            }
        }
    }
    
    private fun loadUpcomingGames() {
        viewModelScope.launch {
            gameRepository.getUpcomingGames(page = 1, pageSize = 10).collectLatest { state ->
                screenState = screenState.copy(upcomingGames = state)
            }
        }
    }
    
    private fun loadRecommendedGames() {
        viewModelScope.launch {
            // Using popular tags for recommendations: singleplayer, multiplayer, action
            // Fetch more games (50) to ensure we have enough after filtering
            gameRepository.getRecommendedGames(tags = "singleplayer,multiplayer", page = 1, pageSize = 50).collectLatest { state ->
                if (state is RequestState.Success) {
                    lastRecommended = state.data
                    applyRecommendationFilter()
                } else {
                    screenState = screenState.copy(recommendedGames = state)
                }
            }
        }
    }
    
    private fun applyRecommendationFilter() {
        if (lastRecommended.isEmpty()) return
        
        val excludeIds = screenState.gamesInLibrary + screenState.gameListIds
        val filtered = lastRecommended.filter { it.id !in excludeIds }
        screenState = screenState.copy(recommendedGames = RequestState.Success(filtered))
    }
    
    private fun loadNewReleases() {
        viewModelScope.launch {
            gameRepository.getNewReleases(page = 1, pageSize = 10).collectLatest { state ->
                screenState = screenState.copy(newReleases = state)
            }
        }
    }
    
    fun retry() {
        loadAllData()
    }
    
    fun isGameInLibrary(gameId: Int): Boolean {
        return screenState.gamesInLibrary.contains(gameId) || screenState.gameListIds.contains(gameId)
    }
    
    fun addGameToLibrary(
        game: Game,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {},
        onAlreadyInLibrary: () -> Unit = {}
    ) {
        viewModelScope.launch {
            // Check if game is already in library
            if (isGameInLibrary(game.id)) {
                onAlreadyInLibrary()
                return@launch
            }
            
            when (val result = userGamesRepository.addGame(game)) {
                is RequestState.Success -> onSuccess()
                is RequestState.Error -> onError(result.message)
                else -> {}
            }
        }
    }
}


