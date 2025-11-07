package com.example.arcadia.presentation.screens.detailsScreen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.Game
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class DetailsScreenViewModel : ViewModel() {

    var gameState by mutableStateOf<RequestState<Game>>(RequestState.Idle)
        private set

    fun loadGameDetails(gameId: Int) {
        gameState = RequestState.Loading

        viewModelScope.launch {
            // Simulate network delay
            delay(1000L)

            try {
                val game = createMockGame(gameId)
                gameState = RequestState.Success(game)
            } catch (e: Exception) {
                gameState = RequestState.Error(
                    message = "Failed to load game details"
                )
            }
        }
    }

    fun retry() {
        val currentGame = (gameState as? RequestState.Success)?.data
        currentGame?.let { game ->
            loadGameDetails(game.id)
        }
    }

    private fun createMockGame(id: Int): Game {
        return Game(
            id = id,
            slug = "hollow-knight",
            name = "Hollow Knight",
            released = "2017-02-24",
            backgroundImage = "@drawable/hollow_knight_background.jpg",
            rating = 8.9,
            metacritic = 87,
            playtime = 25,
            platforms = listOf("PC", "Nintendo Switch", "PlayStation 4", "Xbox One", "macOS", "Linux"),
            genres = listOf("Action-Adventure", "Metroidvania", "Platformer"),
            tags = listOf("2D", "Dark Fantasy", "Singleplayer", "Atmospheric", "Difficult", "Great Soundtrack")
        )
    }
}