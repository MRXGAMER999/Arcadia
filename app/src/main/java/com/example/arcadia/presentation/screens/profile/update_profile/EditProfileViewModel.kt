package com.example.arcadia.presentation.screens.profile.update_profile

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.example.arcadia.domain.model.Gamer
import com.example.arcadia.presentation.base.BaseViewModel
import com.example.arcadia.domain.repository.GamerRepository
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Saved state from database
data class EditProfileScreenState(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val username: String = "",
    val country: String = "",
    val city: String = "",
    val gender: String = "",
    val description: String = "",
    val profileImageUrl: String? = null,
    val profileComplete: Boolean = false,
    val steamId: String = "",
    val xboxGamertag: String = "",
    val psnId: String = ""
)

// Local editing state (not yet saved)
data class EditProfileLocalState(
    val name: String = "",
    val username: String = "",
    val country: String = "",
    val city: String = "",
    val gender: String = "",
    val description: String = "",
    val profileImageUrl: String? = null,
    val steamId: String = "",
    val xboxGamertag: String = "",
    val psnId: String = ""
)

class EditProfileViewModel(
    private val gamerRepository: GamerRepository
): BaseViewModel() {
    
    var screenReady: RequestState<Unit> by mutableStateOf(RequestState.Loading)
        private set
        
    var screenState: EditProfileScreenState by mutableStateOf(EditProfileScreenState())
        private set
    
    // Local editing state - not committed until save
    var localState: EditProfileLocalState by mutableStateOf(EditProfileLocalState())
        private set
    
    // Image upload state
    var isUploadingImage by mutableStateOf(false)
        private set
    
    // Validation functions for local state
    // Only Name and Username are required (email, photo come from Google)
    fun validateName(): String = when {
        localState.name.isEmpty() -> "Name is required"
        localState.name.length < 3 -> "Name must be at least 3 characters"
        localState.name.any { !it.isLetter() && !it.isWhitespace() } -> "Name cannot contain symbols"
        else -> ""
    }

    // Username is optional - only validate format if filled
    fun validateUsername(): String = when {
        localState.username.isEmpty() -> "" // Optional
        localState.username.length < 3 -> "Username must be at least 3 characters"
        localState.username.any { !it.isLetterOrDigit() && it != '_' } -> "Username can only contain letters, numbers, and underscores"
        else -> ""
    }

    // Optional fields - only validate format if filled
    fun validateCountry(): String = "" // Optional
    
    fun validateCity(): String = "" // Optional
    
    fun validateGender(): String = "" // Optional
    
    fun validateDescription(): String = when {
        localState.description.isEmpty() -> "" // Empty is valid (optional field)
        localState.description.any { it in "@#$%^&*()" } -> "Bio cannot contain symbols"
        else -> ""
    }
    
    // Only Name is required
    val isFormValid: Boolean
        get() = validateName().isEmpty() &&
                validateUsername().isEmpty() &&
                validateUsername().isEmpty() &&
                validateDescription().isEmpty()
    
    init {
        loadGamerData()
    }
    
    fun reloadData() {
        loadGamerData()
    }
    
    private fun loadGamerData() {
        screenReady = RequestState.Loading
        screenState = EditProfileScreenState()
        
        launchWithKey("load_gamer_data") {
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
                        profileImageUrl = fetchedGamer.profileImageUrl,
                        profileComplete = fetchedGamer.profileComplete,
                        steamId = fetchedGamer.steamId ?: "",
                        xboxGamertag = fetchedGamer.xboxGamertag ?: "",
                        psnId = fetchedGamer.psnId ?: ""
                    )
                    
                    // Initialize local state with saved values
                    localState = EditProfileLocalState(
                        name = fetchedGamer.name,
                        username = fetchedGamer.username,
                        country = fetchedGamer.country ?: "",
                        city = fetchedGamer.city ?: "",
                        gender = fetchedGamer.gender ?: "",
                        description = fetchedGamer.description ?: "",
                        profileImageUrl = fetchedGamer.profileImageUrl,
                        steamId = fetchedGamer.steamId ?: "",
                        xboxGamertag = fetchedGamer.xboxGamertag ?: "",
                        psnId = fetchedGamer.psnId ?: ""
                    )
                    
                    screenReady = RequestState.Success(Unit)
                } else if (data.isError()) {
                    screenReady = RequestState.Error(data.getErrorMessage())
                }
            }
        }
    }
    
    // Update local editing state (not saved yet)
    fun updateLocalName(value: String) {
        localState = localState.copy(name = value)
    }
    
    fun updateLocalUsername(value: String) {
        localState = localState.copy(username = value)
    }
    
    fun updateLocalCountry(value: String) {
        localState = localState.copy(country = value, city = "") // Reset city when country changes
    }
    
    fun updateLocalCity(value: String) {
        localState = localState.copy(city = value)
    }
    
    fun updateLocalGender(value: String) {
        localState = localState.copy(gender = value)
    }
    
    fun updateLocalDescription(value: String) {
        localState = localState.copy(description = value)
    }
    
    fun updateLocalSteamId(value: String) {
        localState = localState.copy(steamId = value)
    }
    
    fun updateLocalXboxGamertag(value: String) {
        localState = localState.copy(xboxGamertag = value)
    }
    
    fun updateLocalPsnId(value: String) {
        localState = localState.copy(psnId = value)
    }
    
    // Handle profile image selection and upload
    fun uploadProfileImage(
        imageUri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        isUploadingImage = true
        launchWithKey("upload_image") {
            gamerRepository.uploadProfileImage(
                imageUri = imageUri,
                onSuccess = { downloadUrl ->
                    // Update local state with new image URL
                    localState = localState.copy(profileImageUrl = downloadUrl)
                    isUploadingImage = false
                    onSuccess()
                },
                onError = { error ->
                    isUploadingImage = false
                    onError(error)
                }
            )
        }
    }
    
    // Commit local changes and save to database
    fun saveProfile(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        launchWithKey("save_profile") {
            gamerRepository.updateGamer(
                gamer = Gamer(
                    id = screenState.id,
                    name = localState.name,
                    email = screenState.email,
                    username = localState.username,
                    country = localState.country,
                    city = localState.city,
                    gender = localState.gender,
                    description = localState.description,
                    profileImageUrl = localState.profileImageUrl,
                    profileComplete = true,
                    steamId = localState.steamId.takeIf { it.isNotEmpty() },
                    xboxGamertag = localState.xboxGamertag.takeIf { it.isNotEmpty() },
                    psnId = localState.psnId.takeIf { it.isNotEmpty() }
                ),
                onSuccess = {
                    // Update saved state with local state after successful save
                    screenState = screenState.copy(
                        name = localState.name,
                        username = localState.username,
                        country = localState.country,
                        city = localState.city,
                        gender = localState.gender,
                        description = localState.description,
                        profileImageUrl = localState.profileImageUrl,
                        profileComplete = true,
                        steamId = localState.steamId,
                        xboxGamertag = localState.xboxGamertag,
                        psnId = localState.psnId
                    )
                    onSuccess()
                },
                onError = onError
            )
        }
    }
    
    // Reset local state to saved state (cancel changes)
    fun resetLocalState() {
        localState = EditProfileLocalState(
            name = screenState.name,
            username = screenState.username,
            country = screenState.country,
            city = screenState.city,
            gender = screenState.gender,
            description = screenState.description,
            profileImageUrl = screenState.profileImageUrl,
            steamId = screenState.steamId,
            xboxGamertag = screenState.xboxGamertag,
            psnId = screenState.psnId
        )
    }
}