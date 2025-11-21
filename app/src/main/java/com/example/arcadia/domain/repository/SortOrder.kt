package com.example.arcadia.domain.repository

enum class SortOrder {
    NEWEST_FIRST,  // Sort by addedAt descending
    OLDEST_FIRST,  // Sort by addedAt ascending
    TITLE_A_Z,     // Sort by name ascending
    TITLE_Z_A,     // Sort by name descending
    RATING_HIGH,   // Sort by rating descending
    RATING_LOW,    // Sort by rating ascending
    RELEASE_NEW,   // Sort by release date descending
    RELEASE_OLD    // Sort by release date ascending
}

