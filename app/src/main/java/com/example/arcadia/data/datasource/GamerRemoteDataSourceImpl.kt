package com.example.arcadia.data.datasource

import android.util.Log
import com.example.arcadia.BuildConfig
import com.example.arcadia.util.AppwriteConstants
import io.appwrite.ID
import io.appwrite.models.InputFile
import io.appwrite.models.Row
import io.appwrite.services.Realtime
import io.appwrite.services.Storage
import io.appwrite.services.TablesDB
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class GamerRemoteDataSourceImpl(
    tablesDbLazy: Lazy<TablesDB>,
    storageLazy: Lazy<Storage>,
    realtimeLazy: Lazy<Realtime>
) : GamerRemoteDataSource {

    private val tablesDb by tablesDbLazy
    private val storage by storageLazy
    private val realtime by realtimeLazy

    companion object {
        private const val TAG = "GamerRemoteDataSource"
        private val DATABASE_ID = BuildConfig.APPWRITE_DATABASE_ID
        private val USERS_COLLECTION_ID = AppwriteConstants.USERS_COLLECTION_ID
        private val PROFILE_IMAGES_BUCKET_ID = AppwriteConstants.PROFILE_IMAGES_BUCKET_ID
    }

    override suspend fun getUser(userId: String): Row<Map<String, Any>> {
        return tablesDb.getRow(
            databaseId = DATABASE_ID,
            tableId = USERS_COLLECTION_ID,
            rowId = userId
        )
    }

    override suspend fun createUser(userId: String, data: Map<String, Any?>): Row<Map<String, Any>> {
        return tablesDb.createRow(
            databaseId = DATABASE_ID,
            tableId = USERS_COLLECTION_ID,
            rowId = userId,
            data = data
        )
    }

    override suspend fun updateUser(userId: String, data: Map<String, Any?>): Row<Map<String, Any>> {
        return tablesDb.updateRow(
            databaseId = DATABASE_ID,
            tableId = USERS_COLLECTION_ID,
            rowId = userId,
            data = data
        )
    }

    override suspend fun uploadProfileImage(file: InputFile): String {
        val result = storage.createFile(
            bucketId = PROFILE_IMAGES_BUCKET_ID,
            fileId = ID.unique(),
            file = file
        )
        return result.id
    }

    override suspend fun getProfileImageUrl(fileId: String): String {
        // Build a direct view URL so the UI/Coil can fetch the image.
        val endpoint = BuildConfig.APPWRITE_ENDPOINT.trimEnd('/')
        return "$endpoint/storage/buckets/$PROFILE_IMAGES_BUCKET_ID/files/$fileId/view?project=${BuildConfig.APPWRITE_PROJECT_ID}"
    }

    override suspend fun deleteProfileImage(fileId: String) {
        try {
            storage.deleteFile(
                bucketId = PROFILE_IMAGES_BUCKET_ID,
                fileId = fileId
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete profile image file: ${e.message}")
        }
    }

    override fun observeUser(userId: String): Flow<Row<Map<String, Any>>> = callbackFlow {
        try {
            val result = trySend(getUser(userId))
            if (result.isFailure) {
                Log.w(TAG, "Failed to send initial user data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching initial user: ${e.message}", e)
        }

        val subscription = realtime.subscribe(
            "databases.$DATABASE_ID.tables.$USERS_COLLECTION_ID.rows.$userId"
        ) {
            Log.d(TAG, "Realtime update received for user: $userId")
            launch {
                try {
                    val result = trySend(getUser(userId))
                    if (result.isFailure) {
                        Log.w(TAG, "Failed to send updated user data, buffer may be full")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing user: ${e.message}", e)
                }
            }
        }

        awaitClose { subscription.close() }
    }.buffer(Channel.CONFLATED)
}
