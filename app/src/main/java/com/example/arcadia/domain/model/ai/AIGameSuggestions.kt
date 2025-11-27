package com.example.arcadia.domain.model.ai

/**
 * A single game recommendation with confidence score.
 */
data class GameRecommendation(
    val name: String,
    val confidence: Int = 50 // 1-100, higher = better match
)

/**
 * Result from AI game suggestions.
 * Contains a list of suggested game names with optional reasoning.
 * Games are sorted by confidence score (highest first).
 */
data class AIGameSuggestions(
    val games: List<String>,
    val recommendations: List<GameRecommendation> = emptyList(),
    val reasoning: String? = null,
    val fromCache: Boolean = false
) {
    /**
     * Get games sorted by confidence (highest first).
     * Falls back to original games list if recommendations are empty.
     */
    fun getGamesSortedByConfidence(): List<String> {
        return if (recommendations.isNotEmpty()) {
            recommendations.sortedByDescending { it.confidence }.map { it.name }
        } else {
            games
        }
    }
}
