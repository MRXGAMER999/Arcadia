package com.example.arcadia.domain.repository

import com.example.arcadia.domain.model.ai.Badge
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing featured badges on user profiles.
 * Featured badges are AI-generated achievements that users can select
 * to display on their public profile (max 3).
 * 
 * Requirements: 8.3
 */
interface FeaturedBadgesRepository {
    /**
     * Saves the user's selected featured badges to Appwrite.
     * 
     * @param userId The ID of the user whose badges are being saved
     * @param badges The list of badges to feature (max 3)
     * @return Result indicating success or failure
     */
    suspend fun saveFeaturedBadges(userId: String, badges: List<Badge>): Result<Unit>
    
    /**
     * Retrieves the featured badges for a user as a Flow.
     * Emits updates when the badges change in Appwrite.
     * 
     * @param userId The ID of the user whose badges to retrieve
     * @return Flow emitting the list of featured badges
     */
    fun getFeaturedBadges(userId: String): Flow<List<Badge>>
}
