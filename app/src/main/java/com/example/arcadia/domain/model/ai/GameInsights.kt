package com.example.arcadia.domain.model.ai

/**
 * Represents AI-generated insights about a user's gaming profile.
 * Contains personality analysis, play style assessment, and recommendations.
 */
data class GameInsights(
    val personalityAnalysis: String,
    val preferredGenres: List<String>,
    val playStyle: String,
    val funFacts: List<String>,
    val recommendations: String
)
