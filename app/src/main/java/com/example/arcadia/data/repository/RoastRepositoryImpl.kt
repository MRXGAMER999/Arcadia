package com.example.arcadia.data.repository

import com.example.arcadia.data.local.dao.RoastDao
import com.example.arcadia.data.local.entity.toEntity
import com.example.arcadia.data.local.entity.toRoastInsights
import com.example.arcadia.domain.model.ai.RoastInsights
import com.example.arcadia.domain.repository.RoastRepository
import com.example.arcadia.domain.repository.RoastWithTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of RoastRepository using Room database for local storage.
 * 
 * Uses a single-row pattern where only one roast is stored at a time.
 * New roasts automatically replace the previous one.
 */
class RoastRepositoryImpl(
    private val roastDao: RoastDao
) : RoastRepository {
    
    /**
     * Get the last saved roast as a Flow for reactive updates.
     * Converts the Room entity to domain model.
     * 
     * @return Flow emitting the last saved RoastInsights or null
     */
    override fun getLastRoast(): Flow<RoastInsights?> {
        return roastDao.getLastRoast().map { entity ->
            entity?.toRoastInsights()
        }
    }
    
    /**
     * Get the last saved roast with its generation timestamp.
     * Converts the Room entity to domain model with timestamp.
     * 
     * @return Flow emitting the last saved RoastWithTimestamp or null
     */
    override fun getLastRoastWithTimestamp(): Flow<RoastWithTimestamp?> {
        return roastDao.getLastRoast().map { entity ->
            entity?.let {
                RoastWithTimestamp(
                    roast = it.toRoastInsights(),
                    generatedAt = it.generatedAt
                )
            }
        }
    }
    
    /**
     * Save a roast to local storage, replacing any existing roast.
     * Converts the domain model to Room entity before saving.
     * 
     * @param roast The RoastInsights to save
     */
    override suspend fun saveRoast(roast: RoastInsights) {
        roastDao.saveRoast(roast.toEntity())
    }
    
    /**
     * Clear the saved roast from local storage.
     */
    override suspend fun clearRoast() {
        roastDao.clearRoast()
    }
}
