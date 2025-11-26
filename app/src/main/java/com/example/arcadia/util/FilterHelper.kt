package com.example.arcadia.util

import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.presentation.base.SortCriterion

object FilterHelper {

    fun filterByGenres(
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

    fun filterByStatuses(
        games: List<GameListEntry>,
        selectedStatuses: Set<GameStatus>
    ): List<GameListEntry> {
        if (selectedStatuses.isEmpty()) return games
        return games.filter { it.status in selectedStatuses }
    }

    fun applyFilters(
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

    fun sortGames(
        games: List<GameListEntry>,
        sortBy: SortCriterion
    ): List<GameListEntry> {
        return when (sortBy) {
            is SortCriterion.Title -> games.sortedBy { it.name.lowercase() }
            is SortCriterion.TitleDesc -> games.sortedByDescending { it.name.lowercase() }
            is SortCriterion.DateAdded -> games.sortedBy { it.addedAt }
            is SortCriterion.DateAddedDesc -> games.sortedByDescending { it.addedAt }
            is SortCriterion.Rating -> {
                val (rated, unrated) = games.partition { it.rating != null }
                rated.sortedBy { it.rating!! } + unrated
            }
            is SortCriterion.RatingDesc -> {
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

    fun extractGenres(games: List<GameListEntry>): List<String> {
        return games
            .flatMap { it.genres }
            .distinct()
            .sorted()
    }

    fun getAllStatuses(): List<GameStatus> {
        return GameStatus.entries.toList()
    }
}
