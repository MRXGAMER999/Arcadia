package com.example.arcadia.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ScreenshotResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("results")
    val results: List<ScreenshotDetailDto>
)

data class ScreenshotDetailDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("image")
    val image: String,
    @SerializedName("width")
    val width: Int?,
    @SerializedName("height")
    val height: Int?
)

