package com.example.arcadia.domain.repository

import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.UserGame
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.Flow

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

interface UserGamesRepository {
    
    /**
     * Get all games in user's library
     */
    fun getUserGames(sortOrder: SortOrder = SortOrder.NEWEST_FIRST): Flow<RequestState<List<UserGame>>>
    
    /**
     * Get user games filtered by genre
     */
    fun getUserGamesByGenre(genre: String, sortOrder: SortOrder = SortOrder.NEWEST_FIRST): Flow<RequestState<List<UserGame>>>
    
    /**
     * Add a game to user's library
     */
    suspend fun addGame(game: Game): RequestState<Unit>
    
    /**
     * Remove a game from user's library
     */
    suspend fun removeGame(gameId: String): RequestState<Unit>
    
    /**
     * Check if a game is in user's library
     */
    suspend fun isGameInLibrary(rawgId: Int): Boolean
}

