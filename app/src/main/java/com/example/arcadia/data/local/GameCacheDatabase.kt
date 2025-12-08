package com.example.arcadia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.arcadia.data.local.dao.CachedGamesDao
import com.example.arcadia.data.local.dao.RecommendationFeedbackDao
import com.example.arcadia.data.local.dao.RoastDao
import com.example.arcadia.data.local.entity.AIRecommendationRemoteKey
import com.example.arcadia.data.local.entity.CachedGameEntity
import com.example.arcadia.data.local.entity.RecommendationFeedbackEntity
import com.example.arcadia.data.local.entity.RoastEntity

/**
 * Room database for caching game data and recommendation feedback.
 * 
 * This database stores:
 * - AI recommendations with full game details (for offline support)
 * - Remote keys for Paging 3 RemoteMediator state
 * - Recommendation feedback for AI improvement loop
 * - User's last generated roast
 * 
 * Separate from StudioCacheDatabase to keep concerns separate and allow
 * independent versioning/migration strategies.
 */
@Database(
    entities = [
        CachedGameEntity::class,
        AIRecommendationRemoteKey::class,
        RecommendationFeedbackEntity::class,
        RoastEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class GameCacheDatabase : RoomDatabase() {
    
    abstract fun cachedGamesDao(): CachedGamesDao
    abstract fun recommendationFeedbackDao(): RecommendationFeedbackDao
    abstract fun roastDao(): RoastDao
    
    companion object {
        @Volatile
        private var INSTANCE: GameCacheDatabase? = null
        
        /**
         * Get singleton instance of the database.
         * Uses double-checked locking for thread safety.
         */
        fun getInstance(context: Context): GameCacheDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): GameCacheDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GameCacheDatabase::class.java,
                "game_cache_db"
            )
                // OK for cache DB - data can be re-fetched
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
        
        /**
         * Clear the singleton instance (useful for testing).
         */
        fun clearInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}
