package com.example.arcadia.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GamesResponse(
    @SerialName("count")
    val count: Int,
    @SerialName("next")
    val next: String? = null,
    @SerialName("previous")
    val previous: String? = null,
    @SerialName("results")
    val results: List<GameDto>
)

@Serializable
data class GameDto(
    @SerialName("id")
    val id: Int,
    @SerialName("slug")
    val slug: String,
    @SerialName("name")
    val name: String,
    @SerialName("released")
    val released: String? = null,
    @SerialName("tba")
    val tba: Boolean,
    @SerialName("background_image")
    val backgroundImage: String? = null,
    @SerialName("rating")
    val rating: Double,
    @SerialName("rating_top")
    val ratingTop: Int,
    @SerialName("ratings_count")
    val ratingsCount: Int,
    @SerialName("metacritic")
    val metacritic: Int? = null,
    @SerialName("playtime")
    val playtime: Int,
    @SerialName("platforms")
    val platforms: List<PlatformWrapper>? = null,
    @SerialName("genres")
    val genres: List<GenreDto>? = null,
    @SerialName("tags")
    val tags: List<TagDto>? = null,
    @SerialName("short_screenshots")
    val shortScreenshots: List<ScreenshotDto>? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("description_raw")
    val descriptionRaw: String? = null
)

@Serializable
data class PlatformWrapper(
    @SerialName("platform")
    val platform: PlatformDto
)

@Serializable
data class PlatformDto(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("slug")
    val slug: String
)

@Serializable
data class GenreDto(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("slug")
    val slug: String
)

@Serializable
data class TagDto(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("slug")
    val slug: String,
    @SerialName("language")
    val language: String,
    @SerialName("games_count")
    val gamesCount: Int
)

@Serializable
data class ScreenshotDto(
    @SerialName("id")
    val id: Int,
    @SerialName("image")
    val image: String
)


