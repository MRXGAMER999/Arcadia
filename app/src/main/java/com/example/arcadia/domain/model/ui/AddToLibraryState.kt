package com.example.arcadia.domain.model.ui

/**
 * Represents the state of adding a game to the user's library.
 * Used across multiple screens (Home, Details, etc.)
 */
sealed class AddToLibraryState {
    data object Idle : AddToLibraryState()
    data class Loading(val gameId: Int) : AddToLibraryState()
    data class Success(val message: String, val gameId: Int) : AddToLibraryState()
    data class Error(val message: String, val gameId: Int) : AddToLibraryState()
    
    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    
    fun getLoadingGameId(): Int? = (this as? Loading)?.gameId
    fun getSuccessGameId(): Int? = (this as? Success)?.gameId
    fun getErrorGameId(): Int? = (this as? Error)?.gameId
}
