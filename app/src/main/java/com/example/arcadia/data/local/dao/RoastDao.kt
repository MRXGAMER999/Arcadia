package com.example.arcadia.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.arcadia.data.local.entity.RoastEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for roast storage.
 * 
 * Uses a single-row pattern where only one roast is stored at a time.
 * New roasts replace the previous one automatically via REPLACE conflict strategy.
 */
@Dao
interface RoastDao {
    
    /**
     * Get the last saved roast as a Flow for reactive updates.
     * Returns null if no roast has been saved.
     */
    @Query("SELECT * FROM roast_table WHERE userId = :userId")
    fun getLastRoast(userId: String): Flow<RoastEntity?>
    
    /**
     * Save a roast, replacing any existing roast.
     * Uses REPLACE strategy to ensure only one roast exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRoast(roast: RoastEntity)
    
    /**
     * Clear the saved roast.
     */
    @Query("DELETE FROM roast_table WHERE userId = :userId")
    suspend fun clearRoast(userId: String)
}
