package com.example.arcadia.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScreenshotResponse(
    @SerialName("count")
    val count: Int,
    @SerialName("results")
    val results: List<ScreenshotDetailDto>
)

@Serializable
data class ScreenshotDetailDto(
    @SerialName("id")
    val id: Int,
    @SerialName("image")
    val image: String,
    @SerialName("width")
    val width: Int? = null,
    @SerialName("height")
    val height: Int? = null
)

