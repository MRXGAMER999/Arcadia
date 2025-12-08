package com.example.arcadia.presentation.screens.roast.util

import android.content.Context
import android.provider.Settings

/**
 * Helper to check for reduced motion preferences.
 * 
 * Requirements: 12.1, 12.2, 12.3
 */
object MotionPreferences {
    /**
     * Checks if the user has enabled "Remove animations" or reduced motion in system settings.
     */
    fun isReduceMotionEnabled(context: Context): Boolean {
        return try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val transitionScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            
            // If either scale is 0, animations are disabled
            durationScale == 0f || transitionScale == 0f
        } catch (e: Exception) {
            false
        }
    }
}
