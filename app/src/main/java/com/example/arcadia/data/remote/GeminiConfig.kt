package com.example.arcadia.data.remote

import com.example.arcadia.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig

/**
 * Configuration object for Gemini AI model settings.
 * Centralizes all AI-related configuration values and model creation.
 */
object GeminiConfig {
    
    const val MODEL_NAME = "gemini-2.0-flash"
    
    /**
     * Configuration for JSON-structured responses (game suggestions).
     * Uses lower temperature for more accurate, predictable results.
     */
    object JsonModel {
        const val TEMPERATURE = 0.4f
        const val TOP_K = 32
        const val TOP_P = 0.9f
        const val MAX_OUTPUT_TOKENS = 2048
        const val RESPONSE_MIME_TYPE = "application/json"
    }
    
    /**
     * Configuration for creative text responses (profile analysis).
     * Uses higher temperature for more creative, varied results.
     */
    object TextModel {
        const val TEMPERATURE = 0.8f
        const val TOP_K = 40
        const val TOP_P = 0.95f
        const val MAX_OUTPUT_TOKENS = 2048
    }
    
    /**
     * Creates a GenerativeModel configured for JSON-structured responses.
     * Best for game suggestions where structured output is needed.
     */
    fun createJsonModel(): GenerativeModel {
        return GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = JsonModel.TEMPERATURE
                topK = JsonModel.TOP_K
                topP = JsonModel.TOP_P
                maxOutputTokens = JsonModel.MAX_OUTPUT_TOKENS
                responseMimeType = JsonModel.RESPONSE_MIME_TYPE
            }
        )
    }
    
    /**
     * Creates a GenerativeModel configured for creative text responses.
     * Best for profile analysis where natural language output is needed.
     */
    fun createTextModel(): GenerativeModel {
        return GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = TextModel.TEMPERATURE
                topK = TextModel.TOP_K
                topP = TextModel.TOP_P
                maxOutputTokens = TextModel.MAX_OUTPUT_TOKENS
            }
        )
    }
}
