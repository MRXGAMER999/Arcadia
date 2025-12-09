package com.example

import android.app.Application
import android.os.Build
import com.example.arcadia.BuildConfig
import com.example.arcadia.data.local.StudioCacheManager
import com.example.arcadia.di.appModule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class MyApplication : Application() {

    companion object {
        private const val EMULATOR_HOST = "192.168.1.8"
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize OneSignal
        initializeOneSignal()


//            try {
//                val host = EMULATOR_HOST
//
//                val firestoreInstance = FirebaseFirestore.getInstance()
//                val authInstance = FirebaseAuth.getInstance()
//                val storageInstance = FirebaseStorage.getInstance()
//
//                firestoreInstance.useEmulator(host, 8080)
//                authInstance.useEmulator(host, 9099)
//                storageInstance.useEmulator(host, 9199)
//
//                android.util.Log.d("MyApplication", "═══════════════════════════════════════")
//                android.util.Log.d("MyApplication", "Firebase Emulator configured successfully")
//                android.util.Log.d("MyApplication", "Host: $host")
//                android.util.Log.d("MyApplication", "- Firestore: $host:8080")
//                android.util.Log.d("MyApplication", "- Auth: $host:9099")
//                android.util.Log.d("MyApplication", "- Storage: $host:9199")
//                android.util.Log.d("MyApplication", "═══════════════════════════════════════")
//
//                // Test connection
//                android.util.Log.d("MyApplication", "Testing connection to emulator...")
//            } catch (e: IllegalStateException) {
//                android.util.Log.w("MyApplication", "Emulator already configured or setup failed", e)
//            } catch (e: Exception) {
//                android.util.Log.e("MyApplication", "Failed to configure Firebase Emulator", e)
//            }


        startKoin {
            androidLogger()
            androidContext(this@MyApplication)
            modules(appModule)
        }

        // Prefetch popular studios cache on startup (fire-and-forget)
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        applicationScope.launch {
            try {
                val cacheManager: StudioCacheManager = get()
                cacheManager.prefetchPopularStudios()
                android.util.Log.d(TAG, "Studio cache prefetched")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Studio prefetch failed", e)
            }
        }
    }
    
    /**
     * Initializes OneSignal SDK for push notifications.
     * Requirements: 10.1, 10.2
     */
    private fun initializeOneSignal() {
        val appId = BuildConfig.ONESIGNAL_APP_ID
        
        if (appId.isBlank()) {
            android.util.Log.w(TAG, "OneSignal App ID not configured. Push notifications disabled.")
            return
        }
        
        try {
            // Enable verbose logging for debugging (disable in production)
            OneSignal.Debug.logLevel = LogLevel.WARN
            
            // Initialize OneSignal with the app ID from BuildConfig
            OneSignal.initWithContext(this, appId)
            
            // Request notification permission on Android 13+ (API 33+)
            // This will prompt the user for POST_NOTIFICATIONS permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                CoroutineScope(Dispatchers.Main).launch {
                    OneSignal.Notifications.requestPermission(false)
                }
            }
            
            // If user is already logged in, ensure OneSignal is logged in too
            // This handles the case where the app was updated or reinstalled
            syncOneSignalLoginState()
            
            android.util.Log.d(TAG, "OneSignal initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to initialize OneSignal", e)
        }
    }
    
    /**
     * Syncs OneSignal login state with Firebase Auth.
     * If user is already logged in to Firebase, ensure OneSignal is logged in too.
     * This handles app updates/reinstalls where OneSignal state might be lost.
     */
    private fun syncOneSignalLoginState() {
        try {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // User is logged in to Firebase, ensure OneSignal is logged in
                OneSignal.login(currentUser.uid)
                android.util.Log.d(TAG, "OneSignal login synced for existing user: ${currentUser.uid}")
                
                // Try to save the player ID to Firestore
                val subscriptionId = OneSignal.User.pushSubscription.id
                if (!subscriptionId.isNullOrBlank()) {
                    saveOneSignalPlayerIdToFirestore(currentUser.uid, subscriptionId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to sync OneSignal login state", e)
        }
    }
    
    /**
     * Saves the OneSignal player ID to Firestore.
     */
    private fun saveOneSignalPlayerIdToFirestore(userId: String, playerId: String) {
        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("oneSignalPlayerId", playerId)
                .addOnSuccessListener {
                    android.util.Log.d(TAG, "OneSignal player ID saved to Firestore: $playerId")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e(TAG, "Failed to save OneSignal player ID: ${e.message}", e)
                }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error saving OneSignal player ID", e)
        }
    }
}