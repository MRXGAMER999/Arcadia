package com.example.arcadia.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Request Deduplicator - prevents duplicate simultaneous network requests.
 * 
 * When multiple callers request the same resource at the same time,
 * only one actual network request is made and the result is shared.
 * 
 * This implementation uses CompletableDeferred which is not tied to a
 * coroutine scope, preventing cancellation from propagating to other waiters.
 * 
 * Usage:
 * ```
 * val result = deduplicator.dedupe("games_popular_page_1") {
 *     apiService.getGames(page = 1)
 * }
 * ```
 */
class RequestDeduplicator {
    
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<*>>()
    private val mutex = Mutex()
    
    /**
     * Deduplicate a request by key. If a request with the same key is already
     * in flight, wait for its result instead of making a new request.
     * 
     * @param key Unique identifier for this request (e.g., "games_popular_1")
     * @param block The suspend function that performs the actual request
     * @return The result of the request
     */
    private sealed class DedupeResult<T> {
        data class FoundExisting<T>(val deferred: Deferred<T>) : DedupeResult<T>()
        data class CreatedNew<T>(val deferred: CompletableDeferred<T>) : DedupeResult<T>()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> dedupe(key: String, block: suspend () -> T): T {
        // Check if there's already an in-flight request
        val existing = inFlightRequests[key] as? Deferred<T>
        if (existing != null && existing.isActive) {
            try {
                return existing.await()
            } catch (e: CancellationException) {
                // The existing request was cancelled, we'll create a new one below
            }
        }
        
        // Use mutex to prevent race conditions when creating new requests
        val dedupeResult = mutex.withLock {
            // Double-check after acquiring lock
            val existingAfterLock = inFlightRequests[key] as? Deferred<T>
            if (existingAfterLock != null && existingAfterLock.isActive) {
                DedupeResult.FoundExisting(existingAfterLock)
            } else {
                // Create new CompletableDeferred (not tied to any scope)
                val newDeferred = CompletableDeferred<T>()
                inFlightRequests[key] = newDeferred
                DedupeResult.CreatedNew(newDeferred)
            }
        }
        
        return when (dedupeResult) {
            is DedupeResult.FoundExisting -> {
                try {
                    dedupeResult.deferred.await()
                } catch (e: CancellationException) {
                    // Check if the current coroutine is cancelled. If so, rethrow.
                    currentCoroutineContext().ensureActive()
                    // The existing request was cancelled, but we are still active.
                    // Retry with a fresh request
                    dedupe(key, block)
                }
            }
            is DedupeResult.CreatedNew -> {
                val completableDeferred = dedupeResult.deferred
                try {
                    val result = block()
                    completableDeferred.complete(result)
                    result
                } catch (e: CancellationException) {
                    // Don't propagate cancellation to other waiters - let them retry
                    completableDeferred.cancel(e)
                    throw e
                } catch (e: Exception) {
                    completableDeferred.completeExceptionally(e)
                    throw e
                } finally {
                    // Only remove if this is still the same request (not replaced)
                    inFlightRequests.remove(key, completableDeferred)
                }
            }
        }
    }
    
    /**
     * Cancel and remove a specific in-flight request.
     */
    fun cancel(key: String) {
        inFlightRequests.remove(key)?.cancel()
    }
    
    /**
     * Cancel all in-flight requests.
     */
    fun cancelAll() {
        inFlightRequests.values.forEach { it.cancel() }
        inFlightRequests.clear()
    }
    
    /**
     * Check if a request with the given key is currently in flight.
     */
    fun isInFlight(key: String): Boolean {
        return inFlightRequests[key]?.isActive == true
    }
    
    /**
     * Get the count of currently in-flight requests.
     */
    fun inFlightCount(): Int {
        return inFlightRequests.count { it.value.isActive }
    }
}
