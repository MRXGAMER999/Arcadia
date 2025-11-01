package com.example.arcadia.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.arcadia.presentation.screens.onBoarding.OnBoardingScreen
import kotlinx.serialization.Serializable

@Serializable
object HomeScreenKey : NavKey
@Serializable
object ProfileScreenKey : NavKey
@Serializable
object OnboardingScreenKey : NavKey

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
) {

    val backStack = rememberNavBackStack(OnboardingScreenKey)

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
                is ProfileScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        //ProfileScreen()
                    }
                }
                is OnboardingScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        OnBoardingScreen(
                            onFinish = {}
                        )
                    }
                }
                else -> error("Unknown NavKey: $key")
            }
        }
    )
}