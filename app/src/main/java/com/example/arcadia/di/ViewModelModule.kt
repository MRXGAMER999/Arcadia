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
    viewModel { HomeViewModel(get(), get(), get(), get(), get()) }
    viewModel { DiscoveryViewModel(get(), get(), get()) }
    viewModel { MyGamesViewModel(get(), get()) }
    viewModel { DetailsScreenViewModel(get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get()) }
    viewModel { AnalyticsViewModel(get(), get()) }
}