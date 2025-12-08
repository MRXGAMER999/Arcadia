package com.example.arcadia.domain.repository

import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.ai.AIGameSuggestions
import com.example.arcadia.domain.model.ai.Badge
import com.example.arcadia.domain.model.ai.GameInsights
import com.example.arcadia.domain.model.ai.RoastInsights
import com.example.arcadia.domain.model.ai.RoastStats
import com.example.arcadia.domain.model.ai.StreamingInsights
import com.example.arcadia.domain.model.ai.StudioExpansionResult
import com.example.arcadia.domain.model.ai.StudioMatch
import com.example.arcadia.domain.model.ai.StudioSearchResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for interacting with AI services for game analysis and suggestions.
 * This interface abstracts the underlying AI provider (Gemini, Groq, etc.) allowing
 * easy switching between providers.
 * 
 * Following Clean Architecture principles, this interface only contains method signatures.
 * Data classes are defined separately in domain/model/ai/ package.
 */
interface AIRepository {

    // ==================== Game Suggestions ====================

    /**
     * Ask AI to suggest game names based on a natural language query.
     * Results are cached to avoid redundant API calls.
     *
     * @param userQuery The user's natural language query for game suggestions
     * @param count The number of games to suggest
     * @param forceRefresh If true, bypass cache and fetch fresh results
     * @return Result containing AI game suggestions or an error
     */
    suspend fun suggestGames(
        userQuery: String, 
        count: Int = 10,
        forceRefresh: Boolean = false
    ): Result<AIGameSuggestions>

    /**
     * Ask AI to suggest games based on the user's existing library.
     *
     * @param games List of games in the user's library
     * @param count Number of games to suggest
     * @param forceRefresh Whether to bypass cache
     * @param excludeGames List of game names to exclude from recommendations (already recommended)
     * @return Result containing AI game suggestions or an error
     */
    suspend fun getLibraryBasedRecommendations(
        games: List<GameListEntry>,
        count: Int = 10,
        forceRefresh: Boolean = false,
        excludeGames: List<String> = emptyList()
    ): Result<AIGameSuggestions>

    // ==================== Profile Analysis ====================

    /**
     * Analyzes a user's gaming profile and returns personalized insights
     *
     * @param games The list of games in the user's library
     * @return Result containing game insights or an error
     */
    suspend fun analyzeGamingProfile(games: List<GameListEntry>): Result<GameInsights>
    
    /**
     * Analyzes a user's gaming profile with streaming support.
     * Emits partial results as they are generated for better UX.
     *
     * @param games The list of games in the user's library
     * @return Flow emitting streaming insights
     */
    fun analyzeGamingProfileStreaming(games: List<GameListEntry>): Flow<StreamingInsights>
    
    // ==================== Studio Expansion ====================
    
    /**
     * Get expanded studios (subsidiaries) for a parent studio.
     * Returns display names for UI presentation.
     *
     * @param parentStudio The name of the parent studio
     * @return Set of studio display names including the parent and all subsidiaries
     */
    suspend fun getExpandedStudios(parentStudio: String): Set<String>
    
    /**
     * Get comma-separated slugs for RAWG API filtering.
     *
     * @param parentStudio The name of the parent studio
     * @return Comma-separated string of studio slugs for API queries
     */
    suspend fun getStudioSlugs(parentStudio: String): String
    
    /**
     * Get full expansion result with both names and slugs.
     *
     * @param parentStudio The name of the parent studio
     * @return StudioExpansionResult containing both display names and slugs
     */
    suspend fun getStudioExpansionResult(parentStudio: String): StudioExpansionResult
    
    /**
     * Expand multiple studios in parallel.
     *
     * @param studios List of parent studio names
     * @return Map of studio name to expansion result
     */
    suspend fun expandMultipleStudios(studios: List<String>): Map<String, StudioExpansionResult>
    
    /**
     * Search for studios/publishers matching a query.
     * Returns instant results from local cache, then enriches with AI results.
     * 
     * @param query The search query (partial studio name)
     * @param includePublishers Whether to include publishers in results
     * @param includeDevelopers Whether to include developers in results
     * @param limit Maximum number of results to return
     * @return StudioSearchResult with matching studios
     */
    suspend fun searchStudios(
        query: String,
        includePublishers: Boolean = true,
        includeDevelopers: Boolean = true,
        limit: Int = 10
    ): StudioSearchResult
    
    /**
     * Get instant local suggestions (from hardcoded + cached studios).
     * This is synchronous and returns immediately for responsive UI.
     * 
     * @param query The search query (partial studio name)
     * @param limit Maximum number of suggestions
     * @return List of studio matches from local data only
     */
    fun getLocalStudioSuggestions(query: String, limit: Int = 5): List<StudioMatch>
    
    // ==================== Cache Management ====================
    
    /**
     * Clears all caches (suggestions, profile analysis, and studio expansion)
     */
    fun clearCache()
    
    // ==================== Roast Generation ====================
    
    /**
     * Generates a personalized roast based on the user's gaming statistics.
     * The roast includes a headline, list of things they could have done,
     * a prediction, a wholesome closer, and a roast title with emoji.
     *
     * @param stats The user's gaming statistics for personalization
     * @return Result containing RoastInsights on success, or an error
     * 
     * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
     */
    suspend fun generateRoast(stats: RoastStats): Result<RoastInsights>

    /**
     * Generates a roast using streaming, emitting chunks as they arrive.
     * 
     * @param stats The user's gaming statistics for personalization
     * @return Flow emitting chunks of the generated text
     * 
     * Requirements: 3.1
     */
    fun generateRoastStreaming(stats: RoastStats): Flow<String>
    
    /**
     * Generates AI-powered badges based on the user's gaming patterns.
     * Returns 5-7 unique badges with titles, emojis, and reasons.
     *
     * @param stats The user's gaming statistics for badge generation
     * @return Result containing list of Badges on success, or an error
     * 
     * Requirements: 7.1, 7.2
     */
    suspend fun generateBadges(stats: RoastStats): Result<List<Badge>>
}
