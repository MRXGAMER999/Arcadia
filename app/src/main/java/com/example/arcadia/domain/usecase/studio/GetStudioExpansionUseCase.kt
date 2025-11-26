package com.example.arcadia.domain.usecase.studio

import com.example.arcadia.domain.model.ai.StudioExpansionResult
import com.example.arcadia.domain.repository.AIRepository

/**
 * Use case for expanding a parent studio into all its subsidiaries.
 * For example, "Microsoft" would expand to include Xbox Game Studios,
 * Bethesda, 343 Industries, etc.
 */
class GetStudioExpansionUseCase(
    private val aiRepository: AIRepository
) {
    /**
     * Get full expansion result for a parent studio.
     * 
     * @param parentStudio The name of the parent studio
     * @return StudioExpansionResult with display names and API slugs
     */
    suspend operator fun invoke(parentStudio: String): StudioExpansionResult {
        if (parentStudio.isBlank()) {
            return StudioExpansionResult.fallback(parentStudio)
        }
        return aiRepository.getStudioExpansionResult(parentStudio)
    }
    
    /**
     * Get only the display names for UI presentation.
     * 
     * @param parentStudio The name of the parent studio
     * @return Set of studio display names
     */
    suspend fun getDisplayNames(parentStudio: String): Set<String> {
        if (parentStudio.isBlank()) return setOf(parentStudio)
        return aiRepository.getExpandedStudios(parentStudio)
    }
    
    /**
     * Get only the slugs for API filtering.
     * 
     * @param parentStudio The name of the parent studio
     * @return Comma-separated string of slugs
     */
    suspend fun getSlugs(parentStudio: String): String {
        if (parentStudio.isBlank()) return parentStudio.lowercase().replace(" ", "-")
        return aiRepository.getStudioSlugs(parentStudio)
    }
    
    /**
     * Expand multiple studios in parallel.
     * 
     * @param studios List of parent studio names
     * @return Map of studio name to expansion result
     */
    suspend fun expandMultiple(studios: List<String>): Map<String, StudioExpansionResult> {
        if (studios.isEmpty()) return emptyMap()
        return aiRepository.expandMultipleStudios(studios)
    }
}
