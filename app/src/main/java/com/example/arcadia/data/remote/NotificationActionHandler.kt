package com.example.arcadia.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.arcadia.MainActivity
import com.onesignal.notifications.INotificationClickEvent
import com.onesignal.notifications.INotificationClickListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Handles notification action button clicks (Accept/Decline friend requests).
 * Calls the Appwrite function to process the action.
 */
class NotificationActionHandler(
    private val appContext: Context
) : INotificationClickListener {
    
    companion object {
        private const val TAG = "NotificationAction"
        private const val APPWRITE_FUNCTION_URL = "https://693cd02b0004e2e3a23b.fra.appwrite.run"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient()
    
    override fun onClick(event: INotificationClickEvent) {
        val actionId = event.result.actionId // "accept" or "decline"
        val data = event.notification.additionalData
        
        Log.d(TAG, "=== Notification Click Event ===")
        Log.d(TAG, "ActionId: $actionId")
        Log.d(TAG, "Data: $data")
        Log.d(TAG, "Notification title: ${event.notification.title}")
        
        // If the user tapped the notification body (no action button),
        // open the app (deep link if provided; otherwise open MainActivity).
        if (actionId.isNullOrBlank()) {
            Log.d(TAG, "No action button clicked, opening app")
            try {
                // For friend notifications, route directly to Friend Requests screen.
                val deepLink: Uri? = if (data?.optString("type") == "friend_request") {
                    Uri.parse("arcadia://friends/requests")
                } else {
                    null
                }

                val intent = Intent(appContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    if (deepLink != null) {
                        action = Intent.ACTION_VIEW
                        setData(deepLink)
                    }
                }
                appContext.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open app from notification tap", e)
            }
            return
        }
        
        // Action buttons should NOT open the app; just process in background.
        if (data == null) {
            Log.e(TAG, "No additional data in notification")
            return
        }
        
        val type = data.optString("type")
        val requestId = data.optString("requestId")
        
        Log.d(TAG, "Type: $type, RequestId: $requestId")
        
        if (type == "friend_request" && requestId.isNotBlank()) {
            Log.d(TAG, "Processing friend request action: $actionId for request: $requestId")
            scope.launch {
                callAppwriteFunction(actionId, requestId)
            }
        } else {
            Log.w(TAG, "Invalid data - type: $type, requestId: $requestId")
        }
    }
    
    private suspend fun callAppwriteFunction(action: String, requestId: String) {
        try {
            val jsonBody = JSONObject().apply {
                put("action", action)
                put("requestId", requestId)
            }
            
            val request = Request.Builder()
                .url(APPWRITE_FUNCTION_URL)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.d(TAG, "Friend action '$action' processed successfully for request: $requestId")
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Failed to process friend action. Code: ${response.code}, Error: $errorBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call Appwrite function: ${e.message}", e)
        }
    }
}
