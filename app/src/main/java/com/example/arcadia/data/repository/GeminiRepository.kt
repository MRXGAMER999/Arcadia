package com.example.arcadia.data.repository

import android.util.Log
import com.example.arcadia.BuildConfig
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GeminiRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Implementation of GeminiRepository for interacting with Google's Gemini AI
 */
class GeminiRepositoryImpl : GeminiRepository {

    // Model for structured data tasks (Game Suggestions) - Strict, JSON-only
    private val jsonModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.4f // Lower temperature for accuracy
            topK = 32
            topP = 0.9f
            maxOutputTokens = 2048
            responseMimeType = "application/json"
        }
    )

    // Model for creative writing tasks (Profile Analysis) - Creative, Free-form text
    private val textModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.8f // Higher temperature for creative personality analysis
            topK = 40
            topP = 0.95f
            maxOutputTokens = 2048
            // No MIME type enforcement - allows natural text
        }
    )

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
            val prompt = """
You are an elite gaming curator and industry expert with deep knowledge of game mechanics, narrative structures, and player psychology. Your goal is to provide the perfect game recommendations based on the user's intent.

User Query: "$userQuery"
Target Count: $count games

# Analysis Steps:
1. **Decode Intent:** Analyze the user's query to understand what they *really* want (e.g., "relaxing" = low stress/cozy; "hard" = challenge/mastery; "story" = narrative focus).
2. **Identify Key Elements:** If they mention a specific game (e.g., "like Elden Ring"), identify its core pillars (exploration, difficulty, environmental storytelling) and find games that share those specific pillars.
3. **Select Candidates:** Choose exactly $count games that best fit the criteria.

# Selection Rules:
1. **Strict Reality Check:** Return ONLY real, commercially released video games. No mods, no unreleased titles, no hallucinations.
2. **Title Precision:** Use the exact official English title (e.g., "The Legend of Zelda: Breath of the Wild", not just "BotW").
3. **Quality Filter:** Prioritize games with high critical acclaim (Metacritic/Steam ratings) unless the user specifically asks for "bad" or "trash" games.
4. **Diversity:** Unless the query is very specific (e.g., "FPS games"), provide a mix of AAA hits and top-tier indie gems.
5. **Relevance:** If a time period is mentioned, strictly adhere to it.
6. **Sorting:** Order the list by relevance to the query, with the absolute best match first.

# Output Format:
Respond ONLY with a valid JSON object. Do not include markdown formatting (like ```json).
{
  "games": ["Exact Title 1", "Exact Title 2", ...],
  "reasoning": "A concise, persuasive summary (max 2 sentences) explaining WHY these specific games fit the user's query. Highlight the common thread connecting them."
}

# Examples:
- Query: "games like Stardew Valley"
  -> {"games": ["Animal Crossing: New Horizons", "Graveyard Keeper", "Coral Island", ...], "reasoning": "These titles capture the cozy farming simulation loop, relationship building, and relaxing pacing you enjoy in Stardew Valley."}

- Query: "hardest games ever"
  -> {"games": ["Sekiro: Shadows Die Twice", "Super Meat Boy", "Celeste", ...], "reasoning": "These games are renowned for their punishing difficulty curves, requiring precise mechanical mastery and patience."}
            """.trimIndent()

            Log.d("GeminiRepository", "Asking Gemini for game suggestions: $userQuery")
            
            val response = jsonModel.generateContent(prompt)
            val text = response.text?.trim() ?: throw Exception("Empty response from AI")
            
            Log.d("GeminiRepository", "Gemini response: $text")
            
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
            
            Log.d("GeminiRepository", "Parsed ${parsed.games.size} game suggestions")
            Result.success(GeminiRepository.AIGameSuggestions(
                games = parsed.games,
                reasoning = parsed.reasoning
            ))
            
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error getting game suggestions", e)
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
            val prompt = buildAnalysisPrompt(gameData)

            Log.d("GeminiRepository", "Sending prompt to Gemini AI...")

            val response = textModel.generateContent(prompt)
            val analysisText = response.text ?: throw Exception("Empty response from AI")

            Log.d("GeminiRepository", "Received response: ${analysisText.take(100)}...")

            val insights = parseAIResponse(analysisText, games)
            Result.success(insights)
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error analyzing profile", e)
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

    private fun buildAnalysisPrompt(gameData: String): String {
        return """
You are an expert gaming psychologist and data analyst specializing in player behavior patterns. Analyze this gaming profile and provide deep, personalized insights that feel truly unique to this player.

$gameData

# Analysis Guidelines:
- Be specific and reference actual numbers from their data
- Avoid generic statements that could apply to anyone
- Use an enthusiastic, friendly tone that makes insights feel like discoveries
- Focus on patterns, not just statistics
- Make connections between different data points (e.g., "You rate Action games 2 points higher than average")

# Output Format (follow exactly):

**PERSONALITY ANALYSIS**
Write 2-3 sentences that capture this player's unique gaming identity. Consider:
- Their completion rate (are they a finisher or explorer?)
- Genre diversity (specialist vs generalist?)
- Rating patterns (harsh critic or easy to please?)
- Time investment (casual weekends or dedicated daily player?)
- What their dropped games reveal about their preferences

Example: "You're a selective perfectionist with refined tastes. Your 85% completion rate shows you choose games carefully and see them through, while your 9.2 average rating reveals you know what you love before hitting 'play.' The 200+ hours invested suggest gaming isn't just a hobby—it's a passion."

**PLAY STYLE**
Describe in 1-2 sentences HOW they play games. Consider:
- Do they focus on one game or juggle multiple?
- Quick experiences or epic journeys?
- Genre hopper or loyal to a niche?
- What their platform choices reveal

Example: "You're a marathon runner, not a sprinter—committing deeply to expansive worlds rather than chasing the next release. Your 3 concurrent 'Playing' games suggest you like variety, but the 50+ hour average playtime shows you're no quitter."

**FUN FACTS**
Provide exactly 3 specific, data-driven fun facts. Make them surprising and personal:
- Start each with "•"
- Reference actual games, numbers, or patterns from their data
- Make observations they might not have noticed
- Compare to typical gaming behaviors when relevant
- Use concrete numbers

Examples:
• You've invested 47 hours into Strategy games but rated them 8.5/10 on average—higher than your 7.8 RPG average despite playing 3x more RPGs. Hidden passion?
• Your completion rate drops to 40% for games over 60 hours, but jumps to 95% for 20-40 hour experiences. You're a 'sweet spot' gamer who loves meaty, not marathon content.
• Every game you've given a 10/10 rating features either 'Great Story' or 'Emotional' tags—narrative clearly hooks you harder than gameplay innovation.

**RECOMMENDATIONS**
In 2-3 sentences, suggest specific game types, genres, or even franchises based on:
- Their highest-rated genres
- Underexplored genres that match their valued aspects
- Gaps in their library that complement their preferences
- Consider their dropped games to avoid bad matches

Example: "Based on your love for narrative-driven RPGs with tactical combat, games like 'Divinity: Original Sin 2' or 'Disco Elysium' would be perfect additions. Your high ratings for indie titles suggest you'd appreciate 'Hades' or 'Hollow Knight' for that sweet spot 30-hour playtime you favor. Consider branching into Metroidvanias—they share your loved 'Challenging' aspect but offer tighter, more replayable experiences."

# Critical Rules:
- NEVER use placeholder phrases like "based on the data" or "according to your profile"
- ALWAYS use specific numbers from their actual data
- Make every sentence feel like a personal discovery about THEM
- If they have <5 games, acknowledge they're building their collection and focus on what their choices reveal
- If completion rate is low, frame it positively ("You're an explorer sampling the vast gaming landscape")
- Connect patterns across sections (reference genres in fun facts, etc.)
- When mentioning specific game titles, wrap them with <<GAME: and >> markers. For example: "<<GAME:The Witcher 3>>" or "<<GAME:Dark Souls III>>"

Now analyze this player's profile with these guidelines in mind.
        """.trimIndent()
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
            Log.e("GeminiRepository", "Error parsing AI response", e)
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
}

