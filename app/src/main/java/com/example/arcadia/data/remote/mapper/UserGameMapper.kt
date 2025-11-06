package com.example.arcadia.data.remote.mapper

import com.example.arcadia.data.remote.dto.UserGameDto
import com.example.arcadia.domain.model.UserGame

fun UserGameDto.toUserGame(documentId: String): UserGame {
    return UserGame(
        id = documentId,
        rawgId = rawgId,
        name = name,
        backgroundImage = backgroundImage,
        addedAt = addedAt,
        genres = genres
    )
}

fun UserGame.toDto(): UserGameDto {
    return UserGameDto(
        rawgId = rawgId,
        name = name,
        backgroundImage = backgroundImage,
        addedAt = addedAt,
        genres = genres
    )
}

