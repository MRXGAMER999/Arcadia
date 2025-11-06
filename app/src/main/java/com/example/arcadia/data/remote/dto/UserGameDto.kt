package com.example.arcadia.data.remote.dto

data class UserGameDto(
    val rawgId: Int = 0,
    val name: String = "",
    val backgroundImage: String? = null,
    val addedAt: Long = 0L,
    val genres: List<String> = emptyList()
)

