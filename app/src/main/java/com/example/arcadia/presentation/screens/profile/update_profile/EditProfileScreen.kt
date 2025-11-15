package com.example.arcadia.presentation.screens.profile.update_profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.presentation.screens.profile.components.ProfileDropdown
import com.example.arcadia.presentation.screens.profile.components.ProfileImagePicker
import com.example.arcadia.presentation.screens.profile.components.ProfileTextField
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.util.Countries
import com.example.arcadia.util.DisplayResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EditProfileScreen(
    onNavigationIconClicked: (() -> Unit)? = null,
    onNavigateToHome: (() -> Unit)? = null
) {
    val viewModel: EditProfileViewModel = koinViewModel()
    val screenState = viewModel.screenState
    val localState = viewModel.localState
    val screenReady = viewModel.screenReady
    val coroutineScope = rememberCoroutineScope()

    var showValidationErrors by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }

    // Track if this is the first time completing the profile
    val initialProfileComplete = remember { screenState.profileComplete }

    val genders = listOf("Male", "Female")

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "MY PROFILE",
                        fontSize = 30.sp,
                        fontFamily = BebasNeueFont,
                        color = TextSecondary
                    )
                },
                navigationIcon = {
                    if (onNavigationIconClicked != null) {
                        IconButton(onClick = onNavigationIconClicked) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = TextSecondary
                )
            )
        }
    ) { paddingValues ->
        screenReady.DisplayResult(
            modifier = Modifier.padding(paddingValues),
            onLoading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(color = ButtonPrimary)
                }
            },
            onError = { errorMessage ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFFF3535)
                        )
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            onSuccess = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .imePadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Scrollable content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profile Image Picker
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileImagePicker(
                            imageUrl = null,
                            onImageClick = {
                                // TODO: Implement image picker
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Name Field
                        ProfileTextField(
                            value = localState.name,
                            onValueChange = viewModel::updateLocalName,
                            label = "Name",
                            placeholder = "",
                            isError = showValidationErrors && viewModel.validateName().isNotEmpty(),
                            errorMessage = if (showValidationErrors) viewModel.validateName() else null
                        )

                        // Email Field (Read-only)
                        ProfileTextField(
                            value = screenState.email,
                            onValueChange = {},
                            label = "Email",
                            placeholder = "Example@gmail.com",
                            readOnly = true,
                            enabled = false,
                            trailingIcon = {}  // No clear button for disabled field
                        )

                        // Username Field
                        ProfileTextField(
                            value = localState.username,
                            onValueChange = viewModel::updateLocalUsername,
                            label = "Username",
                            placeholder = "",
                            isError = showValidationErrors && viewModel.validateUsername().isNotEmpty(),
                            errorMessage = if (showValidationErrors) viewModel.validateUsername() else null
                        )

                        // Country and City Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Country Dropdown
                            ProfileDropdown(
                                label = "Country",
                                options = Countries.countries,
                                selected = localState.country,
                                onSelected = viewModel::updateLocalCountry,
                                modifier = Modifier.weight(1f),
                                isError = showValidationErrors && viewModel.validateCountry().isNotEmpty(),
                                errorMessage = if (showValidationErrors) viewModel.validateCountry() else null
                            )

                            // City Dropdown
                            ProfileDropdown(
                                label = "City",
                                options = Countries.getCitiesForCountry(localState.country),
                                selected = localState.city,
                                onSelected = viewModel::updateLocalCity,
                                modifier = Modifier.weight(1f),
                                isError = showValidationErrors && viewModel.validateCity().isNotEmpty(),
                                errorMessage = if (showValidationErrors) viewModel.validateCity() else null
                            )
                        }

                        // Gender Dropdown
                        ProfileDropdown(
                            label = "Gender",
                            options = genders,
                            selected = localState.gender,
                            onSelected = viewModel::updateLocalGender,
                            isError = showValidationErrors && viewModel.validateGender().isNotEmpty(),
                            errorMessage = if (showValidationErrors) viewModel.validateGender() else null
                        )

                        // Bio/Description Field
                        ProfileTextField(
                            value = localState.description,
                            onValueChange = viewModel::updateLocalDescription,
                            label = "Bio",
                            singleLine = false,
                            maxLines = 5,
                            minLines = 4,
                            isError = showValidationErrors && viewModel.validateDescription().isNotEmpty(),
                            errorMessage = if (showValidationErrors) viewModel.validateDescription() else null
                        )
                    }

                    // Update Button Section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Error message
                        AnimatedVisibility(
                            visible = updateError.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Text(
                                text = updateError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFF3535),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Update Button - Always enabled
                        Button(
                            onClick = {
                                // Always show validation errors when button is clicked
                                showValidationErrors = true
                                
                                if (!viewModel.isFormValid) {
                                    // Show error message but don't update
                                    updateError = "Please fill all the required fields"
                                } else if (!isUpdating) {
                                    // Form is valid, save to database
                                    updateError = ""
                                    isUpdating = true
                                    
                                    viewModel.saveProfile(
                                        onSuccess = {
                                            coroutineScope.launch {
                                                isUpdating = false
                                                showValidationErrors = false // Reset validation errors on success
                                                showSuccessDialog = true
                                                delay(2000)
                                                showSuccessDialog = false

                                                // If this is the first time completing the profile, navigate to home
                                                if (!initialProfileComplete && onNavigateToHome != null) {
                                                    delay(500)
                                                    onNavigateToHome()
                                                }
                                            }
                                        },
                                        onError = { error ->
                                            isUpdating = false
                                            updateError = error
                                        }
                                    )
                                }
                            },
                            enabled = !isUpdating, // Only disable when updating
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonPrimary,
                                contentColor = Surface,
                                disabledContainerColor = ButtonPrimary.copy(alpha = 0.5f),
                                disabledContentColor = Surface.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(24.dp),
                                    color = Surface,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Surface
                                    )
                                    Spacer(modifier = Modifier.padding(4.dp))
                                    Text(
                                        text = "Update",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = Surface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        )
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text(
                        text = "OK",
                        color = ButtonPrimary
                    )
                }
            },
            title = {
                Text(
                    text = "Profile Updated!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextSecondary
                )
            },
            text = {
                Text(
                    text = "Your profile has been successfully updated.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary.copy(alpha = 0.8f)
                )
            },
            containerColor = Surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

