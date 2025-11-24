package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.StudioFilterType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * High-performance parallel game filtering by studio.
 * Uses chunked processing for optimal performance on large lists.
 */
class ParallelGameFilter(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    companion object {
        // Pre-compiled regex patterns for performance
        private val SUFFIX_REGEX = Regex("\\s+(inc\\.?|llc|ltd\\.?|studios?|entertainment|games?|interactive)$", RegexOption.IGNORE_CASE)
        private val NON_ALPHANUMERIC_REGEX = Regex("[^a-z0-9]")
        private const val MIN_CHUNK_SIZE = 50
    }

    // Normalizer for studio name matching using pre-compiled regex
    private val studioNormalizer: (String) -> String = { name ->
        name.lowercase()
            .replace(SUFFIX_REGEX, "")
            .replace(NON_ALPHANUMERIC_REGEX, "")
            .trim()
    }

    /**
     * Filter games by studios using parallel chunked processing.
     * Optimal chunk size based on list size and available cores.
     */
    suspend fun filterByStudios(
        games: List<Game>,
        expandedStudios: Set<String>,
        filterType: StudioFilterType = StudioFilterType.BOTH
    ): List<Game> = withContext(dispatcher) {
        if (expandedStudios.isEmpty()) return@withContext games
        if (games.isEmpty()) return@withContext emptyList()

        // Pre-normalize studio names for O(1) lookup
        val normalizedStudios = expandedStudios.map(studioNormalizer).toSet()

        // Determine optimal chunk size
        val cores = Runtime.getRuntime().availableProcessors()
        val chunkSize = maxOf(MIN_CHUNK_SIZE, games.size / (cores * 2))

        // Parallel filtering with chunking
        games.chunked(chunkSize).map { chunk ->
            async {
                chunk.filter { game ->
                    matchesStudios(game, normalizedStudios, filterType)
                }
            }
        }.awaitAll().flatten()
    }

    private fun matchesStudios(
        game: Game,
        normalizedStudios: Set<String>,
        filterType: StudioFilterType
    ): Boolean {
        val checkDevelopers = filterType == StudioFilterType.DEVELOPER || filterType == StudioFilterType.BOTH
        val checkPublishers = filterType == StudioFilterType.PUBLISHER || filterType == StudioFilterType.BOTH

        if (checkDevelopers) {
            val matchesDev = game.developers.any { dev ->
                studioNormalizer(dev) in normalizedStudios
            }
            if (matchesDev) return true
        }

        if (checkPublishers) {
            val matchesPub = game.publishers.any { pub ->
                studioNormalizer(pub) in normalizedStudios
            }
            if (matchesPub) return true
        }

        return false
    }

    /**
     * Streaming filter for very large lists - processes in batches.
     * Emits results as they're filtered for faster UI updates.
     */
    fun filterByStudiosFlow(
        games: List<Game>,
        expandedStudios: Set<String>,
        filterType: StudioFilterType = StudioFilterType.BOTH,
        batchSize: Int = 100
    ): Flow<List<Game>> = flow {
        val normalizedStudios = expandedStudios.map(studioNormalizer).toSet()

        games.chunked(batchSize).forEach { batch ->
            val filtered = batch.filter { game ->
                matchesStudios(game, normalizedStudios, filterType)
            }
            if (filtered.isNotEmpty()) {
                emit(filtered)
            }
        }
    }.flowOn(dispatcher)
}
