package com.example.arcadia.util

import android.content.Context
import android.content.SharedPreferences
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.util.Constants.PREFERENCES_KEY
import com.example.arcadia.util.Constants.PREFERENCES_NAME
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class FilterPreset(
    val name: String,
    val genres: Set<String>,
    val statuses: Set<GameStatus>
)

class PreferencesManager(context: Context) {
    private val preferences: SharedPreferences = 
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_SELECTED_GENRES = "selected_genres"
        private const val KEY_SELECTED_STATUSES = "selected_statuses"
        private const val KEY_FILTER_PRESETS = "filter_presets"
    }
    
    fun setOnBoardingCompleted(completed: Boolean) {
        preferences.edit { putBoolean(PREFERENCES_KEY, completed) }
    }
    
    fun isOnBoardingCompleted(): Boolean {
        return preferences.getBoolean(PREFERENCES_KEY, false)
    }
    
    fun saveSelectedGenres(genres: Set<String>) {
        preferences.edit { 
            putStringSet(KEY_SELECTED_GENRES, genres)
        }
    }
    
    fun getSelectedGenres(): Set<String> {
        return preferences.getStringSet(KEY_SELECTED_GENRES, emptySet()) ?: emptySet()
    }
    
    fun saveSelectedStatuses(statuses: Set<GameStatus>) {
        val statusNames = statuses.map { it.name }.toSet()
        preferences.edit { 
            putStringSet(KEY_SELECTED_STATUSES, statusNames)
        }
    }
    
    fun getSelectedStatuses(): Set<GameStatus> {
        val statusNames = preferences.getStringSet(KEY_SELECTED_STATUSES, emptySet()) ?: emptySet()
        return statusNames.mapNotNull { name ->
            try {
                GameStatus.valueOf(name)
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toSet()
    }
    
    fun saveFilterPreset(preset: FilterPreset) {
        val presets = getFilterPresets().toMutableList()
        // Remove existing preset with same name
        presets.removeAll { it.name == preset.name }
        presets.add(preset)
        
        val jsonArray = JSONArray()
        presets.forEach { p ->
            val jsonObject = JSONObject().apply {
                put("name", p.name)
                put("genres", JSONArray(p.genres.toList()))
                put("statuses", JSONArray(p.statuses.map { it.name }))
            }
            jsonArray.put(jsonObject)
        }
        
        preferences.edit { 
            putString(KEY_FILTER_PRESETS, jsonArray.toString())
        }
    }
    
    fun getFilterPresets(): List<FilterPreset> {
        val json = preferences.getString(KEY_FILTER_PRESETS, null) ?: return emptyList()
        
        return try {
            val jsonArray = JSONArray(json)
            val presets = mutableListOf<FilterPreset>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val name = jsonObject.getString("name")
                
                val genresArray = jsonObject.getJSONArray("genres")
                val genres = mutableSetOf<String>()
                for (j in 0 until genresArray.length()) {
                    genres.add(genresArray.getString(j))
                }
                
                val statusesArray = jsonObject.getJSONArray("statuses")
                val statuses = mutableSetOf<GameStatus>()
                for (j in 0 until statusesArray.length()) {
                    try {
                        statuses.add(GameStatus.valueOf(statusesArray.getString(j)))
                    } catch (e: IllegalArgumentException) {
                        // Skip invalid status
                    }
                }
                
                presets.add(FilterPreset(name, genres, statuses))
            }
            
            presets
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun deleteFilterPreset(presetName: String) {
        val presets = getFilterPresets().toMutableList()
        presets.removeAll { it.name == presetName }
        
        val jsonArray = JSONArray()
        presets.forEach { p ->
            val jsonObject = JSONObject().apply {
                put("name", p.name)
                put("genres", JSONArray(p.genres.toList()))
                put("statuses", JSONArray(p.statuses.map { it.name }))
            }
            jsonArray.put(jsonObject)
        }
        
        preferences.edit { 
            putString(KEY_FILTER_PRESETS, jsonArray.toString())
        }
    }
}

