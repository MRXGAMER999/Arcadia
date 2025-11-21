package com.example.arcadia.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.arcadia.R

data class PlatformInfo(
    val name: String,
    val iconRes: Int,
    val color: Color
)

val platformMap = mapOf(
    // PlayStation variants
    "PlayStation" to PlatformInfo("PlayStation", R.drawable.playstation_ic, Color(0xFF003791)),
    "PlayStation 5" to PlatformInfo("PlayStation", R.drawable.playstation_ic, Color(0xFF003791)),
    "PlayStation 4" to PlatformInfo("PlayStation", R.drawable.playstation_ic, Color(0xFF003791)),
    "PlayStation 3" to PlatformInfo("PlayStation", R.drawable.playstation_ic, Color(0xFF003791)),
    "PS Vita" to PlatformInfo("PlayStation", R.drawable.playstation_ic, Color(0xFF003791)),
    "PSP" to PlatformInfo("PlayStation", R.drawable.playstation_ic, Color(0xFF003791)),
    
    // Xbox variants
    "Xbox" to PlatformInfo("Xbox", R.drawable.xbox_ic, Color(0xFF107C10)),
    "Xbox Series S/X" to PlatformInfo("Xbox", R.drawable.xbox_ic, Color(0xFF107C10)),
    "Xbox One" to PlatformInfo("Xbox", R.drawable.xbox_ic, Color(0xFF107C10)),
    "Xbox 360" to PlatformInfo("Xbox", R.drawable.xbox_ic, Color(0xFF107C10)),
    
    // Nintendo variants
    "Nintendo Switch" to PlatformInfo("Nintendo", R.drawable.nintendo_switch_ic, Color(0xFFE60012)),
    "Nintendo 3DS" to PlatformInfo("Nintendo", R.drawable.nintendo_switch_ic, Color(0xFFE60012)),
    "Nintendo DS" to PlatformInfo("Nintendo", R.drawable.nintendo_switch_ic, Color(0xFFE60012)),
    "Wii U" to PlatformInfo("Nintendo", R.drawable.nintendo_switch_ic, Color(0xFFE60012)),
    "Wii" to PlatformInfo("Nintendo", R.drawable.nintendo_switch_ic, Color(0xFFE60012)),
    
    // PC variants
    "PC" to PlatformInfo("PC", R.drawable.pc_ic, Color(0xFF00A4EF)),
    "macOS" to PlatformInfo("PC", R.drawable.pc_ic, Color(0xFF00A4EF)),
    "Linux" to PlatformInfo("PC", R.drawable.pc_ic, Color(0xFF00A4EF))
)

fun getPlatformInfo(platformName: String): PlatformInfo? {
    return platformMap[platformName]
}

fun getUniquePlatforms(platforms: List<String>): List<PlatformInfo> {
    val uniquePlatforms = mutableMapOf<String, PlatformInfo>()
    
    platforms.forEach { platformName ->
        val info = getPlatformInfo(platformName)
        if (info != null && !uniquePlatforms.containsKey(info.name)) {
            uniquePlatforms[info.name] = info
        }
    }
    
    return uniquePlatforms.values.toList()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlatformIcons(
    platforms: List<String>,
    modifier: Modifier = Modifier,
    iconSize: Dp = 16.dp,
    showBackground: Boolean = true
) {
    val uniquePlatforms = getUniquePlatforms(platforms)
    
    if (uniquePlatforms.isNotEmpty()) {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            uniquePlatforms.forEach { platform ->
                if (showBackground) {
                    Icon(
                        painter = painterResource(id = platform.iconRes),
                        contentDescription = platform.name,
                        tint = platform.color,
                        modifier = Modifier
                            .size(iconSize + 8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.9f))
                            .padding(3.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = platform.iconRes),
                        contentDescription = platform.name,
                        tint = platform.color,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    }
}

