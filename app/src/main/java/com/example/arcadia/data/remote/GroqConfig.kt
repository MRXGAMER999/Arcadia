package com.example.arcadia.data.remote

import com.example.arcadia.BuildConfig

/**
 * Configuration for Groq API with Kimi K2 model
 */
object GroqConfig {
    
    const val BASE_URL = "https://api.groq.com/"
    const val MODEL_NAME = "moonshotai/kimi-k2-instruct-0905"
    
    val API_KEY: String get() = BuildConfig.GROQ_API_KEY
    
    /**
     * Configuration for JSON-structured responses (game suggestions)
     * Lower temperature (0.2) ensures valid JSON and accurate game titles.
     */
    object JsonModel {
        const val TEMPERATURE = 0.2f
        const val MAX_TOKENS = 16384
    }
    
    /**
     * Configuration for creative text responses (profile analysis)
     * Higher temperature (0.6) allows for more unique personality insights
     * while still maintaining the required JSON structure.
     */
    object TextModel {
        const val TEMPERATURE = 0.6f
        const val MAX_TOKENS = 16384
    }
}
