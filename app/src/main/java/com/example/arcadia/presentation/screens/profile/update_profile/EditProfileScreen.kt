package com.example.arcadia.presentation.screens.profile.update_profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.presentation.screens.profile.components.ProfileDropdown
import com.example.arcadia.presentation.screens.profile.components.ProfileTextField
import com.example.arcadia.util.Countries
import com.example.arcadia.util.DisplayResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

// 🎨 Colors
val Background = Color(0xFF00123B)
val FieldBg = Color(0xFF00123B)
val FieldTxt = Color(0xFFDCDCDC)
val Border = Color(0xFFDCDCDC)
val BorderCyanOnSelect = Color(0xFF62B4DA)
val ErrorRed = Color(0xFFFF3535)
val ButtonBlue = Color(0xFF62B4DA)
val ButtonTxt = Color(0xFF00123B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigationIconClicked: (() -> Unit)? = null,
    onNavigateToHome: (() -> Unit)? = null
) {
    val viewModel: EditProfileViewModel = koinViewModel()
    val screenState = viewModel.screenState
    val screenReady = viewModel.screenReady
    val coroutineScope = rememberCoroutineScope()

    var showValidationErrors by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf("") }
    var isUpdating by remember { mutableStateOf(false) }
    
    // Track if this is the first time completing the profile (capture initial state)
    val initialProfileComplete = remember { screenState.profileComplete }
    val isFirstTimeCompletion by remember { derivedStateOf { !initialProfileComplete } }

    val genders = listOf("Male", "Female", "Other")

    val allValid by remember { derivedStateOf { viewModel.isFormValid } }

    Surface(color = Background, modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Background,
            topBar = {
                if (onNavigationIconClicked != null) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Edit Profile",
                                color = FieldTxt,
                                fontSize = 24.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigationIconClicked) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = ButtonBlue
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Background,
                            titleContentColor = FieldTxt
                        )
                    )
                }
            }
        ) { paddingValues ->
        screenReady.DisplayResult(
            modifier = Modifier.padding(paddingValues),
            onLoading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ButtonBlue)
                }
            },
            onError = { errorMessage ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage, color = ErrorRed, fontSize = 16.sp)
                }
            },
            onSuccess = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {

                        // Name
                        ProfileTextField(
                            value = screenState.name,
                            onValueChange = viewModel::updateName,
                            label = "Name",
                            placeholder = "Enter your name",
                            isError = showValidationErrors && viewModel.validateName().isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (showValidationErrors && viewModel.validateName().isNotEmpty())
                            Text(viewModel.validateName(), color = ErrorRed, fontSize = 12.sp)

                        // Email
                        ProfileTextField(
                            value = screenState.email,
                            onValueChange = {},
                            label = "Email",
                            readOnly = true,
                            enabled = false
                        )

                        // Username
                        ProfileTextField(
                            value = screenState.username,
                            onValueChange = viewModel::updateUsername,
                            label = "Username",
                            placeholder = "Enter your username",
                            isError = showValidationErrors && viewModel.validateUsername().isNotEmpty(),
                        )
                        if (showValidationErrors && viewModel.validateUsername().isNotEmpty())
                            Text(viewModel.validateUsername(), color = ErrorRed, fontSize = 12.sp)

                        // Country + City Row
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            ProfileDropdown(
                                label = "Country",
                                options = Countries.countries,
                                selected = screenState.country,
                                onSelected = viewModel::updateCountry,
                                modifier = Modifier.weight(1f),
                                isError = showValidationErrors && viewModel.validateCountry().isNotEmpty()
                            )

                            ProfileDropdown(
                                label = "City",
                                options = Countries.getCitiesForCountry(screenState.country),
                                selected = screenState.city,
                                onSelected = viewModel::updateCity,
                                modifier = Modifier.weight(1f),
                                isError = showValidationErrors && viewModel.validateCity().isNotEmpty()
                            )
                        }
                        if (showValidationErrors && viewModel.validateCountry().isNotEmpty())
                            Text(viewModel.validateCountry(), color = ErrorRed, fontSize = 12.sp)
                        if (showValidationErrors && viewModel.validateCity().isNotEmpty())
                            Text(viewModel.validateCity(), color = ErrorRed, fontSize = 12.sp)

                        // Gender
                        ProfileDropdown(
                            label = "Gender",
                            options = genders,
                            selected = screenState.gender,
                            onSelected = viewModel::updateGender,
                            isError = showValidationErrors && viewModel.validateGender().isNotEmpty()
                        )
                        if (showValidationErrors && viewModel.validateGender().isNotEmpty())
                            Text(viewModel.validateGender(), color = ErrorRed, fontSize = 12.sp)

                        // Bio
                        ProfileTextField(
                            value = screenState.description,
                            onValueChange = viewModel::updateDescription,
                            label = "Bio",
                            singleLine = false,
                            placeholder = "More about yourself...",
                            isError = showValidationErrors && viewModel.validateDescription().isNotEmpty(),
                            modifier = Modifier.height(120.dp)
                        )
                        if (showValidationErrors && viewModel.validateDescription().isNotEmpty())
                            Text(viewModel.validateDescription(), color = ErrorRed, fontSize = 12.sp)
                    }

                    // Update Button + Error
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = {
                                showValidationErrors = true
                                if (!viewModel.isFormValid) {
                                    updateError = "Please fill all the info first"
                                } else if (!isUpdating) {
                                    updateError = ""
                                    isUpdating = true
                                    viewModel.updateGamer(
                                        onSuccess = {
                                            coroutineScope.launch {
                                                isUpdating = false
                                                showPopup = true
                                                delay(2000)
                                                showPopup = false
                                                
                                                // If this is the first time completing the profile, navigate to home
                                                if (isFirstTimeCompletion && onNavigateToHome != null) {
                                                    delay(1000) // Give time for notification to show
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
                            enabled = allValid && !isUpdating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonBlue,
                                disabledContainerColor = ButtonBlue.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(
                                    color = ButtonTxt,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Updating...", color = ButtonTxt, fontSize = 18.sp)
                            } else {
                                Icon(Icons.Default.Check, contentDescription = "Update", tint = ButtonTxt)
                                Spacer(Modifier.width(8.dp))
                                Text("Update", color = ButtonTxt, fontSize = 18.sp)
                            }
                        }

                        if (updateError.isNotEmpty())
                            Text(updateError, color = ErrorRed, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    }

                    // Popup
                    if (showPopup) {
                        AlertDialog(
                            onDismissRequest = { showPopup = false },
                            confirmButton = {
                                TextButton(onClick = { showPopup = false }) {
                                    Text("OK", color = ButtonBlue)
                                }
                            },
                            title = { Text("Profile Updated!", color = FieldTxt) },
                            text = { Text("Your profile data has been successfully updated.", color = FieldTxt) },
                            containerColor = FieldBg,
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        )
        }
    }
}
