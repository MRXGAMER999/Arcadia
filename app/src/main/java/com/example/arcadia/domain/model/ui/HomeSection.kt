package com.example.arcadia.domain.model.ui

import com.example.arcadia.domain.model.Game
import com.example.arcadia.util.RequestState

/**
 * Represents a section in the home screen.
 * Each section has its own loading state and data.
 */
sealed class HomeSection {
    abstract val title: String
    abstract val isEmpty: Boolean
    
    /**
     * Popular games section showing trending titles.
     */
    data class Popular(
        val games: RequestState<List<Game>> = RequestState.Idle
    ) : HomeSection() {
        override val title: String = "Popular Games"
        override val isEmpty: Boolean = games is RequestState.Success && games.data.isEmpty()
    }
    
    /**
     * Upcoming games section showing anticipated releases.
     */
    data class Upcoming(
        val games: RequestState<List<Game>> = RequestState.Idle
    ) : HomeSection() {
        override val title: String = "Upcoming Games"
        override val isEmpty: Boolean = games is RequestState.Success && games.data.isEmpty()
    }
    
    /**
     * Recommended games section with personalized suggestions.
     */
    data class Recommended(
        val games: RequestState<List<Game>> = RequestState.Idle,
        val hasActiveFilters: Boolean = false
    ) : HomeSection() {
        override val title: String = if (hasActiveFilters) "Filtered Games" else "Recommended For You"
        override val isEmpty: Boolean = games is RequestState.Success && games.data.isEmpty()
    }
    
    /**
     * New releases section showing recently released games.
     */
    data class NewReleases(
        val games: RequestState<List<Game>> = RequestState.Idle
    ) : HomeSection() {
        override val title: String = "New Releases"
        override val isEmpty: Boolean = games is RequestState.Success && games.data.isEmpty()
    }
}

/**
 * Extension to check if a section is loading.
 */
fun HomeSection.isLoading(): Boolean = when (this) {
    is HomeSection.Popular -> games is RequestState.Loading
    is HomeSection.Upcoming -> games is RequestState.Loading
    is HomeSection.Recommended -> games is RequestState.Loading
    is HomeSection.NewReleases -> games is RequestState.Loading
}

/**
 * Extension to check if a section has an error.
 */
fun HomeSection.hasError(): Boolean = when (this) {
    is HomeSection.Popular -> games is RequestState.Error
    is HomeSection.Upcoming -> games is RequestState.Error
    is HomeSection.Recommended -> games is RequestState.Error
    is HomeSection.NewReleases -> games is RequestState.Error
}

/**
 * Extension to get error message from a section.
 */
fun HomeSection.getErrorMessage(): String? = when (this) {
    is HomeSection.Popular -> (games as? RequestState.Error)?.message
    is HomeSection.Upcoming -> (games as? RequestState.Error)?.message
    is HomeSection.Recommended -> (games as? RequestState.Error)?.message
    is HomeSection.NewReleases -> (games as? RequestState.Error)?.message
}
