package com.example.arcadia.data.remote

/**
 * Configuration for AI provider selection.
 * Change the CURRENT_PROVIDER value to switch between AI backends.
 * 
 * Usage:
 * ```kotlin
 * // To switch providers, simply change this value:
 * val CURRENT_PROVIDER = AIProvider.GEMINI  // Use Google Gemini
 * val CURRENT_PROVIDER = AIProvider.GROQ    // Use Groq with Kimi K2
 * ```
 */
object AIConfig {
    
    /**
     * Available AI providers
     */
    enum class AIProvider {
        /**
         * Google Gemini AI
         * - Supports streaming
         * - Uses gemini-2.5-flash model
         * - Requires GEMINI_API_KEY in local.properties
         */
        GEMINI,
        
        /**
         * Groq with Kimi K2 model
         * - Fast inference
         * - Uses moonshotai/kimi-k2-instruct model
         * - Requires GROQ_API_KEY in local.properties
         */
        GROQ
    }
    
    // ============================================================
    // CHANGE THIS VALUE TO SWITCH AI PROVIDERS
    // ============================================================
    
    /**
     * The currently active AI provider.
     * Change this value to switch between Gemini and Groq.
     */
    val CURRENT_PROVIDER: AIProvider = AIProvider.GROQ
    
    // ============================================================
    
    /**
     * Check if Gemini is the active provider
     */
    val isGemini: Boolean
        get() = CURRENT_PROVIDER == AIProvider.GEMINI
    
    /**
     * Check if Groq is the active provider
     */
    val isGroq: Boolean
        get() = CURRENT_PROVIDER == AIProvider.GROQ
    
    /**
     * Get a human-readable name for the current provider
     */
    val providerName: String
        get() = when (CURRENT_PROVIDER) {
            AIProvider.GEMINI -> "Google Gemini"
            AIProvider.GROQ -> "Groq (Kimi K2)"
        }
}
