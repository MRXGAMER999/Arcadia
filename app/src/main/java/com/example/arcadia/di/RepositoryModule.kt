package com.example.arcadia.di

import com.example.arcadia.data.GamerRepositoryImpl
import com.example.arcadia.data.local.StudioCacheDatabase
import com.example.arcadia.data.local.StudioCacheManager
import com.example.arcadia.data.remote.GeminiConfig
import com.example.arcadia.data.remote.GroqApiService
import com.example.arcadia.data.repository.FallbackAIRepository
import com.example.arcadia.data.repository.GameListRepositoryImpl
import com.example.arcadia.data.repository.GameRepositoryImpl
import com.example.arcadia.data.repository.GeminiRepositoryImpl
import com.example.arcadia.data.repository.GroqRepositoryImpl
import com.example.arcadia.domain.repository.AIRepository
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.domain.repository.GameRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
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
    
    // ==================== AI Repositories ====================
    
    /** Groq AI Repository (Primary - faster, cheaper) */
    single<AIRepository>(named("groq")) { 
        GroqRepositoryImpl(
            groqApiService = get<GroqApiService>(),
            studioCacheManager = get()
        )
    }
    
    /** Gemini AI Repository (Fallback - more reliable) */
    single<AIRepository>(named("gemini")) { 
        GeminiRepositoryImpl(
            jsonModel = GeminiConfig.createJsonModel(),
            textModel = GeminiConfig.createTextModel(),
            studioCacheManager = get()
        )
    }
    
    /**
     * Main AI Repository with automatic fallback.
     * 
     * Strategy:
     * - Primary: Groq (Llama 3.3 70B) - Fast and cost-effective
     * - Fallback: Gemini (Flash 2.5) - Reliable backup
     * 
     * On any Groq error (rate limit, network, etc.), automatically
     * switches to Gemini for seamless user experience.
     */
    single<AIRepository> { 
        FallbackAIRepository(
            primaryRepository = get(named("groq")),
            fallbackRepository = get(named("gemini"))
        )
    }
}
