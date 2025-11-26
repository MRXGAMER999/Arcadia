package com.example.arcadia.domain.model

/**
 * Sealed class representing all possible AI-related errors.
 * Provides user-friendly messages and categorization for proper error handling.
 */
sealed class AIError : Exception() {
    
    /**
     * Network-related errors (no internet, timeout, etc.)
     */
    data class NetworkError(
        override val message: String = "Unable to connect. Please check your internet connection.",
        override val cause: Throwable? = null
    ) : AIError()
    
    /**
     * API rate limit exceeded
     */
    data class RateLimitError(
        override val message: String = "Too many requests. Please wait a moment and try again.",
        val retryAfterMs: Long? = null
    ) : AIError()
    
    /**
     * Invalid or unparseable response from AI
     */
    data class InvalidResponseError(
        override val message: String = "Received an invalid response. Please try again.",
        val rawResponse: String? = null
    ) : AIError()
    
    /**
     * Empty response from AI
     */
    data class EmptyResponseError(
        override val message: String = "No results found. Try a different search query."
    ) : AIError()
    
    /**
     * API-specific errors (authentication, quota, etc.)
     */
    data class ApiError(
        val code: Int,
        override val message: String = "AI service error. Please try again later.",
        override val cause: Throwable? = null
    ) : AIError()
    
    /**
     * Content was blocked by safety filters
     */
    data class ContentBlockedError(
        override val message: String = "Your request couldn't be processed. Please try rephrasing."
    ) : AIError()
    
    /**
     * Generic/unknown errors
     */
    data class UnknownError(
        override val message: String = "Something went wrong. Please try again.",
        override val cause: Throwable? = null
    ) : AIError()
    
    /**
     * Request was cancelled (e.g., user navigated away)
     */
    data class CancelledError(
        override val message: String = "Request was cancelled."
    ) : AIError()
    
    companion object {
        /**
         * Maps a generic exception to an appropriate AIError type
         */
        fun from(throwable: Throwable): AIError {
            return when {
                throwable is AIError -> throwable
                
                // Network errors
                throwable is java.net.UnknownHostException ||
                throwable is java.net.SocketTimeoutException ||
                throwable is java.net.ConnectException ||
                throwable.message?.contains("network", ignoreCase = true) == true ||
                throwable.message?.contains("timeout", ignoreCase = true) == true -> {
                    NetworkError(cause = throwable)
                }
                
                // Rate limiting
                throwable.message?.contains("rate limit", ignoreCase = true) == true ||
                throwable.message?.contains("quota", ignoreCase = true) == true ||
                throwable.message?.contains("429", ignoreCase = true) == true -> {
                    RateLimitError()
                }
                
                // Content blocked
                throwable.message?.contains("blocked", ignoreCase = true) == true ||
                throwable.message?.contains("safety", ignoreCase = true) == true ||
                throwable.message?.contains("SAFETY", ignoreCase = true) == true -> {
                    ContentBlockedError()
                }
                
                // Cancellation
                throwable is kotlinx.coroutines.CancellationException -> {
                    CancelledError()
                }
                
                // JSON parsing errors
                throwable is kotlinx.serialization.SerializationException ||
                throwable.message?.contains("parse", ignoreCase = true) == true ||
                throwable.message?.contains("json", ignoreCase = true) == true -> {
                    InvalidResponseError(rawResponse = throwable.message)
                }
                
                // API errors with codes
                throwable.message?.contains("401") == true -> {
                    ApiError(401, "Authentication failed. Please restart the app.")
                }
                throwable.message?.contains("403") == true -> {
                    ApiError(403, "Access denied. Please try again later.")
                }
                throwable.message?.contains("500") == true ||
                throwable.message?.contains("503") == true -> {
                    ApiError(500, "AI service is temporarily unavailable.")
                }
                
                // Default
                else -> UnknownError(cause = throwable)
            }
        }
        
        /**
         * Returns a user-friendly message for display
         */
        fun AIError.userFriendlyMessage(): String = this.message ?: "An error occurred"
        
        /**
         * Returns whether this error is retryable
         */
        fun AIError.isRetryable(): Boolean = when (this) {
            is NetworkError -> true
            is RateLimitError -> true
            is ApiError -> code in listOf(500, 502, 503, 504)
            is UnknownError -> true
            is InvalidResponseError -> true
            is EmptyResponseError -> false
            is ContentBlockedError -> false
            is CancelledError -> false
        }
    }
}
