package com.example.arcadia.domain.repository

import com.example.arcadia.domain.model.ai.RoastInsights
import kotlinx.coroutines.flow.Flow

data class RoastWithTimestamp(
    val roast: RoastInsights,
    val badges: List<com.example.arcadia.domain.model.ai.Badge>,
    val generatedAt: Long
)

interface RoastRepository {
    fun getLastRoast(userId: String): Flow<RoastInsights?>
    fun getLastRoastWithTimestamp(userId: String): Flow<RoastWithTimestamp?>
    suspend fun saveRoast(userId: String, roast: RoastInsights, badges: List<com.example.arcadia.domain.model.ai.Badge>)
    suspend fun clearRoast(userId: String)
}
