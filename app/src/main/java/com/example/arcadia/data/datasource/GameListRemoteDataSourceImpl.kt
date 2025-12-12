package com.example.arcadia.data.datasource

import android.util.Log
import com.example.arcadia.BuildConfig
import com.example.arcadia.util.AppwriteConstants
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.Row
import io.appwrite.services.Realtime
import io.appwrite.services.TablesDB
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class GameListRemoteDataSourceImpl(
    tablesDbLazy: Lazy<TablesDB>,
    realtimeLazy: Lazy<Realtime>
) : GameListRemoteDataSource {

    private val tablesDb by tablesDbLazy
    private val realtime by realtimeLazy

    companion object {
        private const val TAG = "GameListRemoteDataSource"
        private val DATABASE_ID = BuildConfig.APPWRITE_DATABASE_ID
        private val GAMES_COLLECTION_ID = AppwriteConstants.GAMES_COLLECTION_ID
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_RAWG_ID = "rawgId"
        private const val FIELD_STATUS = "status"
        private const val FIELD_GENRES = "genres"
        
        private const val IMPORTANCE_UPDATE_MAX_RETRIES = 3
        private val IMPORTANCE_UPDATE_RETRY_CODES = setOf(429, 500, 502, 503, 504)
    }

    private suspend fun fetchGames(userId: String): List<Row<Map<String, Any>>> {
        return tablesDb.listRows(
            databaseId = DATABASE_ID,
            tableId = GAMES_COLLECTION_ID,
            queries = listOf(
                Query.equal(FIELD_USER_ID, userId),
                Query.limit(1000)
            )
        ).rows
    }

    override fun observeGames(userId: String): Flow<List<Row<Map<String, Any>>>> = callbackFlow {
        var currentList = try {
            fetchGames(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching initial games: ${e.message}", e)
            emptyList()
        }
        trySend(currentList)

        val subscription = realtime.subscribe(
            "databases.$DATABASE_ID.tables.$GAMES_COLLECTION_ID.rows"
        ) { response ->
            val payload = response.payload as? Map<String, Any?>
            val docUserId = payload?.get(FIELD_USER_ID) as? String
            
            if (docUserId == userId && payload != null) {
                launch {
                    val eventType = response.events.firstOrNull() ?: ""
                    val docId = payload["\$id"] as? String
                    
                    if (docId != null) {
                        try {
                            if (eventType.contains(".delete")) {
                                currentList = currentList.filter { it.id != docId }
                                trySend(currentList)
                            } else {
                                // For create/update, fetch the single fresh row
                                // This ensures we have the correct Row object structure
                                try {
                                    val row = getGame(docId)
                                    if (eventType.contains(".create")) {
                                        currentList = currentList + row
                                    } else {
                                        // Update
                                        currentList = currentList.map { if (it.id == docId) row else it }
                                    }
                                    trySend(currentList)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing row update: ${e.message}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating local list: ${e.message}", e)
                        }
                    }
                }
            }
        }

        awaitClose { subscription.close() }
    }

    override suspend fun getGames(userId: String): List<Row<Map<String, Any>>> {
        return fetchGames(userId)
    }

    override suspend fun addGame(userId: String, data: Map<String, Any?>): Row<Map<String, Any>> {
        return tablesDb.createRow(
            databaseId = DATABASE_ID,
            tableId = GAMES_COLLECTION_ID,
            rowId = ID.unique(),
            data = data
        )
    }

    override suspend fun updateGame(gameId: String, data: Map<String, Any?>): Row<Map<String, Any>> {
        return tablesDb.updateRow(
            databaseId = DATABASE_ID,
            tableId = GAMES_COLLECTION_ID,
            rowId = gameId,
            data = data
        )
    }

    override suspend fun updateGameImportance(gameId: String, importance: Int, updatedAt: Long) {
        updateImportanceWithRetry(gameId, importance, updatedAt)
    }

    private suspend fun updateImportanceWithRetry(
        gameId: String, 
        importance: Int, 
        updatedAt: Long, 
        attempt: Int = 0
    ) {
        try {
            tablesDb.updateRow(
                databaseId = DATABASE_ID,
                tableId = GAMES_COLLECTION_ID,
                rowId = gameId,
                data = mapOf(
                    "importance" to importance,
                    "updatedAt" to updatedAt
                )
            )
        } catch (e: AppwriteException) {
            val shouldRetry = e.code in IMPORTANCE_UPDATE_RETRY_CODES && attempt < IMPORTANCE_UPDATE_MAX_RETRIES
            if (!shouldRetry) throw e
            
            kotlinx.coroutines.delay(250L shl attempt)
            updateImportanceWithRetry(gameId, importance, updatedAt, attempt + 1)
        }
    }

    override suspend fun removeGame(gameId: String) {
        tablesDb.deleteRow(
            databaseId = DATABASE_ID,
            tableId = GAMES_COLLECTION_ID,
            rowId = gameId
        )
    }

    override suspend fun getGame(gameId: String): Row<Map<String, Any>> {
        return tablesDb.getRow(
            databaseId = DATABASE_ID,
            tableId = GAMES_COLLECTION_ID,
            rowId = gameId
        )
    }

    override suspend fun getGameByRawgId(userId: String, rawgId: Int): Row<Map<String, Any>>? {
        val docs = tablesDb.listRows(
            databaseId = DATABASE_ID,
            tableId = GAMES_COLLECTION_ID,
            queries = listOf(
                Query.equal(FIELD_USER_ID, userId),
                // Appwrite table currently stores rawgId as STRING
                Query.equal(FIELD_RAWG_ID, rawgId.toString()),
                Query.limit(1)
            )
        )
        return docs.rows.firstOrNull()
    }

    override fun observeGamesByStatus(userId: String, status: String): Flow<List<Row<Map<String, Any>>>> = callbackFlow {
        // Initial fetch
        val fetch = suspend {
             tablesDb.listRows(
                databaseId = DATABASE_ID,
                tableId = GAMES_COLLECTION_ID,
                queries = listOf(
                    Query.equal(FIELD_USER_ID, userId),
                    Query.equal(FIELD_STATUS, status),
                    Query.limit(1000)
                )
            ).rows
        }
        
        try {
            trySend(fetch())
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching games by status: ${e.message}", e)
        }

        // We subscribe to all updates for this user, but re-fetch the filtered list
        // because an update might change the status field, moving it in/out of view.
        val subscription = realtime.subscribe("databases.$DATABASE_ID.tables.$GAMES_COLLECTION_ID.rows") { response ->
            val payload = response.payload as? Map<String, Any?>
            val docUserId = payload?.get(FIELD_USER_ID) as? String
            
            if (docUserId == userId) {
                launch {
                    try {
                        trySend(fetch())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error refreshing games by status: ${e.message}", e)
                    }
                }
            }
        }
        awaitClose { subscription.close() }
    }

    override fun observeGamesByGenre(userId: String, genre: String): Flow<List<Row<Map<String, Any>>>> = callbackFlow {
        val fetch = suspend {
             tablesDb.listRows(
                databaseId = DATABASE_ID,
                tableId = GAMES_COLLECTION_ID,
                queries = listOf(
                    Query.equal(FIELD_USER_ID, userId),
                    Query.contains(FIELD_GENRES, genre), // Using contains for array match
                    Query.limit(1000)
                )
            ).rows
        }
        
        try {
            trySend(fetch())
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching games by genre: ${e.message}", e)
        }

        val subscription = realtime.subscribe("databases.$DATABASE_ID.tables.$GAMES_COLLECTION_ID.rows") { response ->
            val payload = response.payload as? Map<String, Any?>
            val docUserId = payload?.get(FIELD_USER_ID) as? String
            
            if (docUserId == userId) {
                launch {
                    try {
                        trySend(fetch())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error refreshing games by genre: ${e.message}", e)
                    }
                }
            }
        }
        awaitClose { subscription.close() }
    }

    override fun observeGame(gameId: String): Flow<Row<Map<String, Any>>> = callbackFlow {
        try {
            trySend(getGame(gameId))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching game: ${e.message}", e)
        }

        val subscription = realtime.subscribe("databases.$DATABASE_ID.tables.$GAMES_COLLECTION_ID.rows.$gameId") {
            launch {
                try {
                    trySend(getGame(gameId))
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing game: ${e.message}", e)
                }
            }
        }
        awaitClose { subscription.close() }
    }
}