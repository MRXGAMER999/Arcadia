package com.example.arcadia.domain.usecase

/**
 * Enum representing different gaming personalities based on playstyle analysis.
 */
enum class GamingPersonality(val title: String, val description: String) {
    Completionist("The Completionist", "You finish almost everything you start. Dedication is your middle name."),
    Explorer("The Explorer", "You love trying new things across many genres. Variety is the spice of life."),
    Specialist("The Specialist", "You know what you like and stick to it. A master of your favorite genres."),
    Socialite("The Socialite", "You prefer multiplayer experiences and shared worlds."),
    Casual("The Casual Gamer", "You play for fun when you can. No pressure, just vibes."),
    Hardcore("The Hardcore", "High hours, tough genres, high completion. You breathe gaming.")
}

/**
 * Use case for determining a user's gaming personality based on their statistics.
 * Encapsulates the personality analysis algorithm.
 */
class DetermineGamingPersonalityUseCase {
    
    /**
     * Determines the user's gaming personality based on their statistics.
     * 
     * @param completedGames Number of completed games
     * @param playedCount Total number of games played (excluding want-to-play)
     * @param uniqueGenres Number of unique genres in library
     * @param totalHours Total hours played across all games
     * @return The determined GamingPersonality
     */
    operator fun invoke(
        completedGames: Int,
        playedCount: Int,
        uniqueGenres: Int,
        totalHours: Int
    ): GamingPersonality {
        if (playedCount == 0) return GamingPersonality.Casual

        val completionRatio = completedGames.toFloat() / playedCount.toFloat()
        
        return when {
            completionRatio > 0.8f && totalHours > 50 -> GamingPersonality.Completionist
            uniqueGenres > 8 -> GamingPersonality.Explorer
            totalHours > 200 -> GamingPersonality.Hardcore
            uniqueGenres <= 3 && playedCount > 5 -> GamingPersonality.Specialist
            else -> GamingPersonality.Casual
        }
    }
    
    /**
     * Determines the user's gaming personality from GamingStats.
     * 
     * @param stats The calculated gaming statistics
     * @return The determined GamingPersonality
     */
    operator fun invoke(stats: GamingStats): GamingPersonality {
        val playedCount = stats.totalGames - stats.wantToPlayGames
        return invoke(
            completedGames = stats.completedGames,
            playedCount = playedCount,
            uniqueGenres = stats.topGenres.size,
            totalHours = stats.totalHours
        )
    }
}
