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
    

    suspend fun removeGameFromList(entryId: String): RequestState<Unit>
    

    suspend fun isGameInList(rawgId: Int): Boolean
    

    suspend fun getEntryIdByRawgId(rawgId: Int): String?
}

