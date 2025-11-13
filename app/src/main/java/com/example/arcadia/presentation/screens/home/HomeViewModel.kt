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

sealed class AddToLibraryState {
    data object Idle : AddToLibraryState()
    data class Loading(val gameId: Int) : AddToLibraryState()
    data class Success(val message: String, val gameId: Int) : AddToLibraryState()
    data class Error(val message: String, val gameId: Int) : AddToLibraryState()
}

data class HomeScreenState(
    val popularGames: RequestState<List<Game>> = RequestState.Idle,
    val upcomingGames: RequestState<List<Game>> = RequestState.Idle,
    val recommendedGames: RequestState<List<Game>> = RequestState.Idle,
    val newReleases: RequestState<List<Game>> = RequestState.Idle,
    val gamesInLibrary: Set<Int> = emptySet(), // Track rawgIds of games in library
    val gameListIds: Set<Int> = emptySet(), // Track rawgIds of games in game list (WANT, PLAYING, etc.)
    val animatingGameIds: Set<Int> = emptySet(), // Games currently animating out
    val addToLibraryState: AddToLibraryState = AddToLibraryState.Idle
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
    
    fun addGameToLibrary(game: Game) {
        viewModelScope.launch {
            // Prevent duplicate requests - check if already loading this game
            if (screenState.addToLibraryState is AddToLibraryState.Loading) {
                val currentLoading = screenState.addToLibraryState as AddToLibraryState.Loading
                if (currentLoading.gameId == game.id) {
                    return@launch // Already loading this game
                }
            }

            // Check if game is already in library
            if (isGameInLibrary(game.id)) {
                screenState = screenState.copy(
                    addToLibraryState = AddToLibraryState.Error(
                        message = "${game.name} is already in your library",
                        gameId = game.id
                    )
                )
                // Auto-reset after 2 seconds
                delay(2000)
                screenState = screenState.copy(addToLibraryState = AddToLibraryState.Idle)
                return@launch
            }
            
            // Set loading state
            screenState = screenState.copy(
                addToLibraryState = AddToLibraryState.Loading(gameId = game.id),
                animatingGameIds = screenState.animatingGameIds + game.id
            )

            try {
                // Double-check if game is in library (race condition protection)
                val alreadyInLibrary = userGamesRepository.isGameInLibrary(game.id)
                if (alreadyInLibrary) {
                    screenState = screenState.copy(
                        addToLibraryState = AddToLibraryState.Idle,
                        animatingGameIds = screenState.animatingGameIds - game.id
                    )
                    return@launch
                }

                when (val result = userGamesRepository.addGame(game)) {
                    is RequestState.Success -> {
                        screenState = screenState.copy(
                            addToLibraryState = AddToLibraryState.Success(
                                message = "${game.name} added to library!",
                                gameId = game.id
                            )
                        )

                        // Wait for animation to complete (600ms total: 300ms delay + 300ms animation)
                        delay(600)

                        // Remove from animating set to allow filtering
                        screenState = screenState.copy(
                            animatingGameIds = screenState.animatingGameIds - game.id
                        )

                        // Reapply filter to remove the game from the list
                        applyRecommendationFilter()

                        // Auto-reset success state after showing notification
                        delay(1500) // Give time for notification to show
                        screenState = screenState.copy(addToLibraryState = AddToLibraryState.Idle)
                    }
                    is RequestState.Error -> {
                        screenState = screenState.copy(
                            addToLibraryState = AddToLibraryState.Error(
                                message = result.message,
                                gameId = game.id
                            ),
                            animatingGameIds = screenState.animatingGameIds - game.id
                        )
                        // Auto-reset error state after 3 seconds
                        delay(3000)
                        screenState = screenState.copy(addToLibraryState = AddToLibraryState.Idle)
                    }
                    else -> {
                        screenState = screenState.copy(
                            addToLibraryState = AddToLibraryState.Error(
                                message = "Unexpected error occurred",
                                gameId = game.id
                            ),
                            animatingGameIds = screenState.animatingGameIds - game.id
                        )
                        delay(3000)
                        screenState = screenState.copy(addToLibraryState = AddToLibraryState.Idle)
                    }
                }
            } catch (e: Exception) {
                screenState = screenState.copy(
                    addToLibraryState = AddToLibraryState.Error(
                        message = e.localizedMessage ?: "An error occurred",
                        gameId = game.id
                    ),
                    animatingGameIds = screenState.animatingGameIds - game.id
                )
                delay(3000)
                screenState = screenState.copy(addToLibraryState = AddToLibraryState.Idle)
            }
        }
    }

    fun resetAddToLibraryState() {
        screenState = screenState.copy(addToLibraryState = AddToLibraryState.Idle)
    }
}


