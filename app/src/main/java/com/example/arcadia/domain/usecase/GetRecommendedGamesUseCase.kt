package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.Flow

/**
 * Use case for fetching recommended games based on tags.
 * Encapsulates the business logic for personalized recommendations.
 */
class GetRecommendedGamesUseCase(
    private val gameRepository: GameRepository
) {
    companion object {
        const val DEFAULT_TAGS = "singleplayer,multiplayer"
    }
    
    operator fun invoke(
        tags: String = DEFAULT_TAGS,
        page: Int = 1,
        pageSize: Int = 40
    ): Flow<RequestState<List<Game>>> {
        return gameRepository.getRecommendedGames(tags, page, pageSize)
    }
}
