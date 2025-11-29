package com.example.arcadia.data.remote

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

OUTPUT FORMAT (JSON ONLY):
{
  "personality": "A warm, insightful description of their gaming identity (2-3 sentences). Focus on 'why' they play.",
  "play_style": "Describe how they approach games (e.g., 'You're a completionist who...', 'You prefer short, intense experiences...').",
  "insights": [
    "A surprising observation about their habits.",
    "A specific strength or quirk in their gaming history.",
    "A pattern they might not have noticed themselves."
  ],
  "recommendations": "Recommend 3 specific games they DO NOT own. Wrap titles in <<GAME:Title>> tags (e.g. 'You should try <<GAME:Elden Ring>> because...'). Explain why for each."
}
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
    
    // Removed unused "V2", "V4" and helper methods to keep the file clean and focused.
}