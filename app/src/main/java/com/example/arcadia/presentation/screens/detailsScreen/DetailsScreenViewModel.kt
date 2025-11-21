package com.example.arcadia.presentation.screens.detailsScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadia.data.remote.mapper.toGameListEntry
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class AddToLibraryState {
    data object Idle : AddToLibraryState()
    data object Loading : AddToLibraryState()
    data class Success(val message: String) : AddToLibraryState()
    data class Error(val message: String) : AddToLibraryState()
}

data class DetailsUiState(
    val gameState: RequestState<Game> = RequestState.Idle,
    val isInLibrary: Boolean = false, 
    val addToLibraryState: AddToLibraryState = AddToLibraryState.Idle,
    val addToListInProgress: Boolean = false,
    val errorMessage: String? = null,
    val showRatingSheet: Boolean = false,
    val tempGameEntry: GameListEntry? = null // For the rating sheet
)

class DetailsScreenViewModel(
    private val gameRepository: GameRepository,
    private val gameListRepository: GameListRepository
) : ViewModel() {

    var uiState by mutableStateOf(DetailsUiState())
        private set

    private var currentGameId: Int? = null
    private var detailsJob: kotlinx.coroutines.Job? = null
    private var entryJob: kotlinx.coroutines.Job? = null

    fun loadGameDetails(gameId: Int) {
        // Cancel any existing jobs to prevent race conditions
        detailsJob?.cancel()
        entryJob?.cancel()

        currentGameId = gameId
        uiState = uiState.copy(gameState = RequestState.Loading, errorMessage = null)

        // Fetch details with media (trailer + screenshots)
        detailsJob = viewModelScope.launch {
            gameRepository.getGameDetailsWithMedia(gameId).collect { state ->
                uiState = uiState.copy(gameState = state)
                // If success, prepare temp entry
                if (state is RequestState.Success) {
                     // We might want to check if we already have an entry for this game
                     // to populate the sheet with existing data if it's already in library
                     // But 'isInLibrary' check is separate.
                }
            }
        }

        // Check membership flags concurrently
        entryJob = viewModelScope.launch {
            val inList = gameListRepository.isGameInList(gameId)
            uiState = uiState.copy(isInLibrary = inList)
            
            if (inList) {
                // If in list, fetch the full entry to display stats or edit
                val entryId = gameListRepository.getEntryIdByRawgId(gameId)
                if (entryId != null) {
                    gameListRepository.getGameEntry(entryId).collect { entryState ->
                        if (entryState is RequestState.Success) {
                            uiState = uiState.copy(tempGameEntry = entryState.data)
                        }
                    }
                }
            }
        }
    }

    fun onAddToLibraryClick() {
        val game = (uiState.gameState as? RequestState.Success)?.data ?: return
        
        if (uiState.isInLibrary) {
            // If already in library, we just open the sheet to edit (tempGameEntry should be populated)
             uiState = uiState.copy(showRatingSheet = true)
        } else {
            // If new, create a fresh entry and open sheet
            val newEntry = game.toGameListEntry()
            uiState = uiState.copy(tempGameEntry = newEntry, showRatingSheet = true)
        }
    }
    
    fun dismissRatingSheet() {
        uiState = uiState.copy(showRatingSheet = false)
    }

    fun saveGameEntry(entry: GameListEntry) {
        uiState = uiState.copy(
            showRatingSheet = false,
            addToListInProgress = true,
            addToLibraryState = AddToLibraryState.Loading
        )

        viewModelScope.launch {
            // Check if we are updating or adding
            if (uiState.isInLibrary) {
                // Update
                when (val result = gameListRepository.updateGameEntry(entry)) {
                    is RequestState.Success -> {
                         uiState = uiState.copy(
                             addToLibraryState = AddToLibraryState.Success("Game updated!"),
                             addToListInProgress = false,
                             tempGameEntry = entry // Update local display
                         )
                    }
                    is RequestState.Error -> {
                        uiState = uiState.copy(
                            addToLibraryState = AddToLibraryState.Error(result.message),
                            addToListInProgress = false
                        )
                    }
                    else -> {}
                }
            } else {
                // Add new
                when (val result = gameListRepository.addGameListEntry(entry)) {
                    is RequestState.Success -> {
                         // We need the ID for the entry, result.data is ID
                         val savedEntry = entry.copy(id = result.data)
                         uiState = uiState.copy(
                             isInLibrary = true,
                             addToLibraryState = AddToLibraryState.Success("Added to library!"),
                             addToListInProgress = false,
                             tempGameEntry = savedEntry
                         )
                    }
                    is RequestState.Error -> {
                        uiState = uiState.copy(
                            addToLibraryState = AddToLibraryState.Error(result.message),
                            addToListInProgress = false
                        )
                    }
                    else -> {}
                }
            }
            
            // Reset notification after delay
            if (uiState.addToLibraryState is AddToLibraryState.Success || uiState.addToLibraryState is AddToLibraryState.Error) {
                delay(2000)
                uiState = uiState.copy(addToLibraryState = AddToLibraryState.Idle)
            }
        }
    }

    fun retry() {
        currentGameId?.let { loadGameDetails(it) }
    }
}
