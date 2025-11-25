package com.example.arcadia.util

import android.util.Log
import com.example.arcadia.data.remote.dto.GameDto
import com.example.arcadia.data.remote.mapper.toGame
import com.example.arcadia.domain.model.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Extension functions for safe API calls with consistent error handling.
 * Reduces boilerplate code in repository implementations.
 */

/**
 * Wraps an API call in a Flow with RequestState, handling loading, success, and error states.
 * 
 * @param tag Log tag for error reporting
 * @param errorMessage Custom error message prefix
 * @param apiCall The suspend function that performs the API call
 * @return Flow of RequestState
 */
inline fun <T> safeApiFlow(
    tag: String,
    errorMessage: String,
    crossinline apiCall: suspend () -> T
): Flow<RequestState<T>> = flow {
    try {
        emit(RequestState.Loading)
        val result = apiCall()
        emit(RequestState.Success(result))
    } catch (e: Exception) {
        Log.e(tag, "$errorMessage: ${e.message}", e)
        emit(RequestState.Error("$errorMessage: ${e.message}"))
    }
}.flowOn(Dispatchers.IO)

/**
 * Wraps an API call that returns a list of GameDto, converting to Game objects.
 * 
 * @param tag Log tag for error reporting
 * @param errorMessage Custom error message prefix
 * @param apiCall The suspend function that returns list of GameDto
 * @return Flow of RequestState<List<Game>>
 */
inline fun safeGameListApiFlow(
    tag: String,
    errorMessage: String,
    crossinline apiCall: suspend () -> List<GameDto>
): Flow<RequestState<List<Game>>> = flow {
    try {
        emit(RequestState.Loading)
        val dtos = apiCall()
        val games = dtos.map { it.toGame() }
        emit(RequestState.Success(games))
    } catch (e: Exception) {
        Log.e(tag, "$errorMessage: ${e.message}", e)
        emit(RequestState.Error("$errorMessage: ${e.message}"))
    }
}.flowOn(Dispatchers.IO)

/**
 * Executes a suspend function and returns a Result.
 * Useful for one-shot operations that don't need Flow.
 * 
 * @param tag Log tag for error reporting
 * @param errorMessage Custom error message prefix
 * @param block The suspend function to execute
 * @return Result<T> containing success or failure
 */
suspend inline fun <T> safeCall(
    tag: String,
    errorMessage: String,
    crossinline block: suspend () -> T
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Log.e(tag, "$errorMessage: ${e.message}", e)
        Result.failure(e)
    }
}

/**
 * Executes a suspend function and returns a RequestState.
 * Useful for one-shot operations that return RequestState.
 * 
 * @param tag Log tag for error reporting
 * @param errorMessage Custom error message prefix
 * @param block The suspend function to execute
 * @return RequestState<T>
 */
suspend inline fun <T> safeRequestState(
    tag: String,
    errorMessage: String,
    crossinline block: suspend () -> T
): RequestState<T> {
    return try {
        RequestState.Success(block())
    } catch (e: Exception) {
        Log.e(tag, "$errorMessage: ${e.message}", e)
        RequestState.Error("$errorMessage: ${e.message}")
    }
}
