package com.example.arcadia.domain.repository

import com.example.arcadia.domain.model.Gamer
import com.example.arcadia.util.RequestState
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

interface GamerRepository {
    fun getCurrentUserId(): String?
    suspend fun createUser(
        user: FirebaseUser?,
        onSuccess: (profileComplete: Boolean) -> Unit,
        onError: (String) -> Unit
    )

    fun readCustomerFlow(): Flow<RequestState<Gamer>>
    suspend fun signOut(): RequestState<Unit>
}