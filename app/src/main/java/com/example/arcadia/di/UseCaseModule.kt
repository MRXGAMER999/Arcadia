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
import com.example.arcadia.domain.usecase.GetPendingFriendRequestsCountUseCase
import com.example.arcadia.domain.usecase.GetRecommendedGamesUseCase
import com.example.arcadia.domain.usecase.GetUpcomingGamesUseCase
import com.example.arcadia.domain.usecase.ParallelGameFilter
import com.example.arcadia.domain.usecase.RemoveGameFromLibraryUseCase
import com.example.arcadia.domain.usecase.SearchGamesUseCase
import com.example.arcadia.domain.usecase.SortGamesUseCase
import com.example.arcadia.domain.usecase.UpdateGameEntryUseCase
import com.example.arcadia.domain.usecase.friend.AcceptFriendRequestUseCase
import com.example.arcadia.domain.usecase.friend.CancelFriendRequestUseCase
import com.example.arcadia.domain.usecase.friend.DeclineFriendRequestUseCase
import com.example.arcadia.domain.usecase.friend.GetFriendshipStatusUseCase
import com.example.arcadia.domain.usecase.friend.GetPendingRequestsUseCase
import com.example.arcadia.domain.usecase.friend.GetSentRequestsUseCase
import com.example.arcadia.domain.usecase.friend.RemoveFriendUseCase
import com.example.arcadia.domain.usecase.friend.SearchUsersUseCase
import com.example.arcadia.domain.usecase.friend.SendFriendRequestUseCase
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
    
    // ==================== Social / Friends Use Cases ====================
    
    /** Observes pending friend requests for badge display */
    factory { GetPendingFriendRequestsCountUseCase(get(), get()) }
    
    /** Sends a friend request with full validation (auth, reciprocal, cooldown, limits) */
    factory { SendFriendRequestUseCase(get(), get()) }
    
    /** Accepts a friend request with friends limit validation */
    factory { AcceptFriendRequestUseCase(get(), get()) }
    
    /** Declines a friend request (marks as declined for cooldown tracking) */
    factory { DeclineFriendRequestUseCase(get()) }
    
    /** Cancels a sent friend request */
    factory { CancelFriendRequestUseCase(get()) }
    
    /** Removes a friend from both users' lists */
    factory { RemoveFriendUseCase(get(), get()) }
    
    /** Searches users with enriched friendship status */
    factory { SearchUsersUseCase(get(), get()) }
    
    /** Gets realtime friendship status for profile screens */
    factory { GetFriendshipStatusUseCase(get(), get()) }
    
    /** Gets incoming friend requests with realtime updates */
    factory { GetPendingRequestsUseCase(get(), get()) }
    
    /** Gets outgoing friend requests with realtime updates */
    factory { GetSentRequestsUseCase(get(), get()) }
    
    // ==================== Studio Use Cases ====================
    
    /** Searches for studios/developers/publishers using AI */
    factory { SearchStudiosUseCase(get()) }
    
    /** Expands a parent studio to find subsidiaries */
    factory { GetStudioExpansionUseCase(get()) }
    
    /** Gets local studio suggestions without AI */
    factory { GetLocalStudioSuggestionsUseCase(get()) }
}
