package com.example.arcadia.di

import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import okio.Path.Companion.toOkioPath

val imageLoaderModule = module {
    single<ImageLoader> {
        ImageLoader.Builder(androidContext())
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(androidContext(), 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(androidContext().cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizePercent(0.02)
                    .minimumMaxSizeBytes(10 * 1024 * 1024)
                    .maximumMaxSizeBytes(250 * 1024 * 1024)
                    .build()
            }
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = get<OkHttpClient>()))
            }
            .build()
    }
}
