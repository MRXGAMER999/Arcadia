package com.example.arcadia.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.arcadia.data.datasource.GamerRemoteDataSource
import com.example.arcadia.data.mapper.GamerMapper
import com.example.arcadia.domain.model.Gamer
import com.example.arcadia.domain.model.ProfileSection
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.util.RequestState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.onesignal.OneSignal
import io.appwrite.models.InputFile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Refactored GamerRepository implementation.
 * Delegates data operations to GamerRemoteDataSource and mapping to GamerMapper.
 */
class GamerRepositoryImpl(
    private val context: Context,
    private val remoteDataSource: GamerRemoteDataSource
) : GamerRepository {

    companion object {
        private const val TAG = "GamerRepositoryImpl"
        private val json = Json { 
            ignoreUnknownKeys = true 
            encodeDefaults = true
        }
    }

    override fun getCurrentUserId(): String? {
        return Firebase.auth.currentUser?.uid
    }

    override suspend fun createUser(
        user: FirebaseUser?,
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            if (user == null) {
                onError("User is not available")
                return
            }

            try {
                val existingDoc = remoteDataSource.getUser(user.uid)
                val profileComplete = existingDoc.data["profileComplete"] as? Boolean ?: false
                loginToOneSignal(user.uid)
                onSuccess(profileComplete)
            } catch (e: Exception) {
                // Assume 404 or error means create new
                // Ideally check for 404 specifically, but for now generic catch
                Log.d(TAG, "Creating new user document")
                
                val userData = mapOf(
                    "name" to (user.displayName ?: "Unknown"),
                    "email" to (user.email ?: "Unknown"),
                    "username" to "",
                    "profileImageUrl" to (user.photoUrl?.toString()),
                    "profileComplete" to false,
                    "isProfilePublic" to true,
                    "friendRequestsSentToday" to 0
                )

                remoteDataSource.createUser(user.uid, userData)
                loginToOneSignal(user.uid)
                onSuccess(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in createUser: ${e.message}", e)
            onError(e.message ?: "Error while creating customer")
        }
    }

    override fun readCustomerFlow(): Flow<RequestState<Gamer>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(RequestState.Error("User is not available"))
            close()
            return@callbackFlow
        }

        val job = remoteDataSource.observeUser(userId)
            .map { row ->
                val gamer = GamerMapper.toGamer(row)
                RequestState.Success(gamer) as RequestState<Gamer>
            }
            .catch { e ->
                Log.e(TAG, "Error observing user: ${e.message}", e)
                emit(RequestState.Error("Error fetching user: ${e.message}"))
            }
            .onEach { trySend(it) }
            .launchIn(this)

        awaitClose { job.cancel() }
    }

    override fun getGamer(userId: String): Flow<RequestState<Gamer>> = callbackFlow {
        val job = remoteDataSource.observeUser(userId)
            .map { row ->
                val gamer = GamerMapper.toGamer(row)
                RequestState.Success(gamer) as RequestState<Gamer>
            }
            .catch { e ->
                Log.e(TAG, "Error observing gamer: ${e.message}", e)
                emit(RequestState.Error("Error fetching user: ${e.message}"))
            }
            .onEach { trySend(it) }
            .launchIn(this)

        awaitClose { job.cancel() }
    }

    override suspend fun updateGamer(
        gamer: Gamer,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val userId = getCurrentUserId() ?: return onError("User is not available")

            val updates = mutableMapOf<String, Any?>(
                "name" to gamer.name,
                "username" to gamer.username,
                "country" to gamer.country,
                "city" to gamer.city,
                "gender" to gamer.gender,
                "description" to gamer.description,
                "profileComplete" to gamer.profileComplete,
                "steamId" to gamer.steamId,
                "xboxGamertag" to gamer.xboxGamertag,
                "psnId" to gamer.psnId
            )

            if (gamer.profileImageUrl != null) {
                updates["profileImageUrl"] = gamer.profileImageUrl
            }

            remoteDataSource.updateUser(gamer.id, updates)
            onSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating gamer: ${e.message}", e)
            onError(e.message ?: "Error updating profile")
        }
    }

    override suspend fun updateGamer(
        customSections: List<ProfileSection>?,
        isProfilePublic: Boolean?
    ): RequestState<Unit> {
        try {
            val userId = getCurrentUserId() ?: return RequestState.Error("User is not available")
            val updates = mutableMapOf<String, Any?>()
            
            if (customSections != null) {
                updates["customSections"] = json.encodeToString(customSections)
            }
            if (isProfilePublic != null) {
                updates["isProfilePublic"] = isProfilePublic
            }
            
            if (updates.isNotEmpty()) {
                remoteDataSource.updateUser(userId, updates)
            }
            return RequestState.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating gamer settings: ${e.message}", e)
            return RequestState.Error(e.message ?: "Error updating settings")
        }
    }

    override suspend fun uploadProfileImage(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val userId = getCurrentUserId() ?: return onError("User is not available")
            
            val file = File(context.cacheDir, "profile_image.jpg")
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return onError("Unable to read image from the selected source")

            inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!file.exists() || file.length() == 0L) {
                return onError("Selected image is empty or could not be read")
            }

            // Delete old profile image if present
            runCatching {
                val existingUrl = remoteDataSource.getUser(userId).data["profileImageUrl"] as? String
                extractFileIdFromUrl(existingUrl)?.let { remoteDataSource.deleteProfileImage(it) }
            }.onFailure { e ->
                Log.w(TAG, "Failed to delete previous profile image: ${e.message}")
            }

            val inputFile = InputFile.fromFile(file)
            val fileId = remoteDataSource.uploadProfileImage(inputFile)

            // Get the accessible URL (or ID) and persist it on the user document
            val imageUrl = remoteDataSource.getProfileImageUrl(fileId)
            remoteDataSource.updateUser(userId, mapOf("profileImageUrl" to imageUrl))

            onSuccess(imageUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading profile image: ${e.message}", e)
            onError(e.message ?: "Error uploading image")
        }
    }

    override suspend fun deleteProfileImage(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val userId = getCurrentUserId() ?: return onError("User is not available")

            // Delete stored file if we have its id
            runCatching {
                val existingUrl = remoteDataSource.getUser(userId).data["profileImageUrl"] as? String
                extractFileIdFromUrl(existingUrl)?.let { remoteDataSource.deleteProfileImage(it) }
            }.onFailure { e ->
                Log.w(TAG, "Failed to delete profile image file: ${e.message}")
            }

            remoteDataSource.updateUser(userId, mapOf("profileImageUrl" to null))
            onSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting profile image: ${e.message}", e)
            onError(e.message ?: "Error deleting profile image")
        }
    }

    override suspend fun signOut(): RequestState<Unit> {
        return try {
            logoutFromOneSignal()
            Firebase.auth.signOut()
            RequestState.Success(Unit)
        } catch (e: Exception) {
            RequestState.Error(e.message ?: "Error signing out")
        }
    }

    private suspend fun loginToOneSignal(userId: String) {
        try {
            OneSignal.login(userId)
            val subscriptionId = OneSignal.User.pushSubscription.id
            if (!subscriptionId.isNullOrBlank()) {
                remoteDataSource.updateUser(userId, mapOf("oneSignalPlayerId" to subscriptionId))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to login to OneSignal", e)
        }
    }

    private fun logoutFromOneSignal() {
        try {
            OneSignal.logout()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to logout from OneSignal", e)
        }
    }

    private fun extractFileIdFromUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val marker = "/files/"
            val idx = url.indexOf(marker)
            if (idx >= 0) {
                val start = idx + marker.length
                val end = url.indexOf('/', start).takeIf { it >= 0 } ?: url.length
                url.substring(start, end)
            } else {
                url // if only fileId was stored
            }
        } catch (_: Exception) {
            null
        }
    }
}
