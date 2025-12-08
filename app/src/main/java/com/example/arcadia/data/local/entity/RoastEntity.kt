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
    val id: Int = 1,
    val headline: String,
    val couldHaveList: String,  // JSON array as string
    val prediction: String,
    val wholesomeCloser: String,
    val roastTitle: String,
    val roastTitleEmoji: String,
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
    
    return RoastInsights(
        headline = headline,
        couldHaveList = parsedCouldHaveList,
        prediction = prediction,
        wholesomeCloser = wholesomeCloser,
        roastTitle = roastTitle,
        roastTitleEmoji = roastTitleEmoji
    )
}

/**
 * Convert domain model to Room entity.
 */
fun RoastInsights.toEntity(generatedAt: Long = System.currentTimeMillis()): RoastEntity =
    RoastEntity(
        headline = headline,
        couldHaveList = Json.encodeToString(couldHaveList),
        prediction = prediction,
        wholesomeCloser = wholesomeCloser,
        roastTitle = roastTitle,
        roastTitleEmoji = roastTitleEmoji,
        generatedAt = generatedAt
    )
