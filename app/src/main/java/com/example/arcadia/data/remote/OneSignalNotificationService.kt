package com.example.arcadia.data.remote

import android.util.Log
import com.example.arcadia.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Service for sending push notifications via OneSignal REST API.
 * Handles friend request and acceptance notifications.
 * 
 * Requirements: 10.6, 10.7, 10.8
 */
class OneSignalNotificationService(
    private val client: OkHttpClient = OkHttpClient()
) {
    companion object {
        private const val TAG = "OneSignalNotification"
        private const val ONESIGNAL_API_URL = "https://onesignal.com/api/v1/notifications"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val appId: String = BuildConfig.ONESIGNAL_APP_ID
    private val restApiKey: String = BuildConfig.ONESIGNAL_REST_API_KEY

    /**
     * Sends a push notification when a friend request is sent.
     * 
     * Requirement 10.6: WHEN a friend request is sent THEN the Arcadia System SHALL send 
     * a notification to the recipient with title "New Friend Request" and body 
     * "@{senderUsername} wants to be your friend!"
     * 
     * @param recipientPlayerId The OneSignal player ID of the recipient
     * @param senderUsername The username of the sender
     * @return True if notification was sent successfully, false otherwise
     */
    suspend fun sendFriendRequestNotification(
        recipientPlayerId: String,
        senderUsername: String
    ): Boolean {
        return sendNotification(
            playerId = recipientPlayerId,
            title = "New Friend Request",
            body = "@$senderUsername wants to be your friend!"
        )
    }

    /**
     * Sends a push notification when a friend request is accepted.
     * 
     * Requirement 10.7: WHEN a friend request is accepted THEN the Arcadia System SHALL send 
     * a notification to the original sender with title "Friend Request Accepted" and body 
     * "@{accepterUsername} accepted your friend request!"
     * 
     * @param recipientPlayerId The OneSignal player ID of the original request sender
     * @param accepterUsername The username of the user who accepted the request
     * @return True if notification was sent successfully, false otherwise
     */
    suspend fun sendFriendAcceptedNotification(
        recipientPlayerId: String,
        accepterUsername: String
    ): Boolean {
        return sendNotification(
            playerId = recipientPlayerId,
            title = "Friend Request Accepted",
            body = "@$accepterUsername accepted your friend request!"
        )
    }

    /**
     * Sends a push notification via OneSignal REST API.
     * 
     * Requirement 10.8: IF notification delivery fails THEN the Arcadia System SHALL log 
     * the error without affecting the friend operation
     * 
     * @param playerId The OneSignal player ID to send the notification to
     * @param title The notification title
     * @param body The notification body
     * @return True if notification was sent successfully, false otherwise
     */
    private suspend fun sendNotification(
        playerId: String,
        title: String,
        body: String
    ): Boolean = withContext(Dispatchers.IO) {
        // Validate configuration
        if (appId.isBlank() || restApiKey.isBlank()) {
            Log.w(TAG, "OneSignal not configured - missing app ID or REST API key")
            return@withContext false
        }

        if (playerId.isBlank()) {
            Log.w(TAG, "Cannot send notification - recipient player ID is blank")
            return@withContext false
        }

        try {
            val jsonBody = JSONObject().apply {
                put("app_id", appId)
                put("include_player_ids", JSONArray().put(playerId))
                put("headings", JSONObject().put("en", title))
                put("contents", JSONObject().put("en", body))
            }

            val request = Request.Builder()
                .url(ONESIGNAL_API_URL)
                .addHeader("Authorization", "Basic $restApiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            
            return@withContext if (response.isSuccessful) {
                Log.d(TAG, "Notification sent successfully to player: $playerId")
                true
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Failed to send notification. Code: ${response.code}, Error: $errorBody")
                false
            }
        } catch (e: Exception) {
            // Requirement 10.8: Log error without affecting friend operation
            Log.e(TAG, "Failed to send notification: ${e.message}", e)
            false
        }
    }
}
