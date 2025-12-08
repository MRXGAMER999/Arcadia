package com.example.arcadia.data.remote

import com.example.arcadia.domain.model.ai.RoastStats

/**
 * Object containing natural language prompt templates for Gemini AI.
 * Focused on qualitative analysis and human-like curation over "scoring" or "math".
 */
object GeminiPrompts {

    /**
     * Generates a prompt for general game suggestions.
     * Persona: Expert Game Curator.
     */
    fun gameSuggestionPrompt(userQuery: String, count: Int): String {
        val currentYear = java.time.Year.now().value
        val currentMonth = java.time.LocalDate.now().monthValue
        
        return """
You are a world-class Video Game Curator. The user has a request: "$userQuery"

Your task: Curate a collection of $count games that perfectly answer this request.

GUIDELINES:
1.  **Understand the Vibe**: Look beyond just keywords. What is the *feeling* the user wants?
2.  **Quality First**: Prioritize games that are generally well-regarded or cult classics.
3.  **Diverse Selection**: Unless the user asked for a specific franchise, try to mix big hits with hidden gems.
4.  **Released Games Only**: Suggest ONLY games released before $currentYear-$currentMonth. Do not suggest upcoming titles.

OUTPUT FORMAT (JSON ONLY):
{
  "games": ["Exact Title 1", "Exact Title 2"],
  "reasoning": "A brief, conversational note to the user explaining why these games were chosen for them."
}
""".trimIndent()
    }

    /**
     * Generates a prompt for gaming profile analysis.
     * Persona: Gaming Psychologist / Profiler.
     * 
     * Uses a hybrid format: plain text sections followed by JSON recommendations.
     * This is more reliable than full JSON since LLMs handle prose better.
     */
    fun profileAnalysisPrompt(gameData: String): String = """
Act as a "Gaming Psychologist" and analyze the user's gaming history below.
Your goal is to understand *who* they are as a player, not just count stats.

$gameData

ANALYSIS GOALS:
1.  **Identify the "Player DNA"**: What motivates them? Challenge? Story? Exploration? Comfort?
2.  **Spot Patterns**: Do they binge-play? Do they drop long games? Do they stick to one developer?
3.  **Avoid Generic Advice**: Don't say "try new genres". Be specific based on their actual behavior.
4.  **STRICTLY EXCLUDE OWNED GAMES**: NEVER recommend a game that appears in the user's history above. This is critical.

OUTPUT FORMAT (Use EXACTLY this structure with section markers):

===PERSONALITY===
Write 2-3 warm, insightful sentences about their gaming identity. Focus on 'why' they play.

===PLAY_STYLE===
Describe how they approach games (e.g., 'You're a completionist who...', 'You prefer short, intense experiences...').

===INSIGHTS===
- A surprising observation about their habits.
- A specific strength or quirk in their gaming history.
- A pattern they might not have noticed themselves.

===RECOMMENDATIONS===
{"games":[{"name":"Exact Game Title","reason":"Why this game fits them"},{"name":"Another Title","reason":"Why this fits"},{"name":"Third Title","reason":"Why this fits"}]}

RULES:
- Write naturally in the text sections, no JSON there
- The RECOMMENDATIONS section MUST be valid JSON on a single line
- Recommend exactly 3 games they DO NOT own
- Game names must be exact titles that exist in game databases
""".trimIndent()

    /**
     * Generates a prompt for library-based recommendations.
     * Persona: The ultimate "I know a guy" for games.
     * 
     * @param libraryData String representation of the user's library.
     * @param exclusionList Comma-separated list of games to NOT recommend.
     * @param count Number of recommendations to generate.
     */
    fun libraryBasedRecommendationPromptV3(libraryData: String, exclusionList: String, count: Int): String {
        val currentYear = java.time.Year.now().value
        val currentMonth = java.time.LocalDate.now().monthValue
        
        val exclusionSection = if (exclusionList.isNotBlank()) {
            "\n⛔ DO NOT RECOMMEND THESE (Already Owned/Recommended): $exclusionList"
        } else ""
        
        return """
You are a gaming expert. Suggest exactly $count games based on the user's library.

LIBRARY:
$libraryData
$exclusionSection

RULES:
1. NO games from exclusion list above
2. Only released games (as of $currentYear-$currentMonth)
3. Match their favorite developers/genres
4. Mix: 60% modern (2018+), 30% classic (2010-2017), 10% older gems

TIERS:
- PERFECT_MATCH: Same studio as favorite or spiritual successor
- STRONG_MATCH: Matches 3+ aspects of taste
- GOOD_MATCH: Matches genre + quality
- DECENT_MATCH: Expands horizons

OUTPUT JSON:
{
  "games": [
    {
      "name": "Exact Game Title",
      "tier": "PERFECT_MATCH",
      "why": "Short 1-2 sentence reason connecting to their library",
      "badges": ["Tag1", "Tag2"],
      "developer": "Studio Name",
      "year": 2022,
      "similarTo": ["Game1", "Game2"]
    }
  ],
  "reasoning": "1 sentence summary"
}

IMPORTANT:
- "why" field: 1-2 SHORT sentences max. Be concise!
- badges: 2-3 short tags (2-4 words each)
- name must match RAWG database exactly
""".trimIndent()
    }

    /**
     * Generates a prompt for personalized gaming roasts.
     * Persona: Savage but wholesome gaming roast comedian.
     * 
     * The roast uses specific user stats to create personalized, absurd comparisons
     * while always ending on a positive note to make it shareable.
     * 
     * @param stats The user's gaming statistics for personalization.
     * @return A prompt string that generates structured roast content.
     * 
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
     */
    fun gamingRoastPrompt(stats: RoastStats): String {
        val topGenresFormatted = stats.topGenres.joinToString { "${it.first} (${it.second})" }
        
        return """
You are a savage but ultimately wholesome gaming roast comedian. 
Roast this gamer based on their stats, but end on a positive note.

STATS:
- Hours Played: ${stats.hoursPlayed}
- Total Games: ${stats.totalGames}
- Completion Rate: ${stats.completionRate}%
- Completed: ${stats.completedGames}, Dropped: ${stats.droppedGames}
- Top Genres: $topGenresFormatted
- Gaming Personality: ${stats.gamingPersonality}
- Average Rating: ${stats.averageRating}

RULES:
- Be SPECIFIC - use their actual numbers in jokes
- Be ABSURD - funnier comparisons are better
- NO mean-spirited personal attacks - only mock gaming habits
- Calculate real-world comparisons based on actual hours
- Always end wholesome - makes it shareable

OUTPUT FORMAT (use EXACTLY these section markers):

===HEADLINE===
[One killer opening roast line - references their specific stats]

===COULD_HAVE===
[5 absurd/funny real-world things they could have accomplished]
[Each on its own line, be specific and creative]
[Mix practical and absurd comparisons]

===PREDICTION===
[Funny prediction about their gaming future based on current trajectory]

===WHOLESOME===
[Genuine positive closer - makes it shareable without feeling mean]

===ROAST_TITLE===
[Short funny title for their gamer profile - becomes a badge, 2-4 words]

===ROAST_EMOJI===
[Single emoji that represents their roast title]
""".trimIndent()
    }

    /**
     * Generates a prompt for AI badge generation based on gaming stats.
     * Persona: Achievement badge creator with humor.
     * 
     * Generates 5-7 unique badges with titles, emojis, and reasons
     * based on the user's gaming patterns and statistics.
     * 
     * @param stats The user's gaming statistics for badge generation.
     * @return A prompt string that generates JSON-formatted badges.
     * 
     * Requirements: 7.1, 7.2
     */
    fun badgeGenerationPrompt(stats: RoastStats): String {
        val topGenresFormatted = stats.topGenres.joinToString { "${it.first} (${it.second})" }
        
        return """
Generate 5-7 unique, funny achievement badges for this gamer.

STATS:
- Hours Played: ${stats.hoursPlayed}
- Total Games: ${stats.totalGames}
- Completion Rate: ${stats.completionRate}%
- Completed: ${stats.completedGames}, Dropped: ${stats.droppedGames}
- Top Genres: $topGenresFormatted
- Gaming Personality: ${stats.gamingPersonality}
- Average Rating: ${stats.averageRating}

RULES:
- Each badge should be unique and specific to their stats
- Titles should be 2-4 words, punchy and memorable
- Reasons should be one-liner explanations (funny)
- Mix positive and self-deprecating badges

OUTPUT FORMAT (JSON only):
{
  "badges": [
    {"title": "Badge Title", "emoji": "🎮", "reason": "Why they earned this"},
    ...
  ]
}
""".trimIndent()
    }
}