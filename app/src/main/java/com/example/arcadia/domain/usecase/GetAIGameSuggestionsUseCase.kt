package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.repository.GeminiRepository

/**
 * Use case for getting AI-powered game suggestions.
 * Encapsulates the business logic for AI search functionality.
 */
class GetAIGameSuggestionsUseCase(
    private val geminiRepository: GeminiRepository
) {
    /**
     * Gets AI-powered game suggestions based on a natural language query.
     * 
     * @param query The user's natural language query
     * @param count The number of games to suggest
     * @return Result containing AI suggestions or an error
     */
    suspend operator fun invoke(
        query: String,
        count: Int = 8
    ): Result<GeminiRepository.AIGameSuggestions> {
        return geminiRepository.suggestGames(query, count)
    }
}
