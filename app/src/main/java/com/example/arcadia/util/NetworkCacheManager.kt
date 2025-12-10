package com.example.arcadia.util

import android.util.LruCache
import java.util.concurrent.ConcurrentHashMap

/**
 * Network Cache Manager - provides in-memory caching for API responses.
 * 
 * This cache sits between the ViewModel and Repository layers to provide
 * instant access to previously fetched data without hitting the network.
 * 
 * Features:
 * - LRU (Least Recently Used) eviction policy
 * - Time-based expiration
 * - Thread-safe operations
 * - Type-safe cache entries
 */
class NetworkCacheManager {
    
    companion object {
        private const val DEFAULT_CACHE_SIZE = 50 // Number of entries
        private const val DEFAULT_CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
        private const val SHORT_CACHE_DURATION_MS = 2 * 60 * 1000L // 2 minutes
        private const val LONG_CACHE_DURATION_MS = 15 * 60 * 1000L // 15 minutes
    }
    
    // Cache entry with timestamp
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long,
        val expirationMs: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > expirationMs
    }
    
    // Main cache using LruCache for automatic eviction
    private val cache = object : LruCache<String, CacheEntry<*>>(DEFAULT_CACHE_SIZE) {
        override fun sizeOf(key: String, value: CacheEntry<*>): Int = 1
    }
    
    // Track cache statistics for debugging
    private val stats = CacheStats()
    
    /**
     * Get a cached value if it exists and hasn't expired.
     * 
     * @param key The cache key
     * @return The cached value, or null if not found or expired
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cache.get(key) as? CacheEntry<T>
        
        return when {
            entry == null -> {
                stats.misses++
                null
            }
            entry.isExpired() -> {
                cache.remove(key)
                stats.expirations++
                null
            }
            else -> {
                stats.hits++
                entry.data
            }
        }
    }
    
    /**
     * Put a value in the cache with default expiration.
     * 
     * @param key The cache key
     * @param value The value to cache
     * @param durationMs Cache duration in milliseconds (default: 5 minutes)
     */
    fun <T> put(key: String, value: T, durationMs: Long = DEFAULT_CACHE_DURATION_MS) {
        cache.put(key, CacheEntry(value, System.currentTimeMillis(), durationMs))
    }
    
    /**
     * Put a value with short-term caching (2 minutes).
     * Good for frequently changing data.
     */
    fun <T> putShort(key: String, value: T) {
        put(key, value, SHORT_CACHE_DURATION_MS)
    }
    
    /**
     * Put a value with long-term caching (15 minutes).
     * Good for rarely changing data like game details.
     */
    fun <T> putLong(key: String, value: T) {
        put(key, value, LONG_CACHE_DURATION_MS)
    }
    
    /**
     * Get or compute a value. If the value is not cached or expired,
     * compute it using the provided block and cache the result.
     * 
     * @param key The cache key
     * @param durationMs Cache duration in milliseconds
     * @param compute Block to compute the value if not cached
     * @return The cached or computed value
     */
    suspend fun <T> getOrPut(
        key: String,
        durationMs: Long = DEFAULT_CACHE_DURATION_MS,
        compute: suspend () -> T
    ): T {
        get<T>(key)?.let { return it }
        
        val value = compute()
        put(key, value, durationMs)
        return value
    }
    
    /**
     * Remove a specific entry from the cache.
     */
    fun remove(key: String) {
        cache.remove(key)
    }
    
    /**
     * Remove all entries matching a prefix.
     * Useful for invalidating related cache entries.
     */
    fun removeByPrefix(prefix: String) {
        cache.snapshot().keys
            .filter { it.startsWith(prefix) }
            .forEach { cache.remove(it) }
    }
    
    /**
     * Clear all cached entries.
     */
    fun clear() {
        cache.evictAll()
        stats.reset()
    }
    
    /**
     * Get cache statistics for debugging.
     */
    fun getStats(): CacheStats = stats.copy()
    
    /**
     * Get the current number of cached entries.
     */
    fun size(): Int = cache.size()
    
    /**
     * Check if a key exists in the cache (even if expired).
     */
    fun contains(key: String): Boolean = cache.get(key) != null
    
    /**
     * Cache statistics for monitoring performance.
     */
    data class CacheStats(
        var hits: Int = 0,
        var misses: Int = 0,
        var expirations: Int = 0
    ) {
        val hitRate: Float
            get() = if (hits + misses > 0) hits.toFloat() / (hits + misses) else 0f
        
        fun reset() {
            hits = 0
            misses = 0
            expirations = 0
        }
        
        fun copy() = CacheStats(hits, misses, expirations)
    }
}

/**
 * Cache key builders for consistent key generation.
 */
object CacheKeys {
    // Game list cache keys
    fun popularGames(page: Int, pageSize: Int) = "games_popular_${page}_$pageSize"
    fun upcomingGames(page: Int, pageSize: Int) = "games_upcoming_${page}_$pageSize"
    fun newReleases(page: Int, pageSize: Int) = "games_new_${page}_$pageSize"
    fun recommendedGames(page: Int, pageSize: Int) = "games_recommended_${page}_$pageSize"
    fun gamesByGenre(genreId: Int, page: Int, pageSize: Int) = "games_genre_${genreId}_${page}_$pageSize"
    fun searchGames(query: String, page: Int, pageSize: Int) = "games_search_${query.hashCode()}_${page}_$pageSize"
    fun filteredGames(hash: Int, page: Int) = "games_filtered_${hash}_$page"
    fun studioGames(slugs: String, page: Int, pageSize: Int) = "games_studio_${slugs.hashCode()}_${page}_$pageSize"
    
    // Game details cache keys
    fun gameDetails(gameId: Int) = "game_details_$gameId"
    fun gameDetailsWithMedia(gameId: Int) = "game_details_media_$gameId"
    fun gameScreenshots(gameId: Int) = "game_screenshots_$gameId"
    fun gameVideos(gameId: Int) = "game_videos_$gameId"
    
    // Prefixes for bulk invalidation
    const val GAMES_PREFIX = "games_"
    const val GAME_DETAILS_PREFIX = "game_details_"
}
