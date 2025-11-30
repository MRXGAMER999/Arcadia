package com.example.arcadia.presentation.screens.searchScreen

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.arcadia.domain.model.AIError
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.domain.usecase.AddGameToLibraryUseCase
import com.example.arcadia.domain.usecase.GetAIGameSuggestionsUseCase
import com.example.arcadia.domain.usecase.GetPopularGamesUseCase
import com.example.arcadia.domain.usecase.SearchGamesUseCase
import com.example.arcadia.presentation.base.LibraryAwareViewModel
import com.example.arcadia.util.PreferencesManager
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

data class SearchScreenState(
    val query: String = "",
    val results: RequestState<List<Game>> = RequestState.Idle,
    // AI MODE
    val isAIMode: Boolean = false,
    val aiStatus: String? = null,
    val aiReasoning: String? = null,
    val searchProgress: Pair<Int, Int>? = null,
    val aiError: String? = null,
    val isAIErrorRetryable: Boolean = false,
    val isFromCache: Boolean = false,
    // Search suggestions
    val searchHistory: List<String> = emptyList(),
    val trendingGames: RequestState<List<Game>> = RequestState.Idle,
    val personalizedSuggestions: List<String> = emptyList()
)

/**
 * ViewModel for the Search screen.
 * Uses SearchGamesUseCase, GetAIGameSuggestionsUseCase, and GetPopularGamesUseCase
 * to properly encapsulate business logic.
 */
class SearchViewModel(
    private val gameRepository: GameRepository,
    gameListRepository: GameListRepository,
    private val preferencesManager: PreferencesManager,
    addGameToLibraryUseCase: AddGameToLibraryUseCase,
    private val searchGamesUseCase: SearchGamesUseCase,
    private val getAIGameSuggestionsUseCase: GetAIGameSuggestionsUseCase,
    private val getPopularGamesUseCase: GetPopularGamesUseCase
) : LibraryAwareViewModel(gameListRepository, addGameToLibraryUseCase) {
    
    var screenState by mutableStateOf(SearchScreenState())
        private set
    
    private var userGenres: Set<String> = emptySet()
    
    // AI Search pagination
    private var lastAIQuery = ""
    private var allAIResults = mutableListOf<Game>()
    private var isLoadingMoreAI = false
    
    init {
        loadSearchHistory()
        loadTrendingGames()
    }
    
    override fun onLibraryUpdated(games: List<GameListEntry>) {
        // Extract user's favorite genres
        val genreCounts = mutableMapOf<String, Int>()
        games.forEach { game ->
            game.genres.forEach { genre ->
                genreCounts[genre] = (genreCounts[genre] ?: 0) + 1
            }
        }
        userGenres = genreCounts.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
            .toSet()
        
        // Generate personalized search suggestions
        val suggestions = generatePersonalizedSuggestions(userGenres)
        screenState = screenState.copy(personalizedSuggestions = suggestions)
    }
    
    private fun generatePersonalizedSuggestions(genres: Set<String>): List<String> {
        if (genres.isEmpty()) return emptyList()
        
        val suggestions = mutableListOf<String>()
        genres.take(3).forEach { genre ->
            suggestions.add("Best $genre games")
            suggestions.add("New $genre releases")
        }
        return suggestions.take(5)
    }
    
    private fun loadSearchHistory() {
        val history = preferencesManager.getSearchHistory()
        screenState = screenState.copy(searchHistory = history)
    }
    
    private fun loadTrendingGames() {
        launchWithKey("trending_games") {
            getPopularGamesUseCase(page = 1, pageSize = 6).collect { state ->
                screenState = screenState.copy(trendingGames = state)
            }
        }
    }
    
    fun saveSearchToHistory(query: String) {
        if (query.isBlank()) return
        preferencesManager.addSearchQuery(query)
        loadSearchHistory()
    }
    
    fun removeFromHistory(query: String) {
        preferencesManager.removeSearchQuery(query)
        loadSearchHistory()
    }
    
    fun clearHistory() {
        preferencesManager.clearSearchHistory()
        loadSearchHistory()
    }
    
    fun selectHistoryItem(query: String) {
        screenState = screenState.copy(query = query)
        updateQuery(query)
    }

    fun toggleAIMode() {
        val currentQuery = screenState.query
        val newIsAIMode = !screenState.isAIMode
        
        screenState = screenState.copy(
            isAIMode = newIsAIMode,
            results = RequestState.Idle,
            aiStatus = null,
            aiReasoning = null,
            searchProgress = null,
            aiError = null
        )
        
        if (currentQuery.isNotBlank()) {
            launchWithDebounce(
                key = "search",
                delay = 300L
            ) {
                if (newIsAIMode) {
                    performAISearch(currentQuery)
                } else {
                    searchGamesUseCase(currentQuery, page = 1, pageSize = 40).collect { state ->
                        screenState = screenState.copy(results = state)
                    }
                }
            }
        }
    }
    
    fun updateQuery(newQuery: String) {
        screenState = screenState.copy(query = newQuery)
        
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
        
        launchWithDebounce(
            key = "search",
            delay = if (screenState.isAIMode) 1000L else 500L
        ) {
            saveSearchToHistory(newQuery)
            
            if (screenState.isAIMode) {
                performAISearch(newQuery)
            } else {
                searchGamesUseCase(newQuery, page = 1, pageSize = 40).collect { state ->
                    screenState = screenState.copy(results = state)
                }
            }
        }
    }
    
    /**
     * Immediately perform search without debounce.
     * Used when navigating to search with an initial query (e.g., from AI recommendations).
     */
    fun performSearch() {
        val query = screenState.query
        if (query.isBlank()) return
        
        launchWithKey("search") {
            saveSearchToHistory(query)
            
            if (screenState.isAIMode) {
                performAISearch(query)
            } else {
                searchGamesUseCase(query, page = 1, pageSize = 40).collect { state ->
                    screenState = screenState.copy(results = state)
                }
            }
        }
    }

    private suspend fun performAISearch(query: String) {
        lastAIQuery = query
        allAIResults.clear()
        
        screenState = screenState.copy(
            results = RequestState.Loading,
            aiStatus = "Asking AI for recommendations...",
            aiReasoning = null,
            searchProgress = null,
            aiError = null,
            isAIErrorRetryable = false,
            isFromCache = false
        )
        
        val suggestionsResult = getAIGameSuggestionsUseCase(query, count = 15)
        
        suggestionsResult.onFailure { error ->
            val aiError = if (error is AIError) error else AIError.from(error)
            val isRetryable = with(AIError.Companion) { aiError.isRetryable() }
            
            screenState = screenState.copy(
                results = RequestState.Idle,
                aiStatus = null,
                aiError = aiError.message ?: "AI unavailable",
                isAIErrorRetryable = isRetryable
            )
            return
        }
        
        val suggestions = suggestionsResult.getOrNull() ?: return
        val gameNames = suggestions.games
        
        if (gameNames.isEmpty()) {
            screenState = screenState.copy(
                results = RequestState.Idle,
                aiStatus = null,
                aiError = "No games found for your query",
                isAIErrorRetryable = true
            )
            return
        }
        
        screenState = screenState.copy(
            aiStatus = if (suggestions.fromCache) "Using cached results..." else "Searching ${gameNames.size} games...",
            aiReasoning = suggestions.reasoning,
            searchProgress = Pair(0, gameNames.size),
            aiError = null,
            isFromCache = suggestions.fromCache
        )
        
        val allGames = mutableListOf<Game>()
        val seenIds = mutableSetOf<Int>()
        
        supervisorScope {
            val searchJobs = gameNames.mapIndexed { index, gameName ->
                async {
                    try {
                        var foundGame: Game? = null
                        searchGamesUseCase(gameName, pageSize = 5).collect { result ->
                            if (result is RequestState.Success) {
                                foundGame = result.data.firstOrNull { game ->
                                    game.name.contains(gameName, ignoreCase = true) ||
                                    gameName.contains(game.name, ignoreCase = true) ||
                                    calculateSimilarity(game.name, gameName) > 0.6
                                } ?: result.data.firstOrNull()
                            }
                        }
                        
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
            
            val results = searchJobs.awaitAll()
            
            results.filterNotNull().forEach { game ->
                if (game.id !in seenIds) {
                    seenIds.add(game.id)
                    allGames.add(game)
                }
            }
        }
        
        allAIResults.addAll(allGames)
        
        screenState = screenState.copy(
            results = if (allGames.isEmpty()) {
                RequestState.Idle
            } else {
                RequestState.Success(allAIResults.toList())
            },
            aiStatus = null,
            searchProgress = null,
            aiError = if (allGames.isEmpty()) "Couldn't find any matching games" else null,
            isAIErrorRetryable = allGames.isEmpty(),
            isFromCache = suggestions.fromCache
        )
    }
    
    fun loadMoreAIResults() {
        if (lastAIQuery.isBlank() || isLoadingMoreAI) return
        
        launchWithKey("load_more_ai") {
            isLoadingMoreAI = true
            
            screenState = screenState.copy(
                aiStatus = "Finding more games...",
                searchProgress = null
            )
            
            try {
                // Exclude ALL existing games to prevent duplicates
                val excludeNames = allAIResults.map { it.name }.joinToString(", ")
                val modifiedQuery = "$lastAIQuery (do NOT suggest: $excludeNames)"
                val moreResult = getAIGameSuggestionsUseCase(
                    query = modifiedQuery,
                    count = 10
                )
                
                moreResult.onSuccess { suggestions ->
                    val gameNames = suggestions.games
                    if (gameNames.isEmpty()) {
                        screenState = screenState.copy(
                            aiStatus = null,
                            aiError = "No more games found"
                        )
                        return@launchWithKey
                    }
                    
                    screenState = screenState.copy(
                        aiStatus = "Searching ${gameNames.size} more games...",
                        searchProgress = Pair(0, gameNames.size)
                    )
                    
                    val newGames = mutableListOf<Game>()
                    val existingIds = allAIResults.map { it.id }.toSet()
                    
                    supervisorScope {
                        val searchJobs = gameNames.mapIndexed { index, gameName ->
                            async {
                                try {
                                    var foundGame: Game? = null
                                    searchGamesUseCase(gameName, pageSize = 5).collect { result ->
                                        if (result is RequestState.Success) {
                                            foundGame = result.data.firstOrNull { game ->
                                                game.name.contains(gameName, ignoreCase = true) ||
                                                gameName.contains(game.name, ignoreCase = true) ||
                                                calculateSimilarity(game.name, gameName) > 0.6
                                            } ?: result.data.firstOrNull()
                                        }
                                    }
                                    
                                    screenState = screenState.copy(
                                        searchProgress = Pair(index + 1, gameNames.size)
                                    )
                                    
                                    foundGame
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }
                        
                        val results = searchJobs.awaitAll()
                        results.filterNotNull().forEach { game ->
                            if (game.id !in existingIds) {
                                newGames.add(game)
                            }
                        }
                    }
                    
                    allAIResults.addAll(newGames)
                    
                    screenState = screenState.copy(
                        results = RequestState.Success(allAIResults.toList()),
                        aiStatus = null,
                        searchProgress = null,
                        aiError = if (newGames.isEmpty()) "No more unique games found" else null
                    )
                }
                
                moreResult.onFailure { error ->
                    val aiError = if (error is AIError) error else AIError.from(error)
                    screenState = screenState.copy(
                        aiStatus = null,
                        aiError = aiError.message ?: "Couldn't load more",
                        isAIErrorRetryable = with(AIError.Companion) { aiError.isRetryable() }
                    )
                }
            } finally {
                isLoadingMoreAI = false
            }
        }
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        val words1 = s1.lowercase().split(" ", ":", "-").filter { it.isNotBlank() }.toSet()
        val words2 = s2.lowercase().split(" ", ":", "-").filter { it.isNotBlank() }.toSet()
        
        if (words1.isEmpty() || words2.isEmpty()) return 0.0
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return intersection.toDouble() / union.toDouble()
    }
}