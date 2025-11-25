package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.repository.GeminiRepository

/**
 * Use case for analyzing a user's gaming profile with AI.
 * Encapsulates the business logic for AI profile analysis.
 */
class AnalyzeGamingProfileUseCase(
    private val geminiRepository: GeminiRepository
) {
    /**
     * Analyzes the user's gaming profile and generates personalized insights.
     * 
     * @param games The user's game library
     * @return Result containing game insights or an error
     */
    suspend operator fun invoke(games: List<GameListEntry>): Result<GeminiRepository.GameInsights> {
        if (games.isEmpty()) {
            return Result.failure(IllegalArgumentException("No games to analyze"))
        }
        return geminiRepository.analyzeGamingProfile(games)
    }
}
