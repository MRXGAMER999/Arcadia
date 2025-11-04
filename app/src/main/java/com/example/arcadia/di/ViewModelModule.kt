package com.example.arcadia.di

import com.example.arcadia.presentation.componenets.sign_in.SignInViewModel
import com.example.arcadia.presentation.screens.authScreen.AuthViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { SignInViewModel() }
    viewModel { AuthViewModel(get()) }
}