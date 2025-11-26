package com.example.arcadia.domain.model.ai

/**
 * Result from AI game suggestions.
 * Contains a list of suggested game names with optional reasoning.
 */
data class AIGameSuggestions(
    val games: List<String>,
    val reasoning: String? = null,
    val fromCache: Boolean = false
)
