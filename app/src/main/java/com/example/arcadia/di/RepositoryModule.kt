package com.example.arcadia.di

import com.example.arcadia.data.GamerRepositoryImpl
import com.example.arcadia.data.local.StudioCacheDatabase
import com.example.arcadia.data.local.StudioCacheManager
import com.example.arcadia.data.repository.GameListRepositoryImpl
import com.example.arcadia.data.repository.GameRepositoryImpl
import com.example.arcadia.data.repository.GeminiRepositoryImpl
import com.example.arcadia.data.repository.StudioExpansionRepository
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.domain.repository.GeminiRepository
import com.example.arcadia.domain.usecase.ParallelGameFilter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    single<GamerRepository> { GamerRepositoryImpl() }
    single<GameRepository> { GameRepositoryImpl(get()) }
    single<GameListRepository> { GameListRepositoryImpl() }
    single<GeminiRepository> { GeminiRepositoryImpl() }
    
    // Studio expansion dependencies
    single { StudioCacheDatabase.getInstance(androidContext()) }
    single { StudioCacheManager(get()) }
    single { StudioExpansionRepository(get()) }
    factory { ParallelGameFilter() }
}
