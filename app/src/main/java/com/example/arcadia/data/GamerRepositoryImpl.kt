package com.example.arcadia.data

import com.example.arcadia.domain.model.Gamer
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.util.RequestState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
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
            if (user != null){
                val database = Firebase.firestore
                val userCollection = database.collection("users")
                val userDoc = userCollection.document(user.uid).get().await()
                if(userDoc.exists()){
                    // Existing user - return their profile completion status
                    val gamer = userDoc.toObject(Gamer::class.java)
                    onSuccess(gamer?.profileComplete ?: false)
                } else {
                    // New user - create user with profileComplete = false
                    val userData = hashMapOf(
                        "id" to user.uid,
                        "firstName" to (user.displayName?.split(" ")?.firstOrNull() ?: "Unknown"),
                        "lastName" to (user.displayName?.split(" ")?.lastOrNull() ?: "Unknown"),
                        "email" to (user.email ?: "Unknown"),
                        "profileComplete" to false,
                        "cart" to emptyList<Any>()
                    )

                    userCollection.document(user.uid).set(userData).await()
                    userCollection.document(user.uid)
                        .collection("privateData")
                        .document("role")
                        .set(mapOf("isAdmin" to false))
                        .await()
                    onSuccess(false) // New user, profile not complete
                }

            } else {
                onError("User is not available")
            }

        } catch (e: Exception) {
            onError(e.message ?: "Error while creating customer")
        }
    }

    override fun readCustomerFlow(): Flow<RequestState<Gamer>> = channelFlow {
        try {
            // Get userId fresh each time to handle sign-out/sign-in scenarios
            val userId = getCurrentUserId()
            if (userId != null) {
                val database = Firebase.firestore
                database.collection("users")
                    .document(userId)
                    .snapshots()
                    .collectLatest { documentSnapshot ->
                        // Re-check userId to ensure it hasn't changed
                        val currentUserId = getCurrentUserId()
                        if (currentUserId != userId) {
                            // User changed, stop this flow
                            send(RequestState.Error("User session changed"))
                            return@collectLatest
                        }

                        if (documentSnapshot.exists()) {
                            val customer = documentSnapshot.toObject(Gamer::class.java)
                            if (customer != null) {
                                send(RequestState.Success(customer))
                            } else {
                                send(RequestState.Error("Error parsing customer data"))
                            }
                        } else {
                            send(RequestState.Error("Customer data does not exist"))
                        }
                    }
            } else {
                send(RequestState.Error("User is not available"))
            }
        } catch (e: Exception) {
            send(RequestState.Error("Error fetching customer data: ${e.message}"))
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