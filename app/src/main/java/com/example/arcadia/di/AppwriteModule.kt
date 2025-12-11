package com.example.arcadia.di

import com.example.arcadia.BuildConfig
import io.appwrite.Client
import io.appwrite.services.TablesDB
import io.appwrite.services.Realtime
import io.appwrite.services.Storage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module providing Appwrite service instances for dependency injection.
 * Configures the Appwrite Client with BuildConfig values and provides
 * singleton instances of TablesDB, Storage, and Realtime services.
 */
val appwriteModule = module {
    // Appwrite Client - configured with BuildConfig values
    single {
        Client(androidContext())
            .setEndpoint(BuildConfig.APPWRITE_ENDPOINT)
            .setProject(BuildConfig.APPWRITE_PROJECT_ID)
            // .setSelfSigned(true) // Removed: Deprecated and not needed for Appwrite Cloud
    }
    
    // Appwrite Account service (for Authentication)
    single { io.appwrite.services.Account(get()) }

    // Appwrite TablesDB service (replaces deprecated Databases)
    single { TablesDB(get()) }
    
    // Appwrite Storage service
    single { Storage(get()) }
    
    // Appwrite Realtime service
    single { Realtime(get()) }
}
