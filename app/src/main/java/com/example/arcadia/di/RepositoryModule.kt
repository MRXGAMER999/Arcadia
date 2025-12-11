package com.example.arcadia.di

import com.example.arcadia.data.GamerRepositoryImpl
import com.example.arcadia.data.local.GameCacheDatabase
import com.example.arcadia.data.local.StudioCacheDatabase
import com.example.arcadia.data.local.StudioCacheManager
import com.example.arcadia.data.local.dao.RecommendationFeedbackDao
import com.example.arcadia.data.remote.GroqApiService
import com.example.arcadia.data.remote.OneSignalNotificationService
import com.example.arcadia.data.repository.FallbackAIRepository
import com.example.arcadia.data.repository.FriendsRepositoryImpl
import com.example.arcadia.data.repository.GameListRepositoryImpl
import com.example.arcadia.data.repository.GameRepositoryImpl
import com.example.arcadia.data.repository.GeminiRepository
import com.example.arcadia.data.repository.GroqRepository
import com.example.arcadia.data.repository.PagedGameRepositoryImpl
import com.example.arcadia.data.repository.RoastRepositoryImpl
import com.example.arcadia.data.repository.FeaturedBadgesRepositoryImpl
import com.example.arcadia.domain.repository.AIRepository
import com.example.arcadia.domain.repository.FeaturedBadgesRepository
import com.example.arcadia.domain.repository.FriendsRepository
import com.example.arcadia.domain.repository.GameListRepository
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.domain.repository.GameRepository
import com.example.arcadia.domain.repository.PagedGameRepository
import com.example.arcadia.domain.repository.RoastRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin module for repository dependency injection.
 * Contains all data layer dependencies.
 */
val repositoryModule = module {
    
    // ==================== Data Sources ====================

    single<com.example.arcadia.data.datasource.FriendsRemoteDataSource> {
        com.example.arcadia.data.datasource.FriendsRemoteDataSourceImpl(
            tablesDbLazy = lazy { get() },
            realtimeLazy = lazy { get() }
        )
    }

    single<com.example.arcadia.data.datasource.GameListRemoteDataSource> {
        com.example.arcadia.data.datasource.GameListRemoteDataSourceImpl(
            tablesDbLazy = lazy { get() },
            realtimeLazy = lazy { get() }
        )
    }

    single<com.example.arcadia.data.datasource.GamerRemoteDataSource> {
        com.example.arcadia.data.datasource.GamerRemoteDataSourceImpl(
            tablesDbLazy = lazy { get() },
            storageLazy = lazy { get() },
            realtimeLazy = lazy { get() }
        )
    }

    // ==================== Repositories ====================

    /** Repository for gamer/user data - now using Appwrite (Requirements: 10.2) */
    single<GamerRepository> {
        GamerRepositoryImpl(
            context = androidContext(),
            remoteDataSource = get()
        )
    }

    /** OneSignal notification service for push notifications (Requirements: 10.6, 10.7, 10.8) */
    single { OneSignalNotificationService(get()) }

    /** Repository for friends and friend requests - now using Appwrite (Requirements: 10.2) */
    single<FriendsRepository> {
        FriendsRepositoryImpl(
            remoteDataSource = get(),
            notificationService = get()
        )
    }

    /**
     * Repository for game data from RAWG APIok
     * Now includes caching and request deduplication for faster performance
     */
    single<GameRepository> {
        GameRepositoryImpl(
            apiService = get(),
            cacheManager = get(),
            deduplicator = get()
        )
    }

    /** Repository for user's game list - now using Appwrite (Requirements: 10.2) */
    single<GameListRepository> {
        GameListRepositoryImpl(
            tablesDb = get(),
            realtime = get()
        )
    }
    
    // ==================== Studio Cache Dependencies ====================

    /** Local database for studio caching - lazy to avoid blocking startup */
    single(createdAtStart = false) { StudioCacheDatabase.getInstance(androidContext()) }

    /** Manager for studio cache operations */
    single(createdAtStart = false) { StudioCacheManager(get()) }

    // ==================== Game Cache Dependencies (Paging 3) ====================

    /** Local database for game caching (AI recommendations + feedback) - lazy */
    single(createdAtStart = false) { GameCacheDatabase.getInstance(androidContext()) }

    /** DAO for cached games (used by Paging 3 RemoteMediator) */
    single(createdAtStart = false) { get<GameCacheDatabase>().cachedGamesDao() }

    /** DAO for recommendation feedback (tracks user interactions for AI improvement) */
    single<RecommendationFeedbackDao>(createdAtStart = false) { get<GameCacheDatabase>().recommendationFeedbackDao() }

    /** DAO for roast storage (Requirements: 6.1) */
    single(createdAtStart = false) { get<GameCacheDatabase>().roastDao() }

    /** Repository for roast data persistence (Requirements: 6.1) */
    single<RoastRepository> { RoastRepositoryImpl(get()) }

    /** Repository for featured badges on user profiles - now using Appwrite (Requirements: 6.1, 6.2, 6.3, 10.2) */
    single<FeaturedBadgesRepository> {
        FeaturedBadgesRepositoryImpl(
            remoteDataSource = get()
        )
    }
    
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
