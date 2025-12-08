package com.example.arcadia.presentation.screens.roast

/**
 * Defines the phases of the roast reveal animation sequence.
 * 
 * Requirements: 10.1
 */
enum class RevealPhase {
    HIDDEN,              // Nothing shown yet
    STREAMING,           // Mystical console showing "decoding" runes
    REVEALING_TITLE,     // Title badge dropping in (0ms)
    REVEALING_HEADLINE,  // Headline typing out (300ms)
    REVEALING_COULD_HAVE,// Items sliding in (600ms)
    REVEALING_PREDICTION,// Prediction fading in (1200ms)
    REVEALING_WHOLESOME, // Wholesome fading in (1800ms)
    COMPLETE             // All revealed, buttons visible
}
