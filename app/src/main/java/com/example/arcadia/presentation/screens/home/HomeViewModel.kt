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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class HomeScreenState(
    val popularGames: RequestState<List<Game>> = RequestState.Idle,
    val upcomingGames: RequestState<List<Game>> = RequestState.Idle,
    val recommendedGames: RequestState<List<Game>> = RequestState.Idle,
    val newReleases: RequestState<List<Game>> = RequestState.Idle,
    val gamesInLibrary: Set<Int> = emptySet(), // Track rawgIds of games in library
    val gameListIds: Set<Int> = emptySet(), // Track rawgIds of games in game list (WANT, PLAYING, etc.)
    val animatingGameIds: Set<Int> = emptySet() // Games currently animating out
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
    private var recommendationPage = 1
    private var isLoadingMoreRecommendations = false
    
    init {
        // Start real-time flows only once
        loadGamesInLibrary()
        loadGameListIds()
        // Load initial data
        loadAllData()
    }
    
    fun loadAllData() {
        // Reload one-shot data (no duplicate flows)
        loadPopularGames()
        loadUpcomingGames()
        loadRecommendedGames()
        loadNewReleases()
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
            // Clamp to 40 (RAWG API typical limit)
            gameRepository.getRecommendedGames(tags = "singleplayer,multiplayer", page = 1, pageSize = 40).collectLatest { state ->
                if (state is RequestState.Success) {
                    lastRecommended = state.data
                    recommendationPage = 1
                    applyRecommendationFilter()
                    // Auto-fetch more if filtered results are too few
                    ensureMinimumRecommendations()
                } else {
                    screenState = screenState.copy(recommendedGames = state)
                }
            }
        }
    }
    
    private fun ensureMinimumRecommendations(minCount: Int = 15) {
        viewModelScope.launch {
            val current = screenState.recommendedGames
            if (current is RequestState.Success && current.data.size < minCount && !isLoadingMoreRecommendations) {
                loadMoreRecommendations()
            }
        }
    }
    
    fun loadMoreRecommendations() {
        if (isLoadingMoreRecommendations) return
        
        viewModelScope.launch {
            isLoadingMoreRecommendations = true
            try {
                recommendationPage++
                gameRepository.getRecommendedGames(
                    tags = "singleplayer,multiplayer", 
                    page = recommendationPage, 
                    pageSize = 40
                ).collect { state ->
                    if (state is RequestState.Success) {
                        lastRecommended = lastRecommended + state.data
                        applyRecommendationFilter()
                    }
                    isLoadingMoreRecommendations = false
                }
            } catch (e: Exception) {
                isLoadingMoreRecommendations = false
            }
        }
    }
    
    private fun applyRecommendationFilter() {
        if (lastRecommended.isEmpty()) return
        
        // Exclude games in library but keep games that are currently animating out
        val excludeIds = (screenState.gamesInLibrary + screenState.gameListIds) - screenState.animatingGameIds
        val filtered = lastRecommended.filter { it.id !in excludeIds }
        screenState = screenState.copy(recommendedGames = RequestState.Success(filtered))
        
        // Auto-backfill if filtered results are too few
        ensureMinimumRecommendations()
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
            
            // Mark game as animating to keep it visible during animation
            screenState = screenState.copy(
                animatingGameIds = screenState.animatingGameIds + game.id
            )

            when (val result = userGamesRepository.addGame(game)) {
                is RequestState.Success -> {
                    onSuccess()
                    // Wait for animation to complete (600ms total: 300ms delay + 300ms animation)
                    delay(600)
                    // Remove from animating set to allow filtering
                    screenState = screenState.copy(
                        animatingGameIds = screenState.animatingGameIds - game.id
                    )
                    // Reapply filter to remove the game from the list
                    applyRecommendationFilter()
                }
                is RequestState.Error -> {
                    onError(result.message)
                    // Remove from animating set on error
                    screenState = screenState.copy(
                        animatingGameIds = screenState.animatingGameIds - game.id
                    )
                }
                else -> {}
            }
        }
    }
}


