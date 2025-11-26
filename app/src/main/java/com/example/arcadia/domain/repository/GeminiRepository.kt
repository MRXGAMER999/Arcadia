package com.example.arcadia.domain.repository

import com.example.arcadia.domain.model.GameListEntry
import kotlinx.coroutines.flow.Flow

/**
 * @deprecated Use [AIRepository] instead. This interface is kept for backward compatibility.
 * Repository interface for interacting with AI services for game analysis and suggestions.
 */
@Deprecated(
    message = "Use AIRepository instead",
    replaceWith = ReplaceWith("AIRepository", "com.example.arcadia.domain.repository.AIRepository")
)
typealias GeminiRepository = AIRepository
