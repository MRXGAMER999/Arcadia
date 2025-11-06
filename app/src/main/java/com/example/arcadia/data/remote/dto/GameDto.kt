package com.example.arcadia.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GamesResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("next")
    val next: String?,
    @SerializedName("previous")
    val previous: String?,
    @SerializedName("results")
    val results: List<GameDto>
)

data class GameDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("released")
    val released: String?,
    @SerializedName("tba")
    val tba: Boolean,
    @SerializedName("background_image")
    val backgroundImage: String?,
    @SerializedName("rating")
    val rating: Double,
    @SerializedName("rating_top")
    val ratingTop: Int,
    @SerializedName("ratings_count")
    val ratingsCount: Int,
    @SerializedName("metacritic")
    val metacritic: Int?,
    @SerializedName("playtime")
    val playtime: Int,
    @SerializedName("platforms")
    val platforms: List<PlatformWrapper>?,
    @SerializedName("genres")
    val genres: List<GenreDto>?,
    @SerializedName("tags")
    val tags: List<TagDto>?,
    @SerializedName("short_screenshots")
    val shortScreenshots: List<ScreenshotDto>?
)

data class PlatformWrapper(
    @SerializedName("platform")
    val platform: PlatformDto
)

data class PlatformDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("slug")
    val slug: String
)

data class GenreDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("slug")
    val slug: String
)

data class TagDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("slug")
    val slug: String,
    @SerializedName("language")
    val language: String,
    @SerializedName("games_count")
    val gamesCount: Int
)

data class ScreenshotDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("image")
    val image: String
)


