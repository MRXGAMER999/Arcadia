package com.example.arcadia.data.remote

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * AIClient implementation for Groq API with model fallback support.
 * 
 * This is a thin wrapper around GroqApiService that implements the AIClient interface.
 * Features automatic fallback through multiple models (Kimi K2 → Llama 3.3 → Llama 4 Scout).
 * All business logic (caching, parsing, etc.) is in BaseAIRepository.
 */
class GroqAIClient(
    private val groqApiService: GroqApiService
) : AIClient {
    
    companion object {
        private const val TAG = "GroqAIClient"
    }
    
    override val providerName: String = "Groq (Kimi K2)"
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    override suspend fun generateJsonContent(
        prompt: String,
        temperature: Float,
        maxTokens: Int
    ): String {
        Log.d(TAG, "Generating JSON content...")
        
        val response = executeWithModelFallback { modelName ->
            GroqChatRequest(
                model = modelName,
                messages = listOf(
                    GroqMessage(
                        role = "system",
                        content = "You are a helpful assistant that responds only in valid JSON format. Never include markdown code blocks or any text outside the JSON."
                    ),
                    GroqMessage(role = "user", content = prompt)
                ),
                temperature = temperature,
                maxTokens = maxTokens,
                responseFormat = null // Kimi K2 doesn't support json_object mode
            )
        }
        
        val text = response.choices.firstOrNull()?.message?.content?.trim()
            ?: throw AIClientException.EmptyResponse()
        
        // Clean up response - handle various markdown formats
        return cleanJsonResponse(text)
    }
    
    override suspend fun generateTextContent(
        prompt: String,
        temperature: Float,
        maxTokens: Int
    ): String {
        Log.d(TAG, "Generating text content...")
        
        val response = executeWithModelFallback { modelName ->
            GroqChatRequest(
                model = modelName,
                messages = listOf(
                    GroqMessage(role = "user", content = prompt)
                ),
                temperature = temperature,
                maxTokens = maxTokens
            )
        }
        
        return response.choices.firstOrNull()?.message?.content?.trim()
            ?: throw AIClientException.EmptyResponse()
    }
    
    override fun generateStreamingContent(
        prompt: String,
        temperature: Float,
        maxTokens: Int
    ): Flow<String> = flow {
        Log.d(TAG, "Starting streaming generation...")
        
        val modelsToTry = listOf(GroqConfig.MODEL_NAME) + GroqConfig.FALLBACK_MODELS
        var lastException: Exception? = null
        
        for ((index, modelName) in modelsToTry.withIndex()) {
            try {
                Log.d(TAG, "Trying streaming with model: $modelName")
                
                val request = GroqChatRequest(
                    model = modelName,
                    messages = listOf(
                        GroqMessage(role = "user", content = prompt)
                    ),
                    temperature = temperature,
                    maxTokens = maxTokens,
                    stream = true
                )
                
                val responseBody = groqApiService.streamChatCompletion(
                    authorization = "Bearer ${GroqConfig.API_KEY}",
                    request = request
                )
                
                responseBody.byteStream().bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line?.trim() ?: continue
                        if (currentLine.startsWith("data: ")) {
                            val data = currentLine.removePrefix("data: ")
                            if (data == "[DONE]") break
                            
                            try {
                                val chunk = json.decodeFromString<GroqStreamChunk>(data)
                                val content = chunk.choices.firstOrNull()?.delta?.content ?: ""
                                if (content.isNotEmpty()) {
                                    emit(content)
                                }
                            } catch (e: Exception) {
                                // Ignore parse errors for individual chunks
                            }
                        }
                    }
                }
                
                // Success - return
                return@flow
                
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Streaming failed with model $modelName: ${e.message}")
                
                if (index < modelsToTry.size - 1) {
                    Log.d(TAG, "Trying next model for streaming in 2s...")
                    kotlinx.coroutines.delay(2000)
                    continue
                }
            }
        }
        
        // All models failed
        throw mapException(lastException ?: Exception("All models failed"))
        
    }.flowOn(Dispatchers.IO)
    
    /**
     * Execute request with automatic model fallback.
     */
    private suspend fun executeWithModelFallback(
        createRequest: (modelName: String) -> GroqChatRequest
    ): GroqChatResponse {
        val modelsToTry = listOf(GroqConfig.MODEL_NAME) + GroqConfig.FALLBACK_MODELS
        var lastException: Exception? = null
        
        for ((index, modelName) in modelsToTry.withIndex()) {
            try {
                Log.d(TAG, "Trying model: $modelName")
                
                return groqApiService.chatCompletion(
                    authorization = "Bearer ${GroqConfig.API_KEY}",
                    request = createRequest(modelName)
                )
                
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Model $modelName failed: ${e.message}")
                
                // Check if we should retry with fallback
                if (shouldFallback(e) && index < modelsToTry.size - 1) {
                    Log.d(TAG, "Falling back to next model in 2s...")
                    kotlinx.coroutines.delay(2000)
                    continue
                }
                
                // If it's not a fallback-able error, throw immediately
                if (!shouldFallback(e)) {
                    throw mapException(e)
                }
            }
        }
        
        // All models failed
        throw mapException(lastException ?: Exception("All models failed"))
    }
    
    /**
     * Determine if error should trigger model fallback.
     */
    private fun shouldFallback(e: Exception): Boolean {
        val message = e.message?.lowercase() ?: ""
        return message.contains("rate") ||
               message.contains("limit") ||
               message.contains("429") ||
               message.contains("503") ||
               message.contains("overloaded") ||
               message.contains("capacity")
    }
    
    /**
     * Clean JSON response from markdown formatting.
     */
    private fun cleanJsonResponse(text: String): String {
        var cleaned = text
        if (cleaned.contains("```")) {
            val jsonStart = cleaned.indexOf("{")
            val jsonEnd = cleaned.lastIndexOf("}") + 1
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                cleaned = cleaned.substring(jsonStart, jsonEnd)
            }
        }
        return cleaned.trim()
    }
    
    /**
     * Maps Groq exceptions to AIClientException types.
     */
    private fun mapException(e: Exception): AIClientException {
        val message = e.message ?: "Unknown error"
        return when {
            message.contains("429") ||
            message.contains("rate", ignoreCase = true) ||
            message.contains("limit", ignoreCase = true) -> 
                AIClientException.RateLimited(message)
            
            message.contains("401") ||
            message.contains("api key", ignoreCase = true) ||
            message.contains("unauthorized", ignoreCase = true) -> 
                AIClientException.AuthenticationError(message)
            
            message.contains("network", ignoreCase = true) ||
            message.contains("connection", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) -> 
                AIClientException.NetworkError(message, e)
            
            else -> AIClientException.ApiError(message, e)
        }
    }
    
    @Serializable
    private data class GroqStreamChunk(
        val choices: List<StreamChoice>
    )
    
    @Serializable
    private data class StreamChoice(
        val delta: StreamDelta
    )
    
    @Serializable
    private data class StreamDelta(
        val content: String? = null
    )
}
