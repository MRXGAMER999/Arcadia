package com.example.arcadia.presentation.screens.myGames

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.UserGame
import com.example.arcadia.domain.repository.SortOrder
import com.example.arcadia.domain.repository.UserGamesRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class MyGamesScreenState(
    val games: RequestState<List<UserGame>> = RequestState.Idle,
    val selectedGenre: String? = null,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST
)

class MyGamesViewModel(
    private val userGamesRepository: UserGamesRepository
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
                userGamesRepository.getUserGamesByGenre(selectedGenre, sortOrder)
                    .collect { state ->
                        screenState = screenState.copy(games = state)
                    }
            } else {
                userGamesRepository.getUserGames(sortOrder)
                    .collect { state ->
                        screenState = screenState.copy(games = state)
                    }
            }
        }
    }
    
    fun removeGame(gameId: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            when (val result = userGamesRepository.removeGame(gameId)) {
                is RequestState.Success -> onSuccess()
                is RequestState.Error -> onError(result.message)
                else -> {}
            }
        }
    }
    
    fun retry() {
        loadGames()
    }
}

