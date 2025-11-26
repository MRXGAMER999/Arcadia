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
    fun gameSuggestionPrompt(userQuery: String, count: Int): String = """
Role: Expert Video Game Curator.
Task: Suggest $count video games based on the query: "$userQuery"

Analysis Steps (Internal):
1. Identify the core intent (Genre? Mood? "Like [Game]"? Specific Mechanic?).
2. Select games that match this intent, prioritizing high critical acclaim (>75 Metacritic) unless it's a specific "so bad it's good" request.
3. Ensure diversity: Mix AAA blockbusters with high-quality indie titles / hidden gems.
4. Verify all games are real, released titles with exact English names.
5. Order the final list by relevance and quality (best matches first).

Output Rules:
- JSON ONLY. No markdown.
- "games": Array of exact official English titles.
- "reasoning": A concise explanation of the common thread connecting these suggestions to the query.

Format:
{"games": ["Game 1", "Game 2"], "reasoning": "..."}
    """.trimIndent()

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

}
