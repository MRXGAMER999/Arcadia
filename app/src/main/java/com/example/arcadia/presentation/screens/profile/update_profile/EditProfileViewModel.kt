package com.example.arcadia.presentation.screens.profile.update_profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.Gamer
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class EditProfileScreenState(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val username: String = "",
    val country: String = "",
    val city: String = "",
    val gender: String = "",
    val description: String = "",
    val profileComplete: Boolean = false
)

class EditProfileViewModel(
    private val gamerRepository: GamerRepository
): ViewModel() {
    
    var screenReady: RequestState<Unit> by mutableStateOf(RequestState.Loading)
        private set
        
    var screenState: EditProfileScreenState by mutableStateOf(EditProfileScreenState())
        private set
    
    // Validation functions
    fun validateName(): String = when {
        screenState.name.length < 3 -> "Name must be at least 3 characters"
        screenState.name.any { !it.isLetter() && !it.isWhitespace() } -> "Name cannot contain symbols"
        else -> ""
    }

    fun validateUsername(): String = when {
        screenState.username.length < 3 -> "Username must be at least 3 characters"
        screenState.username.any { !it.isLetterOrDigit() && it != '_' } -> "Username cannot contain symbols"
        else -> ""
    }

    fun validateCountry(): String = if (screenState.country.isEmpty()) "Please select a country" else ""
    
    fun validateCity(): String = if (screenState.city.isEmpty()) "Please select a city" else ""
    
    fun validateGender(): String = if (screenState.gender.isEmpty()) "Please select a gender" else ""
    
    fun validateDescription(): String = when {
        screenState.description.isEmpty() -> "" // Empty is valid (optional field)
        screenState.description.any { it in "@#$%^&*()" } -> "Bio cannot contain symbols"
        else -> ""
    }
    
    val isFormValid: Boolean
        get() = validateName().isEmpty() &&
                validateUsername().isEmpty() &&
                validateCountry().isEmpty() &&
                validateCity().isEmpty() &&
                validateGender().isEmpty() &&
                validateDescription().isEmpty()
    
    private var dataLoadJob: Job? = null
    
    init {
        loadGamerData()
    }
    
    fun reloadData() {
        dataLoadJob?.cancel()
        loadGamerData()
    }
    
    private fun loadGamerData() {
        screenReady = RequestState.Loading
        screenState = EditProfileScreenState()
        
        dataLoadJob = viewModelScope.launch {
            gamerRepository.readCustomerFlow().collectLatest { data ->
                if (data.isSuccess()) {
                    val fetchedGamer = data.getSuccessData()
                    screenState = EditProfileScreenState(
                        id = fetchedGamer.id,
                        name = fetchedGamer.name,
                        email = fetchedGamer.email,
                        username = fetchedGamer.username,
                        country = fetchedGamer.country ?: "",
                        city = fetchedGamer.city ?: "",
                        gender = fetchedGamer.gender ?: "",
                        description = fetchedGamer.description ?: "",
                        profileComplete = fetchedGamer.profileComplete
                    )
                    screenReady = RequestState.Success(Unit)
                } else if (data.isError()) {
                    screenReady = RequestState.Error(data.getErrorMessage())
                }
            }
        }
    }
    
    fun updateName(value: String) {
        screenState = screenState.copy(name = value)
    }
    
    fun updateUsername(value: String) {
        screenState = screenState.copy(username = value)
    }
    
    fun updateCountry(value: String) {
        screenState = screenState.copy(country = value, city = "") // Reset city when country changes
    }
    
    fun updateCity(value: String) {
        screenState = screenState.copy(city = value)
    }
    
    fun updateGender(value: String) {
        screenState = screenState.copy(gender = value)
    }
    
    fun updateDescription(value: String) {
        screenState = screenState.copy(description = value)
    }
    
    fun updateGamer(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            gamerRepository.updateGamer(
                gamer = Gamer(
                    id = screenState.id,
                    name = screenState.name,
                    email = screenState.email,
                    username = screenState.username,
                    country = screenState.country,
                    city = screenState.city,
                    gender = screenState.gender,
                    description = screenState.description,
                    profileComplete = true
                ),
                onSuccess = onSuccess,
                onError = onError
            )
        }
    }
}