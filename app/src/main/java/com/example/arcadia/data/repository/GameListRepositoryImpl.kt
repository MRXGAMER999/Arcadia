package com.example.arcadia.data.repository

import android.util.Log
import com.example.arcadia.data.remote.dto.GameListEntryDto
import com.example.arcadia.data.remote.mapper.toDto
import com.example.arcadia.data.remote.mapper.toGameListEntry
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.SortOrder
import com.example.arcadia.util.RequestState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GameListRepositoryImpl : GameListRepository {
    
    companion object {
        private const val TAG = "GameListRepository"
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_GAME_LIST = "gameList"
        private const val FIELD_ADDED_AT = "addedAt"
        private const val FIELD_STATUS = "status"
        private const val FIELD_GENRES = "genres"
        private const val FIELD_RAWG_ID = "rawgId"
        private const val DEFAULT_FUTURE_DATE = "9999-12-31"
        private const val MIN_RATING = 0f
        private const val MAX_RATING = 10f
    }
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    /**
     * Extension function to apply client-side sorting to game list entries.
     * Extracted to avoid code duplication across multiple query methods.
     */
    private fun List<GameListEntry>.applySorting(sortOrder: SortOrder): List<GameListEntry> = 
        when (sortOrder) {
            SortOrder.TITLE_A_Z -> sortedBy { it.name.lowercase() }
            SortOrder.TITLE_Z_A -> sortedByDescending { it.name.lowercase() }
            SortOrder.RATING_HIGH -> sortedByDescending { it.rating ?: -1f }
            SortOrder.RATING_LOW -> sortedBy { it.rating ?: Float.MAX_VALUE }
            SortOrder.RELEASE_NEW -> sortedByDescending { it.releaseDate ?: "" }
            SortOrder.RELEASE_OLD -> sortedBy { it.releaseDate ?: DEFAULT_FUTURE_DATE }
            else -> this // Already sorted by Firestore (NEWEST_FIRST, OLDEST_FIRST)
        }
    
    /**
     * Determines if the sort order can be handled by Firestore directly.
     */
    private fun usesFirestoreSort(sortOrder: SortOrder): Boolean = 
        sortOrder in listOf(SortOrder.NEWEST_FIRST, SortOrder.OLDEST_FIRST)
    
    /**
     * Gets the Firestore query direction for date-based sorting.
     */
    private fun getFirestoreDirection(sortOrder: SortOrder): Query.Direction = 
        when (sortOrder) {
            SortOrder.NEWEST_FIRST -> Query.Direction.DESCENDING
            SortOrder.OLDEST_FIRST -> Query.Direction.ASCENDING
            else -> Query.Direction.DESCENDING
        }
    
    /**
     * Parses Firestore documents into GameListEntry objects.
     */
    private fun parseGameEntries(documents: List<com.google.firebase.firestore.DocumentSnapshot>): List<GameListEntry> =
        documents.mapNotNull { doc ->
            try {
                doc.toObject(GameListEntryDto::class.java)?.toGameListEntry(doc.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing game entry: ${e.message}", e)
                null
            }
        }
    
    /**
     * Gets the user's game list collection reference.
     */
    private fun getUserGameListCollection(userId: String) = 
        firestore.collection(COLLECTION_USERS)
            .document(userId)
            .collection(COLLECTION_GAME_LIST)
    
    override fun getGameList(sortOrder: SortOrder): Flow<RequestState<List<GameListEntry>>> = callbackFlow {
        var listenerRegistration: ListenerRegistration? = null
        
        try {
            val userId = getCurrentUserId()
            if (userId == null) {
                send(RequestState.Error("User not authenticated"))
                close()
                return@callbackFlow
            }
            
            send(RequestState.Loading)
            
            val useFirestore = usesFirestoreSort(sortOrder)
            val direction = getFirestoreDirection(sortOrder)
            
            listenerRegistration = getUserGameListCollection(userId)
                .let { query ->
                    if (useFirestore) query.orderBy(FIELD_ADDED_AT, direction) else query
                }
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error fetching game list: ${error.message}", error)
                        trySend(RequestState.Error("Failed to fetch games: ${error.message}"))
                        return@addSnapshotListener
                    }
                    
                    val games = snapshot?.documents?.let { parseGameEntries(it) } ?: emptyList()
                    val sortedGames = games.applySorting(sortOrder)
                    trySend(RequestState.Success(sortedGames))
                }
            
            awaitClose { 
                listenerRegistration?.remove()
                Log.d(TAG, "Game list listener removed")
            }
            
        } catch (e: CancellationException) {
            // Normal flow control - don't log as error
            Log.d(TAG, "getGameList flow cancelled")
            listenerRegistration?.remove()
            throw e
        } catch (e: Exception) {
            // Check for AbortFlowException (from .first() operators)
            if (e.toString().contains("AbortFlowException")) {
                Log.d(TAG, "getGameList flow aborted (expected)")
            } else {
                Log.e(TAG, "Error in getGameList: ${e.message}", e)
            }
            listenerRegistration?.remove()
            close()
        }
    }
    
    override fun getGameListByStatus(
        status: GameStatus,
        sortOrder: SortOrder
    ): Flow<RequestState<List<GameListEntry>>> = callbackFlow {
        var listenerRegistration: ListenerRegistration? = null
        
        try {
            val userId = getCurrentUserId()
            if (userId == null) {
                send(RequestState.Error("User not authenticated"))
                close()
                return@callbackFlow
            }
            
            send(RequestState.Loading)
            
            val useFirestore = usesFirestoreSort(sortOrder)
            val direction = getFirestoreDirection(sortOrder)
            
            listenerRegistration = getUserGameListCollection(userId)
                .whereEqualTo(FIELD_STATUS, status.name)
                .let { query ->
                    if (useFirestore) query.orderBy(FIELD_ADDED_AT, direction) else query
                }
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error fetching games by status: ${error.message}", error)
                        trySend(RequestState.Error("Failed to fetch games: ${error.message}"))
                        return@addSnapshotListener
                    }
                    
                    val games = snapshot?.documents?.let { parseGameEntries(it) } ?: emptyList()
                    val sortedGames = games.applySorting(sortOrder)
                    trySend(RequestState.Success(sortedGames))
                }
            
            awaitClose { 
                listenerRegistration?.remove()
                Log.d(TAG, "Game list by status listener removed")
            }
            
        } catch (e: CancellationException) {
            Log.d(TAG, "getGameListByStatus flow cancelled")
            listenerRegistration?.remove()
            throw e
        } catch (e: Exception) {
            if (e.toString().contains("AbortFlowException")) {
                Log.d(TAG, "getGameListByStatus flow aborted (expected)")
            } else {
                Log.e(TAG, "Error in getGameListByStatus: ${e.message}", e)
            }
            listenerRegistration?.remove()
            close()
        }
    }
    
    override fun getGameListByGenre(
        genre: String,
        sortOrder: SortOrder
    ): Flow<RequestState<List<GameListEntry>>> = callbackFlow {
        var listenerRegistration: ListenerRegistration? = null
        
        try {
            val userId = getCurrentUserId()
            if (userId == null) {
                send(RequestState.Error("User not authenticated"))
                close()
                return@callbackFlow
            }
            
            send(RequestState.Loading)
            
            val useFirestore = usesFirestoreSort(sortOrder)
            val direction = getFirestoreDirection(sortOrder)
            
            listenerRegistration = getUserGameListCollection(userId)
                .whereArrayContains(FIELD_GENRES, genre)
                .let { query ->
                    if (useFirestore) query.orderBy(FIELD_ADDED_AT, direction) else query
                }
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error fetching games by genre: ${error.message}", error)
                        trySend(RequestState.Error("Failed to fetch games: ${error.message}"))
                        return@addSnapshotListener
                    }
                    
                    val games = snapshot?.documents?.let { parseGameEntries(it) } ?: emptyList()
                    val sortedGames = games.applySorting(sortOrder)
                    trySend(RequestState.Success(sortedGames))
                }
            
            awaitClose { 
                listenerRegistration?.remove()
                Log.d(TAG, "Game list by genre listener removed")
            }
            
        } catch (e: CancellationException) {
            Log.d(TAG, "getGameListByGenre flow cancelled")
            listenerRegistration?.remove()
            throw e
        } catch (e: Exception) {
            if (e.toString().contains("AbortFlowException")) {
                Log.d(TAG, "getGameListByGenre flow aborted (expected)")
            } else {
                Log.e(TAG, "Error in getGameListByGenre: ${e.message}", e)
            }
            listenerRegistration?.remove()
            close()
        }
    }
    
    override fun getGameEntry(entryId: String): Flow<RequestState<GameListEntry>> = callbackFlow {
        var listenerRegistration: ListenerRegistration? = null
        
        try {
            val userId = getCurrentUserId()
            if (userId == null) {
                send(RequestState.Error("User not authenticated"))
                close()
                return@callbackFlow
            }
            
            send(RequestState.Loading)
            
            listenerRegistration = getUserGameListCollection(userId)
                .document(entryId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error fetching game entry: ${error.message}", error)
                        trySend(RequestState.Error("Failed to fetch game: ${error.message}"))
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val entry = snapshot.toObject(GameListEntryDto::class.java)?.toGameListEntry(snapshot.id)
                            if (entry != null) {
                                trySend(RequestState.Success(entry))
                            } else {
                                trySend(RequestState.Error("Failed to parse game entry"))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing game entry: ${e.message}", e)
                            trySend(RequestState.Error("Failed to parse game entry"))
                        }
                    } else {
                        trySend(RequestState.Error("Game entry not found"))
                    }
                }
            
            awaitClose { 
                listenerRegistration?.remove()
                Log.d(TAG, "Game entry listener removed")
            }
            
        } catch (e: CancellationException) {
            Log.d(TAG, "getGameEntry flow cancelled")
            listenerRegistration?.remove()
            throw e
        } catch (e: Exception) {
            if (e.toString().contains("AbortFlowException")) {
                Log.d(TAG, "getGameEntry flow aborted (expected)")
            } else {
                Log.e(TAG, "Error in getGameEntry: ${e.message}", e)
            }
            listenerRegistration?.remove()
            close()
        }
    }
    
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
                addedAt = currentTime,
                updatedAt = currentTime,
                status = status,
                rating = null,
                review = "",
                hoursPlayed = 0,
                aspects = emptyList(),
                releaseDate = game.released
            )
            
            val docRef = getUserGameListCollection(userId)
                .add(entry.toDto())
                .await()
            
            Log.d(TAG, "Game added to list successfully: ${game.name}")
            RequestState.Success(docRef.id)
            
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
            if (getEntryIdByRawgId(entry.rawgId) != null) {
                return RequestState.Error("Game already in list")
            }
            
            val docRef = getUserGameListCollection(userId)
                .add(entry.toDto())
                .await()
            
            Log.d(TAG, "Game added to list successfully: ${entry.name}")
            RequestState.Success(docRef.id)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error adding game to list: ${e.message}", e)
            RequestState.Error("Failed to add game: ${e.message}")
        }
    }
    
    override suspend fun updateGameStatus(entryId: String, status: GameStatus): RequestState<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return RequestState.Error("User not authenticated")
            
            val updates = mapOf(
                FIELD_STATUS to status.name,
                "updatedAt" to System.currentTimeMillis()
            )
            
            getUserGameListCollection(userId)
                .document(entryId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Game status updated successfully")
            RequestState.Success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating game status: ${e.message}", e)
            RequestState.Error("Failed to update status: ${e.message}")
        }
    }
    
    override suspend fun updateGameRating(entryId: String, rating: Float?): RequestState<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return RequestState.Error("User not authenticated")
            
            // Validate rating
            if (rating != null && (rating < MIN_RATING || rating > MAX_RATING)) {
                return RequestState.Error("Rating must be between $MIN_RATING and $MAX_RATING")
            }
            
            val updates = mapOf(
                "rating" to rating,
                "updatedAt" to System.currentTimeMillis()
            )
            
            getUserGameListCollection(userId)
                .document(entryId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Game rating updated successfully")
            RequestState.Success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating game rating: ${e.message}", e)
            RequestState.Error("Failed to update rating: ${e.message}")
        }
    }
    
    override suspend fun updateGameReview(entryId: String, review: String): RequestState<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return RequestState.Error("User not authenticated")
            
            val updates = mapOf(
                "review" to review,
                "updatedAt" to System.currentTimeMillis()
            )
            
            getUserGameListCollection(userId)
                .document(entryId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Game review updated successfully")
            RequestState.Success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating game review: ${e.message}", e)
            RequestState.Error("Failed to update review: ${e.message}")
        }
    }
    
    override suspend fun updateHoursPlayed(entryId: String, hours: Int): RequestState<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return RequestState.Error("User not authenticated")
            
            if (hours < 0) {
                return RequestState.Error("Hours played cannot be negative")
            }
            
            val updates = mapOf(
                "hoursPlayed" to hours,
                "updatedAt" to System.currentTimeMillis()
            )
            
            getUserGameListCollection(userId)
                .document(entryId)
                .update(updates)
                .await()
            
            Log.d(TAG, "Hours played updated successfully")
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
            
            // Validate rating
            if (entry.rating != null && (entry.rating < MIN_RATING || entry.rating > MAX_RATING)) {
                return RequestState.Error("Rating must be between $MIN_RATING and $MAX_RATING")
            }
            
            // Update the updatedAt timestamp
            val updatedEntry = entry.copy(updatedAt = System.currentTimeMillis())
            
            getUserGameListCollection(userId)
                .document(entry.id)
                .set(updatedEntry.toDto())
                .await()
            
            Log.d(TAG, "Game entry updated successfully")
            RequestState.Success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating game entry: ${e.message}", e)
            RequestState.Error("Failed to update game: ${e.message}")
        }
    }
    
    override suspend fun removeGameFromList(entryId: String): RequestState<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return RequestState.Error("User not authenticated")
            
            getUserGameListCollection(userId)
                .document(entryId)
                .delete()
                .await()
            
            Log.d(TAG, "Game removed from list successfully")
            RequestState.Success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error removing game from list: ${e.message}", e)
            RequestState.Error("Failed to remove game: ${e.message}")
        }
    }
    
    override suspend fun isGameInList(rawgId: Int): Boolean {
        return try {
            val userId = getCurrentUserId() ?: return false
            
            val snapshot = getUserGameListCollection(userId)
                .whereEqualTo(FIELD_RAWG_ID, rawgId)
                .limit(1)
                .get()
                .await()
            
            !snapshot.isEmpty
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if game is in list: ${e.message}", e)
            false
        }
    }
    
    override suspend fun getEntryIdByRawgId(rawgId: Int): String? {
        return try {
            val userId = getCurrentUserId() ?: return null
            
            val snapshot = getUserGameListCollection(userId)
                .whereEqualTo(FIELD_RAWG_ID, rawgId)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.id
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting entry ID by RAWG ID: ${e.message}", e)
            null
        }
    }
}
