package com.example.arcadia.data.repository

import android.util.Log
import android.util.LruCache
import com.example.arcadia.data.local.HardcodedStudioMappings
import com.example.arcadia.data.local.StudioCacheManager
import com.example.arcadia.data.remote.AIClient
import com.example.arcadia.data.remote.AIClientException
import com.example.arcadia.data.remote.GeminiPrompts
import com.example.arcadia.domain.model.AIError
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.model.ai.AIGameSuggestions
import com.example.arcadia.domain.model.ai.GameRecommendation
import com.example.arcadia.domain.model.ai.MatchReason
import com.example.arcadia.domain.model.ai.GameInsights
import com.example.arcadia.domain.model.ai.StreamingInsights
import com.example.arcadia.domain.model.ai.StudioExpansionResult
import com.example.arcadia.domain.model.ai.StudioMatch
import com.example.arcadia.domain.model.ai.StudioSearchResult
import com.example.arcadia.domain.model.ai.StudioType
import com.example.arcadia.domain.repository.AIRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Base implementation of AIRepository containing all shared business logic.
 * 
 * This abstract class handles:
 * - Caching with TTL for suggestions and profile analysis
 * - Request deduplication to prevent duplicate API calls
 * - Response parsing (all DTO definitions and parsing logic)
 * - Studio expansion with multi-layer caching
 * - Local studio suggestions from hardcoded mappings
 * - Library data formatting for AI analysis
 * - Error handling and AIError mapping
 * 
 * Concrete implementations (GeminiRepository, GroqRepository) only need to:
 * 1. Provide an AIClient instance
 * 2. Optionally provide a StudioCacheManager
 * 
 * This eliminates ~1000 lines of duplicated code per implementation.
 */
abstract class BaseAIRepository(
    private val aiClient: AIClient,
    private val studioCacheManager: StudioCacheManager? = null
) : AIRepository {
    
    // ==================== Logging ====================
    
    protected open val TAG: String = "BaseAIRepository"
    
    // ==================== JSON Parser ====================
    
    protected val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    // ==================== Caching ====================
    
    private data class CacheEntry(
        val suggestions: AIGameSuggestions,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = 
            System.currentTimeMillis() - timestamp > CACHE_TTL_MS
    }
    
    private data class ProfileCacheEntry(
        val insights: GameInsights,
        val gameCount: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = 
            System.currentTimeMillis() - timestamp > PROFILE_CACHE_TTL_MS
    }
    
    private val suggestionsCache = LruCache<String, CacheEntry>(MAX_CACHE_SIZE)
    private var profileCache: ProfileCacheEntry? = null
    private var profileCacheKey: String? = null
    
    // ==================== Request Deduplication ====================
    
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<Result<AIGameSuggestions>>>()
    private val studioInflightRequests = ConcurrentHashMap<String, Deferred<StudioExpansionResult>>()
    private val cacheMutex = Mutex()
    private val deduplicationMutex = Mutex()
    
    // ==================== Utility ====================
    
    private val slugCleanupRegex = Regex("[^a-z0-9-]")
    
    private fun getCacheKey(query: String, count: Int): String =
        "${query.lowercase().trim()}|$count"
    
    // ==================== DTOs for JSON Parsing ====================
    
    @Serializable
    protected data class AIGameSuggestionsDto(
        val games: List<String> = emptyList(),
        val reasoning: String? = null
    )
    
    @Serializable
    protected data class GameRecommendationDto(
        val name: String,
        val confidence: Int = 50
    )
    
    @Serializable
    protected data class GameRecommendationTierDto(
        val name: String,
        val tier: String = "GOOD_MATCH",
        val why: String? = null,
        val badges: List<String> = emptyList(),
        val developer: String? = null,
        val year: Int? = null,
        val similarTo: List<String> = emptyList()
    )
    
    @Serializable
    protected data class GameRecommendationV3Dto(
        val name: String,
        val reasons: List<String> = emptyList(),
        val why: String? = null
    )
    
    @Serializable
    protected data class AIGameSuggestionsV2Dto(
        val games: List<GameRecommendationDto> = emptyList(),
        val reasoning: String? = null
    )
    
    @Serializable
    protected data class AIGameSuggestionsTierDto(
        val games: List<GameRecommendationTierDto> = emptyList(),
        val reasoning: String? = null
    )
    
    @Serializable
    protected data class AIGameSuggestionsV3Dto(
        val games: List<GameRecommendationV3Dto> = emptyList(),
        val reasoning: String? = null
    )
    
    @Serializable
    protected data class ProfileAnalysisDto(
        val personality: String,
        val play_style: String,
        val insights: List<String>,
        val recommendations: String
    )
    
    @Serializable
    protected data class StudioExpansionResponse(
        val studios: List<StudioInfo>
    )
    
    @Serializable
    protected data class StudioInfo(
        val name: String,
        val slug: String
    )
    
    @Serializable
    protected data class StudioSearchResponse(
        val studios: List<StudioSearchInfo>,
        val queryType: String = "studio"
    )
    
    @Serializable
    protected data class StudioSearchInfo(
        val name: String,
        val slug: String,
        val type: String,
        val subsidiaries: Int = 0,
        val relevance: String = "direct"
    )

    // ==================== Game Suggestions ====================

    override suspend fun suggestGames(
        userQuery: String,
        count: Int,
        forceRefresh: Boolean
    ): Result<AIGameSuggestions> {
        val cacheKey = getCacheKey(userQuery, count)
        
        // Check cache first (unless force refresh)
        if (!forceRefresh) {
            cacheMutex.withLock {
                suggestionsCache.get(cacheKey)?.let { entry ->
                    if (!entry.isExpired()) {
                        Log.d(TAG, "Cache hit for query: $userQuery")
                        return Result.success(entry.suggestions.copy(fromCache = true))
                    } else {
                        suggestionsCache.remove(cacheKey)
                    }
                }
            }
        }
        
        // Check for in-flight request (deduplication)
        deduplicationMutex.withLock {
            inFlightRequests[cacheKey]?.let { deferred ->
                Log.d(TAG, "Joining existing request for query: $userQuery")
                return deferred.await()
            }
        }
        
        // Create new request with deduplication
        val deferred = CoroutineScope(Dispatchers.IO).async {
            performSuggestGames(userQuery, count, cacheKey)
        }
        
        deduplicationMutex.withLock {
            inFlightRequests[cacheKey] = deferred
        }
        
        return try {
            deferred.await()
        } finally {
            deduplicationMutex.withLock {
                inFlightRequests.remove(cacheKey)
            }
        }
    }
    
    private suspend fun performSuggestGames(
        userQuery: String,
        count: Int,
        cacheKey: String
    ): Result<AIGameSuggestions> {
        return try {
            val prompt = GeminiPrompts.gameSuggestionPrompt(userQuery, count)
            
            Log.d(TAG, "Asking ${aiClient.providerName} for game suggestions: $userQuery")
            
            val text = aiClient.generateJsonContent(prompt)
            
            Log.d(TAG, "AI response: $text")
            
            val parsed = try {
                json.decodeFromString<AIGameSuggestionsDto>(text)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse JSON response", e)
                return Result.failure(AIError.InvalidResponseError(rawResponse = text))
            }
            
            if (parsed.games.isEmpty()) {
                return Result.failure(AIError.EmptyResponseError(
                    message = "No games found for your query. Try a different search."
                ))
            }
            
            Log.d(TAG, "Parsed ${parsed.games.size} game suggestions")
            
            val suggestions = AIGameSuggestions(
                games = parsed.games,
                reasoning = parsed.reasoning,
                fromCache = false
            )
            
            // Cache the result
            cacheMutex.withLock {
                suggestionsCache.put(cacheKey, CacheEntry(suggestions))
            }
            
            Result.success(suggestions)
            
        } catch (e: AIClientException) {
            Log.e(TAG, "AI client error getting game suggestions", e)
            Result.failure(AIError.from(e))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting game suggestions", e)
            Result.failure(AIError.from(e))
        }
    }

    // ==================== Library-Based Recommendations ====================

    override suspend fun getLibraryBasedRecommendations(
        games: List<GameListEntry>,
        count: Int,
        forceRefresh: Boolean,
        excludeGames: List<String>
    ): Result<AIGameSuggestions> {
        if (games.isEmpty()) {
            return Result.failure(AIError.EmptyResponseError(
                message = "Add games to your library to get personalized recommendations."
            ))
        }

        // Include excludeGames in cache key to differentiate paginated requests
        val excludeHash = excludeGames.sorted().hashCode()
        val libraryHash = games.map { "${it.rawgId}_${it.rating}_${it.status}" }.sorted().hashCode()
        val cacheKey = "library_recs_v3_${libraryHash}_${count}_$excludeHash"

        // Check cache (skip if we have exclusions - pagination request)
        if (!forceRefresh && excludeGames.isEmpty()) {
            cacheMutex.withLock {
                suggestionsCache.get(cacheKey)?.let { entry ->
                    if (!entry.isExpired()) {
                        Log.d(TAG, "Cache hit for library recommendations")
                        return Result.success(entry.suggestions.copy(fromCache = true))
                    } else {
                        suggestionsCache.remove(cacheKey)
                    }
                }
            }
        }

        // Check for in-flight request (deduplication)
        deduplicationMutex.withLock {
            inFlightRequests[cacheKey]?.let { deferred ->
                Log.d(TAG, "Joining existing request for library recommendations")
                return deferred.await()
            }
        }

        // Create new request with deduplication
        val deferred = CoroutineScope(Dispatchers.IO).async {
            performLibraryBasedRecommendations(games, count, cacheKey, excludeGames)
        }

        deduplicationMutex.withLock {
            inFlightRequests[cacheKey] = deferred
        }

        return try {
            deferred.await()
        } finally {
            deduplicationMutex.withLock {
                inFlightRequests.remove(cacheKey)
            }
        }
    }
    
    private suspend fun performLibraryBasedRecommendations(
        games: List<GameListEntry>,
        count: Int,
        cacheKey: String,
        excludeGames: List<String> = emptyList()
    ): Result<AIGameSuggestions> {
        return try {
            val libraryString = buildEnhancedLibraryString(games)
            // Combine library games with already-recommended games for exclusion
            val libraryExclusion = buildExclusionList(games)
            val fullExclusionList = if (excludeGames.isNotEmpty()) {
                val alreadyRecommended = excludeGames.joinToString(", ")
                if (libraryExclusion.isNotBlank()) {
                    "$libraryExclusion, $alreadyRecommended"
                } else {
                    alreadyRecommended
                }
            } else {
                libraryExclusion
            }
            
            val prompt = GeminiPrompts.libraryBasedRecommendationPromptV3(libraryString, fullExclusionList, count)
            
            Log.d(TAG, "Asking ${aiClient.providerName} for library recommendations with ${games.size} games, excluding ${excludeGames.size} already recommended")
            
            val text = aiClient.generateJsonContent(
                prompt = prompt,
                temperature = 0.25f // Slightly more creative for recommendations
            )
            
            Log.d(TAG, "AI response: ${text.take(500)}...")
            
            // Parse response with multi-version fallback
            val suggestions = parseLibraryRecommendationsResponse(text)
                ?: return Result.failure(AIError.InvalidResponseError(rawResponse = text))
            
            // Filter out games already in library
            val filteredSuggestions = filterOwnedGames(suggestions, games)
            
            Log.d(TAG, "Returning ${filteredSuggestions.games.size} recommendations (filtered from ${suggestions.games.size})")
            
            // Cache the result
            cacheMutex.withLock {
                suggestionsCache.put(cacheKey, CacheEntry(filteredSuggestions))
            }
            
            Result.success(filteredSuggestions)
            
        } catch (e: AIClientException) {
            Log.e(TAG, "AI client error getting library recommendations", e)
            Result.failure(AIError.from(e))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting library recommendations", e)
            Result.failure(AIError.from(e))
        }
    }
    
    /**
     * Parse library recommendations response with multi-version fallback.
     * Tries V6 (tiers with badges) → V3 (reasons) → V2 (confidence) → Legacy (simple list)
     * 
     * Enhanced to preserve metadata even when tier/confidence fields are partially missing.
     */
    private fun parseLibraryRecommendationsResponse(text: String): AIGameSuggestions? {
        Log.d(TAG, "Parsing AI response (${text.length} chars): ${text.take(200)}...")
        
        // Try V6 format with tier-based classification, badges, and full metadata
        try {
            val parsed = json.decodeFromString<AIGameSuggestionsTierDto>(text)
            if (parsed.games.isNotEmpty()) {
                val withTiers = parsed.games.count { it.tier.isNotBlank() }
                val withReasons = parsed.games.count { !it.why.isNullOrBlank() }
                val withBadges = parsed.games.count { it.badges.isNotEmpty() }
                val withDevelopers = parsed.games.count { !it.developer.isNullOrBlank() }
                val withYears = parsed.games.count { it.year != null }
                val withSimilarTo = parsed.games.count { it.similarTo.isNotEmpty() }
                Log.d(TAG, "V6 structure: ${parsed.games.size} games, $withTiers tiers, $withReasons reasons, $withBadges badges, $withDevelopers devs, $withYears years, $withSimilarTo similarTo")
                
                val recommendations = parsed.games.map { dto ->
                    // Even if tier is blank, preserve all metadata fields including badges
                    if (dto.tier.isNotBlank()) {
                        GameRecommendation.fromTierString(
                            name = dto.name, 
                            tier = dto.tier, 
                            why = dto.why,
                            badges = dto.badges,
                            developer = dto.developer,
                            year = dto.year,
                            similarTo = dto.similarTo
                        )
                    } else {
                        // Tier is blank but we still have game name and metadata
                        // Create with default confidence but preserve all fields
                        GameRecommendation(
                            name = dto.name, 
                            confidence = 50, 
                            reason = dto.why,
                            badges = dto.badges,
                            developer = dto.developer,
                            year = dto.year,
                            similarTo = dto.similarTo
                        )
                    }
                }.sortedByDescending { it.confidence }
                
                Log.d(TAG, "✓ Parsed V6 tier format: ${recommendations.size} games (with badges and full metadata)")
                return AIGameSuggestions(
                    games = recommendations.map { it.name },
                    recommendations = recommendations,
                    reasoning = parsed.reasoning,
                    fromCache = false
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "V6 tier parsing failed, trying V3: ${e.javaClass.simpleName} - ${e.message}")
        }
        
        // Try V3 format with semantic match reasons
        try {
            val parsed = json.decodeFromString<AIGameSuggestionsV3Dto>(text)
            if (parsed.games.isNotEmpty()) {
                val recommendations = parsed.games.map { dto ->
                    val matchReasons = dto.reasons.mapNotNull { parseMatchReason(it) }
                    if (matchReasons.isNotEmpty()) {
                        GameRecommendation.fromReasons(
                            name = dto.name,
                            reasons = matchReasons,
                            primaryReason = dto.why
                        )
                    } else {
                        // No valid reasons parsed, but preserve the 'why' field
                        GameRecommendation(name = dto.name, confidence = 50, reason = dto.why)
                    }
                }.sortedByDescending { it.confidence }
                
                Log.d(TAG, "Parsed V3 format: ${recommendations.size} games (with metadata preservation)")
                return AIGameSuggestions(
                    games = recommendations.map { it.name },
                    recommendations = recommendations,
                    reasoning = parsed.reasoning,
                    fromCache = false
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "V3 parsing failed, trying V2: ${e.message}")
        }
        
        // Try V2 format with confidence scores
        try {
            val parsed = json.decodeFromString<AIGameSuggestionsV2Dto>(text)
            if (parsed.games.isNotEmpty()) {
                val recommendations = parsed.games
                    .sortedByDescending { it.confidence }
                    .map { GameRecommendation.fromLegacyScore(it.name, it.confidence) }
                
                Log.d(TAG, "Parsed V2 format: ${recommendations.size} games (with confidence scores)")
                return AIGameSuggestions(
                    games = recommendations.map { it.name },
                    recommendations = recommendations,
                    reasoning = parsed.reasoning,
                    fromCache = false
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "V2 parsing failed, trying legacy: ${e.message}")
        }
        
        // Try legacy format (simple list) - convert to GameRecommendation for consistency
        try {
            val parsed = json.decodeFromString<AIGameSuggestionsDto>(text)
            if (parsed.games.isNotEmpty()) {
                // Convert legacy format to recommendations with default confidence
                // This ensures all games get saved with proper metadata structure
                val recommendations = parsed.games.map { gameName ->
                    GameRecommendation(name = gameName, confidence = 50, reason = null)
                }
                
                Log.d(TAG, "Parsed legacy format: ${parsed.games.size} games (converted to recommendations)")
                return AIGameSuggestions(
                    games = parsed.games,
                    recommendations = recommendations,
                    reasoning = parsed.reasoning,
                    fromCache = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "All parsing attempts failed", e)
        }
        
        return null
    }
    
    private fun filterOwnedGames(
        suggestions: AIGameSuggestions, 
        ownedGames: List<GameListEntry>
    ): AIGameSuggestions {
        val libraryNames = ownedGames.map { it.name.lowercase() }.toSet()
        
        val filteredGames = suggestions.games.filter { gameName ->
            val lowerName = gameName.lowercase()
            !libraryNames.any { libraryGame ->
                lowerName == libraryGame ||
                (lowerName.contains(libraryGame) && 
                 (lowerName.contains("goty") || lowerName.contains("deluxe") || 
                  lowerName.contains("ultimate") || lowerName.contains("complete") ||
                  lowerName.contains("definitive") || lowerName.contains("edition")))
            }
        }
        
        val filteredRecommendations = suggestions.recommendations.filter { rec ->
            filteredGames.contains(rec.name)
        }
        
        return suggestions.copy(
            games = filteredGames,
            recommendations = filteredRecommendations
        )
    }
    
    private fun parseMatchReason(reasonStr: String): MatchReason? {
        val normalized = reasonStr.uppercase().trim().replace(" ", "_")
        return try {
            MatchReason.valueOf(normalized)
        } catch (e: IllegalArgumentException) {
            MatchReason.entries.find { 
                it.name.contains(normalized) || normalized.contains(it.name) ||
                it.name.replace("_", "").contains(normalized.replace("_", ""))
            }
        }
    }

    // ==================== Profile Analysis ====================

    override suspend fun analyzeGamingProfile(games: List<GameListEntry>): Result<GameInsights> {
        return try {
            if (games.isEmpty()) {
                return Result.failure(AIError.EmptyResponseError(
                    message = "Add games to your library to unlock AI insights."
                ))
            }
            
            val cacheKey = generateProfileCacheKey(games)
            
            // Check cache
            profileCache?.let { cached ->
                if (profileCacheKey == cacheKey && !cached.isExpired()) {
                    Log.d(TAG, "Profile cache hit (${games.size} games)")
                    return Result.success(cached.insights)
                }
            }

            val gameData = buildGameDataString(games)
            val prompt = GeminiPrompts.profileAnalysisPrompt(gameData)

            Log.d(TAG, "Sending profile analysis to ${aiClient.providerName}...")

            val analysisText = aiClient.generateTextContent(prompt)

            Log.d(TAG, "Received response: ${analysisText.take(100)}...")

            val insights = parseAIResponse(analysisText, games)
            
            // Cache the result
            profileCache = ProfileCacheEntry(insights, games.size)
            profileCacheKey = cacheKey
            
            Result.success(insights)
        } catch (e: AIClientException) {
            Log.e(TAG, "AI client error analyzing profile", e)
            Result.failure(AIError.from(e))
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing profile", e)
            Result.failure(AIError.from(e))
        }
    }
    
    private fun generateProfileCacheKey(games: List<GameListEntry>): String {
        val dataHash = games.sortedBy { it.rawgId }
            .joinToString("|") { "${it.rawgId}:${it.status}:${it.rating}:${it.hoursPlayed}" }
            .hashCode()
        return "profile_${games.size}_$dataHash"
    }

    override fun analyzeGamingProfileStreaming(games: List<GameListEntry>): Flow<StreamingInsights> = flow {
        if (games.isEmpty()) {
            emit(StreamingInsights(
                partialText = "",
                isComplete = true,
                parsedInsights = null
            ))
            return@flow
        }
        
        val gameData = buildGameDataString(games)
        val prompt = GeminiPrompts.profileAnalysisPrompt(gameData)
        
        Log.d(TAG, "Starting streaming analysis with ${aiClient.providerName}...")
        
        // Emit initial progress
        emit(StreamingInsights(
            partialText = "Analyzing your gaming profile...",
            isComplete = false,
            parsedInsights = null
        ))
        
        val fullText = StringBuilder()
        
        try {
            aiClient.generateStreamingContent(prompt).collect { chunk ->
                fullText.append(chunk)
                emit(StreamingInsights(
                    partialText = fullText.toString(),
                    isComplete = false,
                    parsedInsights = null
                ))
            }
            
            val finalText = fullText.toString()
            Log.d(TAG, "Streaming complete. Total length: ${finalText.length}")
            
            val insights = parseAIResponse(finalText, games)
            
            emit(StreamingInsights(
                partialText = finalText,
                isComplete = true,
                parsedInsights = insights
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during streaming", e)
            emit(StreamingInsights(
                partialText = "Error: ${AIError.from(e).message}",
                isComplete = true,
                parsedInsights = null
            ))
        }
    }.flowOn(Dispatchers.IO)

    // ==================== Studio Expansion ====================

    override suspend fun getExpandedStudios(parentStudio: String): Set<String> = 
        withContext(Dispatchers.IO) {
            getStudioExpansionResult(parentStudio).displayNames
        }
    
    override suspend fun getStudioSlugs(parentStudio: String): String = 
        withContext(Dispatchers.IO) {
            getStudioExpansionResult(parentStudio).slugs
        }
    
    override suspend fun getStudioExpansionResult(parentStudio: String): StudioExpansionResult = 
        withContext(Dispatchers.IO) {
            val normalized = parentStudio.lowercase().trim()
            
            // Check L3 hardcoded first (fastest)
            HardcodedStudioMappings.getSubsidiaries(normalized)?.let { studioList ->
                val names = studioList.map { it.displayName }.toSet()
                val slugs = studioList.joinToString(",") { it.slug }
                return@withContext StudioExpansionResult(names, slugs)
            }
            
            // Check cache
            studioCacheManager?.getExpansionData(normalized)?.let { (names, slugs) ->
                return@withContext StudioExpansionResult(names, slugs)
            }
            
            // Deduplicate concurrent requests
            val existingRequest = studioInflightRequests[normalized]
            if (existingRequest != null && existingRequest.isActive) {
                return@withContext try {
                    existingRequest.await()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    createFallbackStudioResult(parentStudio)
                }
            }
            
            // Query AI for unknown studios
            var deferred: Deferred<StudioExpansionResult>? = null
            try {
                deferred = async {
                    try {
                        queryStudioExpansion(parentStudio)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "AI studio expansion failed, using fallback", e)
                        createFallbackStudioResult(parentStudio)
                    }
                }
                studioInflightRequests[normalized] = deferred
                
                val result = deferred.await()
                studioCacheManager?.cacheExpansion(normalized, result.displayNames, result.slugs)
                result
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "Studio request cancelled for: $parentStudio")
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error for studio: $parentStudio", e)
                createFallbackStudioResult(parentStudio)
            } finally {
                studioInflightRequests.remove(normalized)
            }
        }
    
    override suspend fun expandMultipleStudios(studios: List<String>): Map<String, StudioExpansionResult> =
        withContext(Dispatchers.IO) {
            studios.map { studio ->
                async { studio to getStudioExpansionResult(studio) }
            }.awaitAll().toMap()
        }
    
    private fun createFallbackStudioResult(studioName: String): StudioExpansionResult {
        val slug = studioName.lowercase()
            .replace(" ", "-")
            .replace(slugCleanupRegex, "")
        return StudioExpansionResult(setOf(studioName), slug)
    }
    
    private suspend fun queryStudioExpansion(studioName: String): StudioExpansionResult {
        val prompt = """
List all game development studios owned by, affiliated with, or are subsidiaries of "$studioName".
Include the parent company itself.

Return ONLY valid JSON in this exact format, no markdown:
{"studios": [{"name": "Studio Name", "slug": "studio-slug"}, ...]}

The slug should be lowercase, hyphen-separated, suitable for RAWG API (e.g., "bethesda-game-studios", "id-software").
Use official studio names as they appear in game credits.
        """.trimIndent()
        
        val text = aiClient.generateJsonContent(prompt, temperature = 0.2f, maxTokens = 1024)
        
        return try {
            val parsed = json.decodeFromString<StudioExpansionResponse>(text)
            val names = parsed.studios.map { it.name }.toSet()
            val slugs = parsed.studios.joinToString(",") { it.slug }
            StudioExpansionResult(names, slugs)
        } catch (e: Exception) {
            Log.w(TAG, "Studio JSON parse failed: $text", e)
            createFallbackStudioResult(studioName)
        }
    }

    // ==================== Studio Search ====================

    override fun getLocalStudioSuggestions(query: String, limit: Int): List<StudioMatch> {
        if (query.isBlank() || query.length < 2) return emptyList()
        
        val normalizedQuery = query.lowercase().trim()
        val matches = mutableListOf<StudioMatch>()
        
        // Check for game series first
        HardcodedStudioMappings.getStudiosForGameSeries(normalizedQuery)?.let { gameStudios ->
            gameStudios.forEach { studio ->
                matches.add(
                    StudioMatch(
                        name = studio.displayName,
                        slug = studio.slug,
                        type = StudioType.DEVELOPER,
                        subsidiaryCount = 0,
                        isExactMatch = true
                    )
                )
            }
            if (matches.isNotEmpty()) {
                return matches.take(limit)
            }
        }
        
        // Search through studio mappings
        HardcodedStudioMappings.getAllMappings().forEach { (parentKey, studios) ->
            if (parentKey.contains(normalizedQuery)) {
                val parentStudio = studios.firstOrNull() ?: return@forEach
                matches.add(
                    StudioMatch(
                        name = parentKey.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                        slug = parentStudio.slug,
                        type = StudioType.BOTH,
                        subsidiaryCount = studios.size - 1,
                        isExactMatch = parentKey == normalizedQuery
                    )
                )
            }
            
            studios.forEach { studio ->
                if (studio.displayName.lowercase().contains(normalizedQuery) && 
                    !matches.any { it.slug == studio.slug }) {
                    matches.add(
                        StudioMatch(
                            name = studio.displayName,
                            slug = studio.slug,
                            type = StudioType.DEVELOPER,
                            subsidiaryCount = 0,
                            isExactMatch = studio.displayName.lowercase() == normalizedQuery
                        )
                    )
                }
            }
        }
        
        return matches
            .sortedWith(
                compareByDescending<StudioMatch> { it.isExactMatch }
                    .thenByDescending { it.subsidiaryCount }
                    .thenBy { it.name }
            )
            .take(limit)
    }
    
    override suspend fun searchStudios(
        query: String,
        includePublishers: Boolean,
        includeDevelopers: Boolean,
        limit: Int
    ): StudioSearchResult = withContext(Dispatchers.IO) {
        if (query.isBlank() || query.length < 2) {
            return@withContext StudioSearchResult(query, emptyList())
        }
        
        val normalizedQuery = query.lowercase().trim()
        val isKnownGameSeries = HardcodedStudioMappings.isGameSeries(normalizedQuery)
        val localMatches = getLocalStudioSuggestions(query, limit).toMutableList()
        
        // Return immediately for known game series
        if (isKnownGameSeries && localMatches.isNotEmpty()) {
            Log.d(TAG, "Game series detected: '$query' → ${localMatches.map { it.name }}")
            return@withContext StudioSearchResult(
                query = query,
                matches = localMatches.take(limit),
                fromCache = true
            )
        }
        
        // Return local matches if sufficient
        if (localMatches.size >= limit || localMatches.any { it.isExactMatch }) {
            return@withContext StudioSearchResult(
                query = query,
                matches = localMatches.take(limit),
                fromCache = true
            )
        }
        
        // Query AI for additional matches
        try {
            val aiMatches = queryAIStudioSearch(normalizedQuery, includePublishers, includeDevelopers)
            
            aiMatches.forEach { aiMatch ->
                if (!localMatches.any { it.slug == aiMatch.slug || it.name.equals(aiMatch.name, ignoreCase = true) }) {
                    localMatches.add(aiMatch)
                }
            }
            
            StudioSearchResult(
                query = query,
                matches = localMatches.take(limit),
                fromCache = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "AI studio search failed, returning local results", e)
            StudioSearchResult(
                query = query,
                matches = localMatches.take(limit),
                fromCache = true
            )
        }
    }
    
    private suspend fun queryAIStudioSearch(
        query: String,
        includePublishers: Boolean,
        includeDevelopers: Boolean
    ): List<StudioMatch> {
        val typeFilter = when {
            includePublishers && includeDevelopers -> "both developers and publishers"
            includePublishers -> "publishers only"
            includeDevelopers -> "developers only"
            else -> "both developers and publishers"
        }
        
        val prompt = """
Analyze "$query" and find the relevant game studios. Include $typeFilter.

FIRST, determine what the query is:
1. A GAME SERIES (e.g., "Yakuza", "Call of Duty", "Assassin's Creed", "God of War")
2. A GAME TITLE (e.g., "Elden Ring", "Cyberpunk 2077", "Spider-Man")  
3. A STUDIO NAME (e.g., "Ubisoft", "Rockstar", "Nintendo")

SMART RULES:
- If it's a GAME SERIES or GAME TITLE:
  • Return ONLY the studio(s) that actually DEVELOP those games
  • Example: "Yakuza" or "Like a Dragon" → ONLY "Ryu Ga Gotoku Studio" (not all of Sega)
  • Example: "Call of Duty" → Treyarch, Infinity Ward, Sledgehammer Games (the actual developers)
  
- If it's a STUDIO NAME:
  • Return matching studios
  • If it's a parent company, include relevant subsidiaries
  
- For relevance field:
  • "direct" = This studio actually makes/made the games
  • "parent" = This is the parent company that owns the developer
  • "sibling" = Same parent company but doesn't make this specific game

Return up to 6 most relevant studios, prioritizing "direct" developers.

Return ONLY valid JSON, no markdown:
{"queryType": "game_series|game_title|studio", "studios": [{"name": "Studio Name", "slug": "studio-slug", "type": "developer|publisher|both", "subsidiaries": 0, "relevance": "direct|parent|sibling"}]}

Rules for slugs: lowercase, hyphenated, RAWG API compatible (e.g., "ryu-ga-gotoku-studio", "infinity-ward")
        """.trimIndent()
        
        val text = aiClient.generateJsonContent(prompt, temperature = 0.3f, maxTokens = 1024)
        
        return try {
            val parsed = json.decodeFromString<StudioSearchResponse>(text)
            parsed.studios.map { studio ->
                StudioMatch(
                    name = studio.name,
                    slug = studio.slug,
                    type = when (studio.type.lowercase()) {
                        "developer" -> StudioType.DEVELOPER
                        "publisher" -> StudioType.PUBLISHER
                        "both" -> StudioType.BOTH
                        else -> StudioType.UNKNOWN
                    },
                    subsidiaryCount = studio.subsidiaries,
                    isExactMatch = studio.name.lowercase() == query.lowercase()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse AI studio search: $text", e)
            emptyList()
        }
    }

    // ==================== Cache Management ====================

    override fun clearCache() {
        suggestionsCache.evictAll()
        profileCache = null
        profileCacheKey = null
        Log.d(TAG, "All caches cleared")
    }

    // ==================== Helper Methods ====================

    private fun buildGameDataString(games: List<GameListEntry>): String {
        val totalGames = games.size
        val finishedGames = games.count { it.status == GameStatus.FINISHED }
        val playingGames = games.count { it.status == GameStatus.PLAYING }
        val droppedGames = games.count { it.status == GameStatus.DROPPED }
        val totalHours = games.sumOf { it.hoursPlayed }
        val sortedGames = games.sortedByDescending { it.rating ?: 0f }

        return buildString {
            appendLine("=== PLAYER STATS ===")
            appendLine("Total Games: $totalGames")
            appendLine("Finished: $finishedGames")
            appendLine("Playing: $playingGames")
            appendLine("Dropped: $droppedGames")
            appendLine("Total Hours: $totalHours")
            appendLine()

            appendLine("=== GAME LIBRARY (Top 50) ===")
            appendLine("Format: [Status] Name (Rating/10) - Hours Played")
            
            sortedGames.take(50).forEach { game ->
                val ratingStr = if (game.rating != null && game.rating > 0) "${game.rating}" else "Unrated"
                val statusStr = when(game.status) {
                    GameStatus.FINISHED -> "[FINISHED]"
                    GameStatus.PLAYING -> "[PLAYING]"
                    GameStatus.DROPPED -> "[DROPPED]"
                    GameStatus.WANT -> "[WISHLIST]"
                    GameStatus.ON_HOLD -> "[ON HOLD]"
                }
                
                appendLine("$statusStr ${game.name} ($ratingStr) - ${game.hoursPlayed}h")
            }
        }
    }
    
    private fun buildEnhancedLibraryString(games: List<GameListEntry>): String {
        // Separate positive signal games from dropped/disliked games
        // Dropped games with low ratings (<=5) are NEGATIVE signals - don't use for preferences
        val droppedLowRating = games.filter { 
            it.status == GameStatus.DROPPED && (it.rating ?: 0f) <= 5f 
        }
        val positiveSignalGames = games.filter { game ->
            // Exclude dropped games with low ratings from positive signal calculations
            !(game.status == GameStatus.DROPPED && (game.rating ?: 0f) <= 5f)
        }
        
        val sortedGames = games.sortedWith(
            compareByDescending<GameListEntry> { it.rating ?: 0f }
                .thenByDescending { 
                    when (it.status) {
                        GameStatus.FINISHED -> 4
                        GameStatus.PLAYING -> 3
                        GameStatus.ON_HOLD -> 2
                        GameStatus.WANT -> 1
                        GameStatus.DROPPED -> 0
                    }
                }
        )
        
        val totalGames = games.size
        val finishedCount = games.count { it.status == GameStatus.FINISHED }
        val playingCount = games.count { it.status == GameStatus.PLAYING }
        val droppedCount = games.count { it.status == GameStatus.DROPPED }
        // Calculate average rating ONLY from positive signal games
        val avgRating = positiveSignalGames.mapNotNull { it.rating }.average().takeIf { !it.isNaN() }?.let { "%.1f".format(it) } ?: "N/A"
        
        // IMPORTANT: Use positiveSignalGames for preference calculations to avoid contaminating
        // the user's taste profile with games they dropped and disliked
        val genreFrequency = positiveSignalGames.flatMap { it.genres }
            .groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }.take(5)
            .joinToString(", ") { "${it.key}(${it.value})" }
        
        val aspectFrequency = positiveSignalGames.flatMap { it.aspects }
            .groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }.take(5)
            .joinToString(", ") { "${it.key}(${it.value})" }
        
        val devFrequency = positiveSignalGames.flatMap { it.developers }
            .filter { it.isNotBlank() }
            .groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }
            .filter { it.value >= 2 }
            .take(5)
            .joinToString(", ") { "${it.key}(${it.value})" }
        
        val pubFrequency = positiveSignalGames.flatMap { it.publishers }
            .filter { it.isNotBlank() }
            .groupingBy { it }.eachCount().entries
            .sortedByDescending { it.value }
            .filter { it.value >= 2 }
            .take(4)
            .joinToString(", ") { "${it.key}(${it.value})" }
        
        // Build DISLIKED section for dropped games with low ratings
        val dislikedSection = if (droppedLowRating.isNotEmpty()) {
            val dislikedGenres = droppedLowRating.flatMap { it.genres }
                .groupingBy { it }.eachCount().entries
                .sortedByDescending { it.value }.take(5)
                .joinToString(", ") { "${it.key}(${it.value})" }
            val dislikedGames = droppedLowRating.take(10).joinToString(", ") { 
                "${it.name}(${it.rating?.toInt() ?: 0}/10)" 
            }
            buildString {
                appendLine("\n⚠️ DISLIKED/DROPPED (DO NOT recommend similar games):")
                appendLine("Games: $dislikedGames")
                if (dislikedGenres.isNotEmpty()) appendLine("Genres to AVOID: $dislikedGenres")
            }
        } else ""
        
        val header = buildString {
            appendLine("STATS: $totalGames games | Finished:$finishedCount Playing:$playingCount Dropped:$droppedCount | AvgRating:$avgRating")
            appendLine("TOP GENRES (from liked games only): $genreFrequency")
            if (aspectFrequency.isNotEmpty()) appendLine("LOVED ASPECTS: $aspectFrequency")
            if (devFrequency.isNotEmpty()) appendLine("FAVORITE DEVS: $devFrequency")
            if (pubFrequency.isNotEmpty()) appendLine("FAVORITE PUBS: $pubFrequency")
            append(dislikedSection)
            appendLine()
            append("GAMES (by preference):")
        }
        
        val gamesList = sortedGames.take(75).mapIndexed { index, game ->
            val rating = game.rating?.let { "${it.toInt()}/10" } ?: "-"
            val status = when (game.status) {
                GameStatus.FINISHED -> "Done"
                GameStatus.PLAYING -> "Playing"
                GameStatus.ON_HOLD -> "Hold"
                GameStatus.WANT -> "Want"
                GameStatus.DROPPED -> "Drop"
            }
            val genres = game.genres.take(2).joinToString("/")
            val aspects = game.aspects.take(3).joinToString(",")
            val review = if ((game.rating ?: 0f) >= 8f && game.review.isNotBlank()) 
                " \"${game.review.take(60)}\"" else ""
            
            "${index + 1}. ${game.name} [$status|$rating] $genres ${if (aspects.isNotEmpty()) "(${aspects})" else ""}$review"
        }.joinToString("\n")
        
        return header + "\n" + gamesList
    }
    
    private fun buildExclusionList(games: List<GameListEntry>): String {
        return games.map { it.name }.joinToString(", ")
    }

    private fun parseAIResponse(response: String, games: List<GameListEntry>): GameInsights {
        val topGenres = games.flatMap { it.genres }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }

        try {
            // Clean up response - robustly extract JSON from markdown or text
            var cleanJson = response.trim()
            val jsonStart = cleanJson.indexOf("{")
            val jsonEnd = cleanJson.lastIndexOf("}")
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                cleanJson = cleanJson.substring(jsonStart, jsonEnd + 1)
            }

            val parsed = json.decodeFromString<ProfileAnalysisDto>(cleanJson)

            return GameInsights(
                personalityAnalysis = parsed.personality,
                preferredGenres = topGenres,
                playStyle = parsed.play_style,
                funFacts = parsed.insights,
                recommendations = parsed.recommendations
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing AI response", e)
            
            return GameInsights(
                personalityAnalysis = "You're building your gaming journey! Keep adding games to discover your unique gaming identity.",
                preferredGenres = topGenres,
                playStyle = "Your play style is emerging as you build your collection.",
                funFacts = listOf("Add more games to unlock personalized insights!", "Rate your games to see deeper analysis.", "Track your play time for fun statistics!"),
                recommendations = "Keep adding games to your library for personalized recommendations!"
            )
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 2 * 60 * 60 * 1000L // 2 hours
        private const val PROFILE_CACHE_TTL_MS = 2 * 60 * 60 * 1000L // 2 hours
        private const val MAX_CACHE_SIZE = 200
    }
}
