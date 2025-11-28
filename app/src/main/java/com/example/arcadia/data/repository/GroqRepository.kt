package com.example.arcadia.data.repository

import com.example.arcadia.data.local.StudioCacheManager
import com.example.arcadia.data.remote.GroqAIClient
import com.example.arcadia.data.remote.GroqApiService

/**
 * Groq AI Repository implementation using Kimi K2 model with fallbacks.
 * 
 * This class is now extremely simple - all business logic is in BaseAIRepository.
 * It just provides the GroqAIClient to the base class.
 * 
 * Model fallback chain:
 * 1. moonshotai/kimi-k2-instruct (primary - best quality)
 * 2. llama-3.3-70b-versatile (fallback - good quality)
 * 3. meta-llama/llama-4-scout-17b-16e-instruct (last resort)
 * 
 * Benefits:
 * - ~50 lines instead of ~1500 lines
 * - No code duplication with GeminiRepository
 * - Easy to maintain - changes go in BaseAIRepository
 * - Automatic model fallback handled by GroqAIClient
 * 
 * Usage:
 * ```kotlin
 * val repository = GroqRepository(groqApiService, studioCacheManager)
 * val suggestions = repository.suggestGames("games like Dark Souls")
 * ```
 */
class GroqRepository(
    groqApiService: GroqApiService,
    studioCacheManager: StudioCacheManager? = null
) : BaseAIRepository(
    aiClient = GroqAIClient(groqApiService),
    studioCacheManager = studioCacheManager
) {
    override val TAG: String = "GroqRepository"
}

/**
 * Type alias for backward compatibility.
 * Existing code using GroqRepositoryImpl will continue to work.
 */

