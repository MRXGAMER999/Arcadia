package com.example.arcadia.di

import com.example.arcadia.data.remote.GroqApiService
import com.example.arcadia.data.remote.GroqConfig
import com.example.arcadia.data.remote.RawgApiService
import com.example.arcadia.util.NetworkCacheManager
import com.example.arcadia.util.RequestDeduplicator
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {

    // JSON instance for Kotlin Serialization
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
    
    // Request Deduplicator - prevents duplicate simultaneous requests
    single { RequestDeduplicator() }
    
    // Network Cache Manager - in-memory caching for API responses
    single { NetworkCacheManager() }

    // API Key Interceptor
    single<Interceptor>(named("apiKeyInterceptor")) {
        Interceptor { chain ->
            val original = chain.request()
            val originalHttpUrl = original.url
            
            val url = originalHttpUrl.newBuilder()
                .addQueryParameter("key", com.example.arcadia.BuildConfig.RAWG_API_KEY)
                .build()
            
            // Note: Don't manually set Accept-Encoding header - OkHttp handles GZIP transparently
            val requestBuilder = original.newBuilder()
                .url(url)
                .header("Connection", "keep-alive") // Keep connections alive
            
            val request = requestBuilder.build()
            chain.proceed(request)
        }
    }
    
    // Cache Control Interceptor - adds cache headers to responses
    single<Interceptor>(named("cacheInterceptor")) {
        Interceptor { chain ->
            val response = chain.proceed(chain.request())
            val cacheControl = CacheControl.Builder()
                .maxAge(5, TimeUnit.MINUTES) // Cache responses for 5 minutes
                .build()
            response.newBuilder()
                .removeHeader("Pragma") // Remove no-cache headers from server
                .removeHeader("Cache-Control")
                .header("Cache-Control", cacheControl.toString())
                .build()
        }
    }
    
    // Offline Cache Interceptor - serves cached responses when offline
    single<Interceptor>(named("offlineCacheInterceptor")) {
        Interceptor { chain ->
            var request = chain.request()
            // If no network, force cache
            if (!isNetworkAvailable(androidContext())) {
                val cacheControl = CacheControl.Builder()
                    .maxStale(7, TimeUnit.DAYS)
                    .onlyIfCached()
                    .build()
                request = request.newBuilder()
                    .cacheControl(cacheControl)
                    .build()
            }
            chain.proceed(request)
        }
    }
    
    // Logging Interceptor (BASIC level in debug, NONE in release)
    single {
        HttpLoggingInterceptor().apply {
            level = if (com.example.arcadia.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC // Changed from BODY to BASIC for better performance
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    
    // Connection Pool - reuse connections for better performance
    single {
        ConnectionPool(
            maxIdleConnections = 10,
            keepAliveDuration = 5,
            timeUnit = TimeUnit.MINUTES
        )
    }
    
    // OkHttpClient - optimized for performance
    single {
        val cacheDir = androidContext().cacheDir.resolve("http_cache")
        OkHttpClient.Builder()
            // HTTP/2 support for multiplexing multiple requests over single connection
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            // Connection pooling - reuse connections
            .connectionPool(get())
            // Disk cache - 100MB
            .cache(Cache(cacheDir, 100L * 1024 * 1024))
            // Interceptors
            .addInterceptor(get<Interceptor>(named("offlineCacheInterceptor")))
            .addInterceptor(get<Interceptor>(named("apiKeyInterceptor")))
            .addNetworkInterceptor(get<Interceptor>(named("cacheInterceptor")))
            // Only add logging in debug builds
            .apply {
                if (com.example.arcadia.BuildConfig.DEBUG) {
                    addInterceptor(get<HttpLoggingInterceptor>())
                }
            }
            // Reduced timeouts for faster failure detection
            .connectTimeout(10, TimeUnit.SECONDS) // Reduced from 30
            .readTimeout(15, TimeUnit.SECONDS)    // Reduced from 30
            .writeTimeout(10, TimeUnit.SECONDS)   // Reduced from 30
            // Retry on connection failure
            .retryOnConnectionFailure(true)
            .build()
    }
    
    // Retrofit
    single {
        val json = get<Json>()
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(RawgApiService.BASE_URL)
            .client(get())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
    
    // RAWG API Service
    single<RawgApiService> {
        get<Retrofit>().create(RawgApiService::class.java)
    }
    
    // Groq OkHttpClient (no RAWG interceptor, optimized for AI)
    single(named("groqClient")) {
        OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectionPool(get())
            .apply {
                if (com.example.arcadia.BuildConfig.DEBUG) {
                    addInterceptor(get<HttpLoggingInterceptor>())
                }
            }
            .addInterceptor { chain ->
                // Note: Don't manually set Accept-Encoding - OkHttp handles GZIP transparently
                val request = chain.request().newBuilder()
                    .header("Connection", "keep-alive")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS) // Reduced from 60
            .readTimeout(45, TimeUnit.SECONDS)    // Reduced from 60
            .writeTimeout(30, TimeUnit.SECONDS)   // Reduced from 60
            .retryOnConnectionFailure(true)
            .build()
    }
    
    // Groq Retrofit
    single(named("groqRetrofit")) {
        val json = get<Json>()
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(GroqConfig.BASE_URL)
            .client(get(named("groqClient")))
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }
    
    // Groq API Service
    single<GroqApiService> {
        get<Retrofit>(named("groqRetrofit")).create(GroqApiService::class.java)
    }
}

// Helper function to check network availability
private fun isNetworkAvailable(context: android.content.Context): Boolean {
    val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}


