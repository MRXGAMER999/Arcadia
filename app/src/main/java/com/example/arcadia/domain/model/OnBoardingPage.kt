package com.example.arcadia.domain.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.arcadia.R

@Immutable
sealed class OnBoardingPage(
    @DrawableRes
    val image: Int,
    val description: String
) {
    object First : OnBoardingPage(
        image = R.drawable.controller,
        description = "Easily manage your entire game library and track what you're playing, what you've beaten, and what's on your wishlist."
    )
    object Second : OnBoardingPage(
        image = R.drawable.gamer1,
        description = "Get personalized game recommendations and use powerful search filters to discover new titles and find deals."
    )
    object Third : OnBoardingPage(
        image = R.drawable.gamer2,
        description = "Join community hubs to chat with other players, find teammates, and compete in amateur leagues."
    )
}