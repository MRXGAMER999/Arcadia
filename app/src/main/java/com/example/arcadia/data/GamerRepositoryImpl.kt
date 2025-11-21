package com.example.arcadia.data

import android.net.Uri
import com.example.arcadia.domain.model.Gamer
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.util.RequestState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

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
                        "profileImageUrl" to (user.photoUrl?.toString()),
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
                    val updates = mutableMapOf<String, Any?>(
                        "name" to gamer.name,
                        "username" to gamer.username,
                        "country" to gamer.country,
                        "city" to gamer.city,
                        "gender" to gamer.gender,
                        "description" to gamer.description,
                        "profileComplete" to gamer.profileComplete
                    )
                    
                    // Only update profileImageUrl if it's not null
                    if (gamer.profileImageUrl != null) {
                        updates["profileImageUrl"] = gamer.profileImageUrl
                    }
                    
                    userCollection
                        .document(gamer.id)
                        .update(updates)
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

    override suspend fun uploadProfileImage(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val userId = getCurrentUserId()
            if (userId == null) {
                onError("User is not available")
                return
            }

            // Get the current profile image URL to delete the old image
            val database = Firebase.firestore
            val userDoc = database.collection("users").document(userId).get().await()
            val oldImageUrl = userDoc.getString("profileImageUrl")
            
            // Delete the old image if it exists and is from Firebase Storage
            // Don't delete external URLs (e.g., Google profile pictures)
            if (!oldImageUrl.isNullOrEmpty() && oldImageUrl.contains("firebasestorage.googleapis.com")) {
                try {
                    val oldImageRef = Firebase.storage.getReferenceFromUrl(oldImageUrl)
                    oldImageRef.delete().await()
                    android.util.Log.d("GamerRepository", "Old profile image deleted successfully")
                } catch (e: Exception) {
                    // Log but don't fail the upload if deletion fails
                    android.util.Log.w("GamerRepository", "Failed to delete old profile image: ${e.message}")
                }
            } else if (!oldImageUrl.isNullOrEmpty()) {
                android.util.Log.d("GamerRepository", "Skipping deletion of external profile image (e.g., Google): $oldImageUrl")
            }

            // Create a unique filename for the new image
            val imageFileName = "profile_${userId}_${UUID.randomUUID()}.jpg"
            val storageRef = Firebase.storage.reference
            val profileImagesRef = storageRef.child("profile_images/$imageFileName")

            // Upload the new file
            profileImagesRef.putFile(imageUri).await()

            // Get the download URL
            val downloadUrl = profileImagesRef.downloadUrl.await()

            onSuccess(downloadUrl.toString())
        } catch (e: Exception) {
            android.util.Log.e("GamerRepository", "Error uploading profile image: ${e.message}", e)
            onError(e.message ?: "Error uploading profile image")
        }
    }

    override suspend fun deleteProfileImage(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val userId = getCurrentUserId()
            if (userId == null) {
                onError("User is not available")
                return
            }

            // Get the current profile image URL
            val database = Firebase.firestore
            val userDoc = database.collection("users").document(userId).get().await()
            val imageUrl = userDoc.getString("profileImageUrl")
            
            if (imageUrl.isNullOrEmpty()) {
                onError("No profile image to delete")
                return
            }

            // Only delete from Storage if it's a Firebase Storage URL
            // Don't delete external URLs (e.g., Google profile pictures)
            if (imageUrl.contains("firebasestorage.googleapis.com")) {
                val imageRef = Firebase.storage.getReferenceFromUrl(imageUrl)
                imageRef.delete().await()
                android.util.Log.d("GamerRepository", "Profile image deleted from Storage")
            } else {
                android.util.Log.d("GamerRepository", "Skipping Storage deletion for external URL")
            }
            
            // Remove the URL from Firestore (regardless of source)
            database.collection("users")
                .document(userId)
                .update("profileImageUrl", null)
                .await()
            
            android.util.Log.d("GamerRepository", "Profile image URL removed from Firestore")
            onSuccess()
        } catch (e: Exception) {
            android.util.Log.e("GamerRepository", "Error deleting profile image: ${e.message}", e)
            onError(e.message ?: "Error deleting profile image")
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