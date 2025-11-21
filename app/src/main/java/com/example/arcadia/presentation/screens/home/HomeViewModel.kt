package com.example.arcadia.presentation.screens.home

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
    val gamesInLibrary: Set<Int> = emptySet(), // Track rawgIds of games in library (now merged into gameListIds logic)
    val animatingGameIds: Set<Int> = emptySet(), // Games currently animating out
    val addToLibraryState: AddToLibraryState = AddToLibraryState.Idle
)

class HomeViewModel(
    private val gameRepository: GameRepository,
    private val gameListRepository: GameListRepository
) : ViewModel() {
    
    var screenState by mutableStateOf(HomeScreenState())
        private set
    
    // Cache unfiltered recommendations for reapplying filters
    private var lastRecommended: List<Game> = emptyList()
    private var recommendationPage = 1
    private var isLoadingMoreRecommendations = false
    private var lastFilteredRecommendations: List<Game> = emptyList()
    private var lastExcludeIds: Set<Int> = emptySet()

    // Job management to prevent duplicate flows
    private var popularGamesJob: Job? = null
    private var upcomingGamesJob: Job? = null
    private var recommendedGamesJob: Job? = null
    private var newReleasesJob: Job? = null
    private var gameListJob: Job? = null

    // Track games currently being added to prevent duplicate additions of the same game
    private val gamesBeingAdded = mutableSetOf<Int>()

    init {
        // Start real-time flows only once
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
    
    private fun loadGameListIds() {
        gameListJob?.cancel()
        gameListJob = viewModelScope.launch {
            gameListRepository.getGameList().collect { state ->
                if (state is RequestState.Success) {
                    val gameIds = state.data.map { it.rawgId }.toSet()
                    screenState = screenState.copy(gamesInLibrary = gameIds)
                    // Reapply recommendation filter when game list changes
                    applyRecommendationFilter()
                }
            }
        }
    }
    
    private fun loadPopularGames() {
        popularGamesJob?.cancel()
        popularGamesJob = viewModelScope.launch {
            gameRepository.getPopularGames(page = 1, pageSize = 10).collect { state ->
                screenState = screenState.copy(popularGames = state)
            }
        }
    }
    
    private fun loadUpcomingGames() {
        upcomingGamesJob?.cancel()
        upcomingGamesJob = viewModelScope.launch {
            gameRepository.getUpcomingGames(page = 1, pageSize = 10).collect { state ->
                screenState = screenState.copy(upcomingGames = state)
            }
        }
    }
    
    private fun loadRecommendedGames() {
        recommendedGamesJob?.cancel()
        recommendedGamesJob = viewModelScope.launch {
            // Using popular tags for recommendations: singleplayer, multiplayer, action
            // Clamp to 40 (RAWG API typical limit)
            gameRepository.getRecommendedGames(tags = "singleplayer,multiplayer", page = 1, pageSize = 40).collect { state ->
                when (state) {
                    is RequestState.Success -> {
                        android.util.Log.d("HomeViewModel", "Recommendations loaded: ${state.data.size} games")
                        lastRecommended = state.data
                        recommendationPage = 1
                        applyRecommendationFilter()
                        // Auto-fetch more if filtered results are too few
                        ensureMinimumRecommendations()
                    }
                    is RequestState.Loading -> {
                        android.util.Log.d("HomeViewModel", "Loading recommendations...")
                        screenState = screenState.copy(recommendedGames = state)
                    }
                    is RequestState.Error -> {
                        android.util.Log.e("HomeViewModel", "Error loading recommendations: ${state.message}")
                        screenState = screenState.copy(recommendedGames = state)
                    }
                    else -> {
                        screenState = screenState.copy(recommendedGames = state)
                    }
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
        if (lastRecommended.isEmpty()) {
            android.util.Log.d("HomeViewModel", "Filter skipped: no recommendations to filter")
            return
        }

        // Exclude games in library but keep games that are currently animating out
        val excludeIds = screenState.gamesInLibrary - screenState.animatingGameIds

        // Only refilter if exclude set changed AND we have filtered at least once
        // This prevents skipping the initial filter when both sets are empty
        if (excludeIds == lastExcludeIds && lastFilteredRecommendations.isNotEmpty()) {
            android.util.Log.d("HomeViewModel", "Filter skipped: no changes (excludeIds=${excludeIds.size})")
            return
        }

        lastExcludeIds = excludeIds
        val filtered = lastRecommended.filter { it.id !in excludeIds }
        lastFilteredRecommendations = filtered
        android.util.Log.d("HomeViewModel", "Filter applied: ${lastRecommended.size} -> ${filtered.size} games (excluded ${excludeIds.size})")
        screenState = screenState.copy(recommendedGames = RequestState.Success(filtered))
        
        // Auto-backfill if filtered results are too few
        ensureMinimumRecommendations()
    }
    
    private fun loadNewReleases() {
        newReleasesJob?.cancel()
        newReleasesJob = viewModelScope.launch {
            gameRepository.getNewReleases(page = 1, pageSize = 10).collect { state ->
                screenState = screenState.copy(newReleases = state)
            }
        }
    }

    fun retry() {
        loadAllData()
    }
    
    fun isGameInLibrary(gameId: Int): Boolean {
        return screenState.gamesInLibrary.contains(gameId)
    }
    
    fun addGameToLibrary(game: Game) {
        viewModelScope.launch {
            // Check if this specific game is already being added
            synchronized(gamesBeingAdded) {
                if (gamesBeingAdded.contains(game.id)) {
                    return@launch // Already adding this game
                }
                gamesBeingAdded.add(game.id)
            }

            try {
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
                    // Add to library - local state is kept in sync via real-time flow
                    when (val result = gameListRepository.addGameToList(game, GameStatus.WANT)) {
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
            } finally {
                // Remove from tracking set when done
                synchronized(gamesBeingAdded) {
                    gamesBeingAdded.remove(game.id)
                }
            }
        }
    }

    fun resetAddToLibraryState() {
        screenState = screenState.copy(addToLibraryState = AddToLibraryState.Idle)
    }
}
