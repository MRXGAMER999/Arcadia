package com.example.arcadia.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.arcadia.presentation.screens.authScreen.AuthScreen
import com.example.arcadia.presentation.screens.onBoarding.OnBoardingScreen
import com.example.arcadia.presentation.screens.profile.update_profile.UpdateProfileScreen
import com.example.arcadia.util.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.Serializable

@Serializable
object HomeScreenKey : NavKey
@Serializable
object ProfileScreenKey : NavKey

@Serializable
object UpdateProfileScreenKey : NavKey
@Serializable
object OnboardingScreenKey : NavKey
@Serializable
object AuthScreenKey : NavKey

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    val isUserAuthenticated = FirebaseAuth.getInstance().currentUser != null
    val isOnBoardingCompleted = preferencesManager.isOnBoardingCompleted()
    
    // Determine the starting screen based on onboarding and authentication status
    val startDestination = when {
        !isOnBoardingCompleted -> OnboardingScreenKey
        !isUserAuthenticated -> AuthScreenKey
        else -> HomeScreenKey
    }
    
    val backStack = rememberNavBackStack(startDestination)

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        entryProvider = { key ->
            when (key) {
                is HomeScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        //HomeScreen()
                    }
                }
                is UpdateProfileScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        UpdateProfileScreen()
                    }
                }
                is AuthScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        AuthScreen(
                            onNavigateToHome = {
                                backStack.add(HomeScreenKey)
                            },
                            onNavigateToProfile = {
                                backStack.add(UpdateProfileScreenKey)
                            }
                        )
                    }
                }
                is OnboardingScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        OnBoardingScreen(
                            onFinish = {
                                // Mark onboarding as completed
                                preferencesManager.setOnBoardingCompleted(true)
                                // Navigate to auth screen
                                backStack.add(AuthScreenKey)
                            }
                        )
                    }
                }
                else -> error("Unknown NavKey: $key")
            }
        }
    )
}