package com.example.arcadia.domain.repository

import com.example.arcadia.domain.model.Game
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    
    /**
     * Get popular games (ordered by rating and popularity)
     */
    fun getPopularGames(page: Int = 1, pageSize: Int = 10): Flow<RequestState<List<Game>>>
    
    /**
     * Get upcoming games (TBA or releasing in the near future)
     */
    fun getUpcomingGames(page: Int = 1, pageSize: Int = 10): Flow<RequestState<List<Game>>>
    
    /**
     * Get new releases (recently released games)
     */
    fun getNewReleases(page: Int = 1, pageSize: Int = 10): Flow<RequestState<List<Game>>>
    
    /**
     * Get games by genre
     */
    fun getGamesByGenre(genreId: Int, page: Int = 1, pageSize: Int = 10): Flow<RequestState<List<Game>>>
    
    /**
     * Get recommended games based on tags
     */
    fun getRecommendedGames(tags: String, page: Int = 1, pageSize: Int = 10): Flow<RequestState<List<Game>>>
}

