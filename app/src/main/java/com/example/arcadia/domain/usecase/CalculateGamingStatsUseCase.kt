package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus

/**
 * Data class representing genre rating statistics.
 */
data class GenreRatingStats(
    val genre: String,
    val avgRating: Float,
    val count: Int
)

/**
 * Data class representing calculated gaming statistics.
 */
data class GamingStats(
    val totalGames: Int,
    val completedGames: Int,
    val droppedGames: Int,
    val playingGames: Int,
    val wantToPlayGames: Int,
    val onHoldGames: Int,
    val totalHours: Int,
    val completionRate: Float,
    val averageRating: Float,
    val topGenres: List<Pair<String, Int>>,
    val topPlatforms: List<Pair<String, Int>>,
    val genreRatingAnalysis: List<GenreRatingStats>,
    val gamesAddedByYear: List<Pair<String, Int>>,
    val recentTrend: String
)

/**
 * Use case for calculating gaming statistics from a user's library.
 * Encapsulates all the business logic for analytics calculations.
 */
class CalculateGamingStatsUseCase {
    
    /**
     * Calculates comprehensive gaming statistics from the user's game library.
     * 
     * @param games The user's game library
     * @return GamingStats containing all calculated statistics
     */
    operator fun invoke(games: List<GameListEntry>): GamingStats {
        if (games.isEmpty()) {
            return GamingStats(
                totalGames = 0,
                completedGames = 0,
                droppedGames = 0,
                playingGames = 0,
                wantToPlayGames = 0,
                onHoldGames = 0,
                totalHours = 0,
                completionRate = 0f,
                averageRating = 0f,
                topGenres = emptyList(),
                topPlatforms = emptyList(),
                genreRatingAnalysis = emptyList(),
                gamesAddedByYear = emptyList(),
                recentTrend = "Start playing to see trends!"
            )
        }
        
        val totalGames = games.size
        val completed = games.count { it.status == GameStatus.FINISHED }
        val dropped = games.count { it.status == GameStatus.DROPPED }
        val playing = games.count { it.status == GameStatus.PLAYING }
        val wantToPlay = games.count { it.status == GameStatus.WANT }
        val onHold = games.count { it.status == GameStatus.ON_HOLD }
        val hours = games.sumOf { it.hoursPlayed }
        
        // Completion Rate: Finished / (Finished + Playing + Dropped + OnHold) -> Excluding "Want"
        val playedGamesCount = games.count { it.status != GameStatus.WANT }
        val completionRate = if (playedGamesCount > 0) {
            (completed.toFloat() / playedGamesCount.toFloat()) * 100f
        } else 0f

        // Top Genres
        val genreCounts = games.flatMap { it.genres }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(6)

        // Top Platforms
        val platformCounts = games.flatMap { it.platforms }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        // Avg Rating
        val ratedGames = games.filter { it.rating != null && it.rating!! > 0 }
        val avgRating = if (ratedGames.isNotEmpty()) {
            ratedGames.map { it.rating!! }.average().toFloat()
        } else 0f

        // Genre Rating Analysis (Min 2 games to be significant)
        val genreRatings = games.flatMap { game -> 
                game.genres.map { genre -> genre to game.rating } 
            }
            .filter { it.second != null && it.second!! > 0 }
            .groupBy { it.first }
            .map { (genre, ratings) ->
                GenreRatingStats(
                    genre = genre,
                    avgRating = ratings.mapNotNull { it.second }.average().toFloat(),
                    count = ratings.size
                )
            }
            .filter { it.count >= 2 }
            .sortedByDescending { it.avgRating }
            .take(5)

        // Games Added By Year
        val calendar = java.util.Calendar.getInstance()
        val gamesByYear = games.groupBy { 
            calendar.timeInMillis = it.addedAt
            calendar.get(java.util.Calendar.YEAR).toString()
        }.mapValues { it.value.size }.toList().sortedBy { it.first }

        // Trend
        val trend = if (genreCounts.isNotEmpty()) {
            "You've been playing a lot of ${genreCounts.first().first} games recently."
        } else "Start playing to see trends!"

        return GamingStats(
            totalGames = totalGames,
            completedGames = completed,
            droppedGames = dropped,
            playingGames = playing,
            wantToPlayGames = wantToPlay,
            onHoldGames = onHold,
            totalHours = hours,
            completionRate = completionRate,
            averageRating = avgRating,
            topGenres = genreCounts,
            topPlatforms = platformCounts,
            genreRatingAnalysis = genreRatings,
            gamesAddedByYear = gamesByYear,
            recentTrend = trend
        )
    }
}
