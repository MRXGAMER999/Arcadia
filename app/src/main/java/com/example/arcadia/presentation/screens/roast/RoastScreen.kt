package com.example.arcadia.presentation.screens.roast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.presentation.screens.roast.components.BadgeSelector
import com.example.arcadia.presentation.screens.roast.components.RoastErrorState
import com.example.arcadia.presentation.screens.roast.components.RoastLoadingState
import com.example.arcadia.presentation.screens.roast.components.RoastResultCard
import com.example.arcadia.presentation.screens.roast.components.determineErrorType
import com.example.arcadia.presentation.screens.roast.util.RoastShareHelper
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Main Roast Screen composable with empty state, loading state, results state, 
 * error state, and regenerate confirmation dialog.
 * 
 * Requirements: 2.1 - Empty state with "Roast Me" button
 * Requirements: 2.2 - Loading state with fun rotating messages
 * Requirements: 2.3 - Results state with roast content
 * Requirements: 2.4 - Timestamp display
 * Requirements: 2.5 - Share and Regenerate action buttons
 * Requirements: 3.1, 3.2, 3.3 - Regeneration confirmation dialog
 * Requirements: 13.1, 13.2, 13.3 - Error handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoastScreen(
    targetUserId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoastViewModel = koinViewModel { parametersOf(targetUserId) }
) {
    val state = viewModel.state
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            RoastTopBar(
                isFriendRoast = targetUserId != null,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF1A0A0A),
                    Color(0xFF0A1929)
                )
            )
        )
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A0A0A),
                            Color(0xFF0A1929)
                        )
                    )
                )
        ) {
            when {
                // Loading state (Requirements: 2.2)
                state.isLoading -> {
                    RoastLoadingState(message = state.loadingMessage)
                }
                
                // Error state (Requirements: 13.1, 13.2, 13.3)
                state.error != null -> {
                    RoastErrorState(
                        errorMessage = state.error,
                        errorType = determineErrorType(state.error, state.hasInsufficientStats),
                        onRetry = { viewModel.retry() }
                    )
                }
                
                // Results state (Requirements: 2.3, 2.4, 2.5, 8.2, 8.3, 8.4)
                state.roast != null -> {
                    RoastResultsContent(
                        state = state,
                        onRegenerateClick = { viewModel.regenerateRoast() },
                        onShareClick = { 
                            // Requirements: 11.1 - Share roast content
                            RoastShareHelper.shareRoast(context, state.roast)
                        },
                        onBadgeClick = { viewModel.selectBadge(it) },
                        onSaveBadges = { viewModel.saveFeaturedBadges() }
                    )
                }
                
                // Empty state (Requirements: 2.1)
                else -> {
                    RoastEmptyState(
                        hasInsufficientStats = state.hasInsufficientStats,
                        onRoastMeClick = { viewModel.generateRoast() }
                    )
                }
            }
            
            // Regenerate confirmation dialog (Requirements: 3.1, 3.2, 3.3)
            if (state.isRegenerateDialogVisible) {
                RegenerateConfirmationDialog(
                    onKeepCurrent = { viewModel.hideRegenerateDialog() },
                    onRoastAgain = { viewModel.confirmRegenerate() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoastTopBar(
    isFriendRoast: Boolean,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = if (isFriendRoast) "Roast Your Friend" else "Gaming Roast",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}


/**
 * Empty state content with enticing prompt and "Roast Me" button.
 * Requirements: 2.1
 */
@Composable
private fun RoastEmptyState(
    hasInsufficientStats: Boolean,
    onRoastMeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔥",
            fontSize = 80.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Ready to Get Roasted?",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = if (hasInsufficientStats) {
                "Add more games to your library first! You need at least 3 games and 5 hours played."
            } else {
                "Let our AI analyze your gaming habits and deliver a personalized roast you'll never forget."
            },
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onRoastMeClick,
            enabled = !hasInsufficientStats,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B35),
                disabledContainerColor = Color(0xFF444444)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "🔥 Roast Me",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

/**
 * Results content with roast card, badges, and action buttons.
 * Requirements: 2.3, 2.4, 2.5, 8.2, 8.3, 8.4
 */
@Composable
private fun RoastResultsContent(
    state: RoastScreenState,
    onRegenerateClick: () -> Unit,
    onShareClick: () -> Unit,
    onBadgeClick: (com.example.arcadia.domain.model.ai.Badge) -> Unit,
    onSaveBadges: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Roast result card
        RoastResultCard(
            roast = state.roast!!,
            generatedAt = state.generatedAt
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Badge selector (if badges are available)
        // Requirements: 8.2, 8.3, 8.4
        if (state.badges.isNotEmpty()) {
            BadgeSelector(
                badges = state.badges,
                selectedBadges = state.selectedBadges,
                onBadgeClick = onBadgeClick,
                onSaveBadges = onSaveBadges,
                isSaving = state.isSavingBadges,
                isSaved = state.badgesSaved,
                isFriendRoast = state.targetUserId != null,
                maxBadges = RoastScreenState.MAX_FEATURED_BADGES
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Action buttons (Requirements: 2.5)
        ActionButtons(
            onShareClick = onShareClick,
            onRegenerateClick = onRegenerateClick
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}


/**
 * Action buttons for Share and Regenerate.
 * Requirements: 2.5
 */
@Composable
private fun ActionButtons(
    onShareClick: () -> Unit,
    onRegenerateClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Share button
        Button(
            onClick = onShareClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4ADE80)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Share",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Regenerate button
        Button(
            onClick = onRegenerateClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF6B35)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Regenerate",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Confirmation dialog for regenerating a roast.
 * Requirements: 3.1, 3.2, 3.3
 */
@Composable
private fun RegenerateConfirmationDialog(
    onKeepCurrent: () -> Unit,
    onRoastAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepCurrent,
        containerColor = Color(0xFF1E2A47),
        title = {
            Text(
                text = "Generate New Roast?",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "This will replace your current roast. Are you sure you want a fresh serving of burns?",
                color = Color.White.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            Button(
                onClick = onRoastAgain,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B35)
                )
            ) {
                Text(
                    text = "🔥 Roast Me Again",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepCurrent) {
                Text(
                    text = "Keep Current",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    )
}
