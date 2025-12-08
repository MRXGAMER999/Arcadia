package com.example.arcadia.domain.model.ai

/**
 * Validation rules for roast generation.
 * Ensures users have sufficient gaming data before generating a roast.
 */
object RoastValidation {
    const val MIN_GAMES = 3
    const val MIN_HOURS = 5

    /**
     * Checks if the user has insufficient stats for roast generation.
     * @param stats The user's gaming statistics
     * @return true if stats are insufficient, false if roast can be generated
     */
    fun hasInsufficientStats(stats: RoastStats): Boolean =
        stats.totalGames < MIN_GAMES || stats.hoursPlayed < MIN_HOURS
}
