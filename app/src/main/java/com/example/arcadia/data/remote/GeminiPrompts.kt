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
        val currentYear = java.time.Year.now().value
        val currentMonth = java.time.LocalDate.now().monthValue
        return """
You are an expert game recommender. Suggest $count games for this user.

$libraryData

RULES:
1. NEVER suggest games already in library or their GOTY/Deluxe/Complete editions
2. CAN suggest: sequels, prequels, remasters, remakes, same-series games
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

DEVELOPER/PUBLISHER BOOST:
- Check FAVORITE DEVS/PUBS in stats above
- If a dev has 3+ games in library: +15 confidence for their other games
- If a dev has 2 games: +10 confidence
- Example: User has 4 FromSoftware games → strongly recommend other FromSoftware titles
- This is a MAJOR signal - users often love specific developers' style

CONFIDENCE SCORING:
- Favorite developer match (3+ games): +15
- Favorite developer match (2 games): +10
- Metacritic 90+: +10
- Metacritic 80-89: +5
- Perfect genre match: +15
- Similar to highly-rated (9-10) game: +10

JSON only:
{"games":[{"name":"Title","confidence":95}],"reasoning":"brief explanation"}

Sort by confidence descending. Exact English titles only.
    """.trimIndent()
    }
}
