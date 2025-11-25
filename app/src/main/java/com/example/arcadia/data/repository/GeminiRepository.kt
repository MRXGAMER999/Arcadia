package com.example.arcadia.data.repository

import android.util.Log
import com.example.arcadia.data.remote.GeminiConfig
import com.example.arcadia.data.remote.GeminiPrompts
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GeminiRepository
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Implementation of GeminiRepository for interacting with Google's Gemini AI
 */
class GeminiRepositoryImpl : GeminiRepository {

    private val jsonModel = GeminiConfig.createJsonModel()
    private val textModel = GeminiConfig.createTextModel()

    /**
     * Internal DTO for JSON parsing of AI game suggestions
     */
    @Serializable
    private data class AIGameSuggestionsDto(
        val games: List<String>,
        val reasoning: String? = null
    )

    /**
     * Ask Gemini to suggest game names based on a natural language query.
     * Returns a list of game names that can be searched via RAWG API.
     */
    override suspend fun suggestGames(userQuery: String, count: Int): Result<GeminiRepository.AIGameSuggestions> {
        return try {
            val prompt = GeminiPrompts.gameSuggestionPrompt(userQuery, count)

            Log.d(TAG, "Asking Gemini for game suggestions: $userQuery")
            
            val response = jsonModel.generateContent(prompt)
            val text = response.text?.trim() ?: throw Exception("Empty response from AI")
            
            Log.d(TAG, "Gemini response: $text")
            
            // Clean up response
            val cleanJson = text
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            val json = kotlinx.serialization.json.Json { 
                ignoreUnknownKeys = true 
                isLenient = true
            }
            val parsed = json.decodeFromString<AIGameSuggestionsDto>(cleanJson)
            
            Log.d(TAG, "Parsed ${parsed.games.size} game suggestions")
            Result.success(GeminiRepository.AIGameSuggestions(
                games = parsed.games,
                reasoning = parsed.reasoning
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting game suggestions", e)
            Result.failure(e)
        }
    }

    /**
     * Analyzes a user's gaming profile and returns personalized insights
     */
    override suspend fun analyzeGamingProfile(games: List<GameListEntry>): Result<GeminiRepository.GameInsights> {
        return try {
            if (games.isEmpty()) {
                return Result.failure(Exception("No games to analyze"))
            }

            val gameData = buildGameDataString(games)
            val prompt = GeminiPrompts.profileAnalysisPrompt(gameData)

            Log.d(TAG, "Sending prompt to Gemini AI...")

            val response = textModel.generateContent(prompt)
            val analysisText = response.text ?: throw Exception("Empty response from AI")

            Log.d(TAG, "Received response: ${analysisText.take(100)}...")

            val insights = parseAIResponse(analysisText, games)
            Result.success(insights)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing profile", e)
            Result.failure(e)
        }
    }

    /**
     * Builds a formatted string containing all game data for AI analysis
     */
    private fun buildGameDataString(games: List<GameListEntry>): String {
        val totalGames = games.size
        val finishedGames = games.count { it.status == GameStatus.FINISHED }
        val playingGames = games.count { it.status == GameStatus.PLAYING }
        val droppedGames = games.count { it.status == GameStatus.DROPPED }
        val wantToPlayGames = games.count { it.status == GameStatus.WANT }
        val onHoldGames = games.count { it.status == GameStatus.ON_HOLD }
        val totalHours = games.sumOf { it.hoursPlayed }

        val playedGamesCount = games.count { it.status != GameStatus.WANT }
        val completionRate = if (playedGamesCount > 0) {
            (finishedGames.toFloat() / playedGamesCount.toFloat() * 100).toInt()
        } else 0

        val ratedGames = games.filter { it.rating != null && it.rating > 0 }
        val avgRating = if (ratedGames.isNotEmpty()) {
            String.format(Locale.US, "%.1f", ratedGames.mapNotNull { it.rating }.average())
        } else "N/A"

        // Genre distribution
        val genreCounts = games.flatMap { it.genres }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(8)

        // Platform distribution
        val platformCounts = games.flatMap { it.platforms }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        // Aspect analysis
        val aspectCounts = games.flatMap { it.aspects }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(8)

        // Genre ratings
        val genreRatings = games.filter { it.rating != null && it.rating > 0 }
            .flatMap { game -> game.genres.map { genre -> genre to game.rating } }
            .groupBy { it.first }
            .mapValues { (_, ratings) -> ratings.mapNotNull { it.second }.average() }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        // High rated games
        val highRatedGames = games.filter { it.rating != null && it.rating >= 8.0f }
            .sortedByDescending { it.rating }
            .take(10)

        // Dropped games
        val dropped = games.filter { it.status == GameStatus.DROPPED }
            .take(5)

        return buildString {
            appendLine("=== GAMING PROFILE STATISTICS ===")
            appendLine("Total Games: $totalGames")
            appendLine("Finished: $finishedGames (${completionRate}%)")
            appendLine("Currently Playing: $playingGames")
            appendLine("Dropped: $droppedGames")
            appendLine("Want to Play: $wantToPlayGames")
            appendLine("On Hold: $onHoldGames")
            appendLine("Total Hours: $totalHours")
            appendLine("Average Rating: $avgRating/10")
            appendLine()

            appendLine("=== GENRE DISTRIBUTION ===")
            genreCounts.forEach { (genre, count) ->
                val percentage = (count.toFloat() / totalGames * 100).toInt()
                appendLine("$genre: $count games (${percentage}%)")
            }
            appendLine()

            appendLine("=== PLATFORM DISTRIBUTION ===")
            platformCounts.forEach { (platform, count) ->
                appendLine("$platform: $count games")
            }
            appendLine()

            if (genreRatings.isNotEmpty()) {
                appendLine("=== GENRE RATING AVERAGES ===")
                genreRatings.forEach { (genre, avgRating) ->
                    appendLine("$genre: ${String.format(Locale.US, "%.1f", avgRating)}/10")
                }
                appendLine()
            }

            if (aspectCounts.isNotEmpty()) {
                appendLine("=== VALUED GAME ASPECTS ===")
                aspectCounts.forEach { (aspect, count) ->
                    appendLine("$aspect: mentioned $count times")
                }
                appendLine()
            }

            if (highRatedGames.isNotEmpty()) {
                appendLine("=== FAVORITE GAMES (8+ rating) ===")
                highRatedGames.forEach { game ->
                    appendLine("${game.name} - ${game.rating}/10 - Genres: ${game.genres.joinToString(", ")}")
                    if (game.hoursPlayed > 0) appendLine("  Hours played: ${game.hoursPlayed}h")
                    if (game.aspects.isNotEmpty()) appendLine("  Aspects: ${game.aspects.joinToString(", ")}")
                }
                appendLine()
            }

            if (dropped.isNotEmpty()) {
                appendLine("=== DROPPED GAMES ===")
                dropped.forEach { game ->
                    appendLine("${game.name} - Genres: ${game.genres.joinToString(", ")}")
                }
                appendLine()
            }
        }
    }

    /**
     * Parses the AI response and extracts structured insights
     */
    private fun parseAIResponse(response: String, games: List<GameListEntry>): GeminiRepository.GameInsights {
        try {
            val personalitySection = extractSection(response, "PERSONALITY ANALYSIS")
            val playStyleSection = extractSection(response, "PLAY STYLE")
            val funFactsSection = extractSection(response, "FUN FACTS")
            val recommendationsSection = extractSection(response, "RECOMMENDATIONS")

            // Extract fun facts as a list
            val funFacts = funFactsSection.split("\n")
                .filter { it.trim().startsWith("•") || it.trim().startsWith("-") || it.trim().startsWith("*") }
                .map { it.trim().removePrefix("•").removePrefix("-").removePrefix("*").trim() }
                .take(3)

            // Extract top genres from game data
            val topGenres = games.flatMap { it.genres }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .take(3)
                .map { it.first }

            return GeminiRepository.GameInsights(
                personalityAnalysis = personalitySection.ifEmpty { "You're building your gaming journey! Keep adding games to discover your unique gaming identity." },
                preferredGenres = topGenres,
                playStyle = playStyleSection.ifEmpty { "Your play style is emerging as you build your collection." },
                funFacts = funFacts.ifEmpty { listOf("Add more games to unlock personalized insights!", "Rate your games to see deeper analysis.", "Track your play time for fun statistics!") },
                recommendations = recommendationsSection.ifEmpty { "Keep adding games to your library for personalized recommendations!" }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing AI response", e)
            throw Exception("Failed to parse AI response: ${e.message}")
        }
    }

    /**
     * Extracts a section from the AI response between markers
     */
    private fun extractSection(text: String, sectionName: String): String {
        val startMarker = "**$sectionName**"
        val startIndex = text.indexOf(startMarker)
        if (startIndex == -1) return ""

        val contentStart = startIndex + startMarker.length
        val nextSectionIndex = text.indexOf("**", contentStart)

        val sectionText = if (nextSectionIndex != -1) {
            text.substring(contentStart, nextSectionIndex)
        } else {
            text.substring(contentStart)
        }

        return sectionText.trim()
    }

    companion object {
        private const val TAG = "GeminiRepository"
    }
}

