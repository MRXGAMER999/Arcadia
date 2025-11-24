package com.example.arcadia.domain.model

/**
 * Represents the type of studio filter to apply.
 */
enum class StudioFilterType {
    DEVELOPER,
    PUBLISHER,
    BOTH
}

/**
 * State for studio filtering in the UI.
 */
data class StudioFilterState(
    val selectedParentStudio: String? = null,
    val expandedStudios: Set<String> = emptySet(),
    val selectedStudios: Set<String> = emptySet(),
    val filterType: StudioFilterType = StudioFilterType.BOTH,
    val isLoadingExpansion: Boolean = false,
    val isLoadingMore: Boolean = false,
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val error: String? = null
)
