package com.example.arcadia.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.arcadia.presentation.screens.home.HomeViewModel
import com.example.arcadia.presentation.screens.home.components.GameListItem
import com.example.arcadia.presentation.screens.home.components.LargeGameCard
import com.example.arcadia.presentation.screens.home.components.SectionHeader
import com.example.arcadia.presentation.screens.home.components.SmallGameCard
import com.example.arcadia.presentation.screens.myGames.MyGamesScreen
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.util.RequestState
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
sealed interface HomeTabKey : NavKey

@Serializable
object HomeTab : HomeTabKey

@Serializable
object DiscoverTab : HomeTabKey

@Serializable
object LibraryTab : HomeTabKey

@Composable
fun HomeTabsNavContent(
    modifier: Modifier = Modifier,
    selectedIndex: Int,
    onGameClick: (Int) -> Unit = {},
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel
) {
    val homeBackStack = rememberNavBackStack(HomeTab)
    val discoverBackStack = rememberNavBackStack(DiscoverTab)
    val libraryBackStack = rememberNavBackStack(LibraryTab)

    // Animated content with slide and fade transitions
    AnimatedContent(
        targetState = selectedIndex,
        modifier = modifier,
			transitionSpec = tabTransitionSpec(),
        label = "tab_transition"
    ) { targetIndex ->
			val backStack = when (targetIndex) {
				0 -> homeBackStack
				1 -> discoverBackStack
				2 -> libraryBackStack
				else -> homeBackStack
			}

			NavDisplay(
				modifier = Modifier.fillMaxSize(),
				backStack = backStack,
				entryProvider = { key ->
					when (key) {
						is HomeTab -> NavEntry(key) {
							HomeTabRoot(
								onGameClick = onGameClick,
								snackbarHostState = snackbarHostState,
								viewModel = viewModel
							)
						}
						is DiscoverTab -> NavEntry(key) {
							DiscoverTabRoot(
								onGameClick = onGameClick,
								snackbarHostState = snackbarHostState,
								viewModel = viewModel
							)
						}
						is LibraryTab -> NavEntry(key) {
							LibraryTabRoot(onGameClick = onGameClick)
						}
						else -> error("Unknown key for Home tabs backstack: $key")
					}
				}
			)
    }
}

	private fun tabTransitionSpec(): AnimatedContentTransitionScope<Int>.() -> ContentTransform = {
		val slideDirection = if (targetState > initialState) 1 else -1
		val animationDuration = 200

		slideInHorizontally(
			initialOffsetX = { fullWidth -> slideDirection * fullWidth / 3 },
			animationSpec = tween(durationMillis = animationDuration)
		) + fadeIn(
			animationSpec = tween(durationMillis = animationDuration)
		) togetherWith slideOutHorizontally(
			targetOffsetX = { fullWidth -> -slideDirection * fullWidth / 3 },
			animationSpec = tween(durationMillis = animationDuration)
		) + fadeOut(
			animationSpec = tween(durationMillis = animationDuration)
		)
	}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)

@Composable
private fun HomeTabRoot(
    onGameClick: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel
) {
    val screenState = viewModel.screenState
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Surface),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Popular Games Section (Carousel)
            item {
                SectionHeader(
                    title = "Popular Games",
                    onSeeAllClick = { /* TODO */ }
                )

                when (val state = screenState.popularGames) {
                    is RequestState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(color = ButtonPrimary)
                        }
                    }
                    is RequestState.Success -> {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(state.data, key = { it.id }) { game ->
                                LargeGameCard(
                                    game = game,
                                    onClick = { onGameClick(game.id) }
                                )
                            }
                        }
                    }
                    is RequestState.Error -> {
                        ErrorSection(
                            message = state.message,
                            onRetry = { viewModel.retry() }
                        )
                    }
                    else -> {}
                }
            }

            // Upcoming Games Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Upcoming",
                    onSeeAllClick = { /* TODO */ }
                )

                when (val state = screenState.upcomingGames) {
                    is RequestState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(color = ButtonPrimary)
                        }
                    }
                    is RequestState.Success -> {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(state.data, key = { it.id }) { game ->
                                SmallGameCard(
                                    game = game,
                                    onClick = { onGameClick(game.id) }
                                )
                            }
                        }
                    }
                    is RequestState.Error -> {
                        ErrorSection(
                            message = state.message,
                            onRetry = { viewModel.retry() }
                        )
                    }
                    else -> {}
                }
            }

            // Playlist Recommendation Section (as list items)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Playlist Recommendation",
                    onSeeAllClick = { /* TODO */ }
                )
            }
            // Recommended Games Section
            when (val state = screenState.recommendedGames) {
                is RequestState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(color = ButtonPrimary)
                        }
                    }
                }
                is RequestState.Success -> {
                    items(
                        items = state.data.take(3),
                        key = { it.id }
                    ) { game ->
                        GameListItem(
                            game = game,
                            isInLibrary = viewModel.isGameInLibrary(game.id),
                            onClick = { onGameClick(game.id) },
                            onAddToLibrary = {
                                viewModel.addGameToLibrary(game)
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
                is RequestState.Error -> {
                    item {
                        ErrorSection(
                            message = state.message,
                            onRetry = { viewModel.retry() }
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiscoverTabRoot(
    onGameClick: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel
) {
    val screenState = viewModel.screenState
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Surface),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // New Releases Section
            item {
                SectionHeader(
                    title = "New Releases",
                    onSeeAllClick = { /* TODO */ }
                )

                when (val state = screenState.newReleases) {
                    is RequestState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(color = ButtonPrimary)
                        }
                    }
                    is RequestState.Success -> {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(state.data, key = { it.id }) { game ->
                                SmallGameCard(
                                    game = game,
                                    onClick = { onGameClick(game.id) }
                                )
                            }
                        }
                    }
                    is RequestState.Error -> {
                        ErrorSection(
                            message = state.message,
                            onRetry = { viewModel.retry() }
                        )
                    }
                    else -> {}
                }
            }

            // Recommended Games as list items
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Recommended For You",
                    onSeeAllClick = { /* TODO */ }
                )
            }

            when (val state = screenState.recommendedGames) {
                is RequestState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(color = ButtonPrimary)
                        }
                    }
                }
                is RequestState.Success -> {
                    items(
                        items = state.data,
                        key = { it.id }
                    ) { game ->
                        GameListItem(
                            game = game,
                            isInLibrary = viewModel.isGameInLibrary(game.id),
                            onClick = { onGameClick(game.id) },
                            onAddToLibrary = {
                                viewModel.addGameToLibrary(game)
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
                is RequestState.Error -> {
                    item {
                        ErrorSection(
                            message = state.message,
                            onRetry = { viewModel.retry() }
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun LibraryTabRoot(
    onGameClick: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MyGamesScreen(
            onNavigateBack = { /* Don't navigate back, we're in a tab */ },
            onGameClick = onGameClick
        )
    }
}

@Composable
private fun ErrorSection(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚠️",
            fontSize = 48.sp
        )
        Text(
            text = "Something went wrong",
            color = TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = message,
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        TextButton(
            onClick = onRetry,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "Retry",
                color = ButtonPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

