package com.example.arcadia.domain.model.ai

/**
 * Result from studio search containing matching studios.
 * Includes both local cache results and AI-enhanced results.
 */
data class StudioSearchResult(
    val query: String,
    val matches: List<StudioMatch>,
    val fromCache: Boolean = false
) {
    /**
     * Returns true if any matches were found.
     */
    val hasResults: Boolean get() = matches.isNotEmpty()
    
    /**
     * Returns studio names for UI display.
     */
    val displayNames: List<String> get() = matches.map { it.name }
}
