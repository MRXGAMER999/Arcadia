package com.example.arcadia.util

import android.content.Context
import android.content.SharedPreferences
import com.example.arcadia.domain.model.DiscoverySortOrder
import com.example.arcadia.domain.model.DiscoverySortType
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.domain.model.ReleaseTimeframe
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
        
        // Discovery filter keys
        private const val KEY_DISCOVERY_SORT_TYPE = "discovery_sort_type"
        private const val KEY_DISCOVERY_SORT_ORDER = "discovery_sort_order"
        private const val KEY_DISCOVERY_GENRES = "discovery_genres"
        private const val KEY_DISCOVERY_TIMEFRAME = "discovery_timeframe"
        private const val KEY_DISCOVERY_DEVELOPERS = "discovery_developers"
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
    
    // ==================== Discovery Filter Preferences ====================
    
    fun saveDiscoverySortType(sortType: DiscoverySortType) {
        preferences.edit { putString(KEY_DISCOVERY_SORT_TYPE, sortType.name) }
    }
    
    fun getDiscoverySortType(): DiscoverySortType {
        val name = preferences.getString(KEY_DISCOVERY_SORT_TYPE, null)
        return try {
            name?.let { DiscoverySortType.valueOf(it) } ?: DiscoverySortType.POPULARITY
        } catch (e: IllegalArgumentException) {
            DiscoverySortType.POPULARITY
        }
    }
    
    fun saveDiscoverySortOrder(sortOrder: DiscoverySortOrder) {
        preferences.edit { putString(KEY_DISCOVERY_SORT_ORDER, sortOrder.name) }
    }
    
    fun getDiscoverySortOrder(): DiscoverySortOrder {
        val name = preferences.getString(KEY_DISCOVERY_SORT_ORDER, null)
        return try {
            name?.let { DiscoverySortOrder.valueOf(it) } ?: DiscoverySortOrder.DESCENDING
        } catch (e: IllegalArgumentException) {
            DiscoverySortOrder.DESCENDING
        }
    }
    
    fun saveDiscoveryGenres(genres: Set<String>) {
        preferences.edit { putStringSet(KEY_DISCOVERY_GENRES, genres) }
    }
    
    fun getDiscoveryGenres(): Set<String> {
        return preferences.getStringSet(KEY_DISCOVERY_GENRES, emptySet()) ?: emptySet()
    }
    
    fun saveDiscoveryTimeframe(timeframe: ReleaseTimeframe) {
        preferences.edit { putString(KEY_DISCOVERY_TIMEFRAME, timeframe.name) }
    }
    
    fun getDiscoveryTimeframe(): ReleaseTimeframe {
        val name = preferences.getString(KEY_DISCOVERY_TIMEFRAME, null)
        return try {
            name?.let { ReleaseTimeframe.valueOf(it) } ?: ReleaseTimeframe.ALL
        } catch (e: IllegalArgumentException) {
            ReleaseTimeframe.ALL
        }
    }
    
    fun saveDiscoveryDevelopers(developers: Map<String, Set<String>>) {
        val jsonObject = JSONObject()
        developers.forEach { (parent, subStudios) ->
            jsonObject.put(parent, JSONArray(subStudios.toList()))
        }
        preferences.edit { putString(KEY_DISCOVERY_DEVELOPERS, jsonObject.toString()) }
    }
    
    fun getDiscoveryDevelopers(): Map<String, Set<String>> {
        val json = preferences.getString(KEY_DISCOVERY_DEVELOPERS, null) ?: return emptyMap()
        return try {
            val jsonObject = JSONObject(json)
            val result = mutableMapOf<String, Set<String>>()
            jsonObject.keys().forEach { key ->
                val subStudiosArray = jsonObject.getJSONArray(key)
                val subStudios = mutableSetOf<String>()
                for (i in 0 until subStudiosArray.length()) {
                    subStudios.add(subStudiosArray.getString(i))
                }
                result[key] = subStudios
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    fun clearDiscoveryFilters() {
        preferences.edit {
            remove(KEY_DISCOVERY_SORT_TYPE)
            remove(KEY_DISCOVERY_SORT_ORDER)
            remove(KEY_DISCOVERY_GENRES)
            remove(KEY_DISCOVERY_TIMEFRAME)
            remove(KEY_DISCOVERY_DEVELOPERS)
        }
    }
}

