package com.example.arcadia.presentation.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class HomeScreenState(
    val popularGames: RequestState<List<Game>> = RequestState.Idle,
    val upcomingGames: RequestState<List<Game>> = RequestState.Idle,
    val recommendedGames: RequestState<List<Game>> = RequestState.Idle,
    val newReleases: RequestState<List<Game>> = RequestState.Idle
)

class HomeViewModel(
    private val gameRepository: GameRepository
) : ViewModel() {
    
    var screenState by mutableStateOf(HomeScreenState())
        private set
    
    init {
        loadAllData()
    }
    
    fun loadAllData() {
        loadPopularGames()
        loadUpcomingGames()
        loadRecommendedGames()
        loadNewReleases()
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
            gameRepository.getRecommendedGames(tags = "singleplayer,multiplayer", page = 1, pageSize = 10).collectLatest { state ->
                screenState = screenState.copy(recommendedGames = state)
            }
        }
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
}

