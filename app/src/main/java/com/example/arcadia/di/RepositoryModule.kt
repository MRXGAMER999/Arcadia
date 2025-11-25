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
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for repository dependency injection.
 * Contains all data layer dependencies.
 */
val repositoryModule = module {
    
    // ==================== Repositories ====================
    
    /** Repository for gamer/user data */
    single<GamerRepository> { GamerRepositoryImpl() }
    
    /** Repository for game data from RAWG API */
    single<GameRepository> { GameRepositoryImpl(get()) }
    
    /** Repository for user's game list (Firebase) */
    single<GameListRepository> { GameListRepositoryImpl() }
    
    /** Repository for Gemini AI operations */
    single<GeminiRepository> { GeminiRepositoryImpl() }
    
    // ==================== Studio Expansion Dependencies ====================
    
    /** Local database for studio caching */
    single { StudioCacheDatabase.getInstance(androidContext()) }
    
    /** Manager for studio cache operations */
    single { StudioCacheManager(get()) }
    
    /** Repository for studio expansion features */
    single { StudioExpansionRepository(get()) }
}
