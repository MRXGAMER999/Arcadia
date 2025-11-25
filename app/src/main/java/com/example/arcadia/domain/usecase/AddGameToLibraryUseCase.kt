package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.util.RequestState

/**
 * Use case for adding a game to the user's library.
 * Encapsulates the business logic for library management.
 */
class AddGameToLibraryUseCase(
    private val gameListRepository: GameListRepository
) {
    /**
     * Adds a game to the user's library with the specified status.
     * 
     * @param game The game to add
     * @param status The initial status for the game
     * @return RequestState containing the entry ID on success
     */
    suspend operator fun invoke(
        game: Game,
        status: GameStatus
    ): RequestState<String> {
        return gameListRepository.addGameToList(game, status)
    }
}
