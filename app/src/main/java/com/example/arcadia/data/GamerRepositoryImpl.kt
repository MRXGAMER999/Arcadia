package com.example.arcadia.data

import com.example.arcadia.domain.model.Gamer
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.util.RequestState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GamerRepositoryImpl: GamerRepository {
    override fun getCurrentUserId(): String? {
        return Firebase.auth.currentUser?.uid
    }

    override suspend fun createUser(
        user: FirebaseUser?,
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            android.util.Log.d("GamerRepository", "createUser called for user: ${user?.uid}")
            
            if (user != null){
                val database = Firebase.firestore
                android.util.Log.d("GamerRepository", "Got Firestore instance")
                
                val userCollection = database.collection("users")
                android.util.Log.d("GamerRepository", "Checking if user document exists...")
                
                val userDoc = userCollection.document(user.uid).get().await()
                android.util.Log.d("GamerRepository", "User doc exists: ${userDoc.exists()}")
                
                if(userDoc.exists()){
                    // Existing user - return their profile completion status
                    val gamer = userDoc.toObject(Gamer::class.java)
                    android.util.Log.d("GamerRepository", "Existing user, profileComplete: ${gamer?.profileComplete}")
                    onSuccess(gamer?.profileComplete ?: false)
                } else {
                    // New user - create user with profileComplete = false
                    android.util.Log.d("GamerRepository", "Creating new user document")
                    val userData = hashMapOf(
                        "id" to user.uid,
                        "name" to (user.displayName ?: "Unknown"),
                        "email" to (user.email ?: "Unknown"),
                        "username" to "",
                        "country" to null,
                        "city" to null,
                        "gender" to null,
                        "description" to "",
                        "profileComplete" to false
                    )

                    userCollection.document(user.uid).set(userData).await()
                    android.util.Log.d("GamerRepository", "User document created successfully")
                    onSuccess(false) // New user, profile not complete
                }

            } else {
                android.util.Log.e("GamerRepository", "User is null")
                onError("User is not available")
            }

        } catch (e: Exception) {
            android.util.Log.e("GamerRepository", "Error in createUser: ${e.message}", e)
            onError(e.message ?: "Error while creating customer")
        }
    }

    override fun readCustomerFlow(): Flow<RequestState<Gamer>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            send(RequestState.Error("User is not available"))
            close()
            return@callbackFlow
        }

        val database = Firebase.firestore
        val listenerRegistration = database.collection("users")
            .document(userId)
            .addSnapshotListener { documentSnapshot, error ->
                if (error != null) {
                    trySend(RequestState.Error("Error: ${error.message}"))
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    // Try to parse as Gamer
                    var gamer = documentSnapshot.toObject(Gamer::class.java)

                    // Migration: Handle old data structure with firstName/lastName
                    if (gamer != null && gamer.name.isBlank()) {
                        val firstName = documentSnapshot.getString("firstName") ?: ""
                        val lastName = documentSnapshot.getString("lastName") ?: ""
                        val fullName = "$firstName $lastName".trim()

                        if (fullName.isNotBlank()) {
                            // Migrate old structure to new
                            gamer = gamer.copy(name = fullName)

                            // Update Firestore with new structure (fire and forget)
                            database.collection("users")
                                .document(userId)
                                .update(
                                    mapOf(
                                        "name" to fullName,
                                        "username" to (gamer.username.ifBlank { "" })
                                    )
                                )
                                .addOnFailureListener { e ->
                                    android.util.Log.e("GamerRepository", "Migration failed: ${e.message}")
                                }
                        }
                    }

                    if (gamer != null) {
                        trySend(RequestState.Success(gamer))
                    } else {
                        trySend(RequestState.Error("Error parsing customer data"))
                    }
                } else {
                    trySend(RequestState.Error("Customer data does not exist"))
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    override suspend fun updateGamer(
        gamer: Gamer,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val userId = getCurrentUserId()
            if (userId != null) {
                val firestore = Firebase.firestore
                val userCollection = firestore.collection("users")
                val existingUser = userCollection
                    .document(gamer.id)
                    .get()
                    .await()
                if (existingUser.exists()) {
                    userCollection
                        .document(gamer.id)
                        .update(
                            "name", gamer.name,
                            "username", gamer.username,
                            "country", gamer.country,
                            "city", gamer.city,
                            "gender", gamer.gender,
                            "description", gamer.description,
                            "profileComplete", gamer.profileComplete
                        )
                        .await()
                    onSuccess()
                } else {
                    onError("User not found")
                }
            } else {
                onError("User is not available")
            }
        } catch (e: Exception) {
            onError(e.message ?: "Error updating user")
        }
    }

    override suspend fun signOut(): RequestState<Unit> {
        return try {
            Firebase.auth.signOut()
            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Error signing out")
        }
    }
}