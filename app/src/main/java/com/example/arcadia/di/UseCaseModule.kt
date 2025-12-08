package com.example.arcadia.di

import com.example.arcadia.domain.usecase.AddGameToLibraryUseCase
import com.example.arcadia.domain.usecase.AnalyzeGamingProfileUseCase
import com.example.arcadia.domain.usecase.CalculateGamingStatsUseCase
import com.example.arcadia.domain.usecase.DetermineGamingPersonalityUseCase
import com.example.arcadia.domain.usecase.ExtractRoastStatsUseCase
import com.example.arcadia.domain.usecase.FilterGamesUseCase
import com.example.arcadia.domain.usecase.GetAIGameSuggestionsUseCase
import com.example.arcadia.domain.usecase.GetNewReleasesUseCase
import com.example.arcadia.domain.usecase.GetPopularGamesUseCase
import com.example.arcadia.domain.usecase.GetRecommendedGamesUseCase
import com.example.arcadia.domain.usecase.GetUpcomingGamesUseCase
import com.example.arcadia.domain.usecase.ParallelGameFilter
import com.example.arcadia.domain.usecase.RemoveGameFromLibraryUseCase
import com.example.arcadia.domain.usecase.SearchGamesUseCase
import com.example.arcadia.domain.usecase.SortGamesUseCase
import com.example.arcadia.domain.usecase.UpdateGameEntryUseCase
import com.example.arcadia.domain.usecase.studio.GetLocalStudioSuggestionsUseCase
import com.example.arcadia.domain.usecase.studio.GetStudioExpansionUseCase
import com.example.arcadia.domain.usecase.studio.SearchStudiosUseCase
import org.koin.dsl.module

/**
 * Koin module for use case dependency injection.
 * Contains all business logic use cases for the application.
 */
val useCaseModule = module {
    
    // ==================== Game Fetching Use Cases ====================
    
    /** Fetches popular games from the API */
    factory { GetPopularGamesUseCase(get()) }
    
    /** Fetches upcoming game releases */
    factory { GetUpcomingGamesUseCase(get()) }
    
    /** Fetches recently released games */
    factory { GetNewReleasesUseCase(get()) }
    
    /** Fetches recommended games based on tags */
    factory { GetRecommendedGamesUseCase(get()) }
    
    /** Searches games by query */
    factory { SearchGamesUseCase(get()) }
    
    // ==================== Library Management Use Cases ====================
    
    /** Adds a game to the user's library */
    factory { AddGameToLibraryUseCase(get()) }
    
    /** Removes a game from the user's library */
    factory { RemoveGameFromLibraryUseCase(get()) }
    
    /** Updates an existing game entry in the library */
    factory { UpdateGameEntryUseCase(get()) }
    
    // ==================== AI Use Cases ====================
    
    /** Gets AI-powered game suggestions based on natural language queries */
    factory { GetAIGameSuggestionsUseCase(get()) }
    
    /** Analyzes the user's gaming profile using AI */
    factory { AnalyzeGamingProfileUseCase(get()) }
    
    // ==================== Analytics Use Cases ====================
    
    /** Calculates gaming statistics from the user's library */
    factory { CalculateGamingStatsUseCase() }
    
    /** Determines the user's gaming personality type */
    factory { DetermineGamingPersonalityUseCase() }
    
    /** Extracts roast-relevant statistics from AnalyticsState (Requirements: 4.1) */
    factory { ExtractRoastStatsUseCase() }
    
    // ==================== Filtering & Sorting Use Cases ====================
    
    /** Parallel game filtering for performance */
    factory { ParallelGameFilter() }
    
    /** Sorts games by various criteria */
    factory { SortGamesUseCase() }
    
    /** Filters games by various criteria */
    factory { FilterGamesUseCase() }
    
    // ==================== Studio Use Cases ====================
    
    /** Searches for studios/developers/publishers using AI */
    factory { SearchStudiosUseCase(get()) }
    
    /** Expands a parent studio to find subsidiaries */
    factory { GetStudioExpansionUseCase(get()) }
    
    /** Gets local studio suggestions without AI */
    factory { GetLocalStudioSuggestionsUseCase(get()) }
}
