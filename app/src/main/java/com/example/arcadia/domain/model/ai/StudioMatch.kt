package com.example.arcadia.domain.model.ai

/**
 * A single studio match from search.
 * Contains metadata about the studio including type and subsidiary information.
 */
data class StudioMatch(
    val name: String,
    val slug: String,
    val type: StudioType,
    val subsidiaryCount: Int = 0,
    val isExactMatch: Boolean = false
) {
    /**
     * Returns true if this studio has subsidiaries.
     */
    val hasSubsidiaries: Boolean get() = subsidiaryCount > 0
}

/**
 * Type of studio - developer, publisher, or both.
 */
enum class StudioType {
    DEVELOPER,
    PUBLISHER, 
    BOTH,
    UNKNOWN;
    
    companion object {
        /**
         * Parses a string to StudioType.
         */
        fun fromString(value: String): StudioType = when (value.lowercase()) {
            "developer" -> DEVELOPER
            "publisher" -> PUBLISHER
            "both" -> BOTH
            else -> UNKNOWN
        }
    }
}
