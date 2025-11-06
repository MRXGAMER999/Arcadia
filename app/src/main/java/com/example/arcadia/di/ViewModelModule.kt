package com.example.arcadia.di

import com.example.arcadia.presentation.componenets.sign_in.SignInViewModel
import com.example.arcadia.presentation.screens.authScreen.AuthViewModel
import com.example.arcadia.presentation.screens.home.HomeViewModel
import com.example.arcadia.presentation.screens.myGames.MyGamesViewModel
import com.example.arcadia.presentation.screens.profile.update_profile.EditProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { SignInViewModel() }
    viewModel { AuthViewModel(get()) }
    viewModel { EditProfileViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { MyGamesViewModel(get()) }
}