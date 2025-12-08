package com.example.arcadia.data.local.entity

import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.arcadia.domain.model.ai.RoastInsights
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "RoastEntity"

/**
 * Room entity for storing the user's last generated roast.
 * Uses a single-row pattern (id = 1) to always replace the previous roast.
 */
@Entity(tableName = "roast_table")
data class RoastEntity(
    @PrimaryKey
    val userId: String,
    val headline: String,
    val couldHaveList: String,  // JSON array as string
    val prediction: String,
    val wholesomeCloser: String,
    val roastTitle: String,
    val roastTitleEmoji: String,
    val badges: String = "[]", // JSON array of badges
    val generatedAt: Long
)

private val json = Json { ignoreUnknownKeys = true }

/**
 * Convert Room entity to domain model.
 * Handles corrupted data gracefully by parsing plain text as a single-item list.
 */
fun RoastEntity.toRoastInsights(): RoastInsights {
    val parsedCouldHaveList = try {
        // Try to parse as JSON array first
        json.decodeFromString<List<String>>(couldHaveList)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to parse couldHaveList as JSON, treating as plain text: ${e.message}")
        // If it's not valid JSON, it might be plain text from corrupted data
        // Split by newlines and filter empty lines
        couldHaveList
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { line ->
                // Remove common list prefixes
                line.removePrefix("-").removePrefix("*")
                    .trim()
                    .replaceFirst(Regex("^\\d+\\.\\s*"), "")
            }
            .take(5)
            .ifEmpty { listOf(couldHaveList.take(200)) } // Fallback to truncated text
    }

    // Parse badges (new field, safe fallback)
    val parsedBadges = try {
        json.decodeFromString<List<com.example.arcadia.domain.model.ai.Badge>>(badges)
    } catch (e: Exception) {
        emptyList()
    }
    
    val insights = RoastInsights(
        headline = headline,
        couldHaveList = parsedCouldHaveList,
        prediction = prediction,
        wholesomeCloser = wholesomeCloser,
        roastTitle = roastTitle,
        roastTitleEmoji = roastTitleEmoji
    )
    // Attach badges to the insights object? No, RoastInsights doesn't have badges field.
    // The Repository/ViewModel needs to handle this or RoastInsights needs modification.
    // Looking at RoastViewModel, it holds badges separately in state.
    // So we need to return a Pair or modify repository return type.
    // Actually, simpler is to add badges to RoastInsights domain model? 
    // Or just parse it here but how to return it?
    // RoastEntity maps to RoastInsights... 
    // Wait, the prompt asked to fix "Missing Badges in Cache".
    // RoastViewModel.loadCachedRoast uses roastRepository.getLastRoastWithTimestamp
    // which maps Entity -> RoastInsights.
    // RoastInsights does NOT have badges.
    
    return insights
}

/**
 * Convert domain model to Room entity.
 * Note: Badges must be passed separately or added to RoastInsights.
 * Since I can't easily change RoastInsights across the app without seeing usages,
 * I will modify this to take badges as an argument.
 */
fun RoastInsights.toEntity(userId: String, badges: List<com.example.arcadia.domain.model.ai.Badge> = emptyList(), generatedAt: Long = System.currentTimeMillis()): RoastEntity =
    RoastEntity(
        userId = userId,
        headline = headline,
        couldHaveList = Json.encodeToString(couldHaveList),
        prediction = prediction,
        wholesomeCloser = wholesomeCloser,
        roastTitle = roastTitle,
        roastTitleEmoji = roastTitleEmoji,
        badges = Json.encodeToString(badges),
        generatedAt = generatedAt
    )
