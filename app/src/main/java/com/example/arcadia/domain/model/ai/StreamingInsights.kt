package com.example.arcadia.domain.model.ai

/**
 * Represents partial/streaming insights during AI generation.
 * Used for progressive UI updates while AI is generating content.
 */
data class StreamingInsights(
    val partialText: String,
    val isComplete: Boolean,
    val parsedInsights: GameInsights? = null
)
