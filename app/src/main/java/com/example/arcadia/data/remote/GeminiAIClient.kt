package com.example.arcadia.data.remote

import android.util.Log
import com.example.arcadia.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

/**
 * AIClient implementation for Google Gemini.
 * 
 * This is a thin wrapper around GenerativeModel that implements the AIClient interface.
 * All business logic (caching, parsing, etc.) is in BaseAIRepository.
 */
class GeminiAIClient : AIClient {
    
    companion object {
        private const val TAG = "GeminiAIClient"
        private const val MODEL_NAME = "gemini-2.5-flash"
    }
    
    override val providerName: String = "Google Gemini"
    
    // Lazy-initialized models to avoid creating them until needed
    private val jsonModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.2f
                topK = 32
                topP = 0.9f
                maxOutputTokens = 16384
                responseMimeType = "application/json"
            }
        )
    }
    
    private val textModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 16384
            }
        )
    }
    
    override suspend fun generateJsonContent(
        prompt: String,
        temperature: Float,
        maxTokens: Int
    ): String {
        return try {
            Log.d(TAG, "Generating JSON content...")
            val response = jsonModel.generateContent(prompt)
            val text = response.text?.trim()
            
            if (text.isNullOrBlank()) {
                throw AIClientException.EmptyResponse()
            }
            
            // Clean up Gemini's markdown formatting
            text.removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
                
        } catch (e: AIClientException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error generating JSON content", e)
            throw mapException(e)
        }
    }
    
    override suspend fun generateTextContent(
        prompt: String,
        temperature: Float,
        maxTokens: Int
    ): String {
        return try {
            Log.d(TAG, "Generating text content...")
            val response = textModel.generateContent(prompt)
            val text = response.text?.trim()
            
            if (text.isNullOrBlank()) {
                throw AIClientException.EmptyResponse()
            }
            
            text
        } catch (e: AIClientException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error generating text content", e)
            throw mapException(e)
        }
    }
    
    override fun generateStreamingContent(
        prompt: String,
        temperature: Float,
        maxTokens: Int
    ): Flow<String> = flow {
        try {
            Log.d(TAG, "Starting streaming generation...")
            textModel.generateContentStream(prompt).collect { chunk ->
                chunk.text?.let { emit(it) }
            }
        } catch (e: AIClientException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during streaming", e)
            throw mapException(e)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Maps Gemini exceptions to AIClientException types.
     */
    private fun mapException(e: Exception): AIClientException {
        val message = e.message ?: "Unknown error"
        return when {
            message.contains("quota", ignoreCase = true) ||
            message.contains("rate", ignoreCase = true) -> 
                AIClientException.RateLimited(message)
            
            message.contains("api key", ignoreCase = true) ||
            message.contains("authentication", ignoreCase = true) ||
            message.contains("unauthorized", ignoreCase = true) -> 
                AIClientException.AuthenticationError(message)
            
            message.contains("network", ignoreCase = true) ||
            message.contains("connection", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) -> 
                AIClientException.NetworkError(message, e)
            
            else -> AIClientException.ApiError(message, e)
        }
    }
}
