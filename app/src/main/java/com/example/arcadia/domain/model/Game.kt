package com.example.arcadia.domain.model

data class Game(
    val id: Int,
    val slug: String,
    val name: String,
    val released: String?,
    val backgroundImage: String?,
    val rating: Double,
    val ratingTop: Int = 5,
    val ratingsCount: Int = 0,
    val metacritic: Int?,
    val playtime: Int,
    val platforms: List<String>,
    val genres: List<String>,
    val tags: List<String>,
    val screenshots: List<String> = emptyList(),
    val trailerUrl: String? = null,
    val description: String? = null
)


