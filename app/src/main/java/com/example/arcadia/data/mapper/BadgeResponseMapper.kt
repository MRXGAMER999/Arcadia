package com.example.arcadia.data.mapper

import com.example.arcadia.domain.model.ai.Badge
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Mapper for parsing AI-generated badge responses from JSON format.
 */
object BadgeResponseMapper {

    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parses an AI badge response JSON string into a list of Badge objects.
     * 
     * Expected JSON format:
     * {
     *   "badges": [
     *     {"title": "Badge Title", "emoji": "🎮", "reason": "Why they earned this"},
     *     ...
     *   ]
     * }
     *
     * @param response The raw AI response string (JSON)
     * @return Result containing list of Badges on success, or failure with exception
     */
    fun parseBadgesResponse(response: String): Result<List<Badge>> {
        return try {
            // Clean up the response - remove markdown code blocks if present
            val cleanedResponse = cleanJsonResponse(response)
            
            val dto = json.decodeFromString<BadgesResponseDto>(cleanedResponse)
            val badges = dto.badges.map { it.toBadge() }
            
            if (badges.isEmpty()) {
                Result.failure(IllegalArgumentException("No badges found in response"))
            } else {
                Result.success(badges)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cleans up the JSON response by removing markdown code blocks and extra whitespace.
     */
    private fun cleanJsonResponse(response: String): String {
        return response
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}

/**
 * DTO for the badges response wrapper.
 */
@Serializable
internal data class BadgesResponseDto(
    val badges: List<BadgeDto>
)

/**
 * DTO for individual badge data.
 */
@Serializable
internal data class BadgeDto(
    val title: String,
    val emoji: String,
    val reason: String
) {
    fun toBadge() = Badge(
        title = title,
        emoji = emoji,
        reason = reason
    )
}
