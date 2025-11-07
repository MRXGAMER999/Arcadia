package com.example.arcadia.data.remote

import com.example.arcadia.data.remote.dto.GamesResponse
import com.example.arcadia.data.remote.dto.GameDto
import com.example.arcadia.data.remote.dto.MovieResponse
import com.example.arcadia.data.remote.dto.ScreenshotResponse
import retrofit2.http.GET
import retrofit2.http.Path
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
    
    @GET("games/{id}")
    suspend fun getGameDetails(
        @Path("id") gameId: Int
    ): GameDto

    @GET("games/{id}/movies")
    suspend fun getGameVideos(
        @Path("id") gameId: Int
    ): MovieResponse

    @GET("games/{id}/screenshots")
    suspend fun getGameScreenshots(
        @Path("id") gameId: Int
    ): ScreenshotResponse

    companion object {
        const val BASE_URL = "https://api.rawg.io/api/"
    }
}

