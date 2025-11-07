package com.example.arcadia.data.remote

import com.example.arcadia.data.remote.dto.GamesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RawgApiService {
    
    @GET("games")
    suspend fun getGames(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("ordering") ordering: String? = null,
        @Query("dates") dates: String? = null,
        @Query("genres") genres: String? = null,
        @Query("tags") tags: String? = null,
        @Query("search") search: String? = null
    ): GamesResponse
    
    companion object {
        const val BASE_URL = "https://api.rawg.io/api/"
    }
}


