package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus

/**
 * Use case for filtering games in the user's library.
 * Encapsulates filtering logic to avoid duplication across ViewModels.
 */
class FilterGamesUseCase {
    
    /**
     * Filters games based on genre and status criteria.
     * Uses AND logic: games must match ALL selected filters.
     * 
     * @param games The list of games to filter
     * @param genres Set of genres to filter by (empty = no filter)
     * @param statuses Set of statuses to filter by (empty = no filter)
     * @return Filtered list of games
     */
    operator fun invoke(
        games: List<GameListEntry>,
        genres: Set<String> = emptySet(),
        statuses: Set<GameStatus> = emptySet()
    ): List<GameListEntry> {
        // Early return if no filters applied
        if (genres.isEmpty() && statuses.isEmpty()) {
            return games
        }
        
        return games.filter { game ->
            matchesGenreFilter(game, genres) && matchesStatusFilter(game, statuses)
        }
    }
    
    /**
     * Filters games by excluding certain game IDs.
     * Useful for removing games already in library from recommendations.
     * 
     * @param games The list of games to filter
     * @param excludeIds Set of game IDs to exclude
     * @return Filtered list of games
     */
    fun excludeByIds(games: List<GameListEntry>, excludeIds: Set<Int>): List<GameListEntry> {
        if (excludeIds.isEmpty()) return games
        return games.filter { it.rawgId !in excludeIds }
    }
    
    private fun matchesGenreFilter(game: GameListEntry, selectedGenres: Set<String>): Boolean {
        if (selectedGenres.isEmpty()) return true
        // Game must have at least one of the selected genres (case-insensitive)
        return game.genres.any { gameGenre ->
            selectedGenres.any { it.equals(gameGenre, ignoreCase = true) }
        }
    }

    private fun matchesStatusFilter(game: GameListEntry, selectedStatuses: Set<GameStatus>): Boolean {
        if (selectedStatuses.isEmpty()) return true
        return game.status in selectedStatuses
    }
}
