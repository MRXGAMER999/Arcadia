package com.example.arcadia.presentation.screens.roast.util

import kotlin.random.Random

/**
 * Utility for converting text into mystical rune-like characters.
 * 
 * Requirements: 3.2, 3.3
 */
object RuneTextGenerator {
    private val runeChars = listOf(
        'ᚠ', 'ᚢ', 'ᚦ', 'ᚨ', 'ᚱ', 'ᚲ', 'ᚷ', 'ᚹ', 'ᚺ', 'ᚾ',
        'ᛁ', 'ᛃ', 'ᛇ', 'ᛈ', 'ᛉ', 'ᛊ', 'ᛏ', 'ᛒ', 'ᛖ', 'ᛗ',
        'ᛚ', 'ᛜ', 'ᛞ', 'ᛟ', '⚡', "🔥", '☠', '⚔', "🛡", "🔮",
        '◈', '◇', '◆', '○', '●', '◐', '◑', '◒', '◓', '★'
    )
    
    /**
     * Converts actual text to mystical rune-like characters.
     * Preserves length and whitespace structure but scrambles content.
     */
    fun scrambleToRunes(text: String): String {
        return text.map { char ->
            if (char.isWhitespace()) {
                char
            } else {
                runeChars.random()
            }
        }.joinToString("")
    }
}
