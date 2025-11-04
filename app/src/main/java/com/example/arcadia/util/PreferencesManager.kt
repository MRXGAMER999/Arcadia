package com.example.arcadia.util

import android.content.Context
import android.content.SharedPreferences
import com.example.arcadia.util.Constants.PREFERENCES_KEY
import com.example.arcadia.util.Constants.PREFERENCES_NAME
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    private val preferences: SharedPreferences = 
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    
    fun setOnBoardingCompleted(completed: Boolean) {
        preferences.edit { putBoolean(PREFERENCES_KEY, completed) }
    }
    
    fun isOnBoardingCompleted(): Boolean {
        return preferences.getBoolean(PREFERENCES_KEY, false)
    }
}

