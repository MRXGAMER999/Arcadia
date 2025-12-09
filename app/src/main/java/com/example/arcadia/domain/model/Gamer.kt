package com.example.arcadia.domain.model

import com.example.arcadia.domain.model.ai.Badge
import kotlinx.serialization.Serializable


@Serializable
data class Gamer(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val username: String = "",
    val country: String? = null,
    val city: String? = null,
    val gender: String? = null,
    val description: String? = "",
    val profileImageUrl: String? = null,
    val profileComplete: Boolean = false,
    // Gaming platform IDs
    val steamId: String? = null,
    val xboxGamertag: String? = null,
    val psnId: String? = null,
    // Custom profile sections
    val customSections: List<ProfileSection> = emptyList(),
    // Profile visibility settings
    val isProfilePublic: Boolean = true,
    
    // Friends feature fields
    val friendRequestsSentToday: Int = 0,
    val friendRequestsLastResetDate: String? = null,
    val oneSignalPlayerId: String? = null,
    
    // Other fields
    val featuredBadges: List<Badge> = emptyList(),
    val fcmToken: String? = null
)

@Serializable
data class ProfileSection(
    val id: String = "",
    val title: String = "",  // e.g., "Favorite Game of All Time", "Top 5 FPS Games"
    val type: ProfileSectionType = ProfileSectionType.SINGLE_GAME,
    val gameIds: List<Int> = emptyList(),  // RAWG game IDs
    val order: Int = 0
)

@Serializable
enum class ProfileSectionType {
    SINGLE_GAME,      // For "Favorite Game" type sections
    GAME_LIST,        // For "Top 5 FPS Games" type sections
    CURRENTLY_PLAYING // Shows current playing games
}
