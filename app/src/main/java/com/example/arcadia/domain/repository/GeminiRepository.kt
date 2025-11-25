package com.example.arcadia.domain.repository

import com.example.arcadia.domain.model.GameListEntry

/**
 * Repository interface for interacting with AI services for game analysis and suggestions
 */
interface GeminiRepository {

    /**
     * Result from AI game suggestions
     */
    data class AIGameSuggestions(
        val games: List<String>,
        val reasoning: String? = null
    )

    /**
     * Represents AI-generated insights about a user's gaming profile
     */
    data class GameInsights(
        val personalityAnalysis: String,
        val preferredGenres: List<String>,
        val playStyle: String,
        val funFacts: List<String>,
        val recommendations: String
    )

    /**
     * Ask AI to suggest game names based on a natural language query.
     * Returns a list of game names that can be searched via game APIs.
     *
     * @param userQuery The user's natural language query for game suggestions
     * @param count The number of games to suggest
     * @return Result containing AI game suggestions or an error
     */
    suspend fun suggestGames(userQuery: String, count: Int = 10): Result<AIGameSuggestions>

    /**
     * Analyzes a user's gaming profile and returns personalized insights
     *
     * @param games The list of games in the user's library
     * @return Result containing game insights or an error
     */
    suspend fun analyzeGamingProfile(games: List<GameListEntry>): Result<GameInsights>
}
