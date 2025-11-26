package com.example.arcadia.domain.model.ui

import com.example.arcadia.domain.model.Game
import com.example.arcadia.util.RequestState

/**
 * Represents the UI state for the Search screen.
 */
data class SearchUiState(
    // Search query
    val query: String = "",
    val results: RequestState<List<Game>> = RequestState.Idle,
    val gamesInLibrary: Set<Int> = emptySet(),
    
    // AI mode state
    val isAIMode: Boolean = false,
    val aiStatus: String? = null,
    val aiReasoning: String? = null,
    val searchProgress: SearchProgress? = null,
    val aiError: String? = null,
    val isAIErrorRetryable: Boolean = false,
    val isFromCache: Boolean = false,
    
    // Suggestions state
    val searchHistory: List<String> = emptyList(),
    val trendingGames: RequestState<List<Game>> = RequestState.Idle,
    val personalizedSuggestions: List<String> = emptyList(),
    
    // Add to library state
    val gameToAddWithStatus: Game? = null,
    
    // Snackbar state
    val showAddedSnackbar: Boolean = false,
    val addedGameName: String = "",
    val addedGameEntryId: String? = null
) {
    /**
     * Check if there are any results.
     */
    val hasResults: Boolean
        get() = results is RequestState.Success && 
                (results as RequestState.Success<List<Game>>).data.isNotEmpty()
    
    /**
     * Check if search is in progress.
     */
    val isSearching: Boolean
        get() = results is RequestState.Loading
    
    /**
     * Check if AI is processing.
     */
    val isAIProcessing: Boolean
        get() = isAIMode && aiStatus != null
    
    /**
     * Check if a game is in the library.
     */
    fun isGameInLibrary(gameId: Int): Boolean = gameId in gamesInLibrary
    
    /**
     * Create a copy with cleared results.
     */
    fun clearResults(): SearchUiState = copy(
        results = RequestState.Idle,
        aiStatus = null,
        aiReasoning = null,
        searchProgress = null,
        aiError = null,
        isFromCache = false
    )
    
    /**
     * Create a copy showing the snackbar.
     */
    fun showSnackbar(gameName: String, entryId: String?): SearchUiState = 
        copy(
            showAddedSnackbar = true,
            addedGameName = gameName,
            addedGameEntryId = entryId
        )
    
    /**
     * Create a copy dismissing the snackbar.
     */
    fun dismissSnackbar(): SearchUiState = 
        copy(
            showAddedSnackbar = false,
            addedGameName = "",
            addedGameEntryId = null
        )
}

/**
 * Represents search progress during AI search.
 */
data class SearchProgress(
    val current: Int,
    val total: Int
) {
    val percentage: Float
        get() = if (total > 0) current.toFloat() / total else 0f
    
    val isComplete: Boolean
        get() = current >= total
}
