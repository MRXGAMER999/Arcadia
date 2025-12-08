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
     * Parses an AI badge response string into a list of Badge objects.
     * Supports both the custom "===BADGE===" format (primary) and JSON (fallback).
     */
    fun parseBadgesResponse(response: String): Result<List<Badge>> {
        return try {
            // 1. Try parsing custom format first
            val badges = response.split("===BADGE===")
                .filter { it.isNotBlank() }
                .mapNotNull { section ->
                    val title = section.lines().find { it.trim().startsWith("Title:", true) }?.substringAfter(":")?.trim()
                    val emoji = section.lines().find { it.trim().startsWith("Emoji:", true) }?.substringAfter(":")?.trim()
                    val reason = section.lines().find { it.trim().startsWith("Reason:", true) }?.substringAfter(":")?.trim()
                    
                    if (!title.isNullOrBlank() && !emoji.isNullOrBlank() && !reason.isNullOrBlank()) {
                        Badge(title, emoji, reason)
                    } else {
                        null
                    }
                }

            if (badges.isNotEmpty()) {
                return Result.success(badges)
            }

            // 2. Fallback: Try JSON parsing if custom format failed
            val cleanedResponse = cleanJsonResponse(response)
            val dto = json.decodeFromString<BadgesResponseDto>(cleanedResponse)
            val jsonBadges = dto.badges.map { it.toBadge() }
            
            if (jsonBadges.isEmpty()) {
                Result.failure(IllegalArgumentException("No badges found in response"))
            } else {
                Result.success(jsonBadges)
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
