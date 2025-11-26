package com.example.arcadia.domain.model.ui

import com.example.arcadia.domain.model.Game
import com.example.arcadia.util.RequestState

/**
 * Represents the complete UI state for the Home screen.
 * This is the single source of truth for the Home UI.
 */
data class HomeUiState(
    // Section states
    val popularGames: RequestState<List<Game>> = RequestState.Idle,
    val upcomingGames: RequestState<List<Game>> = RequestState.Idle,
    val recommendedGames: RequestState<List<Game>> = RequestState.Idle,
    val newReleases: RequestState<List<Game>> = RequestState.Idle,
    
    // Library state
    val gamesInLibrary: Set<Int> = emptySet(),
    val animatingGameIds: Set<Int> = emptySet(),
    
    // Add to library state
    val addToLibraryState: AddToLibraryState = AddToLibraryState.Idle,
    val gameToAddWithStatus: Game? = null,
    
    // UI state
    val isRefreshing: Boolean = false,
    
    // Snackbar state
    val showAddedSnackbar: Boolean = false,
    val addedGameName: String = "",
    val addedGameEntryId: String? = null
) {
    /**
     * Check if any section is currently loading.
     */
    val isAnyLoading: Boolean
        get() = popularGames is RequestState.Loading ||
                upcomingGames is RequestState.Loading ||
                recommendedGames is RequestState.Loading ||
                newReleases is RequestState.Loading
    
    /**
     * Check if all sections have loaded successfully.
     */
    val isAllLoaded: Boolean
        get() = popularGames is RequestState.Success &&
                upcomingGames is RequestState.Success &&
                recommendedGames is RequestState.Success &&
                newReleases is RequestState.Success
    
    /**
     * Check if a specific game is in the library.
     */
    fun isGameInLibrary(gameId: Int): Boolean = gameId in gamesInLibrary
    
    /**
     * Check if a game is currently being animated (removed from UI).
     */
    fun isGameAnimating(gameId: Int): Boolean = gameId in animatingGameIds
    
    /**
     * Check if a game is being added to library.
     */
    fun isGameBeingAdded(gameId: Int): Boolean = 
        addToLibraryState is AddToLibraryState.Loading && 
        (addToLibraryState as AddToLibraryState.Loading).gameId == gameId
    
    /**
     * Create a copy with updated popular games state.
     */
    fun withPopularGames(state: RequestState<List<Game>>): HomeUiState = 
        copy(popularGames = state)
    
    /**
     * Create a copy with updated upcoming games state.
     */
    fun withUpcomingGames(state: RequestState<List<Game>>): HomeUiState = 
        copy(upcomingGames = state)
    
    /**
     * Create a copy with updated recommended games state.
     */
    fun withRecommendedGames(state: RequestState<List<Game>>): HomeUiState = 
        copy(recommendedGames = state)
    
    /**
     * Create a copy with updated new releases state.
     */
    fun withNewReleases(state: RequestState<List<Game>>): HomeUiState = 
        copy(newReleases = state)
    
    /**
     * Create a copy indicating snackbar should be shown.
     */
    fun showSnackbar(gameName: String, entryId: String?): HomeUiState = 
        copy(
            showAddedSnackbar = true,
            addedGameName = gameName,
            addedGameEntryId = entryId
        )
    
    /**
     * Create a copy dismissing the snackbar.
     */
    fun dismissSnackbar(): HomeUiState = 
        copy(
            showAddedSnackbar = false,
            addedGameName = "",
            addedGameEntryId = null
        )
    
    companion object {
        /**
         * Create an initial state with all sections loading.
         */
        fun loading(): HomeUiState = HomeUiState(
            popularGames = RequestState.Loading,
            upcomingGames = RequestState.Loading,
            recommendedGames = RequestState.Loading,
            newReleases = RequestState.Loading
        )
    }
}
