package com.example.arcadia.data.repository

import android.util.Log
import com.example.arcadia.data.datasource.GameListRemoteDataSource
import com.example.arcadia.data.mapper.GameListMapper
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.SortOrder
import com.example.arcadia.util.RequestState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Implementation of GameListRepository using GameListRemoteDataSource.
 * Handles data mapping, sorting, and orchestration of data operations.
 */
class GameListRepositoryImpl(
    private val remoteDataSource: GameListRemoteDataSource
) : GameListRepository {

    companion object {
        private const val TAG = "GameListRepository"
        private const val DEFAULT_FUTURE_DATE = "9999-12-31"
    }

    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    /** Normalize ratings to one decimal bucket to match legacy ordering rules */
    private fun Float.toOneDecimalBucket(): Float = (this * 10).toInt() / 10f

    /**
     * Extension function to apply client-side sorting to game list entries.
     */
    private fun List<GameListEntry>.applySorting(sortOrder: SortOrder): List<GameListEntry> =
        when (sortOrder) {
            SortOrder.TITLE_A_Z -> sortedBy { it.name.lowercase() }
            SortOrder.TITLE_Z_A -> sortedByDescending { it.name.lowercase() }
            SortOrder.RATING_HIGH -> {
                val (rated, unrated) = partition { it.rating != null }
                rated.sortedWith(
                    compareByDescending<GameListEntry> { it.rating?.toOneDecimalBucket() ?: 0f }
                        .thenByDescending { it.importance }
                        .thenByDescending { it.rating?.toOneDecimalBucket() ?: 0f }
                ) + unrated
            }
            SortOrder.RATING_LOW -> {
                val (rated, unrated) = partition { it.rating != null }
                rated.sortedWith(
                    compareBy<GameListEntry> { it.rating?.toOneDecimalBucket() ?: 0f }
                        .thenByDescending { it.importance }
                        .thenBy { it.rating?.toOneDecimalBucket() ?: 0f }
                ) + unrated
            }
            SortOrder.RELEASE_NEW -> sortedByDescending { it.releaseDate ?: "" }
            SortOrder.RELEASE_OLD -> sortedBy { it.releaseDate ?: DEFAULT_FUTURE_DATE }
            SortOrder.NEWEST_FIRST -> sortedByDescending { it.addedAt }
            SortOrder.OLDEST_FIRST -> sortedBy { it.addedAt }
            else -> this
        }

    override fun getGameList(sortOrder: SortOrder): Flow<RequestState<List<GameListEntry>>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            return kotlinx.coroutines.flow.flowOf(RequestState.Error("User not authenticated"))
        }

        return remoteDataSource.observeGames(userId)
            .map { rows ->
                val entries = rows.map { GameListMapper.toGameListEntry(it) }
                val sorted = entries.applySorting(sortOrder)
                RequestState.Success(sorted) as RequestState<List<GameListEntry>>
            }
            .onStart { emit(RequestState.Loading) }
            .catch { e ->
                Log.e(TAG, "Error fetching game list: ${e.message}", e)
                emit(RequestState.Error("Failed to fetch games: ${e.message}"))
            }
    }

    override fun getGameListForUser(
        userId: String,
        sortOrder: SortOrder
    ): Flow<RequestState<List<GameListEntry>>> {
        return remoteDataSource.observeGames(userId)
            .map { rows ->
                val entries = rows.map { GameListMapper.toGameListEntry(it) }
                val sorted = entries.applySorting(sortOrder)
                RequestState.Success(sorted) as RequestState<List<GameListEntry>>
            }
            .onStart { emit(RequestState.Loading) }
            .catch { e ->
                Log.e(TAG, "Error fetching game list for user $userId: ${e.message}", e)
                emit(RequestState.Error("Failed to fetch games: ${e.message}"))
            }
    }

    override fun getGameListByStatus(
        status: GameStatus,
        sortOrder: SortOrder
    ): Flow<RequestState<List<GameListEntry>>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            return kotlinx.coroutines.flow.flowOf(RequestState.Error("User not authenticated"))
        }

        return remoteDataSource.observeGamesByStatus(userId, status.name)
            .map { rows ->
                val entries = rows.map { GameListMapper.toGameListEntry(it) }
                val sorted = entries.applySorting(sortOrder)
                RequestState.Success(sorted) as RequestState<List<GameListEntry>>
            }
            .onStart { emit(RequestState.Loading) }
            .catch { e ->
                Log.e(TAG, "Error fetching games by status: ${e.message}", e)
                emit(RequestState.Error("Failed to fetch games: ${e.message}"))
            }
    }

    override fun getGameListByGenre(
        genre: String,
        sortOrder: SortOrder
    ): Flow<RequestState<List<GameListEntry>>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            return kotlinx.coroutines.flow.flowOf(RequestState.Error("User not authenticated"))
        }

        return remoteDataSource.observeGamesByGenre(userId, genre)
            .map { rows ->
                val entries = rows.map { GameListMapper.toGameListEntry(it) }
                val sorted = entries.applySorting(sortOrder)
                RequestState.Success(sorted) as RequestState<List<GameListEntry>>
            }
            .onStart { emit(RequestState.Loading) }
            .catch { e ->
                Log.e(TAG, "Error fetching games by genre: ${e.message}", e)
                emit(RequestState.Error("Failed to fetch games: ${e.message}"))
            }
    }

    override fun getGameEntry(entryId: String): Flow<RequestState<GameListEntry>> {
        val userId = getCurrentUserId()
        if (userId == null) {
            return kotlinx.coroutines.flow.flowOf(RequestState.Error("User not authenticated"))
        }

        return remoteDataSource.observeGame(entryId)
            .map { row ->
                val entry = GameListMapper.toGameListEntry(row)
                RequestState.Success(entry) as RequestState<GameListEntry>
            }
            .onStart { emit(RequestState.Loading) }
            .catch { e ->
                Log.e(TAG, "Error fetching game entry: ${e.message}", e)
                emit(RequestState.Error("Failed to fetch game: ${e.message}"))
            }
    }

    override suspend fun addGameToList(game: Game, status: GameStatus): RequestState<String> {
        return try {
            val userId = getCurrentUserId()
                ?: return RequestState.Error("User not authenticated")

            // Check if game already exists
            if (isGameInList(game.id)) {
                return RequestState.Error("Game already in list")
            }

            val currentTime = System.currentTimeMillis()
            val entry = GameListEntry(
                rawgId = game.id,
                name = game.name,
                backgroundImage = game.backgroundImage,
                genres = game.genres,
                platforms = game.platforms,
                developers = game.developers,
                publishers = game.publishers,
                addedAt = currentTime,
                updatedAt = currentTime,
                status = status,
                rating = null,
                review = "",
                hoursPlayed = 0,
                aspects = emptyList(),
                releaseDate = game.released
            )

            val row = remoteDataSource.addGame(userId, GameListMapper.toAppwriteData(entry, userId))
            Log.d(TAG, "Game added to list successfully: ${game.name}")
            RequestState.Success(row.id)

        } catch (e: Exception) {
            Log.e(TAG, "Error adding game to list: ${e.message}", e)
            RequestState.Error("Failed to add game: ${e.message}")
        }
    }

    override suspend fun addGameListEntry(entry: GameListEntry): RequestState<String> {
        return try {
            val userId = getCurrentUserId()
                ?: return RequestState.Error("User not authenticated")

            // Check if game already exists
            if (isGameInList(entry.rawgId)) {
                return RequestState.Error("Game already in list")
            }

            val row = remoteDataSource.addGame(userId, GameListMapper.toAppwriteData(entry, userId))
            Log.d(TAG, "Game added to list successfully: ${entry.name}")
            RequestState.Success(row.id)

        } catch (e: Exception) {
            Log.e(TAG, "Error adding game list entry: ${e.message}", e)
            RequestState.Error("Failed to add game: ${e.message}")
        }
    }

    override suspend fun updateGameStatus(entryId: String, status: GameStatus): RequestState<Unit> {
        return try {
            val updates = mapOf(
                "status" to status.name,
                "updatedAt" to System.currentTimeMillis()
            )
            remoteDataSource.updateGame(entryId, updates)
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating game status: ${e.message}", e)
            RequestState.Error("Failed to update status: ${e.message}")
        }
    }

    override suspend fun updateGameRating(entryId: String, rating: Float?): RequestState<Unit> {
        return try {
            // Validate rating
            if (rating != null && (rating < 0f || rating > 10f)) {
                return RequestState.Error("Rating must be between 0 and 10")
            }

            // Round to one decimal place
            val roundedRating = rating?.let { (it * 10).toInt() / 10f }

            val updates = mapOf(
                "rating" to roundedRating,
                "updatedAt" to System.currentTimeMillis()
            )
            remoteDataSource.updateGame(entryId, updates)
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating game rating: ${e.message}", e)
            RequestState.Error("Failed to update rating: ${e.message}")
        }
    }

    override suspend fun updateGameReview(entryId: String, review: String): RequestState<Unit> {
        return try {
            val updates = mapOf(
                "review" to review,
                "updatedAt" to System.currentTimeMillis()
            )
            remoteDataSource.updateGame(entryId, updates)
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating game review: ${e.message}", e)
            RequestState.Error("Failed to update review: ${e.message}")
        }
    }

    override suspend fun updateHoursPlayed(entryId: String, hours: Int): RequestState<Unit> {
        return try {
            if (hours < 0) {
                return RequestState.Error("Hours played cannot be negative")
            }

            val updates = mapOf(
                "hoursPlayed" to hours,
                "updatedAt" to System.currentTimeMillis()
            )
            remoteDataSource.updateGame(entryId, updates)
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating hours played: ${e.message}", e)
            RequestState.Error("Failed to update hours: ${e.message}")
        }
    }

    override suspend fun updateGameEntry(entry: GameListEntry): RequestState<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return RequestState.Error("User not authenticated")

            if (entry.rating != null && (entry.rating < 0f || entry.rating > 10f)) {
                return RequestState.Error("Rating must be between 0 and 10")
            }

            val updatedEntry = entry.copy(updatedAt = System.currentTimeMillis())
            remoteDataSource.updateGame(entry.id, GameListMapper.toAppwriteData(updatedEntry, userId))
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating game entry: ${e.message}", e)
            RequestState.Error("Failed to update game: ${e.message}")
        }
    }

    override suspend fun updateGamesImportance(updates: Map<String, Int>): RequestState<Unit> = coroutineScope {
        try {
            if (getCurrentUserId() == null) {
                return@coroutineScope RequestState.Error("User not authenticated")
            }

            if (updates.isEmpty()) {
                return@coroutineScope RequestState.Success(Unit)
            }

            val updatedAt = System.currentTimeMillis()
            val chunks = updates.entries.chunked(5)

            chunks.map { chunk ->
                async {
                    chunk.forEach { (entryId, importance) ->
                        remoteDataSource.updateGameImportance(entryId, importance, updatedAt)
                    }
                }
            }.awaitAll()

            Log.d(TAG, "Updated importance for ${updates.size} games")
            RequestState.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating games importance: ${e.message}", e)
            RequestState.Error("Failed to update game order: ${e.message}")
        }
    }

    override suspend fun removeGameFromList(entryId: String): RequestState<Unit> {
        return try {
            remoteDataSource.removeGame(entryId)
            RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing game: ${e.message}", e)
            RequestState.Error("Failed to remove game: ${e.message}")
        }
    }

    override suspend fun isGameInList(rawgId: Int): Boolean {
        return try {
            val userId = getCurrentUserId() ?: return false
            remoteDataSource.getGameByRawgId(userId, rawgId) != null
        } catch (e: Exception) {
            Log.e(TAG, "Error checking game in list: ${e.message}", e)
            false
        }
    }

    override suspend fun getEntryIdByRawgId(rawgId: Int): String? {
        return try {
            val userId = getCurrentUserId() ?: return null
            remoteDataSource.getGameByRawgId(userId, rawgId)?.id
        } catch (e: Exception) {
            Log.e(TAG, "Error getting entry ID: ${e.message}", e)
            null
        }
    }

    override suspend fun migrateLibraryWithDevPub(
        fetchGameDetails: suspend (Int) -> Game?
    ): RequestState<Int> {
        return try {
            val userId = getCurrentUserId()
                ?: return RequestState.Error("User not authenticated")

            val games = remoteDataSource.getGames(userId)
            var updatedCount = 0

            for (row in games) {
                try {
                    val developers = (row.data["developers"] as? List<*>) ?: emptyList<Any>()
                    val publishers = (row.data["publishers"] as? List<*>) ?: emptyList<Any>()
                    val rawgId = (row.data["rawgId"] as? Number)?.toInt() ?: continue

                    // Skip if already has dev/pub data
                    if (developers.isNotEmpty() || publishers.isNotEmpty()) {
                        continue
                    }

                    // Fetch game details
                    val game = fetchGameDetails(rawgId) ?: continue

                    // Update only if we got dev/pub data
                    if (game.developers.isNotEmpty() || game.publishers.isNotEmpty()) {
                        val updates = mutableMapOf<String, Any>()
                        if (game.developers.isNotEmpty()) {
                            updates["developers"] = game.developers
                        }
                        if (game.publishers.isNotEmpty()) {
                            updates["publishers"] = game.publishers
                        }

                        remoteDataSource.updateGame(row.id, updates)
                        updatedCount++
                        Log.d(TAG, "Migrated ${row.data["name"]}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to migrate entry ${row.id}: ${e.message}")
                }
            }

            Log.d(TAG, "Migration complete. Updated $updatedCount entries.")
            RequestState.Success(updatedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error during migration: ${e.message}", e)
            RequestState.Error("Migration failed: ${e.message}")
        }
    }
}