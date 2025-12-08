
package com.example.arcadia.data.repository

import android.util.Log
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.ai.AIGameSuggestions
import com.example.arcadia.domain.model.ai.GameInsights
import com.example.arcadia.domain.model.ai.StreamingInsights
import com.example.arcadia.domain.model.ai.StudioExpansionResult
import com.example.arcadia.domain.model.ai.StudioMatch
import com.example.arcadia.domain.model.ai.StudioSearchResult
import com.example.arcadia.domain.repository.AIRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll

/**
 * Fallback AI Repository that uses Groq as primary and Gemini as fallback.
 * 
 * Strategy:
 * - Try Groq first (faster, cheaper)
 * - On failure, automatically switch to Gemini
 * - Log all fallback events for monitoring
 * 
 * This ensures high availability of AI features even when one provider is down.
 */
class FallbackAIRepository(
    private val primaryRepository: AIRepository,  // Groq
    private val fallbackRepository: AIRepository  // Gemini
) : AIRepository {

    companion object {
        private const val TAG = "FallbackAIRepository"
    }

    // ==================== Game Suggestions ====================

    override suspend fun suggestGames(
        userQuery: String,
        count: Int,
        forceRefresh: Boolean
    ): Result<AIGameSuggestions> {
        return tryWithFallback("suggestGames") {
            primaryRepository.suggestGames(userQuery, count, forceRefresh)
        } ?: fallbackRepository.suggestGames(userQuery, count, forceRefresh)
    }

    override suspend fun getLibraryBasedRecommendations(
        games: List<GameListEntry>,
        count: Int,
        forceRefresh: Boolean,
        excludeGames: List<String>
    ): Result<AIGameSuggestions> {
        return tryWithFallbackAndRetry("getLibraryBasedRecommendations") {
            primaryRepository.getLibraryBasedRecommendations(games, count, forceRefresh, excludeGames)
        } ?: fallbackRepository.getLibraryBasedRecommendations(games, count, forceRefresh, excludeGames)
    }

    // ==================== Profile Analysis ====================

    override suspend fun analyzeGamingProfile(games: List<GameListEntry>): Result<GameInsights> {
        return tryWithFallback("analyzeGamingProfile") {
            primaryRepository.analyzeGamingProfile(games)
        } ?: fallbackRepository.analyzeGamingProfile(games)
    }

    override fun analyzeGamingProfileStreaming(games: List<GameListEntry>): Flow<StreamingInsights> {
        // For streaming, we try primary first and catch errors to switch to fallback
        return primaryRepository.analyzeGamingProfileStreaming(games)
            .catch { e ->
                Log.w(TAG, "Primary streaming failed, switching to fallback: ${e.message}")
                emitAll(fallbackRepository.analyzeGamingProfileStreaming(games))
            }
    }

    // ==================== Studio Expansion ====================

    override suspend fun getExpandedStudios(parentStudio: String): Set<String> {
        return try {
            primaryRepository.getExpandedStudios(parentStudio)
        } catch (e: Exception) {
            Log.w(TAG, "Primary getExpandedStudios failed, using fallback: ${e.message}")
            fallbackRepository.getExpandedStudios(parentStudio)
        }
    }

    override suspend fun getStudioSlugs(parentStudio: String): String {
        return try {
            primaryRepository.getStudioSlugs(parentStudio)
        } catch (e: Exception) {
            Log.w(TAG, "Primary getStudioSlugs failed, using fallback: ${e.message}")
            fallbackRepository.getStudioSlugs(parentStudio)
        }
    }

    override suspend fun getStudioExpansionResult(parentStudio: String): StudioExpansionResult {
        return try {
            primaryRepository.getStudioExpansionResult(parentStudio)
        } catch (e: Exception) {
            Log.w(TAG, "Primary getStudioExpansionResult failed, using fallback: ${e.message}")
            fallbackRepository.getStudioExpansionResult(parentStudio)
        }
    }

    override suspend fun expandMultipleStudios(studios: List<String>): Map<String, StudioExpansionResult> {
        return try {
            primaryRepository.expandMultipleStudios(studios)
        } catch (e: Exception) {
            Log.w(TAG, "Primary expandMultipleStudios failed, using fallback: ${e.message}")
            fallbackRepository.expandMultipleStudios(studios)
        }
    }

    override suspend fun searchStudios(
        query: String,
        includePublishers: Boolean,
        includeDevelopers: Boolean,
        limit: Int
    ): StudioSearchResult {
        return try {
            primaryRepository.searchStudios(query, includePublishers, includeDevelopers, limit)
        } catch (e: Exception) {
            Log.w(TAG, "Primary searchStudios failed, using fallback: ${e.message}")
            fallbackRepository.searchStudios(query, includePublishers, includeDevelopers, limit)
        }
    }

    override fun getLocalStudioSuggestions(query: String, limit: Int): List<StudioMatch> {
        // Local suggestions don't need fallback - they're synchronous and local
        return try {
            primaryRepository.getLocalStudioSuggestions(query, limit)
        } catch (e: Exception) {
            fallbackRepository.getLocalStudioSuggestions(query, limit)
        }
    }

    // ==================== Cache Management ====================

    override fun clearCache() {
        primaryRepository.clearCache()
        fallbackRepository.clearCache()
    }

    // ==================== Helper ====================

    /**
     * Tries the primary operation and returns null if it fails,
     * allowing the caller to fall back to secondary.
     */
    private suspend fun <T> tryWithFallback(
        operationName: String,
        primaryOperation: suspend () -> Result<T>
    ): Result<T>? {
        return try {
            val result = primaryOperation()
            if (result.isSuccess) {
                result
            } else {
                // Check if it's a recoverable error (network, rate limit, etc.)
                val error = result.exceptionOrNull()
                Log.w(TAG, "Primary $operationName returned failure: ${error?.message}")
                null // Signal to use fallback
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.w(TAG, "Primary $operationName threw exception, switching to fallback: ${e.message}")
            null // Signal to use fallback
        }
    }
    
    /**
     * Tries the primary operation with retry logic for rate limits.
     * If rate limited, waits and retries before falling back to secondary.
     */
    private suspend fun <T> tryWithFallbackAndRetry(
        operationName: String,
        primaryOperation: suspend () -> Result<T>
    ): Result<T>? {
        var attempts = 0
        val maxAttempts = 2
        
        while (attempts < maxAttempts) {
            attempts++
            try {
                val result = primaryOperation()
                if (result.isSuccess) return result
                
                val error = result.exceptionOrNull()
                val errorMessage = error?.message?.lowercase() ?: ""
                
                // If rate limited, wait and retry
                if ((errorMessage.contains("rate") || errorMessage.contains("429")) && attempts < maxAttempts) {
                    Log.w(TAG, "Primary $operationName rate limited (attempt $attempts), waiting 3s before retry")
                    kotlinx.coroutines.delay(3000)
                    continue
                }
                
                Log.w(TAG, "Primary $operationName returned failure: ${error?.message}")
                return null
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                
                val errorMessage = e.message?.lowercase() ?: ""
                if ((errorMessage.contains("rate") || errorMessage.contains("429")) && attempts < maxAttempts) {
                    Log.w(TAG, "Primary $operationName rate limited (exception attempt $attempts), waiting 3s before retry")
                    kotlinx.coroutines.delay(3000)
                    continue
                }
                
                Log.w(TAG, "Primary $operationName threw exception, switching to fallback: ${e.message}")
                return null
            }
        }
        return null
    }
}
