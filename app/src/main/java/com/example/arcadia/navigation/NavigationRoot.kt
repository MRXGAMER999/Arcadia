package com.example.arcadia.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.arcadia.presentation.screens.authScreen.AuthScreen
import com.example.arcadia.presentation.screens.home.HomeScreen
import com.example.arcadia.presentation.screens.onBoarding.OnBoardingScreen
import com.example.arcadia.presentation.screens.profile.update_profile.EditProfileScreen
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.util.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.Serializable

@Serializable
object AuthScreenKey : NavKey

@Serializable
object HomeScreenKey : NavKey

@Serializable
object EditProfileScreenKey : NavKey

@Serializable
object OnboardingScreenKey : NavKey

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
        modifier = modifier
            .fillMaxSize()
            .background(Surface), // Set dark blue background to prevent white flash
        backStack = backStack,
        entryProvider = { key ->
            when (key) {
                is AuthScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        AuthScreen(
                            onNavigateToHome = {
                                backStack.remove(key)
                                backStack.add(HomeScreenKey)
                            },
                            onNavigateToProfile = {
                                backStack.remove(key)
                                backStack.add(EditProfileScreenKey)
                            }
                        )
                    }
                }
                is HomeScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        HomeScreen(
                            onSignOut = {
                                backStack.clear()
                                backStack.add(AuthScreenKey)
                            }
                        )
                    }
                }
                is EditProfileScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        EditProfileScreen(
                            onNavigationIconClicked = {
                                backStack.remove(key)
                            },
                            onNavigateToHome = {
                                backStack.remove(key)
                                backStack.add(HomeScreenKey)
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
                                backStack.remove(key)
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