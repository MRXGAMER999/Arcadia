package com.example.arcadia.data.remote

import com.example.arcadia.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig

/**
 * Configuration object for Gemini AI model settings.
 * Centralizes all AI-related configuration values and model creation.
 * 
 * Model options:
 * - "gemini-2.5-flash-lite": Fast, no thinking mode, good for most use cases
 * - "gemini-2.5-flash": Has "thinking" mode enabled by default (uses extra tokens for reasoning)
 * - "gemini-2.0-flash": Stable model without thinking mode
 * - "gemini-1.5-flash": Legacy stable model
 * 
 * Note: gemini-2.5-flash uses thinking mode by default which can interfere with
 * response parsing. Use gemini-2.5-flash-lite or gemini-2.0-flash for more predictable output.
 */
object GeminiConfig {
    
    // Use gemini-2.0-flash for stable behavior without thinking mode overhead
    // Change to "gemini-2.5-flash-lite" if you want the 2.5 generation without thinking
    const val MODEL_NAME = "gemini-2.5-flash"
    
    /**
     * Configuration for JSON-structured responses (game suggestions).
     * Uses lower temperature for more accurate, predictable results.
     */
    object JsonModel {
        const val TEMPERATURE = 0.2f
        const val TOP_K = 32
        const val TOP_P = 0.9f
        const val MAX_OUTPUT_TOKENS = 16384
        const val RESPONSE_MIME_TYPE = "application/json"
    }
    
    /**
     * Configuration for creative text responses (profile analysis).
     * Uses higher temperature for more creative, varied results.
     */
    object TextModel {
        const val TEMPERATURE = 0.7f
        const val TOP_K = 40
        const val TOP_P = 0.95f
        const val MAX_OUTPUT_TOKENS = 16384  // Increased significantly for 2.5 models
    }
    
    /**
     * Creates a GenerativeModel configured for JSON-structured responses.
     * Best for game suggestions where structured output is needed.
     * Disables thinking mode (thinkingBudget=0) for predictable JSON output.
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
     * Note: For gemini-2.5-flash, thinking mode is enabled by default.
     * The model will use "thinking" tokens internally before generating the response.
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
