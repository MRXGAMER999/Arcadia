package com.example.arcadia.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

import okhttp3.ResponseBody
import retrofit2.http.Streaming

/**
 * Retrofit service for Groq API (OpenAI-compatible)
 */
interface GroqApiService {

    @POST("openai/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GroqChatRequest
    ): GroqChatResponse

    @Streaming
    @POST("openai/v1/chat/completions")
    suspend fun streamChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GroqChatRequest
    ): ResponseBody
}

@Serializable
data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Float = 0.3f,
    @SerialName("max_tokens")
    val maxTokens: Int = 16384,
    @SerialName("response_format")
    val responseFormat: GroqResponseFormat? = null,
    val stream: Boolean = false
)

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqResponseFormat(
    val type: String = "json_object"
)

@Serializable
data class GroqChatResponse(
    val id: String,
    val choices: List<GroqChoice>,
    val usage: GroqUsage? = null
)

@Serializable
data class GroqChoice(
    val index: Int,
    val message: GroqMessage,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class GroqUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)
