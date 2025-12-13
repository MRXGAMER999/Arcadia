package com.example.arcadia.data.mapper

import com.example.arcadia.domain.model.Gamer
import com.example.arcadia.domain.model.ProfileSection
import com.example.arcadia.domain.model.ai.Badge
import io.appwrite.models.Row
import kotlinx.serialization.json.Json

object GamerMapper {

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    fun toGamer(doc: Row<Map<String, Any>>): Gamer {
        val data = doc.data
        
        // Parse customSections from JSON string
        val customSectionsJson = data["customSections"] as? String
        val customSections = if (!customSectionsJson.isNullOrEmpty()) {
            try {
                json.decodeFromString<List<ProfileSection>>(customSectionsJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        // Parse featuredBadges from JSON string
        val featuredBadgesJson = data["featuredBadges"] as? String
        val featuredBadges = if (!featuredBadgesJson.isNullOrEmpty()) {
            try {
                json.decodeFromString<List<Badge>>(featuredBadgesJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        return Gamer(
            id = doc.id,
            name = data["name"] as? String ?: "",
            email = data["email"] as? String ?: "",
            username = data["username"] as? String ?: "",
            country = data["country"] as? String,
            city = data["city"] as? String,
            gender = data["gender"] as? String,
            description = data["description"] as? String ?: "",
            profileImageUrl = data["profileImageUrl"] as? String,
            profileComplete = data["profileComplete"] as? Boolean ?: false,
            steamId = data["steamId"] as? String,
            xboxGamertag = data["xboxGamertag"] as? String,
            psnId = data["psnId"] as? String,
            discordUsername = data["discordUsername"] as? String,
            customSections = customSections,
            isProfilePublic = data["isProfilePublic"] as? Boolean ?: true,
            featuredBadges = featuredBadges,
            friendRequestsSentToday = (data["friendRequestsSentToday"] as? Number)?.toInt() ?: 0,
            friendRequestsLastResetDate = data["friendRequestsLastResetDate"] as? String,
            oneSignalPlayerId = data["oneSignalPlayerId"] as? String
        )
    }
}
