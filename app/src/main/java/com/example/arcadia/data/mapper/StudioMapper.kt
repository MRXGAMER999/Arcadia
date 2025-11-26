package com.example.arcadia.data.mapper

import com.example.arcadia.domain.model.ai.StudioMatch
import com.example.arcadia.domain.model.ai.StudioSearchResult
import com.example.arcadia.domain.model.ai.StudioType

/**
 * Mapper for converting studio-related DTOs to domain models.
 */
object StudioMapper {

    /**
     * Converts a type string to StudioType enum.
     */
    fun mapStudioType(type: String): StudioType = when (type.lowercase()) {
        "developer" -> StudioType.DEVELOPER
        "publisher" -> StudioType.PUBLISHER
        "both" -> StudioType.BOTH
        else -> StudioType.UNKNOWN
    }

    /**
     * Creates a StudioMatch from raw data.
     */
    fun createStudioMatch(
        name: String,
        slug: String,
        type: String,
        subsidiaryCount: Int = 0,
        isExactMatch: Boolean = false
    ): StudioMatch = StudioMatch(
        name = name,
        slug = slug,
        type = mapStudioType(type),
        subsidiaryCount = subsidiaryCount,
        isExactMatch = isExactMatch
    )

    /**
     * Creates a StudioSearchResult from a list of matches.
     */
    fun createSearchResult(
        query: String,
        matches: List<StudioMatch>,
        fromCache: Boolean = false
    ): StudioSearchResult = StudioSearchResult(
        query = query,
        matches = matches,
        fromCache = fromCache
    )

    /**
     * Converts display name to URL slug format.
     */
    fun toSlug(displayName: String): String = displayName
        .lowercase()
        .trim()
        .replace(Regex("[^a-z0-9\\s-]"), "")
        .replace(Regex("\\s+"), "-")

    /**
     * Converts slug to display name format.
     */
    fun fromSlug(slug: String): String = slug
        .split("-")
        .joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
}
