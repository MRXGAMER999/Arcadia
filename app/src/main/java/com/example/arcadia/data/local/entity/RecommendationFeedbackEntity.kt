package com.example.arcadia.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks user interactions with AI recommendations for feedback loop.
 * 
 * This data helps improve future AI recommendations by:
 * - Identifying games users actually added to their library
 * - Tracking click-through rates for recommendation quality assessment
 * - Providing "user previously liked from recommendations" data to AI
 * 
 * Used to enhance prompts with: "User previously liked these recommendations: [games]"
 */
@Entity(
    tableName = "recommendation_feedback",
    indices = [
        Index(value = ["gameId"], unique = true),
        Index(value = ["wasAddedToLibrary"]),
        Index(value = ["interactionTimestamp"])
    ]
)
data class RecommendationFeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** RAWG game ID */
    val gameId: Int,
    
    /** Game name for easy lookup in prompts */
    val gameName: String,
    
    /** AI confidence score when recommended (for correlation analysis) */
    val aiConfidence: Float,
    
    /** Whether user clicked on this recommendation */
    val wasClicked: Boolean = false,
    
    /** Whether user added this game to their library */
    val wasAddedToLibrary: Boolean = false,
    
    /** Timestamp of first interaction (click or add) */
    val interactionTimestamp: Long = System.currentTimeMillis(),
    
    /** How long the recommendation was shown before interaction (ms) */
    val timeToInteraction: Long? = null,
    
    /** Position in the recommendation list when shown */
    val positionWhenShown: Int? = null
)
