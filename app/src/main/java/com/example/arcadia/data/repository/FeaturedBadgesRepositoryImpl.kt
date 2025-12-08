package com.example.arcadia.data.repository

import com.example.arcadia.domain.model.ai.Badge
import com.example.arcadia.domain.repository.FeaturedBadgesRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Implementation of FeaturedBadgesRepository using Firestore.
 * 
 * Firestore Structure:
 * users/{userId}
 * └── featuredBadges: [
 *       { "title": "Badge Title", "emoji": "🎮", "reason": "Why earned" },
 *       ...
 *     ]
 * 
 * Requirements: 8.3
 */
class FeaturedBadgesRepositoryImpl : FeaturedBadgesRepository {
    
    private val firestore = Firebase.firestore
    private val usersCollection = firestore.collection("users")
    
    companion object {
        private const val FEATURED_BADGES_FIELD = "featuredBadges"
        private const val MAX_FEATURED_BADGES = 3
    }
    
    override suspend fun saveFeaturedBadges(userId: String, badges: List<Badge>): Result<Unit> {
        return try {
            // Enforce max 3 badges limit
            val badgesToSave = badges.take(MAX_FEATURED_BADGES)
            
            // Convert badges to Firestore-compatible maps
            val badgeMaps = badgesToSave.map { badge ->
                mapOf(
                    "title" to badge.title,
                    "emoji" to badge.emoji,
                    "reason" to badge.reason
                )
            }
            
            usersCollection
                .document(userId)
                .update(FEATURED_BADGES_FIELD, badgeMaps)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FeaturedBadgesRepo", "Error saving featured badges: ${e.message}", e)
            Result.failure(e)
        }
    }

    
    override fun getFeaturedBadges(userId: String): Flow<List<Badge>> = callbackFlow {
        val listenerRegistration = usersCollection
            .document(userId)
            .addSnapshotListener { documentSnapshot, error ->
                if (error != null) {
                    android.util.Log.e("FeaturedBadgesRepo", "Error listening for badges: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val badges = parseFeaturedBadges(documentSnapshot.get(FEATURED_BADGES_FIELD))
                    trySend(badges)
                } else {
                    trySend(emptyList())
                }
            }
        
        awaitClose {
            listenerRegistration.remove()
        }
    }
    
    /**
     * Parses the featuredBadges field from Firestore into a list of Badge objects.
     * Handles various data formats gracefully.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseFeaturedBadges(data: Any?): List<Badge> {
        if (data == null) return emptyList()
        
        return try {
            val badgesList = data as? List<Map<String, Any?>> ?: return emptyList()
            
            badgesList.mapNotNull { badgeMap ->
                val title = badgeMap["title"] as? String
                val emoji = badgeMap["emoji"] as? String
                val reason = badgeMap["reason"] as? String ?: ""
                
                if (title != null && emoji != null) {
                    Badge(title = title, emoji = emoji, reason = reason)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FeaturedBadgesRepo", "Error parsing badges: ${e.message}")
            emptyList()
        }
    }
}
