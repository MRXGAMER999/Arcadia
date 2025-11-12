package com.example.arcadia.presentation.screens.authScreen

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.arcadia.R
import com.example.arcadia.presentation.components.GoogleButton
import com.example.arcadia.presentation.components.sign_in.GoogleAuthUiClient
import com.example.arcadia.presentation.components.sign_in.SignInViewModel
import com.example.arcadia.presentation.screens.authScreen.components.AuthContent
import com.example.arcadia.ui.theme.Surface
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AuthScreen(
    onNavigateToHome:() -> Unit,
    onNavigateToProfile:() -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = koinViewModel()
    val signInViewModel: SignInViewModel = koinViewModel()
    val state by signInViewModel.state.collectAsState()
    var loading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Surface
        ){ paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {


                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val data = result.data
                        if (data != null) {
                            coroutineScope.launch {
                                val client = GoogleAuthUiClient(
                                    context = context,
                                    oneTapClient = Identity.getSignInClient(context)
                                )
                                val signInResult = client.signInWithIntent(data)
                                signInViewModel.onSignInResult(signInResult)
                                loading = false
                            }
                        } else {
                            loading = false
                        }
                    } else {
                        loading = false
                    }
                }

                LaunchedEffect(state.isSignInSuccessful, state.signInError) {
                    try {
                        if (state.isSignInSuccessful) {
                            Log.d("AuthScreen", "Sign-in successful, getting current user")
                            val currentUser = signInViewModel.getCurrentUser()
                            Log.d("AuthScreen", "Current user: ${currentUser?.uid}")

                            if (currentUser == null) {
                                Log.e("AuthScreen", "Current user is null after successful sign-in")
                                Toast.makeText(context, "Sign-in succeeded but user is null", Toast.LENGTH_LONG).show()
                                signInViewModel.resetState()
                                loading = false
                                return@LaunchedEffect
                            }

                            Log.d("AuthScreen", "Creating/fetching customer document")
                            authViewModel.createCustomer(
                                user = currentUser,
                                onSuccess = { profileComplete ->
                                    Log.d("AuthScreen", "Customer created/fetched, profileComplete: $profileComplete")
                                    signInViewModel.resetState()
                                    coroutineScope.launch {
                                        try {
                                            kotlinx.coroutines.delay(800)
                                            // Navigate based on profile completion status
                                            Log.d("AuthScreen", "Navigating to ${if (profileComplete) "Home" else "Profile"}")
                                            if (profileComplete) {
                                                onNavigateToHome()
                                            } else {
                                                onNavigateToProfile()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("AuthScreen", "Navigation error: ${e.message}", e)
                                            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                onError = { error ->
                                    Log.e("AuthScreen", "Error creating customer: $error")
                                    Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                    loading = false
                                    signInViewModel.resetState()
                                }
                            )
                        } else if (state.signInError != null) {
                            Log.e("AuthScreen", "Sign in error: ${state.signInError}")
                            Toast.makeText(context, "Sign in failed: ${state.signInError}", Toast.LENGTH_LONG).show()
                            signInViewModel.resetState()
                            loading = false
                        }
                    } catch (e: Exception) {
                        Log.e("AuthScreen", "Unexpected error in LaunchedEffect: ${e.message}", e)
                        Toast.makeText(context, "Unexpected error: ${e.message}", Toast.LENGTH_LONG).show()
                        signInViewModel.resetState()
                        loading = false
                    }
                }

                AuthContent(modifier = Modifier.weight(1f))

                GoogleButton(
                    loading = loading,
                    onClick = {
                        coroutineScope.launch {
                            loading = true
                            val client = GoogleAuthUiClient(
                                context = context,
                                oneTapClient = Identity.getSignInClient(context)
                            )
                            val intentSender = client.signIn()
                            if (intentSender != null) {
                                launcher.launch(
                                    IntentSenderRequest.Builder(intentSender).build()
                                )
                            } else {
                                loading = false
                                Log.e("AuthScreen", "Failed to initialize Google Sign-In")
                                Toast.makeText(
                                    context,
                                    "Failed to initialize sign-in. Please check your internet connection.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )
            }
        }

    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
fun AuthScreenPreview() {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Surface
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthContent(modifier = Modifier.weight(1f))

                GoogleButton(
                    loading = false,
                    onClick = { }
                )
            }
        }
    }
}
