package com.example.arcadia.data.remote

import com.example.arcadia.BuildConfig

/**
 * Configuration for Groq API with multiple model fallbacks
 * Upgraded from Llama 4 Scout (preview) to Llama 3.3 70B (production)
 * for significantly better reasoning and recommendation quality.
 */
object GroqConfig {
    
    const val BASE_URL = "https://api.groq.com/"
    
    // Primary model - Kimi K2 (best quality but has rate limits)
    const val MODEL_NAME = "moonshotai/kimi-k2-instruct-0905"
    
    // Fallback models in order of preference (used when primary fails)
    val FALLBACK_MODELS = listOf(
        "llama-3.3-70b-versatile",
        "meta-llama/llama-4-scout-17b-16e-instruct"
    )
    
    val API_KEY: String get() = BuildConfig.GROQ_API_KEY
    
    /**
     * Configuration for JSON-structured responses (game suggestions)
     * Lower temperature (0.2) ensures valid JSON and accurate game titles.
     */
    object JsonModel {
        const val TEMPERATURE = 0.2f
        const val MAX_TOKENS = 16384
        const val TOP_P = 0.9f
    }
    
    /**
     * Configuration for library-based recommendations.
     * Balanced settings for diverse yet accurate game picks:
     * - Temperature 0.4: More creative than JSON (0.2) but still grounded
     * - Top_P 0.85: Nucleus sampling to filter unlikely tokens
     * - This produces varied recommendations while avoiding hallucinations
     */
    object RecommendationModel {
        const val TEMPERATURE = 0.4f
        const val MAX_TOKENS = 16384
        const val TOP_P = 0.85f
    }
    
    /**
     * Configuration for creative text responses (profile analysis)
     * Higher temperature (0.6) allows for more unique personality insights
     * while still maintaining the required JSON structure.
     */
    object TextModel {
        const val TEMPERATURE = 0.6f
        const val MAX_TOKENS = 16384
        const val TOP_P = 0.95f
    }
}
