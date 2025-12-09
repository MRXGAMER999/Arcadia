package com.example.arcadia.di

import com.example.arcadia.util.NetworkMonitor
import com.example.arcadia.util.PreferencesManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val utilModule = module {
    single { PreferencesManager(androidContext()) }
    single { NetworkMonitor(androidContext()) }
}
