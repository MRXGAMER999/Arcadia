package com.example.arcadia.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MovieResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("results")
    val results: List<MovieDto>
)

data class MovieDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("preview")
    val preview: String?,
    @SerializedName("data")
    val data: MovieDataDto?
)

data class MovieDataDto(
    @SerializedName("480")
    val quality480: String?,
    @SerializedName("max")
    val qualityMax: String?
)

