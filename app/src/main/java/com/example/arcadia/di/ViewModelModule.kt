package com.example.arcadia.di

import com.example.arcadia.presentation.components.sign_in.SignInViewModel
import com.example.arcadia.presentation.screens.analytics.AnalyticsViewModel
import com.example.arcadia.presentation.screens.authScreen.AuthViewModel
import com.example.arcadia.presentation.screens.detailsScreen.DetailsScreenViewModel
import com.example.arcadia.presentation.screens.home.DiscoveryViewModel
import com.example.arcadia.presentation.screens.home.HomeViewModel
import com.example.arcadia.presentation.screens.myGames.MyGamesViewModel
import com.example.arcadia.presentation.screens.profile.update_profile.EditProfileViewModel
import com.example.arcadia.presentation.screens.searchScreen.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { SignInViewModel() }
    viewModel { AuthViewModel(get()) }
    viewModel { EditProfileViewModel(get()) }
    // HomeViewModel: gameRepository, gameListRepository, aiRepository, preferencesManager, addGameToLibraryUseCase, parallelGameFilter, pagedGameRepository
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { DiscoveryViewModel(get(), get(), get(), get()) }
    // MyGamesViewModel: gameListRepository, preferencesManager, addGameToLibraryUseCase, removeGameFromLibraryUseCase, filterGamesUseCase, sortGamesUseCase
    viewModel { MyGamesViewModel(get(), get(), get(), get(), get(), get()) }
    // DetailsScreenViewModel: gameRepository, gameListRepository, addGameToLibraryUseCase, removeGameFromLibraryUseCase
    viewModel { DetailsScreenViewModel(get(), get(), get(), get()) }
    // SearchViewModel: gameRepository, gameListRepository, preferencesManager, addGameToLibraryUseCase, searchGamesUseCase, getAIGameSuggestionsUseCase, getPopularGamesUseCase
    viewModel { SearchViewModel(get(), get(), get(), get(), get(), get(), get()) }
    // AnalyticsViewModel: gameListRepository, aiRepository, calculateGamingStatsUseCase, determineGamingPersonalityUseCase
    viewModel { AnalyticsViewModel(get(), get(), get(), get()) }
}