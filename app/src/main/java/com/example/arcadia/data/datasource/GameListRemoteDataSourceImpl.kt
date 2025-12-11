package com.example.arcadia.data.datasource

import android.util.Log
import com.example.arcadia.BuildConfig
import com.example.arcadia.util.AppwriteConstants
import io.appwrite.ID
import io.appwrite.Query
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
    }

    override fun observeGames(userId: String): Flow<List<Row<Map<String, Any>>>> = callbackFlow {
        try {
            trySend(fetchGames(userId))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching initial games: ${e.message}", e)
        }

        val subscription = realtime.subscribe(
            "databases.$DATABASE_ID.tables.$GAMES_COLLECTION_ID.rows"
        ) { response ->
            val payload = response.payload as? Map<String, Any?>
            val docUserId = payload?.get(FIELD_USER_ID) as? String
            
            if (docUserId == userId) {
                launch {
                    try {
                        trySend(fetchGames(userId))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error refreshing games: ${e.message}", e)
                    }
                }
            }
        }

        awaitClose { subscription.close() }
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
                Query.equal(FIELD_RAWG_ID, rawgId),
                Query.limit(1)
            )
        )
        return docs.rows.firstOrNull()
    }

    override fun observeGamesByStatus(userId: String, status: String): Flow<List<Row<Map<String, Any>>>> = callbackFlow {
        val queries = listOf(
            Query.equal(FIELD_USER_ID, userId),
            Query.equal("status", status),
            Query.limit(1000)
        )
        
        try {
            trySend(tablesDb.listRows(DATABASE_ID, GAMES_COLLECTION_ID, queries).rows)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching games by status: ${e.message}", e)
        }

        val subscription = realtime.subscribe("databases.$DATABASE_ID.tables.$GAMES_COLLECTION_ID.rows") {
            launch {
                try {
                    trySend(tablesDb.listRows(DATABASE_ID, GAMES_COLLECTION_ID, queries).rows)
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing games by status: ${e.message}", e)
                }
            }
        }
        awaitClose { subscription.close() }
    }

    override fun observeGamesByGenre(userId: String, genre: String): Flow<List<Row<Map<String, Any>>>> = callbackFlow {
        val queries = listOf(
            Query.equal(FIELD_USER_ID, userId),
            Query.search("genres", genre), // Assuming genres is searchable or array contains
            Query.limit(1000)
        )
        
        try {
            trySend(tablesDb.listRows(DATABASE_ID, GAMES_COLLECTION_ID, queries).rows)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching games by genre: ${e.message}", e)
        }

        val subscription = realtime.subscribe("databases.$DATABASE_ID.tables.$GAMES_COLLECTION_ID.rows") {
            launch {
                try {
                    trySend(tablesDb.listRows(DATABASE_ID, GAMES_COLLECTION_ID, queries).rows)
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing games by genre: ${e.message}", e)
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
