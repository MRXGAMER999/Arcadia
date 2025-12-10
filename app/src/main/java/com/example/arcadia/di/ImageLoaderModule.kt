package com.example.arcadia.di

import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import okio.Path.Companion.toOkioPath

val imageLoaderModule = module {
    // ImageLoader - lazy initialization to avoid blocking startup
    // Will be created on first image load request
    single<ImageLoader>(createdAtStart = false) {
        ImageLoader.Builder(androidContext())
            // Increased memory cache for faster repeated loads
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(androidContext(), 0.30) // Increased from 0.25
                    .build()
            }
            // Optimized disk cache settings
            .diskCache {
                DiskCache.Builder()
                    .directory(androidContext().cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.05) // Increased from 0.02
                    .minimumMaxSizeBytes(25 * 1024 * 1024) // Increased from 10MB
                    .maximumMaxSizeBytes(500 * 1024 * 1024) // Increased from 250MB
                    .build()
            }
            // Enable crossfade for smoother image transitions
            .crossfade(true)
            .crossfade(200) // 200ms crossfade duration
            // Aggressive caching policies
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // Components with optimized OkHttp client
            .components {
                add(OkHttpNetworkFetcherFactory(
                    callFactory = {
                        // Use the same optimized OkHttpClient but add image-specific headers
                        get<OkHttpClient>().newBuilder()
                            .addInterceptor { chain ->
                                val request = chain.request().newBuilder()
                                    .header("Accept", "image/webp,image/avif,image/*,*/*;q=0.8") // Prefer modern formats
                                    .build()
                                chain.proceed(request)
                            }
                            .build()
                    }
                ))
                add(AnimatedImageDecoder.Factory())
            }
            .build()
    }
}
