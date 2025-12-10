package com.example.arcadia.di

import com.example.arcadia.presentation.components.sign_in.SignInViewModel
import com.example.arcadia.presentation.screens.analytics.AnalyticsViewModel
import com.example.arcadia.presentation.screens.authScreen.AuthViewModel
import com.example.arcadia.presentation.screens.detailsScreen.DetailsScreenViewModel
import com.example.arcadia.presentation.screens.friends.FriendRequestsViewModel
import com.example.arcadia.presentation.screens.friends.FriendsViewModel
import com.example.arcadia.presentation.screens.home.DiscoveryViewModel
import com.example.arcadia.presentation.screens.home.HomeViewModel
import com.example.arcadia.presentation.screens.myGames.MyGamesViewModel
import com.example.arcadia.presentation.screens.profile.ProfileViewModel
import com.example.arcadia.presentation.screens.profile.update_profile.EditProfileViewModel
import com.example.arcadia.presentation.screens.roast.RoastViewModel
import com.example.arcadia.presentation.screens.searchScreen.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { SignInViewModel() }
    viewModel { AuthViewModel(get()) }
    viewModel { EditProfileViewModel(get()) }
    // ProfileViewModel: gamerRepository, gameListRepository, featuredBadgesRepository, friendsRepository, networkMonitor
    // Requirements: 8.1-8.4, 9.1, 9.2, 9.3, 13.1, 13.2, 13.3, 13.4, 14.6, 14.7 - Load featured badges and friendship status for profile display
    viewModel { (userId: String?) -> ProfileViewModel(get(), get(), get(), get(), get(), userId) }
    // HomeViewModel: gameRepository, gameListRepository, aiRepository, preferencesManager, addGameToLibraryUseCase, pagedGameRepository, getPendingFriendRequestsCountUseCase
    // Requirements: 2.3, 2.4, 2.5 - Pending friend request count for badge display
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { DiscoveryViewModel(get(), get(), get(), get()) }
    // MyGamesViewModel: gameListRepository, preferencesManager, addGameToLibraryUseCase, removeGameFromLibraryUseCase, filterGamesUseCase, sortGamesUseCase, userId (optional)
    viewModel { (userId: String?) -> MyGamesViewModel(get(), get(), get(), get(), get(), get(), userId) }
    // DetailsScreenViewModel: gameRepository, gameListRepository, addGameToLibraryUseCase, removeGameFromLibraryUseCase
    viewModel { DetailsScreenViewModel(get(), get(), get(), get()) }
    // SearchViewModel: gameRepository, gameListRepository, preferencesManager, addGameToLibraryUseCase, searchGamesUseCase, getAIGameSuggestionsUseCase, getPopularGamesUseCase
    viewModel { SearchViewModel(get(), get(), get(), get(), get(), get(), get()) }
    // AnalyticsViewModel: gameListRepository, aiRepository, calculateGamingStatsUseCase, determineGamingPersonalityUseCase
    viewModel { AnalyticsViewModel(get(), get(), get(), get()) }
    // RoastViewModel: aiRepository, roastRepository, gameListRepository, calculateGamingStatsUseCase, determineGamingPersonalityUseCase, extractRoastStatsUseCase, featuredBadgesRepository, gamerRepository, targetUserId (optional)
    // Requirements: 8.2, 8.3, 8.4, 12.2, 12.3
    viewModel { (targetUserId: String?) -> RoastViewModel(get(), get(), get(), get(), get(), get(), get(), get(), targetUserId) }
    // FriendsViewModel: friendsRepository, gamerRepository, networkMonitor
    // Requirements: 1.1, 1.5, 1.6, 2.3, 2.4, 2.5, 3.3, 3.4, 3.6, 3.7, 3.13, 3.14, 3.15, 3.18, 3.24, 13.1, 13.2, 13.3, 13.4
    viewModel { FriendsViewModel(get(), get(), get()) }
    // FriendRequestsViewModel: friendsRepository, gamerRepository, preferencesManager, networkMonitor
    // Requirements: 6.2, 6.3, 6.8, 6.11, 6.13, 7.1, 7.4, 13.1, 13.2, 13.3, 13.4, 17.1
    viewModel { FriendRequestsViewModel(get(), get(), get(), get()) }
}