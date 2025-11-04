package com.example.arcadia.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Custom Arcadia color scheme with dark blue background
private val ArcadiaColorScheme = darkColorScheme(
    primary = ButtonPrimary,
    secondary = LightBlue,
    tertiary = YellowAccent,
    background = Surface, // Dark blue background
    surface = Surface, // Dark blue surface
    onPrimary = Surface,
    onSecondary = TextSecondary,
    onTertiary = Surface,
    onBackground = TextSecondary,
    onSurface = TextSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Surface, // Dark blue background
    surface = Surface // Dark blue surface
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Surface, // Use dark theme for consistency
    surface = Surface
)

@Composable
fun ArcadiaTheme(
    darkTheme: Boolean = true, // Always use dark theme for consistency
    // Dynamic color is disabled for consistent branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always use Arcadia color scheme for consistent dark blue theme
    val colorScheme = ArcadiaColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar and navigation bar to dark blue background
            window.statusBarColor = Surface.toArgb()
            window.navigationBarColor = Surface.toArgb()
            // Make status bar icons light (visible on dark background)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}