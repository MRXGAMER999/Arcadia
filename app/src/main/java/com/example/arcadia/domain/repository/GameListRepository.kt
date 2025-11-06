package com.example.arcadia.domain.repository

import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing user's game list with ratings and status tracking
 */
interface GameListRepository {
    
    /**
     * Get all games in user's game list
     * @param sortOrder Sort order for the list
     * @return Flow of RequestState with list of game entries
     */
    fun getGameList(sortOrder: SortOrder = SortOrder.NEWEST_FIRST): Flow<RequestState<List<GameListEntry>>>
    
    /**
     * Get games filtered by status
     * @param status Filter by game status
     * @param sortOrder Sort order for the list
     * @return Flow of RequestState with filtered list
     */
    fun getGameListByStatus(
        status: GameStatus,
        sortOrder: SortOrder = SortOrder.NEWEST_FIRST
    ): Flow<RequestState<List<GameListEntry>>>
    
    /**
     * Get games filtered by genre
     * @param genre Filter by genre name
     * @param sortOrder Sort order for the list
     * @return Flow of RequestState with filtered list
     */
    fun getGameListByGenre(
        genre: String,
        sortOrder: SortOrder = SortOrder.NEWEST_FIRST
    ): Flow<RequestState<List<GameListEntry>>>
    
    /**
     * Get a specific game entry by its Firestore document ID
     * @param entryId Firestore document ID
     * @return Flow of RequestState with the game entry
     */
    fun getGameEntry(entryId: String): Flow<RequestState<GameListEntry>>
    
    /**
     * Add a game to user's game list
     * @param game Game to add
     * @param status Initial status (default: WANT)
     * @return RequestState indicating success or error
     */
    suspend fun addGameToList(
        game: Game,
        status: GameStatus = GameStatus.WANT
    ): RequestState<String> // Returns document ID on success
    
    /**
     * Update game status
     * @param entryId Firestore document ID
     * @param status New status
     * @return RequestState indicating success or error
     */
    suspend fun updateGameStatus(
        entryId: String,
        status: GameStatus
    ): RequestState<Unit>
    
    /**
     * Update game rating
     * @param entryId Firestore document ID
     * @param rating Rating value (0.0 - 5.0), null to remove rating
     * @return RequestState indicating success or error
     */
    suspend fun updateGameRating(
        entryId: String,
        rating: Float?
    ): RequestState<Unit>
    
    /**
     * Update game review/notes
     * @param entryId Firestore document ID
     * @param review Review text
     * @return RequestState indicating success or error
     */
    suspend fun updateGameReview(
        entryId: String,
        review: String
    ): RequestState<Unit>
    
    /**
     * Update hours played
     * @param entryId Firestore document ID
     * @param hours Hours played
     * @return RequestState indicating success or error
     */
    suspend fun updateHoursPlayed(
        entryId: String,
        hours: Int
    ): RequestState<Unit>
    
    /**
     * Update complete game entry
     * @param entry Updated game entry
     * @return RequestState indicating success or error
     */
    suspend fun updateGameEntry(entry: GameListEntry): RequestState<Unit>
    
    /**
     * Remove a game from user's game list
     * @param entryId Firestore document ID
     * @return RequestState indicating success or error
     */
    suspend fun removeGameFromList(entryId: String): RequestState<Unit>
    
    /**
     * Check if a game is in user's game list
     * @param rawgId RAWG API game ID
     * @return True if game is in list, false otherwise
     */
    suspend fun isGameInList(rawgId: Int): Boolean
    
    /**
     * Get entry ID for a game by its RAWG ID
     * @param rawgId RAWG API game ID
     * @return Entry ID if found, null otherwise
     */
    suspend fun getEntryIdByRawgId(rawgId: Int): String?
}

