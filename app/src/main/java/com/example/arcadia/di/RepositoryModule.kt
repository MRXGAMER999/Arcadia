package com.example.arcadia.di

import com.example.arcadia.data.GamerRepositoryImpl
import com.example.arcadia.data.local.StudioCacheDatabase
import com.example.arcadia.data.local.StudioCacheManager
import com.example.arcadia.data.remote.AIConfig
import com.example.arcadia.data.remote.GeminiConfig
import com.example.arcadia.data.remote.GroqApiService
import com.example.arcadia.data.repository.GameListRepositoryImpl
import com.example.arcadia.data.repository.GameRepositoryImpl
import com.example.arcadia.data.repository.GeminiRepositoryImpl
import com.example.arcadia.data.repository.GroqRepositoryImpl
import com.example.arcadia.domain.repository.AIRepository
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.domain.repository.GameRepository
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
    
    /** 
     * Repository for game data from RAWG API
     * Now includes caching and request deduplication for faster performance
     */
    single<GameRepository> { 
        GameRepositoryImpl(
            apiService = get(),
            cacheManager = get(),
            deduplicator = get()
        ) 
    }
    
    /** Repository for user's game list (Firebase) */
    single<GameListRepository> { GameListRepositoryImpl() }
    
    // ==================== Studio Cache Dependencies ====================
    
    /** Local database for studio caching */
    single { StudioCacheDatabase.getInstance(androidContext()) }
    
    /** Manager for studio cache operations */
    single { StudioCacheManager(get()) }
    
    // ==================== AI Repository ====================
    
    /**
     * AI Repository - automatically selects provider based on AIConfig.CURRENT_PROVIDER
     * 
     * To switch providers, change AIConfig.CURRENT_PROVIDER in:
     * com.example.arcadia.data.remote.AIConfig
     * 
     * Available providers:
     * - AIConfig.AIProvider.GEMINI: Google Gemini AI
     * - AIConfig.AIProvider.GROQ: Groq with Kimi K2 model
     * 
     * Features included:
     * - Game suggestions with caching
     * - Profile analysis with streaming support
     * - Studio expansion with multi-layer caching
     */
    single<AIRepository> { 
        when (AIConfig.CURRENT_PROVIDER) {
            AIConfig.AIProvider.GEMINI -> GeminiRepositoryImpl(
                jsonModel = GeminiConfig.createJsonModel(),
                textModel = GeminiConfig.createTextModel(),
                studioCacheManager = get()
            )
            AIConfig.AIProvider.GROQ -> GroqRepositoryImpl(
                groqApiService = get<GroqApiService>(),
                studioCacheManager = get()
            )
        }
    }
}
