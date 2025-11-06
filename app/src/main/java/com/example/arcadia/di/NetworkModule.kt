package com.example.arcadia.di

import com.example.arcadia.data.remote.RawgApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val networkModule = module {
    
    // API Key Interceptor
    single<Interceptor> {
        Interceptor { chain ->
            val original = chain.request()
            val originalHttpUrl = original.url
            
            val url = originalHttpUrl.newBuilder()
                .addQueryParameter("key", "8fab60496d4b475987e3950a0b40f420")
                .build()
            
            val requestBuilder = original.newBuilder()
                .url(url)
            
            val request = requestBuilder.build()
            chain.proceed(request)
        }
    }
    
    // Logging Interceptor
    single {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }
    
    // OkHttpClient
    single {
        OkHttpClient.Builder()
            .addInterceptor(get<Interceptor>())
            .addInterceptor(get<HttpLoggingInterceptor>())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    // Retrofit
    single {
        Retrofit.Builder()
            .baseUrl(RawgApiService.BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // API Service
    single<RawgApiService> {
        get<Retrofit>().create(RawgApiService::class.java)
    }
}


