package com.example.arcadia.domain.repository

import android.net.Uri
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
    
    suspend fun updateGamer(
        gamer: Gamer,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
    
    suspend fun uploadProfileImage(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    )
    
    suspend fun deleteProfileImage(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
    
    suspend fun signOut(): RequestState<Unit>
}