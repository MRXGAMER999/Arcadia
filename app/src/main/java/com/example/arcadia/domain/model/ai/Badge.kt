package com.example.arcadia.domain.model.ai

import kotlinx.serialization.Serializable

/**
 * Represents an AI-generated badge based on gaming patterns.
 * Badges are fun achievements that can be featured on user profiles.
 */
@Serializable
data class Badge(
    val title: String = "",
    val emoji: String = "",
    val reason: String = ""
)
