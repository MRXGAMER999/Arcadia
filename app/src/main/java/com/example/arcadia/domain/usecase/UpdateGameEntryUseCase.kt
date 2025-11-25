package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.util.RequestState

/**
 * Use case for updating a game entry in the user's library.
 * Encapsulates the business logic for updating game information.
 */
class UpdateGameEntryUseCase(
    private val gameListRepository: GameListRepository
) {
    /**
     * Updates a game entry with new information.
     * 
     * @param entry The updated game entry
     * @return RequestState indicating success or failure
     */
    suspend operator fun invoke(entry: GameListEntry): RequestState<Unit> {
        return gameListRepository.updateGameEntry(entry)
    }
}
