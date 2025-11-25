package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.Flow

/**
 * Use case for searching games.
 * Encapsulates the business logic for game search functionality.
 */
class SearchGamesUseCase(
    private val gameRepository: GameRepository
) {
    operator fun invoke(
        query: String,
        page: Int = 1,
        pageSize: Int = 40
    ): Flow<RequestState<List<Game>>> {
        return gameRepository.searchGames(query, page, pageSize)
    }
}
