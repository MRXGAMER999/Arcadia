package com.example

import android.app.Application
import com.example.arcadia.BuildConfig
import com.example.arcadia.di.appModule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class MyApplication : Application() {

    companion object {
        private const val EMULATOR_HOST = "192.168.1.7"
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            try {
                val host = EMULATOR_HOST

                val firestoreInstance = FirebaseFirestore.getInstance()
                val authInstance = FirebaseAuth.getInstance()
                val storageInstance = FirebaseStorage.getInstance()

                firestoreInstance.useEmulator(host, 8080)
                authInstance.useEmulator(host, 9099)
                storageInstance.useEmulator(host, 9199)

                android.util.Log.d("MyApplication", "═══════════════════════════════════════")
                android.util.Log.d("MyApplication", "Firebase Emulator configured successfully")
                android.util.Log.d("MyApplication", "Host: $host")
                android.util.Log.d("MyApplication", "- Firestore: $host:8080")
                android.util.Log.d("MyApplication", "- Auth: $host:9099")
                android.util.Log.d("MyApplication", "- Storage: $host:9199")
                android.util.Log.d("MyApplication", "═══════════════════════════════════════")

                // Test connection
                android.util.Log.d("MyApplication", "Testing connection to emulator...")
            } catch (e: IllegalStateException) {
                android.util.Log.w("MyApplication", "Emulator already configured or setup failed", e)
            } catch (e: Exception) {
                android.util.Log.e("MyApplication", "Failed to configure Firebase Emulator", e)
            }
        }

        startKoin {
            androidLogger()
            androidContext(this@MyApplication)
            modules(appModule)
        }
    }

}