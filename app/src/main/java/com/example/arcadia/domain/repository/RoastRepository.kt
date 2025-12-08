package com.example.arcadia.domain.repository

import com.example.arcadia.domain.model.ai.RoastInsights
import kotlinx.coroutines.flow.Flow

/**
 * Wrapper class containing roast insights with generation timestamp.
 */
data class RoastWithTimestamp(
    val roast: RoastInsights,
    val generatedAt: Long
)

/**
 * Repository interface for managing roast data persistence.
 * 
 * Provides methods to save, retrieve, and clear the user's last generated roast.
 * Uses a single-row pattern where only one roast is stored at a time.
 */
interface RoastRepository {
    
    /**
     * Get the last saved roast as a Flow for reactive updates.
     * Returns null if no roast has been saved.
     * 
     * @return Flow emitting the last saved RoastInsights or null
     */
    fun getLastRoast(): Flow<RoastInsights?>
    
    /**
     * Get the last saved roast with its generation timestamp.
     * Returns null if no roast has been saved.
     * 
     * @return Flow emitting the last saved RoastWithTimestamp or null
     */
    fun getLastRoastWithTimestamp(): Flow<RoastWithTimestamp?>
    
    /**
     * Save a roast to local storage, replacing any existing roast.
     * 
     * @param roast The RoastInsights to save
     */
    suspend fun saveRoast(roast: RoastInsights)
    
    /**
     * Clear the saved roast from local storage.
     */
    suspend fun clearRoast()
}
