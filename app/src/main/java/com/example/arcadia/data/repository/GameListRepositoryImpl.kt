package com.example.arcadia.data.repository

import android.util.Log
import com.example.arcadia.BuildConfig
import com.example.arcadia.data.remote.mapper.toDto
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.SortOrder
import com.example.arcadia.util.AppwriteConstants
import com.example.arcadia.util.RequestState
import com.google.firebase.auth.FirebaseAuth
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.TablesDB
import io.appwrite.services.Realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Implementation of GameListRepository using Appwrite Database and Realtime.
 * Retains Firebase Authentication for user identity management.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 8.4, 9.1, 9.2, 10.2, 11.2
 */
class GameListRepositoryImpl(
    private val tablesDb: TablesDB,
    private val realtime: Realtime
) : GameListRepository {

    companion object {
        private const val TAG = "GameListRepository"
        private val databaseId = BuildConfig.APPWRITE_DATABASE_ID
        private val gamesCollectionId = AppwriteConstants.GAMES_COLLECTION_ID

        // Field names for Appwrite queries
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_ADDED_AT = "addedAt"
        private const val FIELD_STATUS = "status"
        private const val FIELD_GENRES = "genres"
        private const val FIELD_RAWG_ID = "rawgId"

        private const val DEFAULT_FUTURE_DATE = "9999-12-31"
        private const val MIN_RATING = 0f
        private const val MAX_RATING = 10f

        // Keep this low; Appwrite can throttle bursts of writes (esp. reorder).
        private const val IMPORTANCE_UPDATE_MAX_RETRIES = 3
        private val IMPORTANCE_UPDATE_RETRY_CODES = setOf(429, 500, 502, 503, 504)
    }

    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String? = auth.currentUser?.uid

    /** Normalize ratings to one decimal bucket to match legacy ordering rules */
    private fun Float.toOneDecimalBucket(): Float = (this * 10).toInt() / 10f

    private suspend fun updateImportanceWithRetry(
        entryId: String,
        importance: Int,
        updatedAt: Long,
        attempt: Int = 0
    ) {
        try {
            tablesDb.updateRow(
                databaseId = databaseId,
                tableId = gamesCollectionId,
                rowId = entryId,
                data = mapOf(
                    // Send as Long to match Appwrite numeric attribute expectations.
                    "importance" to importance.toLong(),
                    "updatedAt" to updatedAt
                )
            )
        } catch (e: AppwriteException) {
            val shouldRetry = e.code in IMPORTANCE_UPDATE_RETRY_CODES && attempt < IMPORTANCE_UPDATE_MAX_RETRIES
            if (!shouldRetry) throw e

            // Exponential backoff: 250ms, 500ms, 1000ms
            val backoffMs = 250L shl attempt
            kotlinx.coroutines.delay(backoffMs)
            updateImportanceWithRetry(entryId, importance, updatedAt, attempt + 1)
        }
    }

    /**
     * Extension function to apply client-side sorting to game list entries.
     * Extracted to avoid code duplication across multiple query methods.
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

    /**
     * Parses Appwrite document data into a GameListEntry domain model.
     * Handles missing or null fields gracefully with default values.
     *
     * Requirements: 9.3
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseGameEntryFromDocument(
        data: Map<String, Any?>,
        documentId: String
    ): GameListEntry {
        return GameListEntry(
            id = documentId,
            rawgId = data["rawgId"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0,
            name = data["name"] as? String ?: "",
            backgroundImage = data["backgroundImage"] as? String,
            genres = (data["genres"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            platforms = (data["platforms"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            developers = (data["developers"] as? List<*>)?.filterIsInstance<String>()
                ?: emptyList(),
            publishers = (data["publishers"] as? List<*>)?.filterIsInstance<String>()
                ?: emptyList(),
            addedAt = (data["addedAt"] as? Number)?.toLong() ?: 0L,
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L,
            status = GameStatus.fromString(data["status"] as? String ?: "WANT"),
            rating = (data["rating"] as? Number)?.toFloat()?.toOneDecimalBucket(),
            review = data["review"] as? String ?: "",
            hoursPlayed = (data["hoursPlayed"] as? Number)?.toInt() ?: 0,
            aspects = (data["aspects"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            releaseDate = data["releaseDate"] as? String,
            importance = (data["importance"] as? Number)?.toInt() ?: 0
        )
    }

    /**
     * Converts a GameListEntry to a map for Appwrite document creation/update.
     * Includes userId field for ownership tracking.
     *
     * Requirements: 4.1, 11.2
     */
    private fun GameListEntry.toAppwriteData(userId: String): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "rawgId" to rawgId,
            "name" to name,
            "backgroundImage" to backgroundImage,
            "genres" to genres,
            "platforms" to platforms,
            "developers" to developers,
            "publishers" to publishers,
            "addedAt" to addedAt,
            "updatedAt" to updatedAt,
            "status" to status.name,
            // Store rating in the same 1-decimal bucket used for sorting
            "rating" to rating?.toOneDecimalBucket(),
            "review" to review,
            "hoursPlayed" to hoursPlayed,
            "aspects" to aspects,
            "releaseDate" to releaseDate,
            "importance" to importance
        )
    }


        /**


         * Gets the user's game list with real-time updates using Appwrite Realtime.


         * 


         * Requirements: 4.2, 4.3, 8.4


         */


        override fun getGameList(sortOrder: SortOrder): Flow<RequestState<List<GameListEntry>>> = callbackFlow {


            try {


                val userId = getCurrentUserId()


                if (userId == null) {


                    send(RequestState.Error("User not authenticated"))


                    close()


                    return@callbackFlow


                }


                


                send(RequestState.Loading)


                


                // Local cache of the list


                var currentList = listOf<GameListEntry>()


                


                // Initial fetch


                try {


                    val response = tablesDb.listRows(


                        databaseId = databaseId,


                        tableId = gamesCollectionId,


                        queries = listOf(


                            Query.equal(FIELD_USER_ID, userId),


                            Query.limit(1000)


                        )


                    )


                    


                    val games = response.rows.map { doc ->


                        parseGameEntryFromDocument(doc.data, doc.id)


                    }


                    currentList = games.applySorting(sortOrder)


                    trySend(RequestState.Success(currentList))


                } catch (e: AppwriteException) {


                    Log.e(TAG, "Error fetching game list: ${e.message}", e)


                    trySend(RequestState.Error("Failed to fetch games: ${e.message}"))


                }


                


                // Subscribe to collection changes for this user's games


                val channel = "databases.$databaseId.tables.$gamesCollectionId.rows"


                val subscription = realtime.subscribe(channel) { response ->


                    try {


                        @Suppress("UNCHECKED_CAST")


                        val payload = response.payload as? Map<String, Any?>


                        val docUserId = payload?.get(FIELD_USER_ID) as? String


                        


                        // Only process updates for current user's games


                        if (docUserId == userId && payload != null) {


                            // Use this@callbackFlow to ensure correct scope resolution


                            this@callbackFlow.launch(kotlinx.coroutines.Dispatchers.IO) {


                                val docId = payload["\$id"] as? String


                            


                                if (docId != null) {


                                    val eventType = response.events.firstOrNull() ?: ""


                                    val entry = parseGameEntryFromDocument(payload, docId)


                                    


                                    // Update local list based on event type


                                    currentList = when {


                                        eventType.contains(".create") -> currentList + entry


                                        eventType.contains(".update") -> currentList.map { if (it.id == entry.id) entry else it }


                                        eventType.contains(".delete") -> currentList.filter { it.id != entry.id }


                                        else -> currentList


                                    }


                                    


                                    // Re-sort and emit


                                    val sorted = currentList.applySorting(sortOrder)


                                    trySend(RequestState.Success(sorted))


                                }


                            }


                        }


                    } catch (e: Exception) {


                        Log.e(TAG, "Error processing realtime update: ${e.message}", e)


                    }


                }


                


                awaitClose { 


                    subscription.close()


                    Log.d(TAG, "Game list listener removed")


                }


                


            } catch (e: CancellationException) {


                Log.d(TAG, "getGameList flow cancelled")


                throw e


            } catch (e: Exception) {


                if (e.toString().contains("AbortFlowException")) {


                    Log.d(TAG, "getGameList flow aborted (expected)")


                } else {


                    Log.e(TAG, "Error in getGameList: ${e.message}", e)


                }


                close()


            }


        }


            /**
             * Gets a specific user's game list (for viewing friend profiles).
             *
             * Requirements: 4.2
             */
            override fun getGameListForUser(
                userId: String,
                sortOrder: SortOrder
            ): Flow<RequestState<List<GameListEntry>>> = callbackFlow {
                try {
                    send(RequestState.Loading)

                    // Initial fetch
                    try {
                        val response = tablesDb.listRows(
                            databaseId = databaseId,
                            tableId = gamesCollectionId,
                            queries = listOf(
                                Query.equal(FIELD_USER_ID, userId),
                                Query.limit(1000)
                            )
                        )

                        val games = response.rows.map { doc ->
                            parseGameEntryFromDocument(doc.data, doc.id)
                        }
                        val sortedGames = games.applySorting(sortOrder)
                        trySend(RequestState.Success(sortedGames))
                    } catch (e: AppwriteException) {
                        Log.e(TAG, "Error fetching game list for user $userId: ${e.message}", e)
                        trySend(RequestState.Error("Failed to fetch games: ${e.message}"))
                    }

                    // Subscribe to collection changes for this user's games
                    val channel = "databases.$databaseId.tables.$gamesCollectionId.rows"
                    val subscription = realtime.subscribe(channel) { response ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val payload = response.payload as? Map<String, Any?>
                            val docUserId = payload?.get(FIELD_USER_ID) as? String

                            // Only process updates for the specified user's games
                            if (docUserId == userId) {
                                this@callbackFlow.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val refreshResponse = tablesDb.listRows(
                                            databaseId = databaseId,
                                            tableId = gamesCollectionId,
                                            queries = listOf(
                                                Query.equal(FIELD_USER_ID, userId),
                                                Query.limit(1000)
                                            )
                                        )

                                        val games = refreshResponse.rows.map { doc ->
                                            parseGameEntryFromDocument(doc.data, doc.id)
                                        }
                                        val sortedGames = games.applySorting(sortOrder)
                                        trySend(RequestState.Success(sortedGames))
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error refreshing game list: ${e.message}", e)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing realtime update: ${e.message}", e)
                        }
                    }

                    awaitClose {
                        subscription.close()
                        Log.d(TAG, "Game list listener removed for user $userId")
                    }

                } catch (e: CancellationException) {
                    Log.d(TAG, "getGameListForUser flow cancelled")
                    throw e
                } catch (e: Exception) {
                    if (e.toString().contains("AbortFlowException")) {
                        Log.d(TAG, "getGameListForUser flow aborted (expected)")
                    } else {
                        Log.e(TAG, "Error in getGameListForUser: ${e.message}", e)
                    }
                    close()
                }
            }


            /**
             * Gets games filtered by status with real-time updates.
             * Uses Query.equal() for both userId and status fields.
             *
             * Requirements: 4.4
             */
            override fun getGameListByStatus(
                status: GameStatus,
                sortOrder: SortOrder
            ): Flow<RequestState<List<GameListEntry>>> = callbackFlow {
                try {
                    val userId = getCurrentUserId()
                    if (userId == null) {
                        send(RequestState.Error("User not authenticated"))
                        close()
                        return@callbackFlow
                    }

                    send(RequestState.Loading)

                    // Initial fetch with status filter
                    try {
                        val response = tablesDb.listRows(
                            databaseId = databaseId,
                            tableId = gamesCollectionId,
                            queries = listOf(
                                Query.equal(FIELD_USER_ID, userId),
                                Query.equal(FIELD_STATUS, status.name),
                                Query.limit(1000)
                            )
                        )

                        val games = response.rows.map { doc ->
                            parseGameEntryFromDocument(doc.data, doc.id)
                        }
                        val sortedGames = games.applySorting(sortOrder)
                        trySend(RequestState.Success(sortedGames))
                    } catch (e: AppwriteException) {
                        Log.e(TAG, "Error fetching games by status: ${e.message}", e)
                        trySend(RequestState.Error("Failed to fetch games: ${e.message}"))
                    }

                    // Subscribe to collection changes
                    val channel = "databases.$databaseId.tables.$gamesCollectionId.rows"
                    val subscription = realtime.subscribe(channel) { response ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val payload = response.payload as? Map<String, Any?>
                            val docUserId = payload?.get(FIELD_USER_ID) as? String

                            if (docUserId == userId) {
                                this@callbackFlow.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val refreshResponse = tablesDb.listRows(
                                            databaseId = databaseId,
                                            tableId = gamesCollectionId,
                                            queries = listOf(
                                                Query.equal(FIELD_USER_ID, userId),
                                                Query.equal(FIELD_STATUS, status.name),
                                                Query.limit(1000)
                                            )
                                        )

                                        val games = refreshResponse.rows.map { doc ->
                                            parseGameEntryFromDocument(doc.data, doc.id)
                                        }
                                        val sortedGames = games.applySorting(sortOrder)
                                        trySend(RequestState.Success(sortedGames))
                                    } catch (e: Exception) {
                                        Log.e(
                                            TAG,
                                            "Error refreshing games by status: ${e.message}",
                                            e
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing realtime update: ${e.message}", e)
                        }
                    }

                    awaitClose {
                        subscription.close()
                        Log.d(TAG, "Game list by status listener removed")
                    }

                } catch (e: CancellationException) {
                    Log.d(TAG, "getGameListByStatus flow cancelled")
                    throw e
                } catch (e: Exception) {
                    if (e.toString().contains("AbortFlowException")) {
                        Log.d(TAG, "getGameListByStatus flow aborted (expected)")
                    } else {
                        Log.e(TAG, "Error in getGameListByStatus: ${e.message}", e)
                    }
                    close()
                }
            }


            /**
             * Gets games filtered by genre with real-time updates.
             * Uses Query.contains() for genres array with userId filter.
             *
             * Requirements: 4.5
             */
            override fun getGameListByGenre(
                genre: String,
                sortOrder: SortOrder
            ): Flow<RequestState<List<GameListEntry>>> = callbackFlow {
                try {
                    val userId = getCurrentUserId()
                    if (userId == null) {
                        send(RequestState.Error("User not authenticated"))
                        close()
                        return@callbackFlow
                    }

                    send(RequestState.Loading)

                    // Initial fetch with genre filter
                    try {
                        val response = tablesDb.listRows(
                            databaseId = databaseId,
                            tableId = gamesCollectionId,
                            queries = listOf(
                                Query.equal(FIELD_USER_ID, userId),
                                Query.contains(FIELD_GENRES, genre),
                                Query.limit(1000)
                            )
                        )

                        val games = response.rows.map { doc ->
                            parseGameEntryFromDocument(doc.data, doc.id)
                        }
                        val sortedGames = games.applySorting(sortOrder)
                        trySend(RequestState.Success(sortedGames))
                    } catch (e: AppwriteException) {
                        Log.e(TAG, "Error fetching games by genre: ${e.message}", e)
                        trySend(RequestState.Error("Failed to fetch games: ${e.message}"))
                    }

                    // Subscribe to collection changes
                    val channel = "databases.$databaseId.tables.$gamesCollectionId.rows"
                    val subscription = realtime.subscribe(channel) { response ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val payload = response.payload as? Map<String, Any?>
                            val docUserId = payload?.get(FIELD_USER_ID) as? String

                            if (docUserId == userId) {
                                this@callbackFlow.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val refreshResponse = tablesDb.listRows(
                                            databaseId = databaseId,
                                            tableId = gamesCollectionId,
                                            queries = listOf(
                                                Query.equal(FIELD_USER_ID, userId),
                                                Query.contains(FIELD_GENRES, genre),
                                                Query.limit(1000)
                                            )
                                        )

                                        val games = refreshResponse.rows.map { doc ->
                                            parseGameEntryFromDocument(doc.data, doc.id)
                                        }
                                        val sortedGames = games.applySorting(sortOrder)
                                        trySend(RequestState.Success(sortedGames))
                                    } catch (e: Exception) {
                                        Log.e(
                                            TAG,
                                            "Error refreshing games by genre: ${e.message}",
                                            e
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing realtime update: ${e.message}", e)
                        }
                    }

                    awaitClose {
                        subscription.close()
                        Log.d(TAG, "Game list by genre listener removed")
                    }

                } catch (e: CancellationException) {
                    Log.d(TAG, "getGameListByGenre flow cancelled")
                    throw e
                } catch (e: Exception) {
                    if (e.toString().contains("AbortFlowException")) {
                        Log.d(TAG, "getGameListByGenre flow aborted (expected)")
                    } else {
                        Log.e(TAG, "Error in getGameListByGenre: ${e.message}", e)
                    }
                    close()
                }
            }


            /**
             * Gets a single game entry with real-time updates.
             */
            override fun getGameEntry(entryId: String): Flow<RequestState<GameListEntry>> =
                callbackFlow {
                    try {
                        val userId = getCurrentUserId()
                        if (userId == null) {
                            send(RequestState.Error("User not authenticated"))
                            close()
                            return@callbackFlow
                        }

                        send(RequestState.Loading)

                        // Initial fetch
                        try {
                            val document = tablesDb.getRow(
                                databaseId = databaseId,
                                tableId = gamesCollectionId,
                                rowId = entryId
                            )

                            val entry = parseGameEntryFromDocument(document.data, document.id)
                            trySend(RequestState.Success(entry))
                        } catch (e: AppwriteException) {
                            if (e.code == 404) {
                                trySend(RequestState.Error("Game entry not found"))
                            } else {
                                Log.e(TAG, "Error fetching game entry: ${e.message}", e)
                                trySend(RequestState.Error("Failed to fetch game: ${e.message}"))
                            }
                        }

                        // Subscribe to document changes
                        val channel =
                            "databases.$databaseId.tables.$gamesCollectionId.rows.$entryId"
                        val subscription = realtime.subscribe(channel) { response ->
                            try {
                                @Suppress("UNCHECKED_CAST")
                                val payload = response.payload as? Map<String, Any?>
                                if (payload != null && payload.isNotEmpty()) {
                                    val entry = parseGameEntryFromDocument(payload, entryId)
                                    trySend(RequestState.Success(entry))
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing realtime update: ${e.message}", e)
                                trySend(RequestState.Error("Failed to parse game entry"))
                            }
                        }

                        awaitClose {
                            subscription.close()
                            Log.d(TAG, "Game entry listener removed")
                        }

                    } catch (e: CancellationException) {
                        Log.d(TAG, "getGameEntry flow cancelled")
                        throw e
                    } catch (e: Exception) {
                        if (e.toString().contains("AbortFlowException")) {
                            Log.d(TAG, "getGameEntry flow aborted (expected)")
                        } else {
                            Log.e(TAG, "Error in getGameEntry: ${e.message}", e)
                        }
                        close()
                    }
                }


            /**
             * Adds a game to the user's library.
             * Creates a document in Appwrite with userId field for ownership tracking.
             * Uses ID.unique() for auto-generated document IDs.
             *
             * Requirements: 4.1, 2.8, 11.2
             */
            override suspend fun addGameToList(
                game: Game,
                status: GameStatus
            ): RequestState<String> {
                return try {
                    val userId = getCurrentUserId()
                        ?: return RequestState.Error("User not authenticated")

                    // Check if game already exists
                    if (getEntryIdByRawgId(game.id) != null) {
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

                    val document = tablesDb.createRow(
                        databaseId = databaseId,
                        tableId = gamesCollectionId,
                        rowId = ID.unique(),
                        data = entry.toAppwriteData(userId)
                    )

                    Log.d(TAG, "Game added to list successfully: ${game.name}")
                    RequestState.Success(document.id)

                } catch (e: AppwriteException) {
                    Log.e(TAG, "Appwrite error adding game to list: ${e.message}", e)
                    RequestState.Error("Failed to add game: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding game to list: ${e.message}", e)
                    RequestState.Error("Failed to add game: ${e.message}")
                }
            }

            /**
             * Adds a GameListEntry directly to the user's library.
             *
             * Requirements: 4.1, 2.8, 11.2
             */
            override suspend fun addGameListEntry(entry: GameListEntry): RequestState<String> {
                return try {
                    val userId = getCurrentUserId()
                        ?: return RequestState.Error("User not authenticated")

                    // Check if game already exists
                    if (getEntryIdByRawgId(entry.rawgId) != null) {
                        return RequestState.Error("Game already in list")
                    }

                    val document = tablesDb.createRow(
                        databaseId = databaseId,
                        tableId = gamesCollectionId,
                        rowId = ID.unique(),
                        data = entry.toAppwriteData(userId)
                    )

                    Log.d(TAG, "Game added to list successfully: ${entry.name}")
                    RequestState.Success(document.id)

                } catch (e: AppwriteException) {
                    Log.e(TAG, "Appwrite error adding game to list: ${e.message}", e)
                    RequestState.Error("Failed to add game: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding game to list: ${e.message}", e)
                    RequestState.Error("Failed to add game: ${e.message}")
                }
            }


            /**
             * Updates the status of a game entry.
             *
             * Requirements: 4.6
             */
            override suspend fun updateGameStatus(
                entryId: String,
                status: GameStatus
            ): RequestState<Unit> {
                return try {
                    val userId = getCurrentUserId()
                        ?: return RequestState.Error("User not authenticated")

                    tablesDb.updateRow(
                        databaseId = databaseId,
                        tableId = gamesCollectionId,
                        rowId = entryId,
                        data = mapOf(
                            FIELD_STATUS to status.name,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )

                    Log.d(TAG, "Game status updated successfully")
                    RequestState.Success(Unit)

                } catch (e: AppwriteException) {
                    Log.e(TAG, "Appwrite error updating game status: ${e.message}", e)
                    RequestState.Error("Failed to update status: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating game status: ${e.message}", e)
                    RequestState.Error("Failed to update status: ${e.message}")
                }
            }

            /**
             * Updates the rating of a game entry.
             *
             * Requirements: 4.6
             */
            override suspend fun updateGameRating(
                entryId: String,
                rating: Float?
            ): RequestState<Unit> {
                return try {
                    val userId = getCurrentUserId()
                        ?: return RequestState.Error("User not authenticated")

                    // Validate rating
                    if (rating != null && (rating < MIN_RATING || rating > MAX_RATING)) {
                        return RequestState.Error("Rating must be between $MIN_RATING and $MAX_RATING")
                    }

                    // Round to one decimal place
                    val roundedRating = rating?.let {
                        (it * 10).toInt() / 10f
                    }

                    tablesDb.updateRow(
                        databaseId = databaseId,
                        tableId = gamesCollectionId,
                        rowId = entryId,
                        data = mapOf(
                            "rating" to roundedRating,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )

                    Log.d(TAG, "Game rating updated successfully")
                    RequestState.Success(Unit)

                } catch (e: AppwriteException) {
                    Log.e(TAG, "Appwrite error updating game rating: ${e.message}", e)
                    RequestState.Error("Failed to update rating: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating game rating: ${e.message}", e)
                    RequestState.Error("Failed to update rating: ${e.message}")
                }
            }

            /**
             * Updates the review of a game entry.
             *
             * Requirements: 4.6
             */
            override suspend fun updateGameReview(
                entryId: String,
                review: String
            ): RequestState<Unit> {
                return try {
                    val userId = getCurrentUserId()
                        ?: return RequestState.Error("User not authenticated")

                    tablesDb.updateRow(
                        databaseId = databaseId,
                        tableId = gamesCollectionId,
                        rowId = entryId,
                        data = mapOf(
                            "review" to review,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )

                    Log.d(TAG, "Game review updated successfully")
                    RequestState.Success(Unit)

                } catch (e: AppwriteException) {
                    Log.e(TAG, "Appwrite error updating game review: ${e.message}", e)
                    RequestState.Error("Failed to update review: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating game review: ${e.message}", e)
                    RequestState.Error("Failed to update review: ${e.message}")
                }
            }

            /**
             * Updates the hours played for a game entry.
             *
             * Requirements: 4.6
             */
            override suspend fun updateHoursPlayed(
                entryId: String,
                hours: Int
            ): RequestState<Unit> {
                return try {
                    val userId = getCurrentUserId()
                        ?: return RequestState.Error("User not authenticated")

                    if (hours < 0) {
                        return RequestState.Error("Hours played cannot be negative")
                    }

                    tablesDb.updateRow(
                        databaseId = databaseId,
                        tableId = gamesCollectionId,
                        rowId = entryId,
                        data = mapOf(
                            "hoursPlayed" to hours,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )

                    Log.d(TAG, "Hours played updated successfully")
                    RequestState.Success(Unit)

                } catch (e: AppwriteException) {
                    Log.e(TAG, "Appwrite error updating hours played: ${e.message}", e)
                    RequestState.Error("Failed to update hours: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating hours played: ${e.message}", e)
                    RequestState.Error("Failed to update hours: ${e.message}")
                }
            }


            /**
             * Updates an entire game entry.
             *
             * Requirements: 4.6
             */
            override suspend fun updateGameEntry(entry: GameListEntry): RequestState<Unit> {
                return try {
                    val userId = getCurrentUserId()
                        ?: return RequestState.Error("User not authenticated")

                    // Validate rating
                    if (entry.rating != null && (entry.rating < MIN_RATING || entry.rating > MAX_RATING)) {
                        return RequestState.Error("Rating must be between $MIN_RATING and $MAX_RATING")
                    }

                    // Update the updatedAt timestamp
                    val updatedEntry = entry.copy(updatedAt = System.currentTimeMillis())

                    tablesDb.updateRow(
                        databaseId = databaseId,
                        tableId = gamesCollectionId,
                        rowId = entry.id,
                        data = updatedEntry.toAppwriteData(userId)
                    )

                    Log.d(TAG, "Game entry updated successfully")
                    RequestState.Success(Unit)

                } catch (e: AppwriteException) {
                    Log.e(TAG, "Appwrite error updating game entry: ${e.message}", e)
                    RequestState.Error("Failed to update game: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating game entry: ${e.message}", e)
                    RequestState.Error("Failed to update game: ${e.message}")
                }
            }

            /**
             * Updates the importance values for multiple games at once.
             * Uses parallel updates with chunking to improve performance while respecting API limits.
             *
             * Requirements: 4.9
             */
            override suspend fun updateGamesImportance(updates: Map<String, Int>): RequestState<Unit> = kotlinx.coroutines.coroutineScope {
                return@coroutineScope try {
                    if (getCurrentUserId() == null) {
                        return@coroutineScope RequestState.Error("User not authenticated")
                    }

                    if (updates.isEmpty()) {
                        return@coroutineScope RequestState.Success(Unit)
                    }

                    // Keep a single timestamp like the Firestore batch path (conceptually one operation).
                    val updatedAt = System.currentTimeMillis()

                    // Parallelize updates in chunks to avoid rate limits while improving speed.
                    // Appwrite might throttle if we send too many concurrent requests.
                    // Chunk size of 5 gives a good balance.
                    val chunks = updates.entries.chunked(5)
                    
                    val deferreds = chunks.map { chunk ->
                        async {
                            chunk.forEach { (entryId, importance) ->
                                updateImportanceWithRetry(entryId = entryId, importance = importance, updatedAt = updatedAt)
                            }
                        }
                    }
                    
                    // Wait for all chunks to complete
                    deferreds.awaitAll()

                    Log.d(TAG, "Updated importance for ${updates.size} games")
                    RequestState.Success(Unit)

                } catch (e: CancellationException) {
                    // Important: cancellation is normal control flow (e.g., user drags again quickly).
                    // Don't convert it into an error state; just propagate.
                    throw e
                } catch (e: AppwriteException) {
                    Log.e(TAG, "Appwrite error updating games importance: ${e.message}", e)
                    RequestState.Error("Failed to update game order: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating games importance: ${e.message}", e)
                    RequestState.Error("Failed to update game order: ${e.message}")
                }
            }


            /**
             * Removes a game from the user's library.
             *
             * Requirements: 4.7
             */
            override suspend fun removeGameFromList(entryId: String): RequestState<Unit> {
                return try {
                    val userId = getCurrentUserId()
                        ?: return RequestState.Error("User not authenticated")

                    tablesDb.deleteRow(
                        databaseId = databaseId,
                        tableId = gamesCollectionId,
                        rowId = entryId
                    )

                    Log.d(TAG, "Game removed from list successfully")
                    RequestState.Success(Unit)

                } catch (e: AppwriteException) {
                    Log.e(TAG, "Appwrite error removing game from list: ${e.message}", e)
                    RequestState.Error("Failed to remove game: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing game from list: ${e.message}", e)
                    RequestState.Error("Failed to remove game: ${e.message}")
                }
            }

            /**
             * Checks if a game is already in the user's library.
             * Uses Query.equal() for userId and rawgId fields.
             *
             * Requirements: 4.8
             */
            override suspend fun isGameInList(rawgId: Int): Boolean {
                return try {
                    val userId = getCurrentUserId() ?: return false

                    val response = tablesDb.listRows(
                        databaseId = databaseId,
                        tableId = gamesCollectionId,
                        queries = listOf(
                            Query.equal(FIELD_USER_ID, userId),
                            Query.equal(FIELD_RAWG_ID, rawgId),
                            Query.limit(1)
                        )
                    )

                    response.rows.isNotEmpty()

                } catch (e: AppwriteException) {
                    Log.e(TAG, "Appwrite error checking if game is in list: ${e.message}", e)
                    false
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking if game is in list: ${e.message}", e)
                    false
                }
            }

            /**
             * Gets the document ID for a game by its RAWG ID.
             * Uses Query.equal() for userId and rawgId fields.
             *
             * Requirements: 4.8
             */
            override suspend fun getEntryIdByRawgId(rawgId: Int): String? {
                return try {
                    val userId = getCurrentUserId() ?: return null

                    val response = tablesDb.listRows(
                        databaseId = databaseId,
                        tableId = gamesCollectionId,
                        queries = listOf(
                            Query.equal(FIELD_USER_ID, userId),
                            Query.equal(FIELD_RAWG_ID, rawgId),
                            Query.limit(1)
                        )
                    )

                    response.rows.firstOrNull()?.id

                } catch (e: AppwriteException) {
                    Log.e(TAG, "Appwrite error getting entry ID by RAWG ID: ${e.message}", e)
                    null
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting entry ID by RAWG ID: ${e.message}", e)
                    null
                }
            }


            /**
             * Migrates existing library entries to include developer/publisher data.
             * Only updates entries that are missing developers AND publishers.
             */
            override suspend fun migrateLibraryWithDevPub(
                fetchGameDetails: suspend (Int) -> Game?
            ): RequestState<Int> {
                return try {
                    val userId = getCurrentUserId()
                        ?: return RequestState.Error("User not authenticated")

                    // Get all entries for this user
                    val response = tablesDb.listRows(
                        databaseId = databaseId,
                        tableId = gamesCollectionId,
                        queries = listOf(
                            Query.equal(FIELD_USER_ID, userId),
                            Query.limit(1000)
                        )
                    )

                    var updatedCount = 0

                    for (doc in response.rows) {
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val developers =
                                (doc.data["developers"] as? List<*>)?.filterIsInstance<String>()
                                    ?: emptyList()

                            @Suppress("UNCHECKED_CAST")
                            val publishers =
                                (doc.data["publishers"] as? List<*>)?.filterIsInstance<String>()
                                    ?: emptyList()
                            val rawgId = (doc.data["rawgId"] as? Number)?.toInt() ?: continue
                            val name = doc.data["name"] as? String ?: "Unknown"

                            // Skip if already has dev/pub data
                            if (developers.isNotEmpty() || publishers.isNotEmpty()) {
                                continue
                            }

                            // Fetch game details from RAWG
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

                                tablesDb.updateRow(
                                    databaseId = databaseId,
                                    tableId = gamesCollectionId,
                                    rowId = doc.id,
                                    data = updates
                                )
                                updatedCount++
                                Log.d(
                                    TAG,
                                    "Migrated $name: devs=${game.developers}, pubs=${game.publishers}"
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to migrate entry ${doc.id}: ${e.message}")
                            // Continue with next entry
                        }
                    }

                    Log.d(TAG, "Migration complete. Updated $updatedCount entries.")
                    RequestState.Success(updatedCount)

                } catch (e: AppwriteException) {
                    Log.e(TAG, "Appwrite error during migration: ${e.message}", e)
                    RequestState.Error("Migration failed: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during migration: ${e.message}", e)
                    RequestState.Error("Migration failed: ${e.message}")
                }
            }
        }

