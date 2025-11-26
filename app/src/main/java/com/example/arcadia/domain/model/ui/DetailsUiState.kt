package com.example.arcadia.domain.model.ui

import com.example.arcadia.domain.model.Game
import com.example.arcadia.util.RequestState

/**
 * Represents the UI state for the Details screen.
 */
data class DetailsUiState(
    val gameState: RequestState<Game> = RequestState.Idle,
    val isInLibrary: Boolean = false,
    val isFavorite: Boolean = false,
    val addToLibraryState: AddToLibraryState = AddToLibraryState.Idle,
    val errorMessage: String? = null
) {
    /**
     * Check if the game details are loaded.
     */
    val isLoaded: Boolean
        get() = gameState is RequestState.Success
    
    /**
     * Check if the game details are loading.
     */
    val isLoading: Boolean
        get() = gameState is RequestState.Loading
    
    /**
     * Check if there's an error.
     */
    val hasError: Boolean
        get() = gameState is RequestState.Error || errorMessage != null
    
    /**
     * Get the error message.
     */
    val error: String?
        get() = errorMessage ?: (gameState as? RequestState.Error)?.message
    
    /**
     * Get the loaded game details.
     */
    val game: Game?
        get() = (gameState as? RequestState.Success)?.data
    
    /**
     * Create a copy with loading state.
     */
    fun loading(): DetailsUiState = copy(
        gameState = RequestState.Loading,
        errorMessage = null
    )
    
    /**
     * Create a copy with success state.
     */
    fun success(game: Game): DetailsUiState = copy(
        gameState = RequestState.Success(game),
        errorMessage = null
    )
    
    /**
     * Create a copy with error state.
     */
    fun error(message: String): DetailsUiState = copy(
        gameState = RequestState.Error(message),
        errorMessage = message
    )
}
