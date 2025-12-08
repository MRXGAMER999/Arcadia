package com.example.arcadia.presentation.screens.roast

/**
 * Mystical loading messages for the roast generation process.
 * 
 * Requirements: 9.1, 9.2
 */
object MysticalLoadingMessages {
    val messages = listOf(
        "🔮 Summoning the roast oracle...",
        "📜 Unrolling the scroll of your gaming sins...",
        "🔥 Heating up the cauldron of truth...",
        "⚡ Consulting the elder gaming gods...",
        "🧪 Mixing a potion of harsh reality...",
        "🧙‍♂️ The oracle is judging your backlog...",
        "🎮 Analyzing your questionable life choices...",
        "💀 Measuring the weight of your dropped games...",
        "🌌 Gazing into the abyss of your playtime...",
        "⏳ Your gaming sins are being calculated..."
    )
    
    fun random(): String = messages.random()
}
