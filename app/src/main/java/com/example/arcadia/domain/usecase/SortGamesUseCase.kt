package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.SortOrder

/**
 * Use case for sorting games in the user's library.
 * Encapsulates sorting logic to avoid duplication across ViewModels.
 */
class SortGamesUseCase {
    
    /**
     * Sorts a list of games based on the specified sort order.
     * Handles null values appropriately for each sort type.
     * For ratings: unrated games always appear at the bottom.
     * When sorting by rating, games with the same rating are sorted by importance (higher first).
     * 
     * @param games The list of games to sort
     * @param sortOrder The desired sort order
     * @return Sorted list of games
     */
    operator fun invoke(games: List<GameListEntry>, sortOrder: SortOrder): List<GameListEntry> {
        return when (sortOrder) {
            SortOrder.TITLE_A_Z -> games.sortedBy { it.name.lowercase() }
            SortOrder.TITLE_Z_A -> games.sortedByDescending { it.name.lowercase() }
            SortOrder.NEWEST_FIRST -> games.sortedByDescending { it.addedAt }
            SortOrder.OLDEST_FIRST -> games.sortedBy { it.addedAt }
            SortOrder.RATING_HIGH -> {
                // Separate rated and unrated games
                val (rated, unrated) = games.partition { it.rating != null }
                // Sort rated games by rating (highest first), then by importance (highest first) for same rating
                rated.sortedWith(
                    compareByDescending<GameListEntry> { it.rating!! }
                        .thenByDescending { it.importance }
                ) + unrated
            }
            SortOrder.RATING_LOW -> {
                // Separate rated and unrated games
                val (rated, unrated) = games.partition { it.rating != null }
                // Sort rated games by rating (lowest first), then by importance (highest first) for same rating
                rated.sortedWith(
                    compareBy<GameListEntry> { it.rating!! }
                        .thenByDescending { it.importance }
                ) + unrated
            }
            SortOrder.RELEASE_NEW -> games.sortedWith(
                compareByDescending(nullsLast()) { it.releaseDate }
            )
            SortOrder.RELEASE_OLD -> games.sortedWith(
                compareBy(nullsLast()) { it.releaseDate }
            )
        }
    }
}
