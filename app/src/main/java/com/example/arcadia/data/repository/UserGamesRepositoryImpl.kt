package com.example.arcadia.data.repository

import android.util.Log
import com.example.arcadia.data.remote.dto.UserGameDto
import com.example.arcadia.data.remote.mapper.toDto
import com.example.arcadia.data.remote.mapper.toUserGame
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.UserGame
import com.example.arcadia.domain.repository.SortOrder
import com.example.arcadia.domain.repository.UserGamesRepository
import com.example.arcadia.util.RequestState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserGamesRepositoryImpl : UserGamesRepository {

    private val TAG = "UserGamesRepository"
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    override fun getUserGames(sortOrder: SortOrder): Flow<RequestState<List<UserGame>>> = callbackFlow {
        try {
            val userId = getCurrentUserId()
            if (userId == null) {
                send(RequestState.Error("User not authenticated"))
                close()
                return@callbackFlow
            }
            
            send(RequestState.Loading)
            
            val direction = when (sortOrder) {
                SortOrder.NEWEST_FIRST -> Query.Direction.DESCENDING
                SortOrder.OLDEST_FIRST -> Query.Direction.ASCENDING
            }
            
            val listenerRegistration = firestore.collection("users")
                .document(userId)
                .collection("myGames")
                .orderBy("addedAt", direction)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error fetching user games: ${error.message}", error)
                        trySend(RequestState.Error("Failed to fetch games: ${error.message}"))
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val games = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(UserGameDto::class.java)?.toUserGame(doc.id)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing game: ${e.message}", e)
                                null
                            }
                        }
                        trySend(RequestState.Success(games))
                    } else {
                        trySend(RequestState.Success(emptyList()))
                    }
                }
            
            awaitClose { listenerRegistration.remove() }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in getUserGames: ${e.message}", e)
            send(RequestState.Error("Error fetching games: ${e.message}"))
            close()
        }
    }
    
    override fun getUserGamesByGenre(
        genre: String,
        sortOrder: SortOrder
    ): Flow<RequestState<List<UserGame>>> = callbackFlow {
        try {
            val userId = getCurrentUserId()
            if (userId == null) {
                send(RequestState.Error("User not authenticated"))
                close()
                return@callbackFlow
            }
            
            send(RequestState.Loading)
            
            val direction = when (sortOrder) {
                SortOrder.NEWEST_FIRST -> Query.Direction.DESCENDING
                SortOrder.OLDEST_FIRST -> Query.Direction.ASCENDING
            }
            
            val listenerRegistration = firestore.collection("users")
                .document(userId)
                .collection("myGames")
                .whereArrayContains("genres", genre)
                .orderBy("addedAt", direction)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error fetching games by genre: ${error.message}", error)
                        trySend(RequestState.Error("Failed to fetch games: ${error.message}"))
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val games = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(UserGameDto::class.java)?.toUserGame(doc.id)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing game: ${e.message}", e)
                                null
                            }
                        }
                        trySend(RequestState.Success(games))
                    } else {
                        trySend(RequestState.Success(emptyList()))
                    }
                }
            
            awaitClose { listenerRegistration.remove() }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in getUserGamesByGenre: ${e.message}", e)
            send(RequestState.Error("Error fetching games: ${e.message}"))
            close()
        }
    }
    
    override suspend fun addGame(game: Game): RequestState<Unit> {
        return try {
            val userId = getCurrentUserId()
            if (userId == null) {
                return RequestState.Error("User not authenticated")
            }
            
            // Check if game already exists to prevent duplicates
            val existingGame = firestore.collection("users")
                .document(userId)
                .collection("myGames")
                .whereEqualTo("rawgId", game.id)
                .limit(1)
                .get()
                .await()

            if (!existingGame.isEmpty) {
                Log.d(TAG, "Game already in library: ${game.name}")
                return RequestState.Success(Unit) // Return success as the game is already there
            }

            val userGame = UserGame(
                rawgId = game.id,
                name = game.name,
                backgroundImage = game.backgroundImage,
                addedAt = System.currentTimeMillis(),
                genres = game.genres
            )
            
            firestore.collection("users")
                .document(userId)
                .collection("myGames")
                .add(userGame.toDto())
                .await()
            
            Log.d(TAG, "Game added successfully: ${game.name}")
            RequestState.Success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error adding game: ${e.message}", e)
            val errorMessage = when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection."
                e.message?.contains("permission", ignoreCase = true) == true ->
                    "Permission denied. Please try again."
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Request timed out. Please try again."
                else -> "Failed to add game. ${e.message ?: "Unknown error"}"
            }
            RequestState.Error(errorMessage)
        }
    }
    
    override suspend fun removeGame(gameId: String): RequestState<Unit> {
        return try {
            val userId = getCurrentUserId()
            if (userId == null) {
                return RequestState.Error("User not authenticated")
            }
            
            firestore.collection("users")
                .document(userId)
                .collection("myGames")
                .document(gameId)
                .delete()
                .await()
            
            Log.d(TAG, "Game removed successfully: $gameId")
            RequestState.Success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error removing game: ${e.message}", e)
            RequestState.Error("Failed to remove game: ${e.message}")
        }
    }
    
    override suspend fun isGameInLibrary(rawgId: Int): Boolean {
        return try {
            val userId = getCurrentUserId() ?: return false
            
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("myGames")
                .whereEqualTo("rawgId", rawgId)
                .limit(1)
                .get()
                .await()
            
            !snapshot.isEmpty
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if game is in library: ${e.message}", e)
            false
        }
    }
}

