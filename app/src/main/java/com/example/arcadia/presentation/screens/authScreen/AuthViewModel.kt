package com.example.arcadia.presentation.screens.authScreen

import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.presentation.base.BaseViewModel
import com.example.arcadia.util.RequestState
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthViewModel(
    private val gamerRepository: GamerRepository
): BaseViewModel() {
    val customer = gamerRepository.readCustomerFlow()
        .stateIn(
            viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = RequestState.Loading
        )
    fun createCustomer(
        user: FirebaseUser?,
        onSuccess: (profileComplete: Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        launchWithKey("create_customer") {
            withContext(Dispatchers.IO) {
                gamerRepository.createUser(
                    user = user,
                    onSuccess = { profileComplete ->
                        viewModelScope.launch(Dispatchers.Main) {
                            onSuccess(profileComplete)
                        }
                    },
                    onError = { error ->
                        viewModelScope.launch(Dispatchers.Main) {
                            onError(error)
                        }
                    }
                )
            }
        }
    }
    fun signOut(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        launchWithKey("sign_out") {
            val result = withContext(Dispatchers.IO) {
                gamerRepository.signOut()
            }
            if (result is RequestState.Success) {
                onSuccess()
            } else if (result is RequestState.Error) {
                onError(result.getErrorMessage())
            }
        }
    }
}