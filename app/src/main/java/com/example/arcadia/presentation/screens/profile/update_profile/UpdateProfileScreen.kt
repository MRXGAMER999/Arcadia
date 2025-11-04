package com.example.arcadia.presentation.screens.profile.update_profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
fun EditProfileScreen() {
    var firstName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("example@gmail.com") }
    var username by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }

    var showValidationErrors by remember { mutableStateOf(false) }
    var showPopup by remember { mutableStateOf(false) }
    var updateError by remember { mutableStateOf("") }

    val countries = mapOf(
        "USA" to listOf("New York", "Los Angeles", "Chicago"),
        "India" to listOf("Delhi", "Mumbai", "Bangalore"),
        "UK" to listOf("London", "Manchester", "Liverpool"),
        "Germany" to listOf("Berlin", "Munich", "Hamburg")
    )

    val genders = listOf("Male", "Female", "Other")

    val existingUsernames = listOf("ali", "mohamed", "existinguser", "sarah123","rfrf_11")

    // Validation Logic
    fun validateName() = when {
        firstName.length < 3 -> "Name must be at least 3 characters"
        firstName.any { !it.isLetter() && !it.isWhitespace() } -> "Name cannot contain symbols"
        else -> ""
    }

    fun validateUsername() = when {
        username.length < 3 -> "Username must be at least 3 characters"
        username.any { !it.isLetterOrDigit() && it != '_' } -> "Username cannot contain symbols"
        existingUsernames.any { it.equals(username, ignoreCase = true) } -> "This username already used"
        else -> ""
    }


    fun validateCountry() = if (country.isEmpty()) "Please select a country" else ""
    fun validateCity() = if (city.isEmpty()) "Please select a city" else ""
    fun validateGender() = if (gender.isEmpty()) "Please select a gender" else ""
    fun validateBio() = if (about.any { it in "@#$%^&*()" }) "Bio cannot contain symbols" else ""

    fun isAllValidPure(): Boolean =
        validateName().isEmpty() &&
                validateUsername().isEmpty() &&
                validateCountry().isEmpty() &&
                validateCity().isEmpty() &&
                validateGender().isEmpty() &&
                validateBio().isEmpty()

    val allValid by remember { derivedStateOf { isAllValidPure() } }

    Surface(color = Background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Name
                ProfileTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = "Name",
                    placeholder = "Enter your name",
                    isError = showValidationErrors && validateName().isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (showValidationErrors && validateName().isNotEmpty())
                    Text(validateName(), color = ErrorRed, fontSize = 12.sp)

                // Email
                ProfileTextField(
                    value = email,
                    onValueChange = {},
                    label = "Email",
                    readOnly = true,
                    enabled = false
                )

                // Username
                ProfileTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "Username",
                    placeholder = "Enter your username",
                    isError = showValidationErrors && validateUsername().isNotEmpty(),
                )
                if (showValidationErrors && validateUsername().isNotEmpty())
                    Text(validateUsername(), color = ErrorRed, fontSize = 12.sp)

                // Country + City Row
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileDropdown(
                        label = "Country",
                        options = countries.keys.toList(),
                        selected = country,
                        onSelected = {
                            country = it
                            city = "" // reset city when country changes
                        },
                        modifier = Modifier.weight(1f),
                        isError = showValidationErrors && validateCountry().isNotEmpty()
                    )

                    ProfileDropdown(
                        label = "City",
                        options = countries[country] ?: emptyList(),
                        selected = city,
                        onSelected = { city = it },
                        modifier = Modifier.weight(1f),
                        isError = showValidationErrors && validateCity().isNotEmpty()
                    )
                }
                if (showValidationErrors && validateCountry().isNotEmpty())
                    Text(validateCountry(), color = ErrorRed, fontSize = 12.sp)
                if (showValidationErrors && validateCity().isNotEmpty())
                    Text(validateCity(), color = ErrorRed, fontSize = 12.sp)

                // Gender
                ProfileDropdown(
                    label = "Gender",
                    options = genders,
                    selected = gender,
                    onSelected = { gender = it },
                    isError = showValidationErrors && validateGender().isNotEmpty()
                )
                if (showValidationErrors && validateGender().isNotEmpty())
                    Text(validateGender(), color = ErrorRed, fontSize = 12.sp)

                // Bio
                ProfileTextField(
                    value = about,
                    onValueChange = { about = it },
                    label = "Bio",
                    singleLine = false,
                    placeholder = "More about yourself...",
                    isError = showValidationErrors && validateBio().isNotEmpty(),
                    modifier = Modifier.height(120.dp)
                )
                if (showValidationErrors && validateBio().isNotEmpty())
                    Text(validateBio(), color = ErrorRed, fontSize = 12.sp)
            }

            // Update Button + Error
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = {
                        showValidationErrors = false
                        showValidationErrors = true
                        if (!isAllValidPure()) {
                            updateError = "Please fill all the info first"
                        } else {
                            updateError = ""
                            showPopup = true
                        }
                    },
                    enabled = true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allValid) ButtonBlue else ButtonBlue.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Update", tint = ButtonTxt)
                    Spacer(Modifier.width(8.dp))
                    Text("Update", color = ButtonTxt, fontSize = 18.sp)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    isError: Boolean = false,
    singleLine: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    enabled: Boolean = true
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = FieldTxt.copy(alpha = 0.7f)) },
        placeholder = { Text(placeholder, color = FieldTxt.copy(alpha = 0.4f)) },
        singleLine = singleLine,
        trailingIcon = trailingIcon,
        isError = isError,
        readOnly = readOnly,
        enabled = enabled,
        textStyle = LocalTextStyle.current.copy(color = FieldTxt),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (isError) ErrorRed else BorderCyanOnSelect,
            unfocusedBorderColor = if (isError) ErrorRed else Border.copy(alpha = 0.5f),
            focusedTextColor = FieldTxt,
            unfocusedTextColor = FieldTxt,
            cursorColor = BorderCyanOnSelect,
            focusedContainerColor = FieldBg,
            unfocusedContainerColor = FieldBg,
            disabledContainerColor = FieldBg.copy(alpha = 0.3f),
            disabledTextColor = FieldTxt.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            label = { Text(label, color = FieldTxt.copy(alpha = 0.7f)) },
            readOnly = true,
            isError = isError,
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = FieldTxt
                )
            },
            textStyle = LocalTextStyle.current.copy(color = FieldTxt),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) ErrorRed else BorderCyanOnSelect,
                unfocusedBorderColor = if (isError) ErrorRed else Border.copy(alpha = 0.5f),
                focusedTextColor = FieldTxt,
                unfocusedTextColor = FieldTxt,
                cursorColor = BorderCyanOnSelect,
                focusedContainerColor = FieldBg,
                unfocusedContainerColor = FieldBg
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(FieldBg)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, color = FieldTxt) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
                if (index < options.lastIndex)
                    Divider(color = BorderCyanOnSelect.copy(alpha = 0.3f), thickness = 1.dp)
            }
        }
    }
}
