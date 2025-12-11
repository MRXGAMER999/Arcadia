package com.example.arcadia.data.repository

import android.util.Log
import com.example.arcadia.data.datasource.GamerRemoteDataSource
import com.example.arcadia.domain.model.ai.Badge
import com.example.arcadia.domain.repository.FeaturedBadgesRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Refactored FeaturedBadgesRepository implementation.
 * Uses GamerRemoteDataSource to access user data.
 */
class FeaturedBadgesRepositoryImpl(
    private val remoteDataSource: GamerRemoteDataSource
) : FeaturedBadgesRepository {
    
    companion object {
        private const val TAG = "FeaturedBadgesRepo"
        private const val FEATURED_BADGES_FIELD = "featuredBadges"
        private const val MAX_FEATURED_BADGES = 3
        
        private val json = Json { 
            ignoreUnknownKeys = true 
            encodeDefaults = true
        }
    }
    
    override suspend fun saveFeaturedBadges(userId: String, badges: List<Badge>): Result<Unit> {
        return try {
            val badgesToSave = badges.take(MAX_FEATURED_BADGES)
            val badgesJson = json.encodeToString(badgesToSave)
            
            remoteDataSource.updateUser(userId, mapOf(FEATURED_BADGES_FIELD to badgesJson))
            
            Log.d(TAG, "Featured badges saved successfully for user: $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving featured badges: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getFeaturedBadges(userId: String): Flow<List<Badge>> {
        return remoteDataSource.observeUser(userId)
            .map { row ->
                parseFeaturedBadges(row.data[FEATURED_BADGES_FIELD])
            }
    }
    
    private fun parseFeaturedBadges(data: Any?): List<Badge> {
        if (data == null) return emptyList()
        
        return try {
            val jsonString = data as? String
            if (jsonString.isNullOrEmpty()) return emptyList()
            
            json.decodeFromString<List<Badge>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing badges JSON: ${e.message}")
            emptyList()
        }
    }
}
