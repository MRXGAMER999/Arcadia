package com.example.arcadia.domain.model.ai

/**
 * Result from studio expansion containing display names and API slugs.
 * Used for expanding a parent studio into all its subsidiaries.
 */
data class StudioExpansionResult(
    val displayNames: Set<String>,
    val slugs: String // Comma-separated slugs for RAWG API
) {
    companion object {
        /**
         * Creates a fallback result with just the original studio name.
         */
        fun fallback(studioName: String): StudioExpansionResult {
            val slug = studioName.lowercase()
                .replace(" ", "-")
                .replace(Regex("[^a-z0-9-]"), "")
            return StudioExpansionResult(setOf(studioName), slug)
        }
    }
}
