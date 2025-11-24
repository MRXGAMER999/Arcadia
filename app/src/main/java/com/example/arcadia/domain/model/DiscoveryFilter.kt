package com.example.arcadia.domain.model

/**
 * Represents the sorting type for discovery.
 */
enum class DiscoverySortType {
    RELEVANCE,
    RATING,
    RELEASE_DATE,
    NAME,
    POPULARITY
}

/**
 * Represents the sorting order.
 */
enum class DiscoverySortOrder {
    ASCENDING,
    DESCENDING
}

/**
 * Represents the release timeframe filter.
 */
enum class ReleaseTimeframe(val displayName: String, val years: Int?) {
    ALL("All Time", null),
    LAST_5_YEARS("Last 5 Years", 5),
    LAST_YEAR("Last Year", 1)
}

/**
 * Represents a developer/studio selection with its sub-studios.
 */
data class DeveloperSelection(
    val parentName: String,
    val subStudios: Set<String> = emptySet()
)

/**
 * State for discovery filtering in the UI.
 */
data class DiscoveryFilterState(
    // Developer/Publisher filter
    val developerSearchQuery: String = "",
    val selectedDevelopers: Map<String, Set<String>> = emptyMap(), // Parent -> SubStudios
    val searchResults: List<String> = emptyList(),
    val expandedStudios: Set<String> = emptySet(), // Currently expanded sub-studios for display
    val isLoadingDevelopers: Boolean = false,
    
    // Sorting - default to popularity
    val sortType: DiscoverySortType = DiscoverySortType.POPULARITY,
    val sortOrder: DiscoverySortOrder = DiscoverySortOrder.DESCENDING,
    
    // Genres
    val selectedGenres: Set<String> = emptySet(),
    val availableGenres: List<String> = listOf(
        "Action", "Adventure", "RPG", "Strategy", "Simulation",
        "Sports", "Racing", "Puzzle", "Shooter", "Fighting",
        "Platformer", "Survival", "Horror", "Indie", "Casual"
    ),
    
    // Release timeframe
    val releaseTimeframe: ReleaseTimeframe = ReleaseTimeframe.ALL,
    
    // Loading states
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val hasActiveFilters: Boolean
        get() = selectedDevelopers.isNotEmpty() ||
                sortType != DiscoverySortType.POPULARITY ||
                selectedGenres.isNotEmpty() ||
                releaseTimeframe != ReleaseTimeframe.ALL
    
    val totalSelectedDevelopersCount: Int
        get() = selectedDevelopers.values.sumOf { it.size.coerceAtLeast(1) }
                
    val activeFilterCount: Int
        get() = (if (selectedDevelopers.isNotEmpty()) 1 else 0) + 
                selectedGenres.size + 
                (if (sortType != DiscoverySortType.POPULARITY) 1 else 0) +
                (if (releaseTimeframe != ReleaseTimeframe.ALL) 1 else 0)
    
    // Get all developer slugs for API call
    fun getAllDeveloperSlugs(): String {
        return selectedDevelopers.flatMap { (parent, subs) ->
            if (subs.isEmpty()) listOf(parent) else subs.toList()
        }.joinToString(",") { it.lowercase().replace(" ", "-") }
    }
}
