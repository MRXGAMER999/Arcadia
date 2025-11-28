package com.example.arcadia.data.repository

import com.example.arcadia.data.local.StudioCacheManager
import com.example.arcadia.data.remote.GeminiAIClient

/**
 * Gemini AI Repository implementation.
 * 
 * This class is now extremely simple - all business logic is in BaseAIRepository.
 * It just provides the GeminiAIClient to the base class.
 * 
 * Benefits:
 * - ~50 lines instead of ~1200 lines
 * - No code duplication with GroqRepository
 * - Easy to maintain - changes go in BaseAIRepository
 * - Easy to test - just mock AIClient
 * 
 * Usage:
 * ```kotlin
 * val repository = GeminiRepository(studioCacheManager)
 * val suggestions = repository.suggestGames("games like Dark Souls")
 * ```
 */
class GeminiRepository(
    studioCacheManager: StudioCacheManager? = null
) : BaseAIRepository(
    aiClient = GeminiAIClient(),
    studioCacheManager = studioCacheManager
) {
    override val TAG: String = "GeminiRepository"
}

/**
 * Type alias for backward compatibility.
 * Existing code using GeminiRepositoryImpl will continue to work.
 */

