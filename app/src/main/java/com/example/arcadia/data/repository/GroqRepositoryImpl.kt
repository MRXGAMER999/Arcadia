package com.example.arcadia.data.repository

import android.util.Log
import android.util.LruCache
import com.example.arcadia.data.local.HardcodedStudioMappings
import com.example.arcadia.data.local.StudioCacheManager
import com.example.arcadia.data.remote.GroqApiService
import com.example.arcadia.data.remote.GroqChatRequest
import com.example.arcadia.data.remote.GroqConfig
import com.example.arcadia.data.remote.GroqMessage
import com.example.arcadia.data.remote.GroqResponseFormat
import com.example.arcadia.data.remote.GeminiPrompts
import com.example.arcadia.domain.model.AIError
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.model.ai.AIGameSuggestions
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
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of AIRepository using Groq API with Kimi K2 model.
 * Features:
 * - Response caching with TTL for game suggestions
 * - Request deduplication to prevent duplicate API calls
 * - Streaming support for profile analysis
 * - Studio expansion with multi-layer caching
 * - Centralized error handling with AIError types
 * - Injected GroqApiService for efficient resource usage
 */
class GroqRepositoryImpl(
    private val groqApiService: GroqApiService,
    private val studioCacheManager: StudioCacheManager? = null
) : AIRepository {

    // ==================== Caching ====================
    
    /**
     * Cache entry with timestamp for TTL validation
     */
    private data class CacheEntry(
        val suggestions: AIGameSuggestions,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = 
            System.currentTimeMillis() - timestamp > CACHE_TTL_MS
    }
    
    /** LRU Cache for game suggestions with max 100 entries */
    private val suggestionsCache = LruCache<String, CacheEntry>(MAX_CACHE_SIZE)
    
    // ==================== Profile Analysis Caching ====================
    
    /**
     * Cache entry for profile analysis with timestamp
     */
    private data class ProfileCacheEntry(
        val insights: GameInsights,
        val gameCount: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean = 
            System.currentTimeMillis() - timestamp > PROFILE_CACHE_TTL_MS
    }
    
    /** Cache for profile analysis (keyed by hash of game IDs) */
    private var profileCache: ProfileCacheEntry? = null
    private var profileCacheKey: String? = null
    
    // ==================== Request Deduplication ====================
    
    /** Tracks in-flight requests to prevent duplicates */
    private val inFlightRequests = ConcurrentHashMap<String, kotlinx.coroutines.Deferred<Result<AIGameSuggestions>>>()
    
    /** Tracks in-flight studio expansion requests */
    private val studioInflightRequests = ConcurrentHashMap<String, Deferred<StudioExpansionResult>>()
    
    /** Mutex for thread-safe cache operations */
    private val cacheMutex = Mutex()
    
    /** Mutex for thread-safe deduplication */
    private val deduplicationMutex = Mutex()
    
    /** JSON parser for API responses */
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    /** Regex for cleaning studio slugs */
    private val slugCleanupRegex = Regex("[^a-z0-9-]")

    /**
     * Internal DTO for JSON parsing of AI game suggestions
     */
    @Serializable
    private data class AIGameSuggestionsDto(
        val games: List<String>,
        val reasoning: String? = null
    )

    /**
     * Internal DTO for JSON parsing of Profile Analysis
     */
    @Serializable
    private data class ProfileAnalysisDto(
        val personality: String,
        val play_style: String,
        val insights: List<String>,
        val recommendations: String
    )

    /**
     * Generates a cache key from query and count
     */
    private fun getCacheKey(query: String, count: Int): String {
        return "${query.lowercase().trim()}|$count"
    }

    /**
     * Ask Groq to suggest game names based on a natural language query.
     * Features caching and request deduplication.
     */
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
                        // Remove expired entry
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

    /**
     * Performs the actual API call for game suggestions
     */
    private suspend fun performSuggestGames(
        userQuery: String,
        count: Int,
        cacheKey: String
    ): Result<AIGameSuggestions> {
        return try {
            val prompt = GeminiPrompts.gameSuggestionPrompt(userQuery, count)
            
            Log.d(TAG, "Asking Groq/Kimi for game suggestions: $userQuery")
            
            val request = GroqChatRequest(
                model = GroqConfig.MODEL_NAME,
                messages = listOf(
                    GroqMessage(
                        role = "system", 
                        content = "You are a helpful assistant that responds only in valid JSON format. Never include markdown code blocks or any text outside the JSON."
                    ),
                    GroqMessage(role = "user", content = prompt)
                ),
                temperature = GroqConfig.JsonModel.TEMPERATURE,
                maxTokens = GroqConfig.JsonModel.MAX_TOKENS,
                responseFormat = null  // Kimi K2 doesn't support json_object mode
            )
            
            val response = groqApiService.chatCompletion(
                authorization = "Bearer ${GroqConfig.API_KEY}",
                request = request
            )
            
            val text = response.choices.firstOrNull()?.message?.content?.trim()
                ?: return Result.failure(AIError.EmptyResponseError())
            
            Log.d(TAG, "Groq response: $text")
            
            // Clean up response - handle various markdown formats
            var cleanJson = text
            // Remove markdown code blocks
            if (cleanJson.contains("```")) {
                val jsonStart = cleanJson.indexOf("{")
                val jsonEnd = cleanJson.lastIndexOf("}") + 1
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    cleanJson = cleanJson.substring(jsonStart, jsonEnd)
                }
            }
            cleanJson = cleanJson.trim()
            
            val json = Json { 
                ignoreUnknownKeys = true 
                isLenient = true
            }
            
            val parsed = try {
                json.decodeFromString<AIGameSuggestionsDto>(cleanJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse JSON response", e)
                return Result.failure(AIError.InvalidResponseError(rawResponse = cleanJson))
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
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting game suggestions", e)
            Result.failure(AIError.from(e))
        }
    }

    /**
     * Analyzes a user's gaming profile and returns personalized insights.
     * Features caching to reduce API calls - cache invalidates when game list changes.
     */
    override suspend fun analyzeGamingProfile(games: List<GameListEntry>): Result<GameInsights> {
        return try {
            if (games.isEmpty()) {
                return Result.failure(AIError.EmptyResponseError(
                    message = "Add games to your library to unlock AI insights."
                ))
            }
            
            // Generate cache key based on game data that affects analysis
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

            Log.d(TAG, "Sending profile analysis to Groq...")

            val request = GroqChatRequest(
                model = GroqConfig.MODEL_NAME,
                messages = listOf(
                    GroqMessage(role = "user", content = prompt)
                ),
                temperature = GroqConfig.TextModel.TEMPERATURE,
                maxTokens = GroqConfig.TextModel.MAX_TOKENS
            )
            
            val response = groqApiService.chatCompletion(
                authorization = "Bearer ${GroqConfig.API_KEY}",
                request = request
            )
            
            val analysisText = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(AIError.EmptyResponseError())

            Log.d(TAG, "Received response: ${analysisText.take(100)}...")

            val insights = parseAIResponse(analysisText, games)
            
            // Cache the result
            profileCache = ProfileCacheEntry(insights, games.size)
            profileCacheKey = cacheKey
            
            Result.success(insights)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing profile", e)
            Result.failure(AIError.from(e))
        }
    }
    
    /**
     * Generates a cache key for profile analysis based on game data.
     * Key changes when: games added/removed, status changed, or ratings updated.
     */
    private fun generateProfileCacheKey(games: List<GameListEntry>): String {
        val dataHash = games.sortedBy { it.rawgId }
            .joinToString("|") { "${it.rawgId}:${it.status}:${it.rating}:${it.hoursPlayed}" }
            .hashCode()
        return "profile_${games.size}_$dataHash"
    }
    
    /**
     * Analyzes a user's gaming profile with streaming support.
     * Emits partial results as they are generated.
     * Uses Server-Sent Events (SSE) for true streaming.
     */
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
        
        Log.d(TAG, "Starting streaming analysis...")
        
        // Emit initial progress
        emit(StreamingInsights(
            partialText = "Analyzing your gaming profile...",
            isComplete = false,
            parsedInsights = null
        ))
        
        val fullText = StringBuilder()
        
        try {
            val request = GroqChatRequest(
                model = GroqConfig.MODEL_NAME,
                messages = listOf(
                    GroqMessage(role = "user", content = prompt)
                ),
                temperature = GroqConfig.TextModel.TEMPERATURE,
                maxTokens = GroqConfig.TextModel.MAX_TOKENS,
                stream = true
            )
            
            val responseBody = groqApiService.streamChatCompletion(
                authorization = "Bearer ${GroqConfig.API_KEY}",
                request = request
            )
            
            responseBody.byteStream().bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line?.trim() ?: continue
                    if (currentLine.startsWith("data: ")) {
                        val data = currentLine.removePrefix("data: ")
                        if (data == "[DONE]") break
                        
                        try {
                            // Parse the partial JSON chunk
                            // We need a simple DTO for the chunk format
                            val chunk = json.decodeFromString<GroqStreamChunk>(data)
                            val content = chunk.choices.firstOrNull()?.delta?.content ?: ""
                            
                            if (content.isNotEmpty()) {
                                fullText.append(content)
                                emit(StreamingInsights(
                                    partialText = fullText.toString(),
                                    isComplete = false,
                                    parsedInsights = null
                                ))
                            }
                        } catch (e: Exception) {
                            // Ignore parse errors for individual chunks
                        }
                    }
                }
            }
            
            val finalText = fullText.toString()
            Log.d(TAG, "Streaming complete. Total length: ${finalText.length}")
            
            val insights = parseAIResponse(finalText, games)
            
            // Emit final complete result
            emit(StreamingInsights(
                partialText = finalText,
                isComplete = true,
                parsedInsights = insights
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during streaming", e)
            // Emit error state
            emit(StreamingInsights(
                partialText = "Error: ${AIError.from(e).message}",
                isComplete = true,
                parsedInsights = null
            ))
        }
    }.flowOn(Dispatchers.IO)

    @Serializable
    private data class GroqStreamChunk(
        val choices: List<StreamChoice>
    )

    @Serializable
    private data class StreamChoice(
        val delta: StreamDelta
    )

    @Serializable
    private data class StreamDelta(
        val content: String? = null
    )
    
    /**
     * Clears all caches (suggestions and profile analysis)
     */
    override fun clearCache() {
        suggestionsCache.evictAll()
        profileCache = null
        profileCacheKey = null
        Log.d(TAG, "All caches cleared")
    }

    /**
     * Builds a formatted string containing all game data for AI analysis
     */
    private fun buildGameDataString(games: List<GameListEntry>): String {
        val totalGames = games.size
        val finishedGames = games.count { it.status == GameStatus.FINISHED }
        val playingGames = games.count { it.status == GameStatus.PLAYING }
        val droppedGames = games.count { it.status == GameStatus.DROPPED }
        val totalHours = games.sumOf { it.hoursPlayed }

        // Sort games by rating (highest first) to prioritize favorites
        val sortedGames = games.sortedByDescending { it.rating ?: 0f }

        return buildString {
            appendLine("=== PLAYER STATS ===")
            appendLine("Total Games: $totalGames")
            appendLine("Finished: $finishedGames")
            appendLine("Playing: $playingGames")
            appendLine("Dropped: $droppedGames")
            appendLine("Total Hours: $totalHours")
            appendLine()

            appendLine("=== GAME LIBRARY ===")
            appendLine("Format: [Status] Name (Rating/10) - Hours Played")
            
            sortedGames.forEach { game ->
                val ratingStr = if (game.rating != null && game.rating > 0) "${game.rating}" else "Unrated"
                val statusStr = when(game.status) {
                    GameStatus.FINISHED -> "[FINISHED]"
                    GameStatus.PLAYING -> "[PLAYING]"
                    GameStatus.DROPPED -> "[DROPPED]"
                    GameStatus.WANT -> "[WISHLIST]"
                    GameStatus.ON_HOLD -> "[ON HOLD]"
                    else -> "[UNKNOWN]"
                }
                
                appendLine("$statusStr ${game.name} ($ratingStr) - ${game.hoursPlayed}h")
            }
        }
    }

    /**
     * Parses the AI response and extracts structured insights
     */
    private fun parseAIResponse(response: String, games: List<GameListEntry>): GameInsights {
        val topGenres = games.flatMap { it.genres }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }

        try {
            // Clean up response - handle various markdown formats
            var cleanJson = response
            if (cleanJson.contains("```")) {
                val jsonStart = cleanJson.indexOf("{")
                val jsonEnd = cleanJson.lastIndexOf("}") + 1
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    cleanJson = cleanJson.substring(jsonStart, jsonEnd)
                }
            }
            cleanJson = cleanJson.trim()

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
            
            // Fallback to default values if parsing fails
            return GameInsights(
                personalityAnalysis = "You're building your gaming journey! Keep adding games to discover your unique gaming identity.",
                preferredGenres = topGenres,
                playStyle = "Your play style is emerging as you build your collection.",
                funFacts = listOf("Add more games to unlock personalized insights!", "Rate your games to see deeper analysis.", "Track your play time for fun statistics!"),
                recommendations = "Keep adding games to your library for personalized recommendations!"
            )
        }
    }

    // ==================== Studio Expansion ====================
    
    /**
     * Get expanded studios (subsidiaries) for a parent studio.
     */
    override suspend fun getExpandedStudios(parentStudio: String): Set<String> = 
        withContext(Dispatchers.IO) {
            getStudioExpansionResult(parentStudio).displayNames
        }
    
    /**
     * Get comma-separated slugs for RAWG API filtering.
     */
    override suspend fun getStudioSlugs(parentStudio: String): String = 
        withContext(Dispatchers.IO) {
            getStudioExpansionResult(parentStudio).slugs
        }
    
    /**
     * Get full expansion result with both names and slugs.
     */
    override suspend fun getStudioExpansionResult(parentStudio: String): StudioExpansionResult = 
        withContext(Dispatchers.IO) {
            val normalized = parentStudio.lowercase().trim()
            
            // Check L3 hardcoded first (fastest)
            HardcodedStudioMappings.getSubsidiaries(normalized)?.let { studioList ->
                val names = studioList.map { it.displayName }.toSet()
                val slugs = studioList.joinToString(",") { it.slug }
                return@withContext StudioExpansionResult(names, slugs)
            }
            
            // Check cache (single lookup for both names and slugs)
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
    
    /**
     * Expand multiple studios in parallel.
     */
    override suspend fun expandMultipleStudios(studios: List<String>): Map<String, StudioExpansionResult> =
        withContext(Dispatchers.IO) {
            studios.map { studio ->
                async { studio to getStudioExpansionResult(studio) }
            }.awaitAll().toMap()
        }
    
    /**
     * Creates a fallback result when API calls fail.
     */
    private fun createFallbackStudioResult(studioName: String): StudioExpansionResult {
        val slug = studioName.lowercase()
            .replace(" ", "-")
            .replace(slugCleanupRegex, "")
        return StudioExpansionResult(setOf(studioName), slug)
    }
    
    @Serializable
    private data class StudioExpansionResponse(
        val studios: List<StudioInfo>
    )
    
    @Serializable
    private data class StudioInfo(
        val name: String,
        val slug: String
    )
    
    /**
     * Query AI for studio expansion.
     */
    private suspend fun queryStudioExpansion(studioName: String): StudioExpansionResult {
        val prompt = """
List all game development studios owned by, affiliated with, or are subsidiaries of "$studioName".
Include the parent company itself.

IMPORTANT: Return ONLY valid JSON in this exact format, no markdown:
{"studios": [{"name": "Studio Name", "slug": "studio-slug"}, ...]}

The slug should be lowercase, hyphen-separated, suitable for RAWG API (e.g., "bethesda-game-studios", "id-software").
Use official studio names as they appear in game credits.
        """.trimIndent()
        
        val request = GroqChatRequest(
            model = GroqConfig.MODEL_NAME,
            messages = listOf(
                GroqMessage(
                    role = "system",
                    content = "You are a helpful assistant that responds only in valid JSON format. Never include markdown code blocks."
                ),
                GroqMessage(role = "user", content = prompt)
            ),
            temperature = 0.2f,
            maxTokens = 1024,
            responseFormat = null
        )
        
        val response = groqApiService.chatCompletion(
            authorization = "Bearer ${GroqConfig.API_KEY}",
            request = request
        )
        
        val text = response.choices.firstOrNull()?.message?.content?.trim()
            ?: throw Exception("Empty Groq response for studio expansion")
        
        return try {
            // Clean up response
            var cleanJson = text
            if (cleanJson.contains("```") || cleanJson.contains("{")) {
                val jsonStart = cleanJson.indexOf("{")
                val jsonEnd = cleanJson.lastIndexOf("}") + 1
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    cleanJson = cleanJson.substring(jsonStart, jsonEnd)
                }
            }
            cleanJson = cleanJson.trim()
            
            val parsed = json.decodeFromString<StudioExpansionResponse>(cleanJson)
            val names = parsed.studios.map { it.name }.toSet()
            val slugs = parsed.studios.joinToString(",") { it.slug }
            StudioExpansionResult(names, slugs)
        } catch (e: Exception) {
            Log.w(TAG, "Studio JSON parse failed: $text", e)
            createFallbackStudioResult(studioName)
        }
    }
    
    // ==================== Studio Search (Improved) ====================
    
    /**
     * Get instant local suggestions from hardcoded mappings.
     * This is synchronous for immediate UI feedback.
     * SMART: Checks for game series first, then studio names.
     */
    override fun getLocalStudioSuggestions(query: String, limit: Int): List<StudioMatch> {
        if (query.isBlank() || query.length < 2) return emptyList()
        
        val normalizedQuery = query.lowercase().trim()
        val matches = mutableListOf<StudioMatch>()
        
        // FIRST: Check if query matches a known game series
        // This returns only the studios that actually make those games
        HardcodedStudioMappings.getStudiosForGameSeries(normalizedQuery)?.let { gameStudios ->
            gameStudios.forEach { studio ->
                matches.add(
                    StudioMatch(
                        name = studio.displayName,
                        slug = studio.slug,
                        type = StudioType.DEVELOPER,
                        subsidiaryCount = 0,
                        isExactMatch = true // Game series match is treated as exact
                    )
                )
            }
            // If we found game series matches, return them immediately
            // Don't dilute with other studio matches
            if (matches.isNotEmpty()) {
                return matches.take(limit)
            }
        }
        
        // SECOND: Search through studio mappings
        HardcodedStudioMappings.getAllMappings().forEach { (parentKey, studios) ->
            // Check if parent name matches
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
            
            // Check individual studios within the group
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
        
        // Sort: exact matches first, then by subsidiary count, then alphabetically
        return matches
            .sortedWith(
                compareByDescending<StudioMatch> { it.isExactMatch }
                    .thenByDescending { it.subsidiaryCount }
                    .thenBy { it.name }
            )
            .take(limit)
    }
    
    /**
     * Search for studios with AI enhancement.
     * Shows local results first, then enriches with AI for unknown studios.
     * SMART: Detects game series/titles and returns only relevant developers.
     */
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
        
        // SMART: Check if this is a known game series first
        // If so, return ONLY the relevant studios without querying AI
        val isKnownGameSeries = HardcodedStudioMappings.isGameSeries(normalizedQuery)
        
        // Start with local suggestions (includes game series detection)
        val localMatches = getLocalStudioSuggestions(query, limit).toMutableList()
        
        // If this is a known game series with matches, return immediately
        // No need to query AI - we know exactly which studios make these games
        if (isKnownGameSeries && localMatches.isNotEmpty()) {
            Log.d(TAG, "Game series detected: '$query' → ${localMatches.map { it.name }}")
            return@withContext StudioSearchResult(
                query = query,
                matches = localMatches.take(limit),
                fromCache = true
            )
        }
        
        // If we have enough local matches or exact matches, return them
        if (localMatches.size >= limit || localMatches.any { it.isExactMatch }) {
            return@withContext StudioSearchResult(
                query = query,
                matches = localMatches.take(limit),
                fromCache = true
            )
        }
        
        // Query AI for additional matches (unknown queries)
        try {
            val aiMatches = queryAIStudioSearch(normalizedQuery, includePublishers, includeDevelopers)
            
            // Merge results, avoiding duplicates
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
    
    @Serializable
    private data class StudioSearchResponse(
        val studios: List<StudioSearchInfo>,
        val queryType: String = "studio" // "studio", "game_series", "game_title"
    )
    
    @Serializable
    private data class StudioSearchInfo(
        val name: String,
        val slug: String,
        val type: String, // "developer", "publisher", "both"
        val subsidiaries: Int = 0,
        val relevance: String = "direct" // "direct" = makes the game, "parent" = owns the studio, "sibling" = same parent
    )
    
    /**
     * Query AI for studio search results.
     * Smart enough to detect game series/titles and return only relevant studios.
     */
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
  • Example: "Assassin's Creed" → Ubisoft Montreal, Ubisoft Quebec (main AC developers)
  • Example: "God of War" → Santa Monica Studio (not all PlayStation Studios)
  • Example: "Halo" → 343 Industries (not all of Xbox Game Studios)
  
- If it's a STUDIO NAME:
  • Return matching studios
  • If it's a parent company, include relevant subsidiaries
  
- For relevance field:
  • "direct" = This studio actually makes/made the games
  • "parent" = This is the parent company that owns the developer
  • "sibling" = Same parent company but doesn't make this specific game

Return up to 6 most relevant studios, prioritizing "direct" developers.

IMPORTANT: Return ONLY valid JSON, no markdown:
{"queryType": "game_series|game_title|studio", "studios": [{"name": "Studio Name", "slug": "studio-slug", "type": "developer|publisher|both", "subsidiaries": 0, "relevance": "direct|parent|sibling"}]}

Rules for slugs: lowercase, hyphenated, RAWG API compatible (e.g., "ryu-ga-gotoku-studio", "infinity-ward")
        """.trimIndent()
        
        val request = GroqChatRequest(
            model = GroqConfig.MODEL_NAME,
            messages = listOf(
                GroqMessage(
                    role = "system",
                    content = "You are a video game industry expert. Respond only in valid JSON. Never include markdown code blocks."
                ),
                GroqMessage(role = "user", content = prompt)
            ),
            temperature = 0.3f,
            maxTokens = 1024,
            responseFormat = null
        )
        
        val response = groqApiService.chatCompletion(
            authorization = "Bearer ${GroqConfig.API_KEY}",
            request = request
        )
        
        val text = response.choices.firstOrNull()?.message?.content?.trim()
            ?: throw Exception("Empty AI response for studio search")
        
        return try {
            var cleanJson = text
            if (cleanJson.contains("```") || cleanJson.contains("{")) {
                val jsonStart = cleanJson.indexOf("{")
                val jsonEnd = cleanJson.lastIndexOf("}") + 1
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    cleanJson = cleanJson.substring(jsonStart, jsonEnd)
                }
            }
            
            val parsed = json.decodeFromString<StudioSearchResponse>(cleanJson)
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

    companion object {
        private const val TAG = "GroqRepository"
        
        /** Cache TTL: 30 minutes (extended from 10 to reduce API calls) */
        private const val CACHE_TTL_MS = 30 * 60 * 1000L
        
        /** Profile analysis cache TTL: 1 hour (profile data changes infrequently) */
        private const val PROFILE_CACHE_TTL_MS = 60 * 60 * 1000L
        
        /** Maximum cache entries */
        private const val MAX_CACHE_SIZE = 100  // Increased from 50
    }
}
