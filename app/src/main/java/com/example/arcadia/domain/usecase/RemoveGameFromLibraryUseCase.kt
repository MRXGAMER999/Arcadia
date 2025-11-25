package com.example.arcadia.domain.usecase

import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.util.RequestState

/**
 * Use case for removing a game from the user's library.
 * Encapsulates the business logic for library removal.
 */
class RemoveGameFromLibraryUseCase(
    private val gameListRepository: GameListRepository
) {
    /**
     * Removes a game from the user's library.
     * 
     * @param entryId The ID of the entry to remove
     * @return RequestState indicating success or failure
     */
    suspend operator fun invoke(entryId: String): RequestState<Unit> {
        return gameListRepository.removeGameFromList(entryId)
    }
    
    /**
     * Removes a game from the user's library using the game entry.
     * 
     * @param entry The game entry to remove
     * @return RequestState indicating success or failure
     */
    suspend operator fun invoke(entry: GameListEntry): RequestState<Unit> {
        return gameListRepository.removeGameFromList(entry.id)
    }
}
