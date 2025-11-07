package com.example.arcadia.data.remote.mapper

import com.example.arcadia.data.remote.dto.GameDto
import com.example.arcadia.domain.model.Game

fun GameDto.toGame(): Game {
    return Game(
        id = id,
        slug = slug,
        name = name,
        released = released,
        backgroundImage = backgroundImage,
        rating = rating,
        ratingTop = ratingTop,
        ratingsCount = ratingsCount,
        metacritic = metacritic,
        playtime = playtime,
        platforms = platforms?.map { it.platform.name } ?: emptyList(),
        genres = genres?.map { it.name } ?: emptyList(),
        tags = tags?.take(5)?.map { it.name } ?: emptyList(), // Limit to 5 tags
        screenshots = shortScreenshots?.map { it.image } ?: emptyList(),
        description = descriptionRaw ?: description
    )
}


