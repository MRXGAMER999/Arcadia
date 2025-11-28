package com.example.arcadia.di

import com.example.arcadia.data.GamerRepositoryImpl
import com.example.arcadia.data.local.GameCacheDatabase
import com.example.arcadia.data.local.StudioCacheDatabase
import com.example.arcadia.data.local.StudioCacheManager
import com.example.arcadia.data.local.dao.RecommendationFeedbackDao
import com.example.arcadia.data.remote.GroqApiService
import com.example.arcadia.data.repository.FallbackAIRepository
import com.example.arcadia.data.repository.GameListRepositoryImpl
import com.example.arcadia.data.repository.GameRepositoryImpl
import com.example.arcadia.data.repository.GeminiRepository
import com.example.arcadia.data.repository.GroqRepository
import com.example.arcadia.data.repository.PagedGameRepositoryImpl
import com.example.arcadia.domain.repository.AIRepository
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.domain.repository.PagedGameRepository
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
    
    // ==================== Game Cache Dependencies (Paging 3) ====================
    
    /** Local database for game caching (AI recommendations + feedback) */
    single { GameCacheDatabase.getInstance(androidContext()) }
    
    /** DAO for cached games (used by Paging 3 RemoteMediator) */
    single { get<GameCacheDatabase>().cachedGamesDao() }
    
    /** DAO for recommendation feedback (tracks user interactions for AI improvement) */
    single<RecommendationFeedbackDao> { get<GameCacheDatabase>().recommendationFeedbackDao() }
    
    /** 
     * Paged Game Repository for Paging 3 integration.
     * Provides AI recommendations with offline caching via RemoteMediator.
     * 
     * Enhanced with:
     * - Progressive loading (high confidence first)
     * - Smarter cache invalidation
     * - Feedback loop for AI improvement
     */
    single<PagedGameRepository> {
        PagedGameRepositoryImpl(
            aiRepository = get(),
            gameRepository = get(),
            gameListRepository = get(),
            cachedGamesDao = get(),
            feedbackDao = get()
        )
    }
    
    // ==================== AI Repositories ====================
    
    /** 
     * Groq AI Repository (Primary - faster, cheaper) 
     * Uses simplified architecture with BaseAIRepository + GroqAIClient
     */
    single<AIRepository>(named("groq")) { 
        GroqRepository(
            groqApiService = get<GroqApiService>(),
            studioCacheManager = get()
        )
    }
    
    /** 
     * Gemini AI Repository (Fallback - more reliable) 
     * Uses simplified architecture with BaseAIRepository + GeminiAIClient
     */
    single<AIRepository>(named("gemini")) { 
        GeminiRepository(
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
