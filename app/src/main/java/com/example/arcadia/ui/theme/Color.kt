package com.example.arcadia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Custom colors for gaming theme
val DarkBlue = Color(0xFF211C84)
val MediumBlue = Color(0xFF4D55CC)
val LightBlue = Color(0xFF7A73D1)
val LightPurple = Color(0xFFB5A8D5)

val DarkNavy = Color(0xFF070F2B)
val DarkIndigo = Color(0xFF1B1A55)
val SlateBlue = Color(0xFF535C91)
val LightSlate = Color(0xFF9290C3)


val GrayDarker = Color(0xFFEBEBEB)
val Surface = Color(0xFF00123B)
val TextPrimary = Surface
val ButtonPrimary = Color(0xFF62B4DA)

val YellowAccent = Color(0xFFFBB02E)

val TextSecondary = Color(0xFFDCDCDC)

val ButtonSecondary = GrayDarker
val ButtonDisabled = GrayDarker

// Rating gradient colors (warm to cool transition with more steps)
object RatingColors {
    // 0.0 - No rating
    val NoRating = TextSecondary.copy(alpha = 0.7f)

    // 0-1 - Extremely bad (Deep red)
    val ExtremelyBadStart = Color(0xFFCC0000)
    val ExtremelyBadEnd = Color(0xFFAA0000)

    // 1-2 - Very bad (Warm red)
    val VeryLowStart = Color(0xFFFF4444)
    val VeryLowEnd = Color(0xFFDD2222)

    // 2-3 - Bad (Red-orange)
    val BadStart = Color(0xFFFF5533)
    val BadEnd = Color(0xFFEE3322)

    // 3-4 - Poor (Orange-red)
    val LowStart = Color(0xFFFF6B35)
    val LowEnd = Color(0xFFFF4444)

    // 4-5 - Below average (Orange)
    val BelowAverageStart = Color(0xFFFF8844)
    val BelowAverageEnd = Color(0xFFFF7733)

    // 5-6 - Average (Yellow-orange)
    val MediumStart = Color(0xFFFBB02E)
    val MediumEnd = Color(0xFFFF8844)

    // 6-7 - Decent (Gold-yellow)
    val DecentStart = Color(0xFFFFCC00)
    val DecentEnd = Color(0xFFFBB02E)

    // 7-7.5 - Good (Gold)
    val AboveAverageStart = Color(0xFFFFD700)
    val AboveAverageEnd = Color(0xFFFFCC00)

    // 7.5-8 - Very good (Gold-lime)
    val VeryGoodStart = Color(0xFFCCFF00)
    val VeryGoodEnd = Color(0xFFFFD700)

    // 8-8.5 - Great (Cool blue-green)
    val GoodStart = Color(0xFF00DDAA)
    val GoodEnd = Color(0xFF88DD55)

    // 8.5-9 - Excellent (Aqua)
    val ExcellentStart = Color(0xFF00DDFF)
    val ExcellentEnd = Color(0xFF00DDAA)

    // 9-9.5 - Outstanding (Cyan)
    val GreatStart = Color(0xFF00D9FF)
    val GreatEnd = Color(0xFF00BBDD)

    // 9.5-10 - Masterpiece (Bright cyan-blue)
    val MasterpieceStart = Color(0xFF00EEFF)
    val MasterpieceEnd = Color(0xFF00CCFF)
}

/**
 * Returns a gradient brush based on rating value (0-10)
 * Lower ratings use warm colors (red, orange), higher ratings use cool colors (blue, cyan)
 * Now with more granular color steps for smoother transitions
 */
fun getRatingGradient(rating: Float): Brush {
    return when {
        rating == 0f -> Brush.linearGradient(
            colors = listOf(RatingColors.NoRating, RatingColors.NoRating)
        )
        rating <= 1f -> Brush.linearGradient(
            colors = listOf(RatingColors.ExtremelyBadStart, RatingColors.ExtremelyBadEnd)
        )
        rating <= 2f -> Brush.linearGradient(
            colors = listOf(RatingColors.VeryLowStart, RatingColors.VeryLowEnd)
        )
        rating <= 3f -> Brush.linearGradient(
            colors = listOf(RatingColors.BadStart, RatingColors.BadEnd)
        )
        rating <= 4f -> Brush.linearGradient(
            colors = listOf(RatingColors.LowStart, RatingColors.LowEnd)
        )
        rating <= 5f -> Brush.linearGradient(
            colors = listOf(RatingColors.BelowAverageStart, RatingColors.BelowAverageEnd)
        )
        rating <= 6f -> Brush.linearGradient(
            colors = listOf(RatingColors.MediumStart, RatingColors.MediumEnd)
        )
        rating <= 7f -> Brush.linearGradient(
            colors = listOf(RatingColors.DecentStart, RatingColors.DecentEnd)
        )
        rating <= 7.5f -> Brush.linearGradient(
            colors = listOf(RatingColors.AboveAverageStart, RatingColors.AboveAverageEnd)
        )
        rating <= 8f -> Brush.linearGradient(
            colors = listOf(RatingColors.VeryGoodStart, RatingColors.VeryGoodEnd)
        )
        rating <= 8.5f -> Brush.linearGradient(
            colors = listOf(RatingColors.GoodStart, RatingColors.GoodEnd)
        )
        rating <= 9f -> Brush.linearGradient(
            colors = listOf(RatingColors.ExcellentStart, RatingColors.ExcellentEnd)
        )
        rating <= 9.5f -> Brush.linearGradient(
            colors = listOf(RatingColors.GreatStart, RatingColors.GreatEnd)
        )
        else -> Brush.linearGradient(
            colors = listOf(RatingColors.MasterpieceStart, RatingColors.MasterpieceEnd)
        )
    }
}

/**
 * Returns the primary color for a rating (for solid color use cases)
 * Now with more granular steps for smoother animated transitions
 */
fun getRatingColor(rating: Float): Color {
    return when {
        rating == 0f -> RatingColors.NoRating
        rating <= 1f -> RatingColors.ExtremelyBadStart
        rating <= 2f -> RatingColors.VeryLowStart
        rating <= 3f -> RatingColors.BadStart
        rating <= 4f -> RatingColors.LowStart
        rating <= 5f -> RatingColors.BelowAverageStart
        rating <= 6f -> RatingColors.MediumStart
        rating <= 7f -> RatingColors.DecentStart
        rating <= 7.5f -> RatingColors.AboveAverageStart
        rating <= 8f -> RatingColors.VeryGoodStart
        rating <= 8.5f -> RatingColors.GoodStart
        rating <= 9f -> RatingColors.ExcellentStart
        rating <= 9.5f -> RatingColors.GreatStart
        else -> RatingColors.MasterpieceStart
    }
}

