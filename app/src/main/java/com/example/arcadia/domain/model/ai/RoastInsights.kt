package com.example.arcadia.domain.model.ai

/**
 * Represents AI-generated roast insights about a user's gaming habits.
 * Contains the roast content sections that make up a complete roast.
 */
data class RoastInsights(
    val headline: String,
    val couldHaveList: List<String>,
    val prediction: String,
    val wholesomeCloser: String,
    val roastTitle: String,
    val roastTitleEmoji: String
)
