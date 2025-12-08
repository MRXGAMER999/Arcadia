package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.model.ai.RoastStats
import com.example.arcadia.presentation.screens.analytics.AnalyticsState

/**
 * Use case for extracting roast-relevant statistics from AnalyticsState.
 * Converts the presentation layer AnalyticsState to the domain model RoastStats
 * for use in AI roast generation.
 */
class ExtractRoastStatsUseCase {

    /**
     * Extracts gaming statistics from AnalyticsState and converts them to RoastStats.
     *
     * @param analyticsState The current analytics state containing user gaming statistics
     * @return RoastStats containing the relevant statistics for roast generation
     */
    operator fun invoke(analyticsState: AnalyticsState): RoastStats {
        return RoastStats(
            hoursPlayed = analyticsState.hoursPlayed,
            totalGames = analyticsState.totalGames,
            completionRate = analyticsState.completionRate,
            completedGames = analyticsState.completedGames,
            droppedGames = analyticsState.droppedGames,
            topGenres = analyticsState.topGenres,
            gamingPersonality = analyticsState.gamingPersonality.title,
            averageRating = analyticsState.averageRating
        )
    }
}
