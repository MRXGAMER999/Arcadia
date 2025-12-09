package com.example.arcadia.presentation.screens.friends.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.arcadia.domain.model.friend.FriendshipStatus
import com.example.arcadia.domain.model.friend.UserSearchResult
import com.example.arcadia.presentation.screens.friends.BottomSheetMode
import com.example.arcadia.presentation.screens.friends.QRCodeMode
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent
import com.example.arcadia.util.QRCodeUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors


/**
 * Bottom sheet for adding friends through search, QR code, or invite link.
 * 
 * Requirements: 3.1, 3.2, 4.1, 4.12, 5.1, 5.2
 * 
 * @param isVisible Whether the bottom sheet is visible
 * @param mode Current mode of the bottom sheet (OPTIONS, SEARCH, QR_CODE)
 * @param qrCodeMode Current QR code tab (MY_CODE or SCAN)
 * @param currentUserId The current user's ID for QR code generation
 * @param searchQuery Current search query
 * @param searchResults List of search results
 * @param isSearching Whether a search is in progress
 * @param searchHint Hint message for search field
 * @param isActionInProgress Whether a friend action is in progress
 * @param onDismiss Callback when the bottom sheet is dismissed
 * @param onModeChange Callback when the mode changes
 * @param onQRCodeModeChange Callback when the QR code tab changes
 * @param onSearchQueryChange Callback when the search query changes
 * @param onClearSearch Callback to clear the search
 * @param onSendFriendRequest Callback to send a friend request
 * @param onAcceptFriendRequest Callback to accept a friend request
 * @param onNavigateToProfile Callback to navigate to a user's profile
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddFriendsBottomSheet(
    isVisible: Boolean,
    mode: BottomSheetMode,
    qrCodeMode: QRCodeMode,
    currentUserId: String,
    searchQuery: String,
    searchResults: List<UserSearchResult>,
    isSearching: Boolean,
    searchHint: String?,
    isActionInProgress: Boolean,
    onDismiss: () -> Unit,
    onModeChange: (BottomSheetMode) -> Unit,
    onQRCodeModeChange: (QRCodeMode) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSendFriendRequest: (UserSearchResult) -> Unit,
    onAcceptFriendRequest: (UserSearchResult) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    if (!isVisible) return
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface
    ) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "BottomSheetModeAnimation"
        ) { currentMode ->
            when (currentMode) {
                BottomSheetMode.OPTIONS -> {
                    OptionsContent(
                        onSearchClick = { onModeChange(BottomSheetMode.SEARCH) },
                        onQRCodeClick = { onModeChange(BottomSheetMode.QR_CODE) },
                        onShareClick = { shareInviteLink(context, currentUserId) }
                    )
                }
                BottomSheetMode.SEARCH -> {
                    SearchContent(
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        isSearching = isSearching,
                        searchHint = searchHint,
                        isActionInProgress = isActionInProgress,
                        onBackClick = { onModeChange(BottomSheetMode.OPTIONS) },
                        onSearchQueryChange = onSearchQueryChange,
                        onClearSearch = onClearSearch,
                        onSendFriendRequest = onSendFriendRequest,
                        onAcceptFriendRequest = onAcceptFriendRequest,
                        onNavigateToProfile = onNavigateToProfile
                    )
                }
                BottomSheetMode.QR_CODE -> {
                    QRCodeContent(
                        qrCodeMode = qrCodeMode,
                        currentUserId = currentUserId,
                        onBackClick = { onModeChange(BottomSheetMode.OPTIONS) },
                        onQRCodeModeChange = onQRCodeModeChange,
                        onNavigateToProfile = onNavigateToProfile,
                        onDismiss = onDismiss
                    )
                }
                BottomSheetMode.HIDDEN -> {
                    // Should not be visible
                }
            }
        }
    }
}


// ==================== OPTIONS MODE ====================

/**
 * Content for the OPTIONS mode showing three options: Search, QR Code, Share Link.
 * Requirements: 3.1
 */
@Composable
private fun OptionsContent(
    onSearchClick: () -> Unit,
    onQRCodeClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        // Title
        Text(
            text = "ADD FRIENDS",
            fontSize = 24.sp,
            fontFamily = BebasNeueFont,
            color = TextSecondary,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Option cards
        OptionCard(
            icon = Icons.Default.Search,
            title = "Search by Username",
            subtitle = "Find friends by their username",
            onClick = onSearchClick
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OptionCard(
            icon = Icons.Default.QrCodeScanner,
            title = "Scan / Show QR Code",
            subtitle = "Scan a friend's code or show yours",
            onClick = onQRCodeClick
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OptionCard(
            icon = Icons.Default.Share,
            title = "Share Invite Link",
            subtitle = "Send your profile link to friends",
            onClick = onShareClick
        )
    }
}

/**
 * A card representing an option in the OPTIONS mode.
 */
@Composable
private fun OptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1B41)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ButtonPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ButtonPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}


// ==================== SEARCH MODE ====================

/**
 * Content for the SEARCH mode with search field and results.
 * Requirements: 3.2, 3.3, 3.4, 3.6, 3.7, 3.9-3.12
 */
@Composable
private fun SearchContent(
    searchQuery: String,
    searchResults: List<UserSearchResult>,
    isSearching: Boolean,
    searchHint: String?,
    isActionInProgress: Boolean,
    onBackClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSendFriendRequest: (UserSearchResult) -> Unit,
    onAcceptFriendRequest: (UserSearchResult) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header with back arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary
                )
            }
            
            Text(
                text = "SEARCH",
                fontSize = 24.sp,
                fontFamily = BebasNeueFont,
                color = TextSecondary,
                letterSpacing = 2.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by username...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = ButtonPrimary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ButtonPrimary,
                unfocusedBorderColor = Color(0xFF252B3B),
                focusedContainerColor = Color(0xFF0F1B41),
                unfocusedContainerColor = Color(0xFF0F1B41),
                cursorColor = ButtonPrimary,
                focusedTextColor = TextSecondary,
                unfocusedTextColor = TextSecondary,
                focusedPlaceholderColor = TextSecondary.copy(alpha = 0.5f),
                unfocusedPlaceholderColor = TextSecondary.copy(alpha = 0.5f)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search results or hint
        when {
            isSearching -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ButtonPrimary)
                }
            }
            searchHint != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = searchHint,
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            searchResults.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(
                        items = searchResults,
                        key = { it.userId }
                    ) { user ->
                        UserSearchResultItem(
                            user = user,
                            isActionInProgress = isActionInProgress,
                            onSendFriendRequest = { onSendFriendRequest(user) },
                            onAcceptFriendRequest = { onAcceptFriendRequest(user) },
                            onNavigateToProfile = { onNavigateToProfile(user.userId) }
                        )
                    }
                }
            }
        }
    }
}


/**
 * A list item displaying a user search result with action button.
 * Requirements: 3.9-3.12
 */
@Composable
private fun UserSearchResultItem(
    user: UserSearchResult,
    isActionInProgress: Boolean,
    onSendFriendRequest: () -> Unit,
    onAcceptFriendRequest: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onNavigateToProfile),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1B41)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF252B3B)),
                contentAlignment = Alignment.Center
            ) {
                if (user.profileImageUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(user.profileImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${user.username}'s avatar",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF252B3B)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = ButtonPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        error = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = ButtonPrimary.copy(alpha = 0.5f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = ButtonPrimary.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Username
            Text(
                text = user.username,
                color = TextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            
            // Action button based on friendship status
            FriendshipActionButton(
                status = user.friendshipStatus,
                isRecentlyDeclined = user.isRecentlyDeclined,
                isActionInProgress = isActionInProgress,
                onSendFriendRequest = onSendFriendRequest,
                onAcceptFriendRequest = onAcceptFriendRequest
            )
        }
    }
}


/**
 * Action button based on friendship status.
 * Requirements: 3.9-3.12
 */
@Composable
private fun FriendshipActionButton(
    status: FriendshipStatus,
    isRecentlyDeclined: Boolean,
    isActionInProgress: Boolean,
    onSendFriendRequest: () -> Unit,
    onAcceptFriendRequest: () -> Unit
) {
    when {
        // Recently declined - show disabled label
        isRecentlyDeclined -> {
            StatusLabel(
                text = "Recently Declined",
                color = TextSecondary.copy(alpha = 0.5f)
            )
        }
        // Already friends - show disabled label
        status == FriendshipStatus.FRIENDS -> {
            StatusLabel(
                text = "Already Friends",
                color = ButtonPrimary.copy(alpha = 0.7f)
            )
        }
        // Request sent - show disabled label
        status == FriendshipStatus.REQUEST_SENT -> {
            StatusLabel(
                text = "Request Sent",
                color = YellowAccent.copy(alpha = 0.7f)
            )
        }
        // Request received - show Accept button
        status == FriendshipStatus.REQUEST_RECEIVED -> {
            Button(
                onClick = onAcceptFriendRequest,
                enabled = !isActionInProgress,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimary,
                    contentColor = Surface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isActionInProgress) {
                    CircularProgressIndicator(
                        color = Surface,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Accept", fontSize = 12.sp)
                }
            }
        }
        // Not friends - show Add Friend button
        else -> {
            Button(
                onClick = onSendFriendRequest,
                enabled = !isActionInProgress,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimary,
                    contentColor = Surface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isActionInProgress) {
                    CircularProgressIndicator(
                        color = Surface,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * A status label for disabled states.
 */
@Composable
private fun StatusLabel(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}


// ==================== QR CODE MODE ====================

/**
 * Content for the QR_CODE mode with MY CODE and SCAN tabs.
 * Requirements: 4.1, 4.2, 4.3, 4.4-4.11
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun QRCodeContent(
    qrCodeMode: QRCodeMode,
    currentUserId: String,
    onBackClick: () -> Unit,
    onQRCodeModeChange: (QRCodeMode) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header with back arrow
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary
                )
            }
            
            Text(
                text = "QR CODE",
                fontSize = 24.sp,
                fontFamily = BebasNeueFont,
                color = TextSecondary,
                letterSpacing = 2.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Segmented tabs
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = qrCodeMode == QRCodeMode.MY_CODE,
                onClick = { onQRCodeModeChange(QRCodeMode.MY_CODE) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = ButtonPrimary,
                    activeContentColor = Surface,
                    inactiveContainerColor = Color(0xFF0F1B41),
                    inactiveContentColor = TextSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("MY CODE")
            }
            
            SegmentedButton(
                selected = qrCodeMode == QRCodeMode.SCAN,
                onClick = { onQRCodeModeChange(QRCodeMode.SCAN) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = ButtonPrimary,
                    activeContentColor = Surface,
                    inactiveContainerColor = Color(0xFF0F1B41),
                    inactiveContentColor = TextSecondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("SCAN")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Tab content
        when (qrCodeMode) {
            QRCodeMode.MY_CODE -> {
                MyCodeContent(currentUserId = currentUserId)
            }
            QRCodeMode.SCAN -> {
                ScanContent(
                    currentUserId = currentUserId,
                    onNavigateToProfile = onNavigateToProfile,
                    onDismiss = onDismiss
                )
            }
        }
    }
}


/**
 * Content for the MY CODE tab showing the user's QR code.
 * Requirements: 4.2, 4.3
 */
@Composable
private fun MyCodeContent(currentUserId: String) {
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isError by remember { mutableStateOf(false) }
    
    // Generate QR code
    LaunchedEffect(currentUserId) {
        qrCodeBitmap = QRCodeUtils.generateProfileQRCode(currentUserId, 512)
        isError = qrCodeBitmap == null
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            isError -> {
                // Error state
                Box(
                    modifier = Modifier
                        .size(256.dp)
                        .background(
                            color = Color(0xFF0F1B41),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Failed to generate QR code",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isError = false
                                qrCodeBitmap = QRCodeUtils.generateProfileQRCode(currentUserId, 512)
                                isError = qrCodeBitmap == null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonPrimary,
                                contentColor = Surface
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            qrCodeBitmap != null -> {
                // QR code display
                Box(
                    modifier = Modifier
                        .size(256.dp)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrCodeBitmap!!.asImageBitmap(),
                        contentDescription = "Your QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            else -> {
                // Loading state
                Box(
                    modifier = Modifier
                        .size(256.dp)
                        .background(
                            color = Color(0xFF0F1B41),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ButtonPrimary)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Let others scan this code to add you",
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}


/**
 * Content for the SCAN tab with camera preview and barcode scanning.
 * Requirements: 4.4-4.11
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ScanContent(
    currentUserId: String,
    onNavigateToProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            // Permission granted - show camera
            cameraPermissionState.status.isGranted -> {
                CameraPreviewWithScanner(
                    currentUserId = currentUserId,
                    onValidQRCodeScanned = { userId ->
                        onDismiss()
                        onNavigateToProfile(userId)
                    },
                    onOwnQRCodeScanned = {
                        Toast.makeText(context, "This is your own profile", Toast.LENGTH_SHORT).show()
                    },
                    onInvalidQRCodeScanned = {
                        Toast.makeText(context, "Not a valid Arcadia profile", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            // Should show rationale
            cameraPermissionState.status.shouldShowRationale -> {
                PermissionRationale(
                    message = "Camera access is needed to scan QR codes",
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }
            // Permission denied - show settings redirect
            else -> {
                PermissionDenied(
                    message = "Camera permission required. Tap to open settings.",
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }
        }
    }
}

/**
 * Permission rationale UI.
 * Requirements: 4.5
 */
@Composable
private fun PermissionRationale(
    message: String,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = ButtonPrimary,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonPrimary,
                contentColor = Surface
            )
        ) {
            Text("Grant Permission")
        }
    }
}

/**
 * Permission denied UI with settings redirect.
 * Requirements: 4.6
 */
@Composable
private fun PermissionDenied(
    message: String,
    onOpenSettings: () -> Unit,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F1B41),
                    contentColor = TextSecondary
                )
            ) {
                Text("Try Again")
            }
            
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimary,
                    contentColor = Surface
                )
            ) {
                Text("Open Settings")
            }
        }
    }
}


/**
 * Camera preview with ML Kit barcode scanning.
 * Requirements: 4.7, 4.8, 4.9, 4.10, 4.11
 */
@Composable
private fun CameraPreviewWithScanner(
    currentUserId: String,
    onValidQRCodeScanned: (String) -> Unit,
    onOwnQRCodeScanned: () -> Unit,
    onInvalidQRCodeScanned: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraError by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    
    // Track last scanned code to prevent duplicate callbacks
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    
    if (cameraError != null) {
        // Camera error state
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = cameraError ?: "Camera is currently unavailable. Please try again.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { cameraError = null },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimary,
                    contentColor = Surface
                )
            ) {
                Text("Retry")
            }
        }
        return
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        
                        val barcodeScanner = BarcodeScanning.getClient()
                        val analysisExecutor = Executors.newSingleThreadExecutor()
                        
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                    if (!isScanning) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }
                                    
                                    @androidx.camera.core.ExperimentalGetImage
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        
                                        barcodeScanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    val rawValue = barcode.rawValue ?: continue
                                                    
                                                    // Prevent duplicate processing
                                                    if (rawValue == lastScannedCode) continue
                                                    lastScannedCode = rawValue
                                                    
                                                    // Parse the QR code
                                                    val userId = QRCodeUtils.parseProfileUrl(rawValue)
                                                    
                                                    when {
                                                        userId == null -> {
                                                            // Invalid QR code
                                                            onInvalidQRCodeScanned()
                                                            // Reset after a delay to allow re-scanning
                                                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                                lastScannedCode = null
                                                            }, 2000)
                                                        }
                                                        userId == currentUserId -> {
                                                            // Own QR code
                                                            onOwnQRCodeScanned()
                                                            // Reset after a delay
                                                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                                lastScannedCode = null
                                                            }, 2000)
                                                        }
                                                        else -> {
                                                            // Valid QR code - stop scanning and navigate
                                                            isScanning = false
                                                            onValidQRCodeScanned(userId)
                                                        }
                                                    }
                                                    break
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }
                        
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        cameraError = "Camera is currently unavailable. Please try again."
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Scanning overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            // Scanning frame indicator
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .background(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text = "Point your camera at a QR code",
        color = TextSecondary.copy(alpha = 0.7f),
        fontSize = 14.sp,
        textAlign = TextAlign.Center
    )
}


// ==================== SHARE INVITE LINK ====================

/**
 * Opens the Android share sheet with the user's profile URL.
 * Requirements: 5.1, 5.2
 */
private fun shareInviteLink(context: Context, userId: String) {
    val inviteMessage = QRCodeUtils.createInviteMessage(userId)
    
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, inviteMessage)
        type = "text/plain"
    }
    
    val shareIntent = Intent.createChooser(sendIntent, "Share your Arcadia profile")
    context.startActivity(shareIntent)
}
