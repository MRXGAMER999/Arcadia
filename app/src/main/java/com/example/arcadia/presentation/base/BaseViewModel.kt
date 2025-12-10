package com.example.arcadia.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException

/**
 * Base ViewModel class with common functionality for all ViewModels.
 * Provides utilities for:
 * - Managing loading states
 * - Job cancellation and replacement
 * - Safe coroutine launching
 * - Common error handling
 * - Notification/snackbar management
 * - Undo operations with timeout
 */
abstract class BaseViewModel : ViewModel() {

    // Job management
    private val jobs = mutableMapOf<String, Job>()

    /**
     * Launches a coroutine that wraps the result in RequestState.
     * Automatically handles loading and error states.
     * 
     * @param stateFlow The MutableStateFlow to update with the result
     * @param block The suspend function to execute
     */
    protected fun <T> launchWithState(
        stateFlow: MutableStateFlow<RequestState<T>>,
        block: suspend () -> T
    ): Job = viewModelScope.launch {
        stateFlow.value = RequestState.Loading
        try {
            val result = block()
            stateFlow.value = RequestState.Success(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            stateFlow.value = RequestState.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Launches a coroutine that collects a Flow and updates state.
     * 
     * @param stateFlow The MutableStateFlow to update
     * @param flow The Flow to collect from
     */
    protected fun <T> launchCollecting(
        stateFlow: MutableStateFlow<RequestState<T>>,
        flow: Flow<RequestState<T>>
    ): Job = viewModelScope.launch {
        flow.collect { state ->
            stateFlow.value = state
        }
    }

    /**
     * Cancels an existing job and launches a new one.
     * Useful for replacing ongoing operations (e.g., search with new query).
     * 
     * @param existingJob The job to cancel (nullable)
     * @param block The suspend function to execute
     * @return The new Job
     */
    protected fun Job?.cancelAndLaunch(
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        this?.cancel()
        return viewModelScope.launch(block = block)
    }

    /**
     * Safely launches a coroutine with error handling.
     * 
     * @param onError Called when an exception occurs
     * @param block The suspend function to execute
     */
    protected fun launchSafe(
        onError: (Exception) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit
    ): Job = viewModelScope.launch {
        try {
            block()
        } catch (e: Exception) {
            onError(e)
        }
    }

    /**
     * Creates a StateFlow from a MutableStateFlow for public exposure.
     * Follows the pattern of exposing immutable state to UI.
     */
    protected fun <T> MutableStateFlow<T>.asImmutable(): StateFlow<T> = asStateFlow()

    /**
     * Launches a job with a unique key. Cancels any previous job with the same key.
     * Useful for managing multiple concurrent operations (e.g., multiple API calls).
     * 
     * @param key Unique identifier for this job
     * @param block The suspend function to execute
     * @return The launched Job
     */
    protected fun launchWithKey(
        key: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        jobs[key]?.cancel()
        val job = viewModelScope.launch(block = block)
        jobs[key] = job
        job.invokeOnCompletion { jobs.remove(key) }
        return job
    }

    /**
     * Launches a coroutine with a debounce delay.
     * Useful for search operations or filtering.
     * 
     * @param key Unique identifier for this job
     * @param delay Delay in milliseconds
     * @param block The suspend function to execute
     */
    protected fun launchWithDebounce(
        key: String,
        delay: Long = 300L,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return launchWithKey(key) {
            kotlinx.coroutines.delay(delay)
            block()
        }
    }

    /**
     * Cancels a job by its key.
     * 
     * @param key The key of the job to cancel
     */
    protected fun cancelJob(key: String) {
        jobs[key]?.cancel()
        jobs.remove(key)
    }

    /**
     * Cancels all jobs managed by this ViewModel.
     */
    protected fun cancelAllJobs() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    /**
     * Shows a notification for a specified duration, then auto-dismisses.
     * 
     * @param setNotification Lambda to show the notification
     * @param clearNotification Lambda to hide the notification
     * @param duration Duration in milliseconds before auto-dismiss
     */
    protected fun showTemporaryNotification(
        setNotification: () -> Unit,
        clearNotification: () -> Unit,
        duration: Long = 2000L
    ): Job = viewModelScope.launch {
        setNotification()
        delay(duration)
        clearNotification()
    }

    /**
     * Executes an undo operation with a countdown timer.
     * If undo is not called within the timeout, executes the actual operation.
     * 
     * @param timeoutMs Timeout in milliseconds before executing the operation
     * @param updateProgress Optional callback for progress updates (0f to 1f)
     * @param onTimeout The operation to execute after timeout
     * @return UndoOperation object for canceling or confirming the operation
     */
    protected fun scheduleUndoOperation(
        timeoutMs: Long = 5000L,
        updateProgress: ((Float) -> Unit)? = null,
        onTimeout: suspend () -> Unit
    ): UndoOperation {
        var cancelledByUser = false

        val job = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val updateInterval = 50L // Update 20 times per second

            // Update progress if callback provided
            if (updateProgress != null) {
                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val progress = 1f - (elapsed.toFloat() / timeoutMs)
                    updateProgress(progress.coerceIn(0f, 1f))
                    delay(updateInterval)
                }
                updateProgress(0f)
            } else {
                delay(timeoutMs)
            }

            // Execute the actual operation
            onTimeout()
        }

        job.invokeOnCompletion { cause ->
            if (cause is CancellationException && !cancelledByUser) {
                // Ensure the timeout operation still executes even if the ViewModel scope is cancelled
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        onTimeout()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // swallow to avoid crashing fallback path
                    }
                }
            }
        }

        return UndoOperation(
            job = job,
            operation = onTimeout,
            scope = viewModelScope,
            onCancel = { cancelledByUser = true }
        )
    }

    override fun onCleared() {
        super.onCleared()
        cancelAllJobs()
    }
}

/**
 * Represents an undo operation that can be cancelled or immediately executed.
 */
class UndoOperation(
    private val job: Job,
    private val operation: suspend () -> Unit,
    private val scope: CoroutineScope,
    private val onCancel: () -> Unit = {}
) {
    /**
     * Cancel the scheduled operation (undo).
     */
    fun cancel() {
        onCancel()
        job.cancel()
    }

    /**
     * Execute the operation immediately, bypassing the countdown.
     */
    fun executeNow() {
        onCancel()
        job.cancel()
        scope.launch { operation() }
    }

    /**
     * Check if the operation is still active (not executed or cancelled).
     */
    fun isActive(): Boolean = job.isActive
}

/**
 * Extension function to cancel a job and return null.
 * Useful for the pattern: job = job.cancelAndNull()
 */
fun Job?.cancelAndNull(): Job? {
    this?.cancel()
    return null
}
