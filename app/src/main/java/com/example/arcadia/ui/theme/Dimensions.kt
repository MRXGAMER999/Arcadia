package com.example.arcadia.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val EXTRA_LARGE_PADDING = 40.dp
val LARGE_PADDING = 20.dp
val MEDIUM_PADDING = 16.dp
val SMALL_PADDING = 10.dp

/**
 * Responsive dimensions that adapt to screen size.
 * Provides consistent scaling across different device sizes.
 */
data class ResponsiveDimens(
    // Screen info
    val screenWidth: Dp,
    val screenHeight: Dp,
    val isCompact: Boolean,
    val isMedium: Boolean,
    val isExpanded: Boolean,
    
    // Padding
    val paddingXSmall: Dp,
    val paddingSmall: Dp,
    val paddingMedium: Dp,
    val paddingLarge: Dp,
    val paddingXLarge: Dp,
    val horizontalPadding: Dp,
    
    // Avatar sizes
    val avatarSmall: Dp,
    val avatarMedium: Dp,
    val avatarLarge: Dp,
    
    // Icon sizes
    val iconSmall: Dp,
    val iconMedium: Dp,
    val iconLarge: Dp,
    
    // Button heights
    val buttonHeightSmall: Dp,
    val buttonHeightMedium: Dp,
    val buttonHeightLarge: Dp,
    
    // Card dimensions
    val cardCornerRadius: Dp,
    val cardElevation: Dp,
    val cardVerticalPadding: Dp,
    val cardHorizontalPadding: Dp,
    
    // Typography
    val fontSizeSmall: TextUnit,
    val fontSizeMedium: TextUnit,
    val fontSizeLarge: TextUnit,
    val fontSizeXLarge: TextUnit,
    val fontSizeTitle: TextUnit,
    
    // Spacing
    val itemSpacing: Dp,
    val sectionSpacing: Dp
)

/**
 * Creates responsive dimensions based on current screen configuration.
 * Adapts UI elements to look great on phones, tablets, and foldables.
 */
@Composable
fun rememberResponsiveDimens(): ResponsiveDimens {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val screenHeightDp = configuration.screenHeightDp.dp
    
    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        val isCompact = screenWidthDp < 600.dp
        val isMedium = screenWidthDp >= 600.dp && screenWidthDp < 840.dp
        val isExpanded = screenWidthDp >= 840.dp
        
        // Scale factor based on screen width (baseline: 360dp)
        val scaleFactor = (screenWidthDp.value / 360f).coerceIn(0.85f, 1.5f)
        
        ResponsiveDimens(
            screenWidth = screenWidthDp,
            screenHeight = screenHeightDp,
            isCompact = isCompact,
            isMedium = isMedium,
            isExpanded = isExpanded,
            
            // Padding - scales with screen size
            paddingXSmall = (4 * scaleFactor).dp,
            paddingSmall = (8 * scaleFactor).dp,
            paddingMedium = (12 * scaleFactor).dp,
            paddingLarge = (16 * scaleFactor).dp,
            paddingXLarge = (24 * scaleFactor).dp,
            horizontalPadding = when {
                isExpanded -> 32.dp
                isMedium -> 24.dp
                else -> (16 * scaleFactor).dp
            },
            
            // Avatar sizes - responsive to screen
            avatarSmall = when {
                isExpanded -> 56.dp
                isMedium -> 52.dp
                else -> (44 * scaleFactor).dp.coerceIn(40.dp, 52.dp)
            },
            avatarMedium = when {
                isExpanded -> 64.dp
                isMedium -> 56.dp
                else -> (48 * scaleFactor).dp.coerceIn(44.dp, 56.dp)
            },
            avatarLarge = when {
                isExpanded -> 80.dp
                isMedium -> 72.dp
                else -> (64 * scaleFactor).dp.coerceIn(56.dp, 72.dp)
            },
            
            // Icon sizes
            iconSmall = when {
                isExpanded -> 20.dp
                isMedium -> 18.dp
                else -> (16 * scaleFactor).dp.coerceIn(14.dp, 20.dp)
            },
            iconMedium = when {
                isExpanded -> 28.dp
                isMedium -> 24.dp
                else -> (20 * scaleFactor).dp.coerceIn(18.dp, 26.dp)
            },
            iconLarge = when {
                isExpanded -> 36.dp
                isMedium -> 32.dp
                else -> (28 * scaleFactor).dp.coerceIn(24.dp, 32.dp)
            },
            
            // Button heights
            buttonHeightSmall = when {
                isExpanded -> 44.dp
                isMedium -> 40.dp
                else -> (36 * scaleFactor).dp.coerceIn(32.dp, 42.dp)
            },
            buttonHeightMedium = when {
                isExpanded -> 52.dp
                isMedium -> 48.dp
                else -> (44 * scaleFactor).dp.coerceIn(40.dp, 50.dp)
            },
            buttonHeightLarge = when {
                isExpanded -> 60.dp
                isMedium -> 56.dp
                else -> (52 * scaleFactor).dp.coerceIn(48.dp, 58.dp)
            },
            
            // Card dimensions
            cardCornerRadius = when {
                isExpanded -> 16.dp
                isMedium -> 14.dp
                else -> (12 * scaleFactor).dp.coerceIn(10.dp, 16.dp)
            },
            cardElevation = 2.dp,
            cardVerticalPadding = when {
                isExpanded -> 8.dp
                isMedium -> 6.dp
                else -> (4 * scaleFactor).dp.coerceIn(4.dp, 8.dp)
            },
            cardHorizontalPadding = when {
                isExpanded -> 20.dp
                isMedium -> 16.dp
                else -> (14 * scaleFactor).dp.coerceIn(12.dp, 18.dp)
            },
            
            // Typography - responsive font sizes
            fontSizeSmall = when {
                isExpanded -> 14.sp
                isMedium -> 13.sp
                else -> (12 * scaleFactor).sp.coerceIn(11.sp, 14.sp)
            },
            fontSizeMedium = when {
                isExpanded -> 18.sp
                isMedium -> 16.sp
                else -> (14 * scaleFactor).sp.coerceIn(13.sp, 17.sp)
            },
            fontSizeLarge = when {
                isExpanded -> 22.sp
                isMedium -> 20.sp
                else -> (18 * scaleFactor).sp.coerceIn(16.sp, 21.sp)
            },
            fontSizeXLarge = when {
                isExpanded -> 28.sp
                isMedium -> 26.sp
                else -> (24 * scaleFactor).sp.coerceIn(22.sp, 28.sp)
            },
            fontSizeTitle = when {
                isExpanded -> 34.sp
                isMedium -> 30.sp
                else -> (26 * scaleFactor).sp.coerceIn(24.sp, 32.sp)
            },
            
            // Spacing
            itemSpacing = when {
                isExpanded -> 12.dp
                isMedium -> 10.dp
                else -> (8 * scaleFactor).dp.coerceIn(6.dp, 12.dp)
            },
            sectionSpacing = when {
                isExpanded -> 24.dp
                isMedium -> 20.dp
                else -> (16 * scaleFactor).dp.coerceIn(12.dp, 20.dp)
            }
        )
    }
}

/**
 * Extension to coerce TextUnit values
 */
private fun TextUnit.coerceIn(min: TextUnit, max: TextUnit): TextUnit {
    return when {
        this.value < min.value -> min
        this.value > max.value -> max
        else -> this
    }
}


