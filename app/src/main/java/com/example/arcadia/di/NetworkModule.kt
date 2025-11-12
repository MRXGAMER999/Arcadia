package com.example.arcadia.di

import com.example.arcadia.data.remote.RawgApiService
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
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

    // API Key Interceptor
    single<Interceptor> {
        Interceptor { chain ->
            val original = chain.request()
            val originalHttpUrl = original.url
            
            val url = originalHttpUrl.newBuilder()
                .addQueryParameter("key", com.example.arcadia.BuildConfig.RAWG_API_KEY)
                .build()
            
            val requestBuilder = original.newBuilder()
                .url(url)
            
            val request = requestBuilder.build()
            chain.proceed(request)
        }
    }
    
    // Logging Interceptor (disable in release)
    single {
        HttpLoggingInterceptor().apply {
            level = if (com.example.arcadia.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    
    // OkHttpClient
    single {
        val cacheDir = androidContext().cacheDir.resolve("http_cache")
        OkHttpClient.Builder()
            .cache(Cache(cacheDir, 100L * 1024 * 1024)) // 100 MB cache
            .addInterceptor(get<Interceptor>())
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
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
    
    // API Service
    single<RawgApiService> {
        get<Retrofit>().create(RawgApiService::class.java)
    }
}


