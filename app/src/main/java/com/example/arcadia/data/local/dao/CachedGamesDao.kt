package com.example.arcadia.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.arcadia.data.local.entity.AIRecommendationRemoteKey
import com.example.arcadia.data.local.entity.CachedGameEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for cached games.
 * 
 * Provides:
 * - PagingSource for Paging 3 integration (AI recommendations)
 * - CRUD operations for game cache
 * - Remote key management for RemoteMediator
 */
@Dao
interface CachedGamesDao {
    
    // ==================== AI Recommendations PagingSource ====================
    
    /**
     * Returns a PagingSource for AI recommendations, ordered by AI confidence (highest first).
     * Used by Paging 3 to efficiently load pages of recommendations.
     */
    @Query("""
        SELECT * FROM cached_games 
        WHERE isAIRecommendation = 1 
        ORDER BY aiRecommendationOrder ASC
    """)
    fun getAIRecommendationsPagingSource(): PagingSource<Int, CachedGameEntity>
    
    /**
     * Returns all AI recommendations as a Flow for observing changes.
     * Useful for getting the current state without paging.
     */
    @Query("""
        SELECT * FROM cached_games 
        WHERE isAIRecommendation = 1 
        ORDER BY aiRecommendationOrder ASC
    """)
    fun getAIRecommendationsFlow(): Flow<List<CachedGameEntity>>
    
    /**
     * Get count of cached AI recommendations.
     */
    @Query("SELECT COUNT(*) FROM cached_games WHERE isAIRecommendation = 1")
    suspend fun getAIRecommendationsCount(): Int
    
    // ==================== Insert/Update Operations ====================
    
    /**
     * Insert a single game, replacing if exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: CachedGameEntity)
    
    /**
     * Insert multiple games, replacing if exists.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<CachedGameEntity>)
    
    // ==================== Query Operations ====================
    
    /**
     * Get a specific game by ID.
     */
    @Query("SELECT * FROM cached_games WHERE id = :gameId")
    suspend fun getGameById(gameId: Int): CachedGameEntity?
    
    /**
     * Check if a game is cached.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM cached_games WHERE id = :gameId)")
    suspend fun isGameCached(gameId: Int): Boolean
    
    /**
     * Get games by IDs (useful for batch lookups).
     */
    @Query("SELECT * FROM cached_games WHERE id IN (:gameIds)")
    suspend fun getGamesByIds(gameIds: List<Int>): List<CachedGameEntity>
    
    // ==================== Delete Operations ====================
    
    /**
     * Clear all AI recommendations (called before refresh).
     */
    @Query("DELETE FROM cached_games WHERE isAIRecommendation = 1")
    suspend fun clearAIRecommendations()
    
    /**
     * Clear all cached games.
     */
    @Query("DELETE FROM cached_games")
    suspend fun clearAll()
    
    /**
     * Delete games cached before a certain time.
     */
    @Query("DELETE FROM cached_games WHERE cachedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
    
    /**
     * Delete games that were cached with a different library hash.
     * Useful for invalidating stale recommendations.
     */
    @Query("DELETE FROM cached_games WHERE isAIRecommendation = 1 AND libraryHashWhenCached != :currentHash")
    suspend fun deleteStaleRecommendations(currentHash: Int)
    
    // ==================== Remote Key Operations ====================
    
    /**
     * Get the remote key for AI recommendations.
     */
    @Query("SELECT * FROM ai_recommendation_remote_keys WHERE id = 0")
    suspend fun getAIRemoteKey(): AIRecommendationRemoteKey?
    
    /**
     * Insert or update the remote key.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRemoteKey(remoteKey: AIRecommendationRemoteKey)
    
    /**
     * Clear the remote key (called on full refresh).
     */
    @Query("DELETE FROM ai_recommendation_remote_keys")
    suspend fun clearRemoteKey()
    
    // ==================== Transaction Operations ====================
    
    /**
     * Atomically clear old recommendations and insert new ones.
     * This ensures the UI never sees partial data.
     */
    @Transaction
    suspend fun refreshAIRecommendations(
        games: List<CachedGameEntity>,
        remoteKey: AIRecommendationRemoteKey
    ) {
        clearAIRecommendations()
        clearRemoteKey()
        insertGames(games)
        insertRemoteKey(remoteKey)
    }
    
    /**
     * Append more recommendations to existing cache.
     */
    @Transaction
    suspend fun appendAIRecommendations(
        games: List<CachedGameEntity>,
        remoteKey: AIRecommendationRemoteKey
    ) {
        insertGames(games)
        insertRemoteKey(remoteKey)
    }
}
