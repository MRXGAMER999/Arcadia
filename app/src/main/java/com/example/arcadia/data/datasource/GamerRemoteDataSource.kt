package com.example.arcadia.data.datasource

import io.appwrite.models.InputFile
import io.appwrite.models.Row
import kotlinx.coroutines.flow.Flow

interface GamerRemoteDataSource {
    suspend fun getUser(userId: String): Row<Map<String, Any>>
    suspend fun createUser(userId: String, data: Map<String, Any?>): Row<Map<String, Any>>
    suspend fun updateUser(userId: String, data: Map<String, Any?>): Row<Map<String, Any>>
    suspend fun uploadProfileImage(file: InputFile): String // Returns file ID
    suspend fun getProfileImageUrl(fileId: String): String
    suspend fun deleteProfileImage(fileId: String)
    fun observeUser(userId: String): Flow<Row<Map<String, Any>>>
}
