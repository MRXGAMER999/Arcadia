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
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GameListRepositoryImpl : GameListRepository {
    
    private val TAG = "GameListRepository"
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    override fun getGameList(sortOrder: SortOrder): Flow<RequestState<List<GameListEntry>>> = callbackFlow {
        try {
            val userId = getCurrentUserId()
            if (userId == null) {
                send(RequestState.Error("User not authenticated"))
                close()
                return@callbackFlow
            }
            
            send(RequestState.Loading)
            
            // For Title and Rating sorting, we need to fetch all and sort client-side
            // For date-based sorting, we can use Firestore orderBy
            val usesFirestoreSort = sortOrder in listOf(SortOrder.NEWEST_FIRST, SortOrder.OLDEST_FIRST)
            
            val direction = when (sortOrder) {
                SortOrder.NEWEST_FIRST -> Query.Direction.DESCENDING
                SortOrder.OLDEST_FIRST -> Query.Direction.ASCENDING
                else -> Query.Direction.DESCENDING // Default for client-side sorting
            }
            
            val listenerRegistration = firestore.collection("users")
                .document(userId)
                .collection("gameList")
                .let { query ->
                    if (usesFirestoreSort) {
                        query.orderBy("addedAt", direction)
                    } else {
                        query // No Firestore sorting, will sort client-side
                    }
                }
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error fetching game list: ${error.message}", error)
                        trySend(RequestState.Error("Failed to fetch games: ${error.message}"))
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val games = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(GameListEntryDto::class.java)?.toGameListEntry(doc.id)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing game entry: ${e.message}", e)
                                null
                            }
                        }
                        
                        // Apply client-side sorting for Title, Rating, and Release Date
                        val sortedGames = when (sortOrder) {
                            SortOrder.TITLE_A_Z -> games.sortedBy { it.name.lowercase() }
                            SortOrder.TITLE_Z_A -> games.sortedByDescending { it.name.lowercase() }
                            SortOrder.RATING_HIGH -> games.sortedByDescending { it.rating ?: -1f }
                            SortOrder.RATING_LOW -> games.sortedBy { it.rating ?: Float.MAX_VALUE }
                            SortOrder.RELEASE_NEW -> games.sortedByDescending { it.releaseDate ?: "" }
                            SortOrder.RELEASE_OLD -> games.sortedBy { it.releaseDate ?: "9999-12-31" }
                            else -> games // Already sorted by Firestore
                        }
                        
                        trySend(RequestState.Success(sortedGames))
                    } else {
                        trySend(RequestState.Success(emptyList()))
                    }
                }
            
            awaitClose { listenerRegistration.remove() }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in getGameList: ${e.message}", e)
            send(RequestState.Error("Error fetching games: ${e.message}"))
            close()
        }
    }
    
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
            
            val usesFirestoreSort = sortOrder in listOf(SortOrder.NEWEST_FIRST, SortOrder.OLDEST_FIRST)
            
            val direction = when (sortOrder) {
                SortOrder.NEWEST_FIRST -> Query.Direction.DESCENDING
                SortOrder.OLDEST_FIRST -> Query.Direction.ASCENDING
                else -> Query.Direction.DESCENDING // Default for client-side sorting
            }
            
            val listenerRegistration = firestore.collection("users")
                .document(userId)
                .collection("gameList")
                .whereEqualTo("status", status.name)
                .let { query ->
                    if (usesFirestoreSort) {
                        query.orderBy("addedAt", direction)
                    } else {
                        query
                    }
                }
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error fetching games by status: ${error.message}", error)
                        trySend(RequestState.Error("Failed to fetch games: ${error.message}"))
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val games = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(GameListEntryDto::class.java)?.toGameListEntry(doc.id)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing game entry: ${e.message}", e)
                                null
                            }
                        }
                        
                        // Apply client-side sorting
                        val sortedGames = when (sortOrder) {
                            SortOrder.TITLE_A_Z -> games.sortedBy { it.name.lowercase() }
                            SortOrder.TITLE_Z_A -> games.sortedByDescending { it.name.lowercase() }
                            SortOrder.RATING_HIGH -> games.sortedByDescending { it.rating ?: -1f }
                            SortOrder.RATING_LOW -> games.sortedBy { it.rating ?: Float.MAX_VALUE }
                            SortOrder.RELEASE_NEW -> games.sortedByDescending { it.releaseDate ?: "" }
                            SortOrder.RELEASE_OLD -> games.sortedBy { it.releaseDate ?: "9999-12-31" }
                            else -> games
                        }
                        
                        trySend(RequestState.Success(sortedGames))
                    } else {
                        trySend(RequestState.Success(emptyList()))
                    }
                }
            
            awaitClose { listenerRegistration.remove() }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in getGameListByStatus: ${e.message}", e)
            send(RequestState.Error("Error fetching games: ${e.message}"))
            close()
        }
    }
    
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
            
            val usesFirestoreSort = sortOrder in listOf(SortOrder.NEWEST_FIRST, SortOrder.OLDEST_FIRST)
            
            val direction = when (sortOrder) {
                SortOrder.NEWEST_FIRST -> Query.Direction.DESCENDING
                SortOrder.OLDEST_FIRST -> Query.Direction.ASCENDING
                else -> Query.Direction.DESCENDING
            }
            
            val listenerRegistration = firestore.collection("users")
                .document(userId)
                .collection("gameList")
                .whereArrayContains("genres", genre)
                .let { query ->
                    if (usesFirestoreSort) {
                        query.orderBy("addedAt", direction)
                    } else {
                        query
                    }
                }
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error fetching games by genre: ${error.message}", error)
                        trySend(RequestState.Error("Failed to fetch games: ${error.message}"))
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val games = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(GameListEntryDto::class.java)?.toGameListEntry(doc.id)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing game entry: ${e.message}", e)
                                null
                            }
                        }
                        
                        // Apply client-side sorting
                        val sortedGames = when (sortOrder) {
                            SortOrder.TITLE_A_Z -> games.sortedBy { it.name.lowercase() }
                            SortOrder.TITLE_Z_A -> games.sortedByDescending { it.name.lowercase() }
                            SortOrder.RATING_HIGH -> games.sortedByDescending { it.rating ?: -1f }
                            SortOrder.RATING_LOW -> games.sortedBy { it.rating ?: Float.MAX_VALUE }
                            SortOrder.RELEASE_NEW -> games.sortedByDescending { it.releaseDate ?: "" }
                            SortOrder.RELEASE_OLD -> games.sortedBy { it.releaseDate ?: "9999-12-31" }
                            else -> games
                        }
                        
                        trySend(RequestState.Success(sortedGames))
                    } else {
                        trySend(RequestState.Success(emptyList()))
                    }
                }
            
            awaitClose { listenerRegistration.remove() }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in getGameListByGenre: ${e.message}", e)
            send(RequestState.Error("Error fetching games: ${e.message}"))
            close()
        }
    }
    
    override fun getGameEntry(entryId: String): Flow<RequestState<GameListEntry>> = callbackFlow {
        try {
            val userId = getCurrentUserId()
            if (userId == null) {
                send(RequestState.Error("User not authenticated"))
                close()
                return@callbackFlow
            }
            
            send(RequestState.Loading)
            
            val listenerRegistration = firestore.collection("users")
                .document(userId)
                .collection("gameList")
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
            
            awaitClose { listenerRegistration.remove() }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in getGameEntry: ${e.message}", e)
            send(RequestState.Error("Error fetching game: ${e.message}"))
            close()
        }
    }
    
    override suspend fun addGameToList(
        game: Game,
        status: GameStatus
    ): RequestState<String> {
        return try {
            val userId = getCurrentUserId()
            if (userId == null) {
                return RequestState.Error("User not authenticated")
            }
            
            // Check if game already exists
            val existingId = getEntryIdByRawgId(game.id)
            if (existingId != null) {
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
            
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("gameList")
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
            if (userId == null) {
                return RequestState.Error("User not authenticated")
            }
            
            // Check if game already exists
            val existingId = getEntryIdByRawgId(entry.rawgId)
            if (existingId != null) {
                return RequestState.Error("Game already in list")
            }
            
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("gameList")
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
            if (userId == null) {
                return RequestState.Error("User not authenticated")
            }
            
            val updates = mapOf(
                "status" to status.name,
                "updatedAt" to System.currentTimeMillis()
            )
            
            firestore.collection("users")
                .document(userId)
                .collection("gameList")
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
            if (userId == null) {
                return RequestState.Error("User not authenticated")
            }
            
            // Validate rating
            if (rating != null && (rating < 0f || rating > 10f)) {
                return RequestState.Error("Rating must be between 0 and 10")
            }
            
            val updates = mapOf(
                "rating" to rating,
                "updatedAt" to System.currentTimeMillis()
            )
            
            firestore.collection("users")
                .document(userId)
                .collection("gameList")
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
            if (userId == null) {
                return RequestState.Error("User not authenticated")
            }
            
            val updates = mapOf(
                "review" to review,
                "updatedAt" to System.currentTimeMillis()
            )
            
            firestore.collection("users")
                .document(userId)
                .collection("gameList")
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
            if (userId == null) {
                return RequestState.Error("User not authenticated")
            }
            
            if (hours < 0) {
                return RequestState.Error("Hours played cannot be negative")
            }
            
            val updates = mapOf(
                "hoursPlayed" to hours,
                "updatedAt" to System.currentTimeMillis()
            )
            
            firestore.collection("users")
                .document(userId)
                .collection("gameList")
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
            if (userId == null) {
                return RequestState.Error("User not authenticated")
            }
            
            // Validate rating
            if (entry.rating != null && (entry.rating < 0f || entry.rating > 10f)) {
                return RequestState.Error("Rating must be between 0 and 10")
            }
            
            // Update the updatedAt timestamp
            val updatedEntry = entry.copy(updatedAt = System.currentTimeMillis())
            
            firestore.collection("users")
                .document(userId)
                .collection("gameList")
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
            if (userId == null) {
                return RequestState.Error("User not authenticated")
            }
            
            firestore.collection("users")
                .document(userId)
                .collection("gameList")
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
            
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("gameList")
                .whereEqualTo("rawgId", rawgId)
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
            
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("gameList")
                .whereEqualTo("rawgId", rawgId)
                .limit(1)
                .get()
                .await()
            
            if (!snapshot.isEmpty) {
                snapshot.documents.first().id
            } else {
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting entry ID by RAWG ID: ${e.message}", e)
            null
        }
    }
}
