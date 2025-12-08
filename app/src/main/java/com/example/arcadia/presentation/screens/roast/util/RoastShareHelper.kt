package com.example.arcadia.presentation.screens.roast.util

import android.content.Context
import android.content.Intent
import com.example.arcadia.domain.model.ai.RoastInsights

/**
 * Helper object for sharing roast content on social media.
 * Formats roast content appropriately for various platforms.
 * 
 * Requirements: 11.1 - Generate shareable content including roast text
 * Requirements: 11.2 - Format content appropriately for social media platforms
 * Requirements: 11.3 - Include roast title and key highlights
 */
object RoastShareHelper {
    
    // App branding
    private const val APP_HASHTAG = "#ArcadiaApp"
    private const val ROAST_HASHTAG = "#GamingRoast"
    
    /**
     * Shares the roast content via Android share sheet.
     * Formats the content to include headline, roast title, and key highlights.
     * 
     * @param context The context to launch the share intent
     * @param roast The RoastInsights to share
     */
    fun shareRoast(context: Context, roast: RoastInsights) {
        val shareText = formatRoastForSharing(roast)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        val chooser = Intent.createChooser(intent, "Share Your Roast")
        context.startActivity(chooser)
    }
    
    /**
     * Formats the roast content for social media sharing.
     * Includes headline, roast title with emoji, and hashtags.
     * Keeps content within reasonable character limits.
     * 
     * @param roast The RoastInsights to format
     * @return Formatted string ready for sharing
     */
    private fun formatRoastForSharing(roast: RoastInsights): String {
        val builder = StringBuilder()
        
        // Add roast title with emoji as the hook
        builder.append("${roast.roastTitleEmoji} ${roast.roastTitle}\n\n")
        
        // Add headline (the main roast line)
        builder.append("\"${roast.headline}\"\n\n")
        
        // Add wholesome closer for positive ending
        builder.append("${roast.wholesomeCloser}\n\n")
        
        // Add hashtags
        builder.append("$ROAST_HASHTAG $APP_HASHTAG 🎮🔥")
        
        return builder.toString()
    }
    
}
