package com.example.arcadia.presentation.screens.detailsScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.arcadia.presentation.components.AddGameSnackbar
import com.example.arcadia.presentation.components.UnsavedChangesSnackbar
import com.example.arcadia.presentation.components.game_rating.GameRatingSheet
import com.example.arcadia.presentation.screens.detailsScreen.components.ErrorState
import com.example.arcadia.presentation.screens.detailsScreen.components.GameDetailsContent
import com.example.arcadia.presentation.screens.detailsScreen.components.GameHeaderSection
import com.example.arcadia.presentation.screens.detailsScreen.components.LoadingState
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.util.RequestState
import org.koin.androidx.compose.koinViewModel

private val HEADER_HEIGHT_PORTRAIT = 350.dp
private val HEADER_HEIGHT_LANDSCAPE = 200.dp
private val TOOLBAR_HEIGHT = 64.dp

// Custom Saver for LazyListState
private val LazyListStateSaver = listSaver<LazyListState, Int>(
    save = { listOf(it.firstVisibleItemIndex, it.firstVisibleItemScrollOffset) },
    restore = { LazyListState(it[0], it[1]) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    gameId: Int,
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: DetailsScreenViewModel = koinViewModel()
    val listState = rememberSaveable(saver = LazyListStateSaver) { LazyListState() }
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    
    val headerHeight = if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
        HEADER_HEIGHT_LANDSCAPE
    } else {
        HEADER_HEIGHT_PORTRAIT
    }
    
    val headerHeightPx = with(density) { headerHeight.toPx() }
    val toolbarHeightPx = with(density) { TOOLBAR_HEIGHT.toPx() }

    // Calculate scroll progress for collapse effect
    val scrollOffset by remember {
        derivedStateOf {
            listState.firstVisibleItemScrollOffset
        }
    }
    
    val firstVisibleItemIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    // 0f = expanded, 1f = collapsed
    val collapseProgress by remember {
        derivedStateOf {
            if (firstVisibleItemIndex > 0) 1f
            else (scrollOffset / (headerHeightPx - toolbarHeightPx)).coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(gameId) {
        viewModel.loadGameDetails(gameId)
    }

    val uiState = viewModel.uiState
    val gamesInLibrary by viewModel.gamesInLibrary.collectAsState()
    val undoState by viewModel.undoState.collectAsState()
    
    val isInLibrary = gameId in gamesInLibrary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
    ) {
        when (val state = uiState.gameState) {
            is RequestState.Loading -> LoadingState(modifier = Modifier.fillMaxSize())
            is RequestState.Success -> {
                val game = state.data
                
                // Parallax Header Image (Behind content)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight)
                        .graphicsLayer {
                            translationY = -scrollOffset * 0.5f // Parallax effect
                            alpha = 1f - collapseProgress // Fade out as it collapses
                        }
                        .zIndex(0f)
                ) {
                    GameHeaderSection(game = game)
                }

                // Scrollable Content
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                ) {
                    // Transparent spacer for header
                    item { 
                        Spacer(modifier = Modifier.height(headerHeight)) 
                    }
                    
                    // Content Body
                    item {
                        // Background for body content to cover the image when scrolling up
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Surface.copy(alpha = 0f), // Gradient blending
                                            Surface,
                                            Surface
                                        ),
                                        startY = 0f,
                                        endY = 100f
                                    )
                                )
                        ) {
                            GameDetailsContent(
                                game = game,
                                isInLibrary = isInLibrary,
                                gameEntry = uiState.tempGameEntry,
                                addToLibraryState = uiState.addToLibraryState,
                                onAddToLibrary = { viewModel.onAddToLibraryClick() },
                                modifier = Modifier.background(Surface) // Solid background for content
                            )
                        }
                    }
                }

                // Sticky Collapsing Toolbar (On Top)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TOOLBAR_HEIGHT + 40.dp) // Extra height for status bar
                        .background(
                            Surface.copy(alpha = collapseProgress * 0.95f) // Fade in background
                        )
                        .zIndex(2f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .background(
                                    color = Surface.copy(alpha = 0.5f * (1f - collapseProgress)),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ButtonPrimary
                            )
                        }
                        
                        // Title fades in when collapsed
                        Text(
                            text = game.name,
                            color = Color.White.copy(alpha = collapseProgress),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .weight(1f)
                        )
                    }
                }
            }
            is RequestState.Error -> ErrorState(
                message = state.message,
                onRetry = { viewModel.retry() },
                modifier = Modifier.fillMaxSize()
            )
            else -> {}
        }
        
        // Game Rating Sheet
        if (uiState.showRatingSheet && uiState.tempGameEntry != null) {
            GameRatingSheet(
                game = uiState.tempGameEntry,
                isOpen = true,
                onDismiss = { viewModel.dismissRatingSheet() },
                onSave = { updatedEntry ->
                    viewModel.saveGameEntry(updatedEntry)
                },
                onRemove = if (isInLibrary) { game ->
                    viewModel.removeGameFromLibrary(game)
                } else null,
                isInLibrary = isInLibrary,
                onDismissWithUnsavedChanges = { unsavedGame ->
                    viewModel.showUnsavedChangesSnackbar(unsavedGame)
                },
                originalEntry = uiState.originalLibraryEntry
            )
        }
        
        // Success snackbar for add/update operations
        // Show undo only for new additions (addedEntry != null), not for updates
        val successState = uiState.addToLibraryState as? AddToLibraryState.Success
        val canUndoAdd = successState?.addedEntry != null
        
        AddGameSnackbar(
            visible = uiState.addToLibraryState is AddToLibraryState.Success,
            gameName = (uiState.gameState as? RequestState.Success)?.data?.name ?: "Game",
            message = successState?.message,
            onUndo = if (canUndoAdd) {{ viewModel.undoAdd() }} else null,
            onDismiss = { viewModel.dismissAddSnackbar() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
        
        // Unsaved changes snackbar
        UnsavedChangesSnackbar(
            visible = uiState.showUnsavedChangesSnackbar,
            onReopen = { viewModel.reopenWithUnsavedChanges() },
            onSave = { viewModel.saveUnsavedChanges() },
            onDismiss = { viewModel.dismissUnsavedChangesSnackbar() },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        // Undo removal snackbar
        AddGameSnackbar(
            visible = undoState.showSnackbar,
            gameName = undoState.item?.name ?: "Game",
            onUndo = { viewModel.undoRemoval() },
            onDismiss = { viewModel.dismissRemovalSnackbar() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
