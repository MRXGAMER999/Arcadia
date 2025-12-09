package com.example.arcadia.util

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Utility object for QR code generation and profile URL parsing.
 * 
 * Profile URL Format: https://mrxgamer999.github.io/Arcadia/profile?id={userId}
 * 
 * Requirements: 4.2, 4.9, 12.1-12.4
 */
object QRCodeUtils {
    
    private const val PROFILE_BASE_URL = "https://mrxgamer999.github.io/Arcadia/profile"
    private const val USER_ID_PARAM = "id"
    
    /**
     * Generates a QR code bitmap encoding the user's profile URL.
     * 
     * @param userId The user's unique identifier
     * @param size The width and height of the QR code in pixels (default: 512)
     * @return Bitmap containing the QR code, or null if generation fails
     */
    fun generateProfileQRCode(userId: String, size: Int = 512): Bitmap? {
        return try {
            val profileUrl = buildProfileUrl(userId)
            generateQRCode(profileUrl, size)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generates a QR code bitmap from any content string.
     * 
     * @param content The content to encode in the QR code
     * @param size The width and height of the QR code in pixels
     * @return Bitmap containing the QR code
     * @throws Exception if QR code generation fails
     */
    private fun generateQRCode(content: String, size: Int): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }
    
    /**
     * Builds a profile URL for the given user ID.
     * 
     * @param userId The user's unique identifier
     * @return The complete profile URL
     */
    fun buildProfileUrl(userId: String): String {
        return "$PROFILE_BASE_URL?$USER_ID_PARAM=$userId"
    }
    
    /**
     * Parses a profile URL and extracts the user ID.
     * 
     * @param url The URL to parse (can be a full URL or just the path with query)
     * @return The extracted user ID, or null if the URL is invalid or not an Arcadia profile URL
     */
    fun parseProfileUrl(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            
            // Validate it's an Arcadia profile URL
            if (!isValidArcadiaProfileUrl(uri)) {
                return null
            }
            
            // Extract the user ID from query parameter
            uri.getQueryParameter(USER_ID_PARAM)?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Checks if a URL is a valid Arcadia profile URL.
     * 
     * @param url The URL string to validate
     * @return true if the URL is a valid Arcadia profile URL
     */
    fun isValidArcadiaProfileUrl(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            isValidArcadiaProfileUrl(uri)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Checks if a URI is a valid Arcadia profile URL.
     * 
     * Valid URLs must:
     * - Have host "mrxgamer999.github.io"
     * - Have path starting with "/Arcadia/profile"
     * - Have a non-empty "id" query parameter
     */
    private fun isValidArcadiaProfileUrl(uri: Uri): Boolean {
        val host = uri.host ?: return false
        val path = uri.path ?: return false
        val userId = uri.getQueryParameter(USER_ID_PARAM)
        
        return host == "mrxgamer999.github.io" &&
               path.startsWith("/Arcadia/profile") &&
               !userId.isNullOrBlank()
    }
    
    /**
     * Creates a shareable invite message with the user's profile URL.
     * 
     * @param userId The user's unique identifier
     * @return The formatted invite message
     */
    fun createInviteMessage(userId: String): String {
        return "Add me on Arcadia!\n${buildProfileUrl(userId)}"
    }
}
