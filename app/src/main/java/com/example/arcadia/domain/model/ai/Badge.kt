package com.example.arcadia.domain.model.ai

/**
 * Represents an AI-generated badge based on gaming patterns.
 * Badges are fun achievements that can be featured on user profiles.
 */
data class Badge(
    val title: String,
    val emoji: String,
    val reason: String
)
