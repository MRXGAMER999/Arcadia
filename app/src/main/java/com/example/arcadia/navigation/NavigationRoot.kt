package com.example.arcadia.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.arcadia.presentation.screens.authScreen.AuthScreen
import com.example.arcadia.presentation.screens.detailsScreen.DetailsScreen
import com.example.arcadia.presentation.screens.home.NewHomeScreen
import com.example.arcadia.presentation.screens.myGames.MyGamesScreen
import com.example.arcadia.presentation.screens.onBoarding.OnBoardingScreen
import com.example.arcadia.presentation.screens.profile.ProfileScreen
import com.example.arcadia.presentation.screens.profile.update_profile.EditProfileScreen
import com.example.arcadia.presentation.screens.searchScreen.SearchScreen
import com.example.arcadia.ui.theme.Surface
import android.app.Activity
import com.example.arcadia.util.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.Serializable

@Serializable
object AuthScreenKey : NavKey

@Serializable
object HomeScreenKey : NavKey

@Serializable
data class ProfileScreenKey(val userId: String? = null) : NavKey

@Serializable
object EditProfileScreenKey : NavKey

@Serializable
object OnboardingScreenKey : NavKey

@Serializable
data class MyGamesScreenKey(
    val userId: String? = null,
    val username: String? = null
) : NavKey

@Serializable
data class SearchScreenKey(val initialQuery: String? = null) : NavKey

@Serializable
object AnalyticsScreenKey : NavKey

@Serializable
data class DetailsScreenKey(val gameId: Int) : NavKey

@Serializable
data class RoastScreenKey(val targetUserId: String? = null) : NavKey

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    val isUserAuthenticated = FirebaseAuth.getInstance().currentUser != null
    val isOnBoardingCompleted = preferencesManager.isOnBoardingCompleted()
    
    // Handle Deep Link (arcadia://profile/{userId} or https://mrxgamer999.github.io/Arcadia/profile?id={userId})
    val activity = context as? Activity
    val intent = activity?.intent
    val deepLinkUserId = remember(intent) {
        intent?.data?.let { uri ->
            when {
                // Custom scheme: arcadia://profile/{userId}
                uri.scheme == "arcadia" && uri.host == "profile" -> {
                    uri.pathSegments.firstOrNull()
                }
                // GitHub Pages: https://mrxgamer999.github.io/Arcadia/profile?id={userId}
                uri.host == "mrxgamer999.github.io" && uri.path?.startsWith("/Arcadia/profile") == true -> {
                    uri.getQueryParameter("id")
                }
                else -> null
            }
        }
    }
    
    // Determine the starting screen based on onboarding and authentication status
    val startDestination = when {
        deepLinkUserId != null -> ProfileScreenKey(deepLinkUserId)
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
        // Add the decorator that preserves saveable state (including scroll positions)
        // for each NavEntry while it's on the back stack
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator()
        ),
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
                        NewHomeScreen(
                            onNavigateToProfile = {
                                backStack.add(ProfileScreenKey())
                            },
                            onNavigateToMyGames = {
                                backStack.add(MyGamesScreenKey())
                            },
                            onNavigateToSearch = { query ->
                                backStack.add(SearchScreenKey(query))
                            },
                            onNavigateToAnalytics = {
                                backStack.add(AnalyticsScreenKey)
                            },
                            onGameClick = { gameId ->
                                backStack.add(DetailsScreenKey(gameId))
                            }
                        )
                    }
                }
                is ProfileScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        ProfileScreen(
                            userId = key.userId,
                            onNavigateBack = {
                                if (backStack.size <= 1) {
                                    backStack.add(HomeScreenKey)
                                    backStack.remove(key)
                                } else {
                                    backStack.remove(key)
                                }
                            },
                            onNavigateToEditProfile = {
                                backStack.add(EditProfileScreenKey)
                            },
                            onNavigateToMyGames = { navUserId, navUsername ->
                                backStack.add(MyGamesScreenKey(navUserId, navUsername))
                            },
                            onNavigateToRoast = { targetUserId ->
                                backStack.add(RoastScreenKey(targetUserId))
                            },
                            onGameClick = { gameId ->
                                backStack.add(DetailsScreenKey(gameId))
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
                is MyGamesScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        MyGamesScreen(
                            userId = key.userId,
                            username = key.username,
                            onNavigateBack = {
                                backStack.remove(key)
                            },
                            onGameClick = { gameId ->
                                backStack.add(DetailsScreenKey(gameId))
                            },
                            onNavigateToAnalytics = {
                                backStack.add(AnalyticsScreenKey)
                            },
                            showBackButton = true
                        )
                    }
                }
                is SearchScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        SearchScreen(
                            initialQuery = key.initialQuery,
                            onBackClick = {
                                backStack.remove(key)
                            },
                            onGameClick = { gameId ->
                                backStack.add(DetailsScreenKey(gameId))
                            }
                        )
                    }
                }
                is AnalyticsScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        com.example.arcadia.presentation.screens.analytics.AnalyticsScreen(
                            onNavigateBack = { backStack.remove(key) },
                            onNavigateToSearch = { query ->
                                backStack.add(SearchScreenKey(query))
                            },
                            onNavigateToRoast = {
                                backStack.add(RoastScreenKey())
                            }
                        )
                    }
                }
                is DetailsScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        DetailsScreen(
                            gameId = key.gameId,
                            onNavigateBack = { backStack.remove(key) }
                        )
                    }
                }
                is RoastScreenKey -> {
                    NavEntry(
                        key = key,
                    ) {
                        com.example.arcadia.presentation.screens.roast.RoastScreen(
                            targetUserId = key.targetUserId,
                            onNavigateBack = { backStack.remove(key) }
                        )
                    }
                }
                else -> error("Unknown NavKey: $key")
            }
        }
    )
}