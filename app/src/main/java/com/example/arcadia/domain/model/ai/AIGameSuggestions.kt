package com.example.arcadia.domain.model.ai

/**
 * A single game recommendation with confidence score and optional reason.
 */
data class GameRecommendation(
    val name: String,
    val confidence: Int = 50, // 1-100, higher = better match
    val reason: String? = null // Brief explanation like "FromSoftware + Souls-like"
) {
    companion object {
        /**
         * Confidence scores for each tier.
         * Higher scores = higher confidence in the recommendation.
         */
        private const val PERFECT_MATCH_SCORE = 95
        private const val STRONG_MATCH_SCORE = 82
        private const val GOOD_MATCH_SCORE = 68
        private const val DECENT_MATCH_SCORE = 55
        private const val DEFAULT_SCORE = 50
        
        /**
         * Create a recommendation from a tier string (V4 format).
         * Converts tier classification to a numeric confidence score.
         * 
         * @param name The game name
         * @param tier One of: PERFECT_MATCH, STRONG_MATCH, GOOD_MATCH, DECENT_MATCH
         * @param why Optional brief explanation for the match
         */
        fun fromTierString(name: String, tier: String, why: String? = null): GameRecommendation {
            val confidence = when (tier.uppercase().trim()) {
                "PERFECT_MATCH", "PERFECT" -> PERFECT_MATCH_SCORE
                "STRONG_MATCH", "STRONG" -> STRONG_MATCH_SCORE
                "GOOD_MATCH", "GOOD" -> GOOD_MATCH_SCORE
                "DECENT_MATCH", "DECENT" -> DECENT_MATCH_SCORE
                else -> DEFAULT_SCORE
            }
            return GameRecommendation(name, confidence, why)
        }
        
        /**
         * Create a recommendation from semantic match reasons (V3 format).
         * Calculates confidence based on the combination of match reasons.
         * 
         * @param name The game name
         * @param reasons List of match reasons that apply
         * @param primaryReason Optional primary reason string for display
         */
        fun fromReasons(
            name: String, 
            reasons: List<MatchReason>, 
            primaryReason: String? = null
        ): GameRecommendation {
            // Calculate confidence by summing reason weights
            val totalWeight = reasons.sumOf { it.weight }
            // Clamp to 1-100 range
            val confidence = totalWeight.coerceIn(1, 100)
            return GameRecommendation(name, confidence, primaryReason)
        }
        
        /**
         * Create a recommendation from a legacy confidence score (V2 format).
         * Simply passes through the score as-is.
         * 
         * @param name The game name
         * @param score The confidence score (1-100)
         */
        fun fromLegacyScore(name: String, score: Int): GameRecommendation {
            return GameRecommendation(name, score.coerceIn(1, 100))
        }
    }
}

/**
 * Match reasons for recommendations with associated weights.
 * Used to calculate confidence scores client-side.
 */
enum class MatchReason(val weight: Int) {
    FAVORITE_DEV(30),      // Same developer as a 9-10 rated game
    SAME_DEV(20),          // Same developer as any liked game
    GENRE_MATCH(15),       // Matches user's top genres
    ASPECT_MATCH(12),      // Matches user's loved aspects
    HIGH_METACRITIC(10),   // 80+ Metacritic score
    SERIES_RELATED(15),    // Related to a liked series
    SIMILAR_GAMEPLAY(12),  // Similar gameplay to liked games
    SPIRITUAL_SUCCESSOR(25), // Spiritual successor to a favorite
    CULT_CLASSIC(8),       // Highly regarded cult classic
    INDIE_GEM(8),          // Quality indie that matches taste
    RECENT_RELEASE(5),     // Recent release in liked genre
    CLASSIC(5)             // Classic/influential in the genre
}

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
