package com.example.arcadia.data.mapper

import com.example.arcadia.domain.model.ai.RoastInsights

/**
 * Mapper for parsing AI-generated roast responses into structured RoastInsights.
 * Uses section delimiters to extract different parts of the roast.
 */
object RoastResponseMapper {

    /**
     * Parses an AI roast response string into a RoastInsights object.
     * 
     * Expected format uses section markers:
     * ===HEADLINE===, ===COULD_HAVE===, ===PREDICTION===, 
     * ===WHOLESOME===, ===ROAST_TITLE===, ===ROAST_EMOJI===
     *
     * @param response The raw AI response string
     * @return Result containing RoastInsights on success, or failure with exception
     */
    fun parseRoastResponse(response: String): Result<RoastInsights> {
        return try {
            val headline = extractSection(response, "HEADLINE")
            val couldHave = extractSection(response, "COULD_HAVE")
            val prediction = extractSection(response, "PREDICTION")
            val wholesome = extractSection(response, "WHOLESOME")
            val roastTitle = extractSection(response, "ROAST_TITLE")
            val roastEmoji = extractSection(response, "ROAST_EMOJI")

            // Validate required sections are present
            if (headline == null || couldHave == null || prediction == null || 
                wholesome == null || roastTitle == null) {
                return Result.failure(
                    IllegalArgumentException("Missing required sections in roast response")
                )
            }

            val couldHaveList = couldHave
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { line ->
                    // Remove common list prefixes like "- ", "* ", "1. ", etc.
                    line.removePrefix("-").removePrefix("*")
                        .trim()
                        .replaceFirst(Regex("^\\d+\\.\\s*"), "")
                }
                .take(5)
                .ifEmpty { listOf("Played more games instead of sleeping (probably)") }

            Result.success(
                RoastInsights(
                    headline = headline.trim(),
                    couldHaveList = couldHaveList,
                    prediction = prediction.trim(),
                    wholesomeCloser = wholesome.trim(),
                    roastTitle = roastTitle.trim(),
                    roastTitleEmoji = roastEmoji?.trim() ?: "🔥"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extracts content between section markers.
     * 
     * @param text The full response text
     * @param section The section name (without === delimiters)
     * @return The extracted section content, or null if not found
     */
    private fun extractSection(text: String, section: String): String? {
        val pattern = "===${section}===\\s*([\\s\\S]*?)(?=\\n===|$)".toRegex()
        return pattern.find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }
}
