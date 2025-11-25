package com.example.arcadia.presentation.screens.searchScreen

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.domain.repository.GeminiRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class SearchScreenState(
    val query: String = "",
    val results: RequestState<List<Game>> = RequestState.Idle,
    val gamesInLibrary: Set<Int> = emptySet(),
    // AI MODE
    val isAIMode: Boolean = false,
    val aiStatus: String? = null,
    val aiReasoning: String? = null,
    val searchProgress: Pair<Int, Int>? = null,  // (current, total) for progress
    val aiError: String? = null  // Separate AI error to avoid flash
)

class SearchViewModel(
    private val gameRepository: GameRepository,
    private val gameListRepository: GameListRepository,
    private val geminiRepository: GeminiRepository
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
            gameListRepository.getGameList().collect { state ->
                if (state is RequestState.Success<*>) {
                    val gameIds = (state.data as List<GameListEntry>).map { it.rawgId }.toSet()
                    screenState = screenState.copy(gamesInLibrary = gameIds)
                }
            }
        }
    }

    /**
     * Toggle AI search mode
     */
    fun toggleAIMode() {
        screenState = screenState.copy(
            isAIMode = !screenState.isAIMode,
            results = RequestState.Idle,
            aiStatus = null,
            aiReasoning = null,
            searchProgress = null,
            aiError = null
        )
    }
    
    fun updateQuery(newQuery: String) {
        screenState = screenState.copy(query = newQuery)
        
        // Cancel previous search job (including ongoing collection)
        searchJob?.cancel()
        
        // If query is empty, reset to idle
        if (newQuery.isBlank()) {
            screenState = screenState.copy(
                results = RequestState.Idle,
                aiStatus = null,
                aiReasoning = null,
                searchProgress = null,
                aiError = null
            )
            return
        }
        
        // Debounce: longer delay for AI mode since it's more expensive
        searchJob = viewModelScope.launch {
            delay(if (screenState.isAIMode) 1000 else 500)
            
            if (screenState.isAIMode) {
                performAISearch(newQuery)
            } else {
                // Regular text search
                gameRepository.searchGames(newQuery, page = 1, pageSize = 40).collect { state ->
                    screenState = screenState.copy(results = state)
                }
            }
        }
    }

    /**
     * Perform AI-powered search:
     * 1. Ask Gemini for game suggestions
     * 2. Search RAWG for each game
     * 3. Combine results
     */
    private suspend fun performAISearch(query: String) {
        screenState = screenState.copy(
            results = RequestState.Loading,
            aiStatus = "Asking AI for recommendations...",
            aiReasoning = null,
            searchProgress = null,
            aiError = null // Clear any previous errors
        )
        
        // Step 1: Get game suggestions from Gemini
        val suggestionsResult = geminiRepository.suggestGames(query, count = 8)
        
        suggestionsResult.onFailure { error ->
            screenState = screenState.copy(
                results = RequestState.Idle, // Use Idle instead of Error to avoid flash
                aiStatus = null,
                aiError = "AI unavailable: ${error.message}"
            )
            return
        }
        
        val suggestions = suggestionsResult.getOrNull() ?: return
        val gameNames = suggestions.games
        
        if (gameNames.isEmpty()) {
            screenState = screenState.copy(
                results = RequestState.Idle,
                aiStatus = null,
                aiError = "No games found for your query"
            )
            return
        }
        
        screenState = screenState.copy(
            aiStatus = "Searching ${gameNames.size} games...",
            aiReasoning = suggestions.reasoning,
            searchProgress = Pair(0, gameNames.size),
            aiError = null
        )
        
        // Step 2: Search RAWG for each game name in parallel
        val allGames = mutableListOf<Game>()
        val seenIds = mutableSetOf<Int>()
        
        supervisorScope {
            val searchJobs = gameNames.mapIndexed { index, gameName ->
                async {
                    try {
                        var foundGame: Game? = null
                        gameRepository.searchGames(gameName, pageSize = 5).collect { result ->
                            if (result is RequestState.Success) {
                                // Take the first result that closely matches
                                foundGame = result.data.firstOrNull { game ->
                                    game.name.contains(gameName, ignoreCase = true) ||
                                    gameName.contains(game.name, ignoreCase = true) ||
                                    calculateSimilarity(game.name, gameName) > 0.6
                                } ?: result.data.firstOrNull()
                            }
                        }
                        
                        // Update progress
                        screenState = screenState.copy(
                            searchProgress = Pair(index + 1, gameNames.size)
                        )
                        
                        foundGame
                    } catch (e: Exception) {
                        Log.e("SearchViewModel", "Error searching for: $gameName", e)
                        null
                    }
                }
            }
            
            // Await all searches
            val results = searchJobs.awaitAll()
            
            // Combine and deduplicate
            results.filterNotNull().forEach { game ->
                if (game.id !in seenIds) {
                    seenIds.add(game.id)
                    allGames.add(game)
                }
            }
        }
        
        // Step 3: Return combined results
        screenState = screenState.copy(
            results = if (allGames.isEmpty()) {
                RequestState.Idle // Use Idle to show custom AI message instead of Error
            } else {
                RequestState.Success(allGames)
            },
            aiStatus = null,
            searchProgress = null,
            aiError = if (allGames.isEmpty()) "Couldn't find any matching games" else null
        )
    }

    /**
     * Simple string similarity check (Jaccard-like)
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val words1 = s1.lowercase().split(" ", ":", "-").filter { it.isNotBlank() }.toSet()
        val words2 = s2.lowercase().split(" ", ":", "-").filter { it.isNotBlank() }.toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return intersection.toDouble() / union.toDouble()
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
