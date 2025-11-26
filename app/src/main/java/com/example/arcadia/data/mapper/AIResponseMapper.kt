package com.example.arcadia.data.mapper

import com.example.arcadia.domain.model.ai.AIGameSuggestions
import com.example.arcadia.domain.model.ai.GameInsights

/**
 * Mapper for converting AI response DTOs to domain models.
 */
object AIResponseMapper {

    /**
     * Creates AIGameSuggestions from parsed response data.
     */
    fun createGameSuggestions(
        games: List<String>,
        reasoning: String? = null,
        fromCache: Boolean = false
    ): AIGameSuggestions = AIGameSuggestions(
        games = games,
        reasoning = reasoning,
        fromCache = fromCache
    )

    /**
     * Creates GameInsights from parsed sections.
     */
    fun createGameInsights(
        personalityAnalysis: String,
        preferredGenres: List<String>,
        playStyle: String,
        funFacts: List<String>,
        recommendations: String
    ): GameInsights = GameInsights(
        personalityAnalysis = personalityAnalysis.ifEmpty { DEFAULT_PERSONALITY },
        preferredGenres = preferredGenres,
        playStyle = playStyle.ifEmpty { DEFAULT_PLAY_STYLE },
        funFacts = funFacts.ifEmpty { DEFAULT_FUN_FACTS },
        recommendations = recommendations.ifEmpty { DEFAULT_RECOMMENDATIONS }
    )

    /**
     * Creates default GameInsights for empty/error cases.
     */
    fun createDefaultInsights(genres: List<String> = emptyList()): GameInsights = GameInsights(
        personalityAnalysis = DEFAULT_PERSONALITY,
        preferredGenres = genres.take(3),
        playStyle = DEFAULT_PLAY_STYLE,
        funFacts = DEFAULT_FUN_FACTS,
        recommendations = DEFAULT_RECOMMENDATIONS
    )

    private const val DEFAULT_PERSONALITY = 
        "You're building your gaming journey! Keep adding games to discover your unique gaming identity."
    
    private const val DEFAULT_PLAY_STYLE = 
        "Your play style is emerging as you build your collection."
    
    private val DEFAULT_FUN_FACTS = listOf(
        "Add more games to unlock personalized insights!",
        "Rate your games to see deeper analysis.",
        "Track your play time for fun statistics!"
    )
    
    private const val DEFAULT_RECOMMENDATIONS = 
        "Keep adding games to your library for personalized recommendations!"
}
