package com.example.arcadia.presentation.base

import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus

/**
 * Base ViewModel for screens that support filtering and sorting of game lists.
 * Provides common functionality for:
 * - Genre filtering
 * - Status filtering
 * - Sorting operations
 * - Filter persistence
 */
abstract class FilterableViewModel : BaseViewModel() {

    /**
     * Filters games based on genre criteria.
     * @param games List of games to filter
     * @param selectedGenres Set of genres to filter by (empty = no filter)
     * @return Filtered list of games
     */
    protected fun filterByGenres(
        games: List<GameListEntry>,
        selectedGenres: Set<String>
    ): List<GameListEntry> {
        if (selectedGenres.isEmpty()) return games

        return games.filter { game ->
            game.genres.any { gameGenre ->
                selectedGenres.any { it.equals(gameGenre, ignoreCase = true) }
            }
        }
    }

    /**
     * Filters games based on status criteria.
     * @param games List of games to filter
     * @param selectedStatuses Set of statuses to filter by (empty = no filter)
     * @return Filtered list of games
     */
    protected fun filterByStatuses(
        games: List<GameListEntry>,
        selectedStatuses: Set<GameStatus>
    ): List<GameListEntry> {
        if (selectedStatuses.isEmpty()) return games
        return games.filter { it.status in selectedStatuses }
    }

    /**
     * Applies multiple filters to a game list.
     * @param games List of games to filter
     * @param selectedGenres Genres to filter by
     * @param selectedStatuses Statuses to filter by
     * @return Filtered list of games
     */
    protected fun applyFilters(
        games: List<GameListEntry>,
        selectedGenres: Set<String> = emptySet(),
        selectedStatuses: Set<GameStatus> = emptySet()
    ): List<GameListEntry> {
        var filtered = games

        if (selectedGenres.isNotEmpty()) {
            filtered = filterByGenres(filtered, selectedGenres)
        }

        if (selectedStatuses.isNotEmpty()) {
            filtered = filterByStatuses(filtered, selectedStatuses)
        }

        return filtered
    }

    /**
     * Sorts games based on the provided sort criterion.
     * @param games List of games to sort
     * @param sortBy Sort criterion
     * @return Sorted list of games
     */
    protected fun sortGames(
        games: List<GameListEntry>,
        sortBy: SortCriterion
    ): List<GameListEntry> {
        return when (sortBy) {
            is SortCriterion.Title -> games.sortedBy { it.name.lowercase() }
            is SortCriterion.TitleDesc -> games.sortedByDescending { it.name.lowercase() }
            is SortCriterion.DateAdded -> games.sortedBy { it.addedAt }
            is SortCriterion.DateAddedDesc -> games.sortedByDescending { it.addedAt }
            is SortCriterion.Rating -> {
                // Rated games first (lowest to highest), unrated at the end
                val (rated, unrated) = games.partition { it.rating != null }
                rated.sortedBy { it.rating!! } + unrated
            }
            is SortCriterion.RatingDesc -> {
                // Rated games first (highest to lowest), unrated at the end
                val (rated, unrated) = games.partition { it.rating != null }
                rated.sortedByDescending { it.rating!! } + unrated
            }
            is SortCriterion.ReleaseDate -> games.sortedWith(
                compareBy(nullsLast()) { it.releaseDate }
            )
            is SortCriterion.ReleaseDateDesc -> games.sortedWith(
                compareByDescending(nullsLast()) { it.releaseDate }
            )
        }
    }

    /**
     * Extracts all unique genres from a list of games.
     */
    protected fun extractGenres(games: List<GameListEntry>): List<String> {
        return games
            .flatMap { it.genres }
            .distinct()
            .sorted()
    }

    /**
     * Gets all available game statuses.
     */
    protected fun getAllStatuses(): List<GameStatus> {
        return GameStatus.entries.toList()
    }
}

/**
 * Represents different sort criteria for game lists.
 */
sealed class SortCriterion {
    data object Title : SortCriterion()
    data object TitleDesc : SortCriterion()
    data object DateAdded : SortCriterion()
    data object DateAddedDesc : SortCriterion()
    data object Rating : SortCriterion()
    data object RatingDesc : SortCriterion()
    data object ReleaseDate : SortCriterion()
    data object ReleaseDateDesc : SortCriterion()
}
