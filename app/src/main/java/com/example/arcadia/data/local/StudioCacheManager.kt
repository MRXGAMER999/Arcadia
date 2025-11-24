package com.example.arcadia.data.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Multi-layer cache orchestrator for studio expansions.
 * L1: In-memory LRU cache (fastest)
 * L2: Room database (persistent)
 * L3: Hardcoded mappings (compile-time)
 */
class StudioCacheManager(
    private val database: StudioCacheDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private const val MAX_CACHE_SIZE = 50
        private const val CACHE_EXPIRATION_DAYS = 30L
    }

    // L1: In-memory LRU cache with max entries (thread-safe access via cacheLock)
    private val memoryCache = object : LinkedHashMap<String, CachedExpansion>(MAX_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedExpansion>): Boolean {
            return size > MAX_CACHE_SIZE
        }
    }
    private val cacheLock = Mutex()

    data class CachedExpansion(
        val displayNames: Set<String>,
        val slugs: String, // Comma-separated slugs for API
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Get cached expansion data (both names and slugs together to ensure consistency).
     */
    private suspend fun getCachedExpansion(parentStudio: String): CachedExpansion? = withContext(dispatcher) {
        val normalized = parentStudio.lowercase().trim()

        // L1: Memory cache (synchronous, fastest)
        cacheLock.withLock {
            memoryCache[normalized]?.let { cached ->
                if (!isExpired(cached.timestamp)) {
                    android.util.Log.d("StudioCacheManager", "L1 HIT (memory): $normalized")
                    return@withContext cached
                } else {
                    android.util.Log.d("StudioCacheManager", "L1 EXPIRED: $normalized")
                }
            }
        }

        // L3: Hardcoded (synchronous, compile-time)
        HardcodedStudioMappings.getSubsidiaryNames(normalized)?.let { names ->
            val slugs = HardcodedStudioMappings.getSubsidiarySlugs(normalized) ?: ""
            val cached = CachedExpansion(names, slugs)
            cacheLock.withLock { 
                memoryCache[normalized] = cached
            }
            android.util.Log.d("StudioCacheManager", "L3 HIT (hardcoded): $normalized")
            return@withContext cached
        }

        // L2: Room database (async)
        val dbResult = database.studioDao().getExpansion(normalized)
        if (dbResult != null && !isExpired(dbResult.timestamp)) {
            val cached = CachedExpansion(
                dbResult.subsidiaries.toSet(), 
                dbResult.slugs ?: "",
                dbResult.timestamp
            )
            cacheLock.withLock { 
                memoryCache[normalized] = cached
            }
            android.util.Log.d("StudioCacheManager", "L2 HIT (Room): $normalized - ${cached.displayNames.size} studios")
            return@withContext cached
        }

        android.util.Log.d("StudioCacheManager", "CACHE MISS: $normalized")
        null // Cache miss - need Gemini
    }

    /**
     * Get expanded studio display names from cache layers.
     */
    suspend fun getExpandedStudios(parentStudio: String): Set<String>? {
        return getCachedExpansion(parentStudio)?.displayNames
    }

    /**
     * Get comma-separated slugs for RAWG API filtering.
     */
    suspend fun getStudioSlugs(parentStudio: String): String? {
        return getCachedExpansion(parentStudio)?.slugs?.takeIf { it.isNotEmpty() }
    }

    /**
     * Get both display names and slugs in a single cache lookup.
     * More efficient when both values are needed.
     */
    suspend fun getExpansionData(parentStudio: String): Pair<Set<String>, String>? {
        val cached = getCachedExpansion(parentStudio) ?: return null
        val slugs = cached.slugs.takeIf { it.isNotEmpty() } ?: return null
        return cached.displayNames to slugs
    }

    /**
     * Cache expansion result in both L1 and L2 layers.
     */
    suspend fun cacheExpansion(
        parentStudio: String, 
        displayNames: Set<String>,
        slugs: String
    ) = withContext(dispatcher) {
        val normalized = parentStudio.lowercase().trim()
        val timestamp = System.currentTimeMillis()

        android.util.Log.d("StudioCacheManager", "CACHING: $normalized - ${displayNames.size} studios to L1+L2")

        coroutineScope {
            launch {
                cacheLock.withLock {
                    memoryCache[normalized] = CachedExpansion(displayNames, slugs, timestamp)
                }
                android.util.Log.d("StudioCacheManager", "L1 WRITE (memory): $normalized")
            }
            launch {
                database.studioDao().insertExpansion(
                    StudioExpansionEntity(normalized, displayNames.toList(), timestamp, slugs)
                )
                android.util.Log.d("StudioCacheManager", "L2 WRITE (Room): $normalized")
            }
        }
    }

    private fun isExpired(timestamp: Long): Boolean {
        val expirationMs = CACHE_EXPIRATION_DAYS * 24 * 60 * 60 * 1000
        return System.currentTimeMillis() - timestamp > expirationMs
    }

    /**
     * Prefetch popular studios on app startup.
     * Thread-safe: acquires cacheLock before modifying memoryCache.
     */
    fun prefetchPopularStudios() {
        runBlocking {
            cacheLock.withLock {
                HardcodedStudioMappings.getAllMappings().forEach { (parent, studioList) ->
                    val names = studioList.map { it.displayName }.toSet()
                    val slugs = studioList.joinToString(",") { it.slug }
                    memoryCache[parent] = CachedExpansion(names, slugs)
                }
            }
        }
        android.util.Log.d("StudioCacheManager", "Prefetched ${HardcodedStudioMappings.getAllMappings().size} popular studios")
    }

    suspend fun cleanupExpiredCache() = withContext(dispatcher) {
        val expirationTime = System.currentTimeMillis() - (CACHE_EXPIRATION_DAYS * 24 * 60 * 60 * 1000)
        database.studioDao().deleteExpired(expirationTime)
    }
}
