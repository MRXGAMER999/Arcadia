package com.example.arcadia.domain.usecase.studio

import com.example.arcadia.domain.model.ai.StudioSearchResult
import com.example.arcadia.domain.repository.AIRepository

/**
 * Use case for searching studios/publishers.
 * Searches for studios matching a query, which can be:
 * - A studio name (e.g., "Ubisoft", "Rockstar")
 * - A game series (e.g., "Yakuza", "Call of Duty")
 * - A game title (e.g., "Elden Ring")
 * 
 * Returns instant local results first, then enriches with AI if needed.
 */
class SearchStudiosUseCase(
    private val aiRepository: AIRepository
) {
    /**
     * Search for studios matching the query.
     * 
     * @param query The search query (studio name, game series, or game title)
     * @param includePublishers Whether to include publishers in results
     * @param includeDevelopers Whether to include developers in results
     * @param limit Maximum number of results
     * @return StudioSearchResult containing matching studios
     */
    suspend operator fun invoke(
        query: String,
        includePublishers: Boolean = true,
        includeDevelopers: Boolean = true,
        limit: Int = 10
    ): StudioSearchResult {
        if (query.isBlank() || query.length < 2) {
            return StudioSearchResult(query, emptyList(), fromCache = true)
        }
        
        return aiRepository.searchStudios(
            query = query,
            includePublishers = includePublishers,
            includeDevelopers = includeDevelopers,
            limit = limit
        )
    }
}
