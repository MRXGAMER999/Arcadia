package com.example.arcadia.data.repository

import android.util.Log
import com.example.arcadia.BuildConfig
import com.example.arcadia.data.local.HardcodedStudioMappings
import com.example.arcadia.data.local.StudioCacheManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Data class for studio expansion result
 */
data class StudioExpansionResult(
    val displayNames: Set<String>,
    val slugs: String // Comma-separated slugs for RAWG API
)

/**
 * Repository for expanding parent studios into their subsidiaries.
 * Uses multi-layer caching with Gemini AI as fallback for unknown studios.
 */
class StudioExpansionRepository(
    private val cacheManager: StudioCacheManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        private val SLUG_CLEANUP_REGEX = Regex("[^a-z0-9-]")
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.2f
            topK = 20
            topP = 0.8f
            maxOutputTokens = 1024
        }
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val inflightRequests = ConcurrentHashMap<String, Deferred<StudioExpansionResult>>()

    /**
     * Get expanded studios with display names for UI.
     */
    suspend fun getExpandedStudios(parentStudio: String): Set<String> = withContext(dispatcher) {
        getExpansionResult(parentStudio).displayNames
    }

    /**
     * Get comma-separated slugs for RAWG API filtering.
     */
    suspend fun getStudioSlugs(parentStudio: String): String = withContext(dispatcher) {
        getExpansionResult(parentStudio).slugs
    }

    /**
     * Get full expansion result with both names and slugs.
     */
    suspend fun getExpansionResult(parentStudio: String): StudioExpansionResult = withContext(dispatcher) {
        val normalized = parentStudio.lowercase().trim()

        // Check L3 hardcoded first (fastest)
        HardcodedStudioMappings.getSubsidiaries(normalized)?.let { studioList ->
            val names = studioList.map { it.displayName }.toSet()
            val slugs = studioList.joinToString(",") { it.slug }
            return@withContext StudioExpansionResult(names, slugs)
        }

        // Check cache (single lookup for both names and slugs)
        cacheManager.getExpansionData(normalized)?.let { (names, slugs) ->
            return@withContext StudioExpansionResult(names, slugs)
        }

        // Deduplicate concurrent requests
        val existingRequest = inflightRequests[normalized]
        if (existingRequest != null && existingRequest.isActive) {
            return@withContext try {
                existingRequest.await()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e // Re-throw cancellation
            } catch (e: Exception) {
                // If existing request failed, create fallback
                createFallbackResult(parentStudio)
            }
        }

        // Query Gemini for unknown studios with proper cleanup
        var deferred: Deferred<StudioExpansionResult>? = null
        try {
            deferred = async {
                try {
                    queryGemini(parentStudio)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("StudioExpansion", "Gemini failed, using fallback", e)
                    createFallbackResult(parentStudio)
                }
            }
            inflightRequests[normalized] = deferred
            
            val result = deferred.await()
            cacheManager.cacheExpansion(normalized, result.displayNames, result.slugs)
            result
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d("StudioExpansion", "Request cancelled for: $parentStudio")
            throw e
        } catch (e: Exception) {
            Log.e("StudioExpansion", "Unexpected error for: $parentStudio", e)
            createFallbackResult(parentStudio)
        } finally {
            inflightRequests.remove(normalized)
        }
    }

    /**
     * Creates a fallback result when API calls fail.
     */
    private fun createFallbackResult(studioName: String): StudioExpansionResult {
        val slug = studioName.lowercase()
            .replace(" ", "-")
            .replace(SLUG_CLEANUP_REGEX, "")
        return StudioExpansionResult(setOf(studioName), slug)
    }

    @Serializable
    private data class GeminiStudioResponse(
        val studios: List<GeminiStudio>
    )

    @Serializable
    private data class GeminiStudio(
        val name: String,
        val slug: String
    )

    private suspend fun queryGemini(studioName: String): StudioExpansionResult {
        val prompt = """
List all game development studios owned by, affiliated with, or are subsidiaries of "$studioName".
Include the parent company itself.

IMPORTANT: Return ONLY valid JSON in this exact format, no markdown:
{"studios": [{"name": "Studio Name", "slug": "studio-slug"}, ...]}

The slug should be lowercase, hyphen-separated, suitable for RAWG API (e.g., "bethesda-game-studios", "id-software").
Use official studio names as they appear in game credits.
        """.trimIndent()

        val response = generativeModel.generateContent(prompt)
        val text = response.text?.trim() ?: throw Exception("Empty Gemini response")

        return try {
            val cleanJson = text
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            val parsed = json.decodeFromString<GeminiStudioResponse>(cleanJson)
            val names = parsed.studios.map { it.name }.toSet()
            val slugs = parsed.studios.joinToString(",") { it.slug }
            StudioExpansionResult(names, slugs)
        } catch (e: Exception) {
            Log.w("StudioExpansion", "JSON parse failed: $text", e)
            // Fallback
            val slug = studioName.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
            StudioExpansionResult(setOf(studioName), slug)
        }
    }

    suspend fun expandMultipleStudios(studios: List<String>): Map<String, StudioExpansionResult> = 
        withContext(dispatcher) {
            studios.map { studio ->
                async { studio to getExpansionResult(studio) }
            }.awaitAll().toMap()
        }
}
