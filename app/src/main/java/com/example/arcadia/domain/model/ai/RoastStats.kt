package com.example.arcadia.domain.model.ai

/**
 * Contains gaming statistics used for generating personalized roasts.
 * These stats are passed to the AI to create relevant, specific roast content.
 */
data class RoastStats(
    val hoursPlayed: Int,
    val totalGames: Int,
    val completionRate: Float,
    val completedGames: Int,
    val droppedGames: Int,
    val topGenres: List<Pair<String, Int>>,
    val gamingPersonality: String,
    val averageRating: Float
)
