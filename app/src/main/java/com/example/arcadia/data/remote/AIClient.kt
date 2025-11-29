package com.example.arcadia.data.remote

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for AI provider API calls.
 * 
 * This interface abstracts the actual API calls, allowing the base repository
 * to focus on business logic (caching, parsing, deduplication) while each
 * provider implements only the networking layer.
 * 
 * Benefits:
 * - Single implementation of business logic in BaseAIRepository
 * - Easy to add new AI providers (Claude, OpenAI, etc.)
 * - Easy to test by mocking this interface
 * - Clear separation of concerns
 */
interface AIClient {
    
    /**
     * Name of the AI provider for logging purposes.
     */
    val providerName: String
    
    /**
     * Generate content with JSON response format.
     * Used for structured responses (game suggestions, studio search).
     * 
     * @param prompt The prompt to send to the AI
     * @param temperature Temperature for generation (0.0-1.0)
     * @param maxTokens Maximum tokens in response
     * @return Raw JSON string response
     * @throws AIClientException on error
     */
    suspend fun generateJsonContent(
        prompt: String,
        temperature: Float = 0.2f,
        maxTokens: Int = 4096
    ): String
    
    /**
     * Generate content with text response format.
     * Used for profile analysis and other text responses.
     * 
     * @param prompt The prompt to send to the AI
     * @param temperature Temperature for generation (0.0-1.0)
     * @param maxTokens Maximum tokens in response
     * @return Raw text string response
     * @throws AIClientException on error
     */
    suspend fun generateTextContent(
        prompt: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 4096
    ): String
    
    /**
     * Generate content with streaming support.
     * Used for profile analysis to show progressive results.
     * 
     * @param prompt The prompt to send to the AI
     * @param temperature Temperature for generation (0.0-1.0)
     * @param maxTokens Maximum tokens in response
     * @return Flow emitting content chunks as they arrive
     */
    fun generateStreamingContent(
        prompt: String,
        temperature: Float = 0.7f,
        maxTokens: Int = 4096
    ): Flow<String>
}

/**
 * Exception thrown by AIClient implementations.
 */
sealed class AIClientException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** Empty response from AI */
    class EmptyResponse(message: String = "AI returned empty response") : AIClientException(message)
    
    /** Rate limited by provider */
    class RateLimited(message: String = "Rate limited by AI provider") : AIClientException(message)
    
    /** Network error */
    class NetworkError(message: String, cause: Throwable? = null) : AIClientException(message, cause)
    
    /** Invalid API key */
    class AuthenticationError(message: String = "Invalid API key") : AIClientException(message)
    
    /** Generic API error */
    class ApiError(message: String, cause: Throwable? = null) : AIClientException(message, cause)
}
