package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.arcadia.BuildConfig
import com.example.arcadia.data.local.StudioCacheManager
import com.example.arcadia.data.remote.NotificationActionHandler
// import com.example.arcadia.data.appwrite.AppwriteClientProvider
import com.example.arcadia.di.appModule
import com.example.arcadia.util.AppwriteConstants
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.notifications.INotificationLifecycleListener
import com.onesignal.notifications.INotificationWillDisplayEvent
import io.appwrite.services.TablesDB
import io.kotzilla.sdk.analytics.koin.analytics
import io.kotzilla.sdk.android.security.apiKey
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
        private const val FRIENDS_CHANNEL_ID_V2 = "arcadia_friends_v2"
    }
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        
        // Create notification channel for heads-up notifications (Android 8.0+)
        createNotificationChannel()

        // Initialize Koin first (critical path)
        startKoin {
            androidLogger()
            androidContext(this@MyApplication)
            // Add kotzilla analytics
            analytics(onConfig = { apiKey() })
            modules(appModule)
        }

        // Defer non-critical initialization to background thread
        applicationScope.launch {
            // Initialize OneSignal off main thread
            initializeOneSignal()

            // Prefetch popular studios cache (fire-and-forget)
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
     * Note: Must be called from background thread, switches to Main for SDK init
     */
    private suspend fun initializeOneSignal() {
        val appId = BuildConfig.ONESIGNAL_APP_ID
        
        if (appId.isBlank()) {
            android.util.Log.w(TAG, "OneSignal App ID not configured. Push notifications disabled.")
            return
        }
        
        try {
            // Switch to Main thread for OneSignal initialization (required by SDK)
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                // Enable verbose logging for debugging (disable in production)
                OneSignal.Debug.logLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.WARN
                
                // Initialize OneSignal with the app ID from BuildConfig
                OneSignal.initWithContext(this@MyApplication, appId)
                
                // Register notification click handler for Accept/Decline buttons
                OneSignal.Notifications.addClickListener(NotificationActionHandler(applicationContext))
                
                // Ensure notifications can display while the app is in the foreground (not "sound only").
                OneSignal.Notifications.addForegroundLifecycleListener(object : INotificationLifecycleListener {
                    override fun onWillDisplay(event: INotificationWillDisplayEvent) {
                        // If the OS would suppress the visual banner in foreground, force-display it.
                        // (Android still controls whether it's a heads-up popup vs shade-only.)
                        android.util.Log.d(TAG, "OneSignal onWillDisplay title=${event.notification.title}")
                        event.notification.display()
                    }
                })
            }
            
            // Sync login state can run on background thread
            syncOneSignalLoginState()
            
            android.util.Log.d(TAG, "OneSignal initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to initialize OneSignal", e)
        }
    }
    
    //    private suspend fun initializeAppwrite() {
    //        runCatching {
    //            AppwriteClientProvider.ping(applicationContext)
    //        }.onSuccess {
    //            android.util.Log.d(TAG, "Appwrite initialized and connectivity ping sent")
    //        }.onFailure { throwable ->
    //            android.util.Log.e(TAG, "Appwrite initialization failed", throwable)
    //        }
    //    }
    
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
                
                // Try to save the player ID to Appwrite
                val subscriptionId = OneSignal.User.pushSubscription.id
                if (subscriptionId.isNotBlank()) {
                    saveOneSignalPlayerIdToAppwrite(currentUser.uid, subscriptionId)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to sync OneSignal login state", e)
        }
    }
    
    /**
     * Saves the OneSignal player ID to Appwrite.
     */
    private fun saveOneSignalPlayerIdToAppwrite(userId: String, playerId: String) {
        applicationScope.launch {
            try {
                val tablesDb: TablesDB = get()
                tablesDb.updateRow(
                    databaseId = BuildConfig.APPWRITE_DATABASE_ID,
                    tableId = AppwriteConstants.USERS_COLLECTION_ID,
                    rowId = userId,
                    data = mapOf("oneSignalPlayerId" to playerId)
                )
                android.util.Log.d(TAG, "OneSignal player ID saved to Appwrite: $playerId")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to save OneSignal player ID to Appwrite: ${e.message}", e)
            }
        }
    }
    
    /**
     * Creates notification channel for heads-up notifications on Android 8.0+.
     * Required for notifications to display properly with sound and popup.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // IMPORTANT: Android "remembers" channel settings per ID and you can't raise importance later.
            // Using a v2 channel ID ensures devices that previously had the channel set to low/silent
            // will get a fresh HIGH-importance channel (heads-up eligible).
            val channel = NotificationChannel(
                FRIENDS_CHANNEL_ID_V2,
                "Friend Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Friend requests and updates"
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            android.util.Log.d(TAG, "Notification channel created: $FRIENDS_CHANNEL_ID_V2")
        }
    }
}