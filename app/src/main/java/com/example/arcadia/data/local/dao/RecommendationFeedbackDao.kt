package com.example.arcadia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.arcadia.data.local.entity.RecommendationFeedbackEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for recommendation feedback tracking.
 * 
 * Provides operations for:
 * - Recording user interactions with recommendations
 * - Querying successful recommendations for prompt enhancement
 * - Analytics on recommendation quality
 */
@Dao
interface RecommendationFeedbackDao {
    
    // ==================== Insert/Update Operations ====================
    
    /**
     * Record a new recommendation being shown to user.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordRecommendationShown(feedback: RecommendationFeedbackEntity)
    
    /**
     * Record multiple recommendations being shown.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordRecommendationsShown(feedbacks: List<RecommendationFeedbackEntity>)
    
    /**
     * Update feedback when user clicks on a recommendation.
     */
    @Query("""
        UPDATE recommendation_feedback 
        SET wasClicked = 1, 
            interactionTimestamp = :timestamp,
            timeToInteraction = :timeToInteraction
        WHERE gameId = :gameId
    """)
    suspend fun recordClick(gameId: Int, timestamp: Long = System.currentTimeMillis(), timeToInteraction: Long? = null)
    
    /**
     * Update feedback when user adds a recommended game to their library.
     */
    @Query("""
        UPDATE recommendation_feedback 
        SET wasAddedToLibrary = 1,
            wasClicked = 1,
            interactionTimestamp = CASE WHEN interactionTimestamp = 0 THEN :timestamp ELSE interactionTimestamp END
        WHERE gameId = :gameId
    """)
    suspend fun recordAddedToLibrary(gameId: Int, timestamp: Long = System.currentTimeMillis())
    
    // ==================== Query Operations ====================
    
    /**
     * Get games that user added to library from recommendations.
     * Used to enhance AI prompts with "User previously liked these recommendations"
     * 
     * @param limit Max number of games to return (most recent first)
     */
    @Query("""
        SELECT * FROM recommendation_feedback 
        WHERE wasAddedToLibrary = 1 
        ORDER BY interactionTimestamp DESC 
        LIMIT :limit
    """)
    suspend fun getSuccessfulRecommendations(limit: Int = 20): List<RecommendationFeedbackEntity>
    
    /**
     * Get game names that user added from recommendations.
     * Optimized for prompt building - returns just the names.
     */
    @Query("""
        SELECT gameName FROM recommendation_feedback 
        WHERE wasAddedToLibrary = 1 
        ORDER BY interactionTimestamp DESC 
        LIMIT :limit
    """)
    suspend fun getSuccessfulRecommendationNames(limit: Int = 20): List<String>
    
    /**
     * Check if a specific game was recommended before.
     */
    @Query("SELECT * FROM recommendation_feedback WHERE gameId = :gameId LIMIT 1")
    suspend fun getFeedbackForGame(gameId: Int): RecommendationFeedbackEntity?
    
    /**
     * Get all feedback as Flow for observing.
     */
    @Query("SELECT * FROM recommendation_feedback ORDER BY interactionTimestamp DESC")
    fun getAllFeedbackFlow(): Flow<List<RecommendationFeedbackEntity>>
    
    // ==================== Analytics Queries ====================
    
    /**
     * Get click-through rate for recommendations.
     * Returns ratio of clicked/total recommendations.
     */
    @Query("""
        SELECT CAST(SUM(CASE WHEN wasClicked = 1 THEN 1 ELSE 0 END) AS FLOAT) / 
               CAST(COUNT(*) AS FLOAT) 
        FROM recommendation_feedback
    """)
    suspend fun getClickThroughRate(): Float?
    
    /**
     * Get conversion rate (recommendations -> library adds).
     */
    @Query("""
        SELECT CAST(SUM(CASE WHEN wasAddedToLibrary = 1 THEN 1 ELSE 0 END) AS FLOAT) / 
               CAST(COUNT(*) AS FLOAT) 
        FROM recommendation_feedback
    """)
    suspend fun getConversionRate(): Float?
    
    /**
     * Get average AI confidence for successful recommendations.
     * Higher value = AI is accurately predicting user preferences.
     */
    @Query("""
        SELECT AVG(aiConfidence) FROM recommendation_feedback 
        WHERE wasAddedToLibrary = 1
    """)
    suspend fun getAverageSuccessfulConfidence(): Float?
    
    /**
     * Get count of total recommendations tracked.
     */
    @Query("SELECT COUNT(*) FROM recommendation_feedback")
    suspend fun getTotalRecommendationsCount(): Int
    
    /**
     * Get count of successful recommendations.
     */
    @Query("SELECT COUNT(*) FROM recommendation_feedback WHERE wasAddedToLibrary = 1")
    suspend fun getSuccessfulRecommendationsCount(): Int
    
    // ==================== Cleanup Operations ====================
    
    /**
     * Clear old feedback data (keep last N days).
     */
    @Query("DELETE FROM recommendation_feedback WHERE interactionTimestamp < :cutoffTimestamp")
    suspend fun clearOldFeedback(cutoffTimestamp: Long)
    
    /**
     * Clear all feedback data.
     */
    @Query("DELETE FROM recommendation_feedback")
    suspend fun clearAllFeedback()
}
