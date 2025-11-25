package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.flow.Flow

/**
 * Use case for fetching new releases.
 * Encapsulates the business logic for retrieving recently released games.
 */
class GetNewReleasesUseCase(
    private val gameRepository: GameRepository
) {
    operator fun invoke(
        page: Int = 1,
        pageSize: Int = 10
    ): Flow<RequestState<List<Game>>> {
        return gameRepository.getNewReleases(page, pageSize)
    }
}
