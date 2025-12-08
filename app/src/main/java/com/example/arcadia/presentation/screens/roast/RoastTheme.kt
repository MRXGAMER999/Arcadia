package com.example.arcadia.presentation.screens.roast

import androidx.compose.ui.graphics.Color

/**
 * Color constants for the Magical Roast Experience.
 * Enhanced for better readability and visual appeal.
 * 
 * Requirements: 6.1, 6.2, 6.4
 */
object RoastTheme {
    // Background gradient - deeper, richer tones
    val backgroundStart = Color(0xFF0A0A0A)  // Rich black
    val backgroundEnd = Color(0xFF1A0808)    // Deep burgundy
    
    // Accent colors - more vibrant and eye-catching
    val fireOrange = Color(0xFFFF7043)      // Brighter orange
    val emberRed = Color(0xFFE53935)        // Vivid red
    
    // Glow colors - more visible
    val glowOrange = Color(0xFFFF7043).copy(alpha = 0.4f)
    val glowRed = Color(0xFFE53935).copy(alpha = 0.4f)
    
    // Wholesome section contrast - warmer, more inviting
    val wholesomeTeal = Color(0xFF4DB6AC)   // Soft teal
    val wholesomeBlue = Color(0xFF26C6DA)   // Bright cyan
    
    // Text colors - improved contrast
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.85f)  // Slightly more visible
    
    // Additional accent colors for sections
    val purpleAccent = Color(0xFF9C27B0)    // For predictions
    val tealAccent = Color(0xFF00897B)      // For wholesome sections
}
