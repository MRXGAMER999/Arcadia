package com.example.arcadia.data.remote

/**
 * Object containing optimized prompt templates for Gemini AI.
 * Prompts are condensed for minimal token usage while maintaining quality output.
 * 
 * Token Optimization Strategy:
 * - Remove redundant instructions
 * - Use concise language
 * - Rely on model's inherent capabilities
 * - Keep examples minimal but effective
 */
object GeminiPrompts {

    /**
     * Generates an optimized prompt for game suggestions.
     * Uses Chain-of-Thought prompting to improve relevance and diversity.
     *
     * @param userQuery The user's natural language query
     * @param count Number of games to suggest
     * @return Complete prompt string
     */
    fun gameSuggestionPrompt(userQuery: String, count: Int): String {
        val currentYear = java.time.Year.now().value
        val currentMonth = java.time.LocalDate.now().monthValue
        return """
Role: Expert Video Game Curator.
Task: Suggest $count video games based on the query: "$userQuery"

Analysis Steps (Internal):
1. Identify the core intent (Genre? Mood? "Like [Game]"? Specific Mechanic?).
2. Select games that match this intent, prioritizing high critical acclaim (>75 Metacritic) unless it's a specific "so bad it's good" request.
3. Ensure diversity: Mix AAA blockbusters with high-quality indie titles / hidden gems.
4. CRITICAL: Only include games that are ALREADY RELEASED as of $currentYear-$currentMonth. NO unreleased/upcoming games.
5. Order the final list by relevance and quality (best matches first).

Output Rules:
- JSON ONLY. No markdown.
- "games": Array of exact official English titles.
- "reasoning": A concise explanation of the common thread connecting these suggestions to the query.

Format:
{"games": ["Game 1", "Game 2"], "reasoning": "..."}
    """.trimIndent()
    }

    /**
     * Generates an optimized prompt for gaming profile analysis.
     * ~600 tokens vs ~1200 tokens in verbose version (50% reduction)
     *
     * @param gameData Formatted string containing user's game statistics
     * @return Complete prompt string
     */
    fun profileAnalysisPrompt(gameData: String): String = """
Analyze the player's gaming profile using ONLY the data below:

$gameData

STEP 1 — INTERNAL INTERPRETATION
Silently extract:
- Genre preferences (frequency, ratings, playtime)
- Playstyle tendencies (focus vs variety, long vs short games)
- Difficulty preference (casual, balanced, challenging)
- Completion habits (story finisher vs explorer vs sampler)
- Rating patterns (what they reward most: story, gameplay, art, mechanics)
- Engagement trends (hours, spikes, abandoned games)
Do NOT output this step.

STEP 2 — RULES (MUST FOLLOW)
- All insights must reference actual numbers from the data.
- No generic statements that could apply to anyone.
- No invented stats, no assumptions beyond trends present in the data.
- Focus on specific behavior patterns, not clichés.
- CRITICAL: Do NOT recommend any game listed in the "GAME LIBRARY" section. Suggest ONLY new games the user does not own.
- Ensure all recommended games are real, released titles (no unreleased/upcoming games).

STEP 3 — OUTPUT STRUCTURE  
Respond ONLY with this exact JSON:

{
  "personality": "2–3 sentences describing their gaming identity using specific numbers and patterns.",
  "play_style": "1–2 sentences describing how they play, referencing concrete behaviors.",
  "insights": [
    "One numeric insight about their habits or performance.",
    "One pattern they likely haven't noticed.",
    "One interesting comparison or anomaly in their data."
  ],
  "recommendations": "2–3 sentences recommending genres or game types based on their highest-rated patterns. Wrap any game title like <<GAME:Title>>."
}

No markdown. No extra commentary. JSON only.
""".trimIndent()

    /**
     * Generates an optimized prompt for library-based game recommendations.
     * This is the legacy version - use libraryBasedRecommendationPromptV2 for better results.
     *
     * @param libraryGames List of games in the user's library (name and genres)
     * @param count Number of games to suggest
     * @return Complete prompt string
     */
    fun libraryBasedRecommendationPrompt(libraryGames: String, count: Int): String {
        return libraryBasedRecommendationPromptV2(libraryGames, count)
    }
    
    /**
     * Enhanced prompt for library-based game recommendations with richer data.
     * Includes user ratings, play status, playtime for smarter recommendations.
     * Sorted by confidence score to show best matches first.
     *
     * @param libraryData Formatted string with full library data (name, rating, status, playtime, genres)
     * @param count Number of games to suggest
     * @return Complete prompt string
     */
    fun libraryBasedRecommendationPromptV2(libraryData: String, count: Int): String {
        return libraryBasedRecommendationPromptV3(libraryData, "", count)
    }
    
    /**
     * Enhanced recommendation prompt with explicit exclusion list.
     * Uses tier-based semantic classification instead of arithmetic scoring.
     * This ensures AI never recommends games the user already owns,
     * even if they weren't included in the detailed analysis.
     *
     * @param libraryData Formatted string with detailed library data for analysis (top games)
     * @param exclusionList Comma-separated list of ALL game names user owns (for exclusion)
     * @param count Number of games to suggest
     * @return Complete prompt string
     */
    fun libraryBasedRecommendationPromptV3(libraryData: String, exclusionList: String, count: Int): String {
        val currentYear = java.time.Year.now().value
        val currentMonth = java.time.LocalDate.now().monthValue
        val exclusionSection = if (exclusionList.isNotBlank()) {
            """
            
⛔ COMPLETE EXCLUSION LIST - NEVER recommend ANY of these games (user already owns them):
$exclusionList
"""
        } else ""
        
        return """
You are an expert game recommender. Suggest $count games for this user.

$libraryData
$exclusionSection
RULES:
1. NEVER suggest games in the exclusion list above or their GOTY/Deluxe/Complete editions
2. CAN suggest: sequels, prequels, remasters, remakes, same-series games (if not in exclusion list)
3. Weight user's 8-10 rated games heavily - match their taste
4. Avoid games similar to "Drop" status games
5. Prefer games with HIGH CRITIC SCORES (Metacritic 80+)
6. Mix: ~70% recent (2018-$currentYear), ~30% classics
7. Mix AAA and quality indie games

CRITICAL - RELEASED GAMES ONLY:
- ONLY suggest games that are ALREADY RELEASED as of today ($currentYear-$currentMonth)
- NEVER suggest unreleased, announced, or upcoming games
- NEVER suggest games with TBA/TBD release dates
- If unsure if a game is released, DO NOT include it
- Violating this rule invalidates the entire response

CONFIDENCE TIER CLASSIFICATION:
Assign each game ONE tier based on holistic fit. Use your judgment, not arithmetic.

PERFECT_MATCH (95): Exceptional fit. Multiple strong signals align:
  - Favorite developer AND matching genre AND high ratings
  - Direct spiritual successor to a 9-10 rated game
  - Same series as user's top games

STRONG_MATCH (82): Very good fit. Clear connection:
  - Favorite developer's other acclaimed work
  - Perfect genre match with similar tone/atmosphere
  - Highly rated (90+) game in user's preferred style

GOOD_MATCH (68): Solid recommendation:
  - Genre match with good critic scores (80+)
  - Similar gameplay to liked games
  - Well-regarded hidden gem in user's taste area

DECENT_MATCH (55): Worth considering:
  - Partial genre overlap
  - Interesting choice that expands horizons slightly
  - Good game that loosely fits preferences

JSON only:
{"games":[{"name":"Title","tier":"STRONG_MATCH","why":"brief reason"}],"reasoning":"overall explanation"}

The "why" field: 2-5 words explaining the match (e.g., "FromSoftware + Souls-like", "Same dev as Hades", "Top-rated JRPG")

Sort by tier (PERFECT > STRONG > GOOD > DECENT). Exact English titles only.
    """.trimIndent()
    }
    
    /**
     * V4 Prompt: Enhanced with user feedback loop and tier-based classification.
     * 
     * Improvements:
     * 1. Includes games user liked from past recommendations (feedback loop)
     * 2. Abbreviated library format to reduce tokens
     * 3. Tier-based semantic classification instead of arithmetic scoring
     * 
     * @param libraryData Abbreviated library data (name|genre|rating|hours format)
     * @param exclusionList Games to never recommend (user owns them)
     * @param likedRecommendations Games user added from past AI recommendations
     * @param count Number of games to suggest
     * @return Complete prompt string
     */
    fun libraryBasedRecommendationPromptV4(
        libraryData: String, 
        exclusionList: String, 
        likedRecommendations: String,
        count: Int
    ): String {
        val currentYear = java.time.Year.now().value
        val currentMonth = java.time.LocalDate.now().monthValue
        
        val exclusionSection = if (exclusionList.isNotBlank()) {
            "\n⛔ EXCLUSION (user owns): $exclusionList"
        } else ""
        
        val feedbackSection = if (likedRecommendations.isNotBlank()) {
            "\n✅ USER LIKED THESE RECOMMENDATIONS: $likedRecommendations\n→ Similar games to these should be PERFECT_MATCH or STRONG_MATCH!"
        } else ""
        
        return """
Expert game recommender. Suggest $count games.

📚 LIBRARY (name|genre|rating|hours):
$libraryData
$exclusionSection
$feedbackSection

RULES:
1. NEVER recommend games in exclusion list or their editions (GOTY/Deluxe/etc)
2. CAN suggest: sequels, prequels, remasters if not excluded
3. Weight 8-10 rated games heavily
4. Avoid games like "Drop" status ones
5. Prefer Metacritic 80+ games
6. Mix: 70% recent (2018-$currentYear), 30% classics
7. RELEASED ONLY as of $currentYear-$currentMonth. NO upcoming games.

CONFIDENCE TIERS (assign ONE per game):
PERFECT_MATCH: Exceptional fit - favorite dev + genre match + high ratings, or direct spiritual successor to 9-10 rated game
STRONG_MATCH: Very good fit - favorite dev's work, perfect genre match, or 90+ Metacritic in user's style
GOOD_MATCH: Solid pick - genre match with 80+ scores, similar gameplay to liked games
DECENT_MATCH: Worth trying - partial overlap, expands horizons slightly

JSON:{"games":[{"name":"Title","tier":"STRONG_MATCH","why":"brief reason"}],"reasoning":"brief"}
"why": 2-5 words (e.g., "FromSoftware + Souls-like", "Top JRPG")
Sort by tier. Exact titles.
    """.trimIndent()
    }
    
    /**
     * Build abbreviated library string for token optimization.
     * Format: "GameName|RPG,Action|9|150h" instead of verbose descriptions.
     * 
     * Reduces token usage by ~40% compared to V3 format.
     */
    fun buildAbbreviatedLibraryString(
        games: List<GameLibraryItem>,
        maxGames: Int = 30
    ): String {
        return games
            .sortedByDescending { (it.rating ?: 0f) * (it.hoursPlayed ?: 1) }
            .take(maxGames)
            .joinToString("\n") { game ->
                val genres = game.genres.take(2).joinToString(",")
                val rating = game.rating?.toInt() ?: "?"
                val hours = game.hoursPlayed?.let { "${it}h" } ?: "?"
                "${game.name}|$genres|$rating|$hours"
            }
    }
    
    /**
     * Simple data class for library items (to avoid domain model dependency)
     */
    data class GameLibraryItem(
        val name: String,
        val genres: List<String>,
        val rating: Float?,
        val hoursPlayed: Int?,
        val status: String?
    )
}
