package com.example.arcadia.domain.usecase.studio

import com.example.arcadia.domain.model.ai.StudioMatch
import com.example.arcadia.domain.repository.AIRepository

/**
 * Use case for getting instant local studio suggestions.
 * This is synchronous and returns immediately without network calls,
 * making it ideal for showing suggestions as the user types.
 */
class GetLocalStudioSuggestionsUseCase(
    private val aiRepository: AIRepository
) {
    /**
     * Get instant suggestions from local cache/hardcoded mappings.
     * 
     * @param query The search query (partial studio name or game series)
     * @param limit Maximum number of suggestions to return
     * @return List of matching studios from local data only
     */
    operator fun invoke(query: String, limit: Int = 5): List<StudioMatch> {
        if (query.isBlank() || query.length < 2) {
            return emptyList()
        }
        return aiRepository.getLocalStudioSuggestions(query, limit)
    }
    
    /**
     * Get suggestion names only (for simple UI display).
     * 
     * @param query The search query
     * @param limit Maximum number of suggestions
     * @return List of studio names
     */
    fun getNames(query: String, limit: Int = 5): List<String> {
        return invoke(query, limit).map { it.name }
    }
}
