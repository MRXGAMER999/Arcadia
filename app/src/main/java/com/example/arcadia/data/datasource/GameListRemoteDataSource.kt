package com.example.arcadia.data.datasource

import io.appwrite.models.Row
import kotlinx.coroutines.flow.Flow

interface GameListRemoteDataSource {
    fun observeGames(userId: String): Flow<List<Row<Map<String, Any>>>>
    suspend fun getGames(userId: String): List<Row<Map<String, Any>>>
    suspend fun addGame(userId: String, data: Map<String, Any?>): Row<Map<String, Any>>
    suspend fun updateGame(gameId: String, data: Map<String, Any?>): Row<Map<String, Any>>
    suspend fun removeGame(gameId: String)
    suspend fun getGame(gameId: String): Row<Map<String, Any>>
    suspend fun getGameByRawgId(userId: String, rawgId: Int): Row<Map<String, Any>>?
    
    // New methods
    fun observeGamesByStatus(userId: String, status: String): Flow<List<Row<Map<String, Any>>>>
    fun observeGamesByGenre(userId: String, genre: String): Flow<List<Row<Map<String, Any>>>>
    fun observeGame(gameId: String): Flow<Row<Map<String, Any>>>
}
