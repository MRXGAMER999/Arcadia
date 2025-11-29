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
    

    fun getGameList(sortOrder: SortOrder = SortOrder.NEWEST_FIRST): Flow<RequestState<List<GameListEntry>>>
    

    fun getGameListByStatus(
        status: GameStatus,
        sortOrder: SortOrder = SortOrder.NEWEST_FIRST
    ): Flow<RequestState<List<GameListEntry>>>
    

    fun getGameListByGenre(
        genre: String,
        sortOrder: SortOrder = SortOrder.NEWEST_FIRST
    ): Flow<RequestState<List<GameListEntry>>>
    

    fun getGameEntry(entryId: String): Flow<RequestState<GameListEntry>>
    

    suspend fun addGameToList(
        game: Game,
        status: GameStatus = GameStatus.WANT
    ): RequestState<String> // Returns document ID on success
    
    suspend fun addGameListEntry(
        entry: GameListEntry
    ): RequestState<String>

    suspend fun updateGameStatus(
        entryId: String,
        status: GameStatus
    ): RequestState<Unit>
    

    suspend fun updateGameRating(
        entryId: String,
        rating: Float?
    ): RequestState<Unit>
    

    suspend fun updateGameReview(
        entryId: String,
        review: String
    ): RequestState<Unit>
    

    suspend fun updateHoursPlayed(
        entryId: String,
        hours: Int
    ): RequestState<Unit>
    

    suspend fun updateGameEntry(entry: GameListEntry): RequestState<Unit>
    
    /**
     * Updates the importance values for multiple games at once.
     * Used for reordering games with the same rating.
     * @param updates Map of entryId to new importance value
     */
    suspend fun updateGamesImportance(updates: Map<String, Int>): RequestState<Unit>
    

    suspend fun removeGameFromList(entryId: String): RequestState<Unit>
    

    suspend fun isGameInList(rawgId: Int): Boolean
    

    suspend fun getEntryIdByRawgId(rawgId: Int): String?
    
    /**
     * Migrates existing library entries to include developer/publisher data.
     * Fetches game details from RAWG API and updates Firebase entries that are missing this data.
     * @param fetchGameDetails Function to fetch game details by rawgId
     * @return Number of entries updated
     */
    suspend fun migrateLibraryWithDevPub(
        fetchGameDetails: suspend (Int) -> Game?
    ): RequestState<Int>
}
