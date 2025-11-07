package com.example.arcadia.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieResponse(
    @SerialName("count")
    val count: Int,
    @SerialName("results")
    val results: List<MovieDto>
)

@Serializable
data class MovieDto(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("preview")
    val preview: String? = null,
    @SerialName("data")
    val data: MovieDataDto? = null
)

@Serializable
data class MovieDataDto(
    @SerialName("480")
    val quality480: String? = null,
    @SerialName("max")
    val qualityMax: String? = null
)

