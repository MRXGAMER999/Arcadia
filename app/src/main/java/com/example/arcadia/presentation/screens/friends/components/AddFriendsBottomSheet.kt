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
import com.example.arcadia.ui.theme.ResponsiveDimens
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent
import com.example.arcadia.ui.theme.rememberResponsiveDimens
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
    actioningUserId: String? = null,
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
                        actioningUserId = actioningUserId,
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
 * Responsive design that adapts to all screen sizes.
 * Requirements: 3.1
 */
@Composable
private fun OptionsContent(
    onSearchClick: () -> Unit,
    onQRCodeClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val dimens = rememberResponsiveDimens()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.horizontalPadding)
            .padding(bottom = dimens.paddingXLarge)
    ) {
        // Title
        Text(
            text = "ADD FRIENDS",
            fontSize = dimens.fontSizeXLarge,
            fontFamily = BebasNeueFont,
            color = TextSecondary,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = dimens.paddingXLarge)
        )
        
        // Option cards
        OptionCard(
            icon = Icons.Default.Search,
            title = "Search by Username",
            subtitle = "Find friends by their username",
            onClick = onSearchClick
        )
        
        Spacer(modifier = Modifier.height(dimens.itemSpacing))
        
        OptionCard(
            icon = Icons.Default.QrCodeScanner,
            title = "Scan / Show QR Code",
            subtitle = "Scan a friend's code or show yours",
            onClick = onQRCodeClick
        )
        
        Spacer(modifier = Modifier.height(dimens.itemSpacing))
        
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
 * Responsive design with adaptive sizing.
 */
@Composable
private fun OptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val dimens = rememberResponsiveDimens()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(dimens.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1B41)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.cardHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(dimens.avatarMedium)
                    .clip(CircleShape)
                    .background(ButtonPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ButtonPrimary,
                    modifier = Modifier.size(dimens.iconMedium)
                )
            }
            
            Spacer(modifier = Modifier.width(dimens.itemSpacing))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = TextSecondary,
                    fontSize = dimens.fontSizeMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = dimens.fontSizeSmall
                )
            }
        }
    }
}


// ==================== SEARCH MODE ====================

/**
 * Content for the SEARCH mode with search field and results.
 * Responsive design that adapts to all screen sizes.
 * Requirements: 3.2, 3.3, 3.4, 3.6, 3.7, 3.9-3.12
 */
@Composable
private fun SearchContent(
    searchQuery: String,
    searchResults: List<UserSearchResult>,
    isSearching: Boolean,
    searchHint: String?,
    isActionInProgress: Boolean,
    actioningUserId: String? = null,
    onBackClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSendFriendRequest: (UserSearchResult) -> Unit,
    onAcceptFriendRequest: (UserSearchResult) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val dimens = rememberResponsiveDimens()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.horizontalPadding)
            .padding(bottom = dimens.paddingXLarge)
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
                    tint = TextSecondary,
                    modifier = Modifier.size(dimens.iconMedium)
                )
            }
            
            Text(
                text = "SEARCH",
                fontSize = dimens.fontSizeXLarge,
                fontFamily = BebasNeueFont,
                color = TextSecondary,
                letterSpacing = 2.sp
            )
        }
        
        Spacer(modifier = Modifier.height(dimens.paddingLarge))
        
        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text(
                    "Search by username...",
                    fontSize = dimens.fontSizeMedium
                ) 
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = ButtonPrimary,
                    modifier = Modifier.size(dimens.iconMedium)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = TextSecondary,
                            modifier = Modifier.size(dimens.iconMedium)
                        )
                    }
                }
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = dimens.fontSizeMedium),
            shape = RoundedCornerShape(dimens.cardCornerRadius),
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
        
        Spacer(modifier = Modifier.height(dimens.paddingLarge))
        
        // Search results or hint - responsive height
        val contentHeight = if (dimens.isExpanded) 350.dp else if (dimens.isMedium) 280.dp else 200.dp
        val maxListHeight = if (dimens.isExpanded) 500.dp else if (dimens.isMedium) 450.dp else 400.dp
        
        when {
            isSearching -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(contentHeight),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ButtonPrimary,
                        modifier = Modifier.size(dimens.iconLarge)
                    )
                }
            }
            searchHint != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(contentHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = searchHint,
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = dimens.fontSizeSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
            searchResults.isNotEmpty() -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight),
                    verticalArrangement = Arrangement.spacedBy(dimens.paddingSmall)
                ) {
                    items(
                        items = searchResults,
                        key = { it.userId }
                    ) { user ->
                        UserSearchResultItem(
                            user = user,
                            isActionInProgress = isActionInProgress && actioningUserId == user.userId,
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
 * Responsive design with adaptive sizing.
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
    val dimens = rememberResponsiveDimens()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onNavigateToProfile),
        shape = RoundedCornerShape(dimens.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F1B41)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.cardHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(dimens.avatarMedium)
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
                            .size(dimens.avatarMedium)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .size(dimens.avatarMedium)
                                    .background(Color(0xFF252B3B)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = ButtonPrimary,
                                    modifier = Modifier.size(dimens.iconMedium)
                                )
                            }
                        },
                        error = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = ButtonPrimary.copy(alpha = 0.5f),
                                modifier = Modifier.size(dimens.avatarMedium * 0.6f)
                            )
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = ButtonPrimary.copy(alpha = 0.5f),
                        modifier = Modifier.size(dimens.avatarMedium * 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(dimens.itemSpacing))
            
            // Username
            Text(
                text = user.username,
                color = TextSecondary,
                fontSize = dimens.fontSizeMedium,
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
 * Responsive design with adaptive sizing.
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
    val dimens = rememberResponsiveDimens()
    
    when {
        // Recently declined - show disabled label
        isRecentlyDeclined -> {
            StatusLabel(
                text = if (dimens.isCompact && dimens.screenWidth < 380.dp) "Declined" else "Recently Declined",
                color = TextSecondary.copy(alpha = 0.5f)
            )
        }
        // Already friends - show disabled label
        status == FriendshipStatus.FRIENDS -> {
            StatusLabel(
                text = if (dimens.isCompact && dimens.screenWidth < 380.dp) "Friends" else "Already Friends",
                color = ButtonPrimary.copy(alpha = 0.7f)
            )
        }
        // Request sent - show disabled label
        status == FriendshipStatus.REQUEST_SENT -> {
            StatusLabel(
                text = if (dimens.isCompact && dimens.screenWidth < 380.dp) "Sent" else "Request Sent",
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
                shape = RoundedCornerShape(dimens.paddingSmall),
                modifier = Modifier.height(dimens.buttonHeightSmall)
            ) {
                if (isActionInProgress) {
                    CircularProgressIndicator(
                        color = Surface,
                        modifier = Modifier.size(dimens.iconSmall),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(dimens.iconSmall)
                    )
                    Spacer(modifier = Modifier.width(dimens.paddingXSmall))
                    Text("Accept", fontSize = dimens.fontSizeSmall)
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
                shape = RoundedCornerShape(dimens.paddingSmall),
                modifier = Modifier.height(dimens.buttonHeightSmall)
            ) {
                if (isActionInProgress) {
                    CircularProgressIndicator(
                        color = Surface,
                        modifier = Modifier.size(dimens.iconSmall),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(dimens.iconSmall)
                    )
                    Spacer(modifier = Modifier.width(dimens.paddingXSmall))
                    Text("Add", fontSize = dimens.fontSizeSmall)
                }
            }
        }
    }
}

/**
 * A status label for disabled states.
 * Responsive design with adaptive sizing.
 */
@Composable
private fun StatusLabel(
    text: String,
    color: Color
) {
    val dimens = rememberResponsiveDimens()
    
    Box(
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(dimens.paddingSmall)
            )
            .padding(
                horizontal = dimens.paddingMedium,
                vertical = dimens.paddingSmall
            )
    ) {
        Text(
            text = text,
            color = color,
            fontSize = dimens.fontSizeSmall,
            fontWeight = FontWeight.Medium
        )
    }
}


// ==================== QR CODE MODE ====================

/**
 * Content for the QR_CODE mode with MY CODE and SCAN tabs.
 * Responsive design that adapts to all screen sizes.
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
    val dimens = rememberResponsiveDimens()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.horizontalPadding)
            .padding(bottom = dimens.paddingXLarge)
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
                    tint = TextSecondary,
                    modifier = Modifier.size(dimens.iconMedium)
                )
            }
            
            Text(
                text = "QR CODE",
                fontSize = dimens.fontSizeXLarge,
                fontFamily = BebasNeueFont,
                color = TextSecondary,
                letterSpacing = 2.sp
            )
        }
        
        Spacer(modifier = Modifier.height(dimens.paddingLarge))
        
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
                    modifier = Modifier.size(dimens.iconSmall)
                )
                Spacer(modifier = Modifier.width(dimens.paddingSmall))
                Text("MY CODE", fontSize = dimens.fontSizeSmall)
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
                    modifier = Modifier.size(dimens.iconSmall)
                )
                Spacer(modifier = Modifier.width(dimens.paddingSmall))
                Text("SCAN", fontSize = dimens.fontSizeSmall)
            }
        }
        
        Spacer(modifier = Modifier.height(dimens.sectionSpacing))
        
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
 * Responsive design with adaptive QR code sizing.
 * Requirements: 4.2, 4.3
 */
@Composable
private fun MyCodeContent(currentUserId: String) {
    val dimens = rememberResponsiveDimens()
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isError by remember { mutableStateOf(false) }
    
    // Responsive QR code size
    val qrCodeSize = when {
        dimens.isExpanded -> 320.dp
        dimens.isMedium -> 280.dp
        else -> minOf(dimens.screenWidth * 0.65f, 256.dp)
    }
    
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
                        .size(qrCodeSize)
                        .background(
                            color = Color(0xFF0F1B41),
                            shape = RoundedCornerShape(dimens.cardCornerRadius)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(dimens.paddingLarge)
                    ) {
                        Text(
                            text = "Failed to generate QR code",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = dimens.fontSizeSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(dimens.paddingLarge))
                        Button(
                            onClick = {
                                isError = false
                                qrCodeBitmap = QRCodeUtils.generateProfileQRCode(currentUserId, 512)
                                isError = qrCodeBitmap == null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonPrimary,
                                contentColor = Surface
                            ),
                            modifier = Modifier.height(dimens.buttonHeightSmall)
                        ) {
                            Text("Retry", fontSize = dimens.fontSizeSmall)
                        }
                    }
                }
            }
            qrCodeBitmap != null -> {
                // QR code display
                Box(
                    modifier = Modifier
                        .size(qrCodeSize)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(dimens.cardCornerRadius)
                        )
                        .padding(dimens.paddingLarge),
                    contentAlignment = Alignment.Center
                ) {
                    qrCodeBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Your QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
            else -> {
                // Loading state
                Box(
                    modifier = Modifier
                        .size(qrCodeSize)
                        .background(
                            color = Color(0xFF0F1B41),
                            shape = RoundedCornerShape(dimens.cardCornerRadius)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ButtonPrimary,
                        modifier = Modifier.size(dimens.iconLarge)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(dimens.paddingLarge))
        
        Text(
            text = "Let others scan this code to add you",
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = dimens.fontSizeSmall,
            textAlign = TextAlign.Center
        )
    }
}


/**
 * Content for the SCAN tab with camera preview and barcode scanning.
 * Responsive design with adaptive sizing.
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
 * Responsive design with adaptive sizing.
 * Requirements: 4.5
 */
@Composable
private fun PermissionRationale(
    message: String,
    onRequestPermission: () -> Unit
) {
    val dimens = rememberResponsiveDimens()
    val contentHeight = if (dimens.isExpanded) 350.dp else if (dimens.isMedium) 320.dp else 300.dp
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(contentHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = ButtonPrimary,
            modifier = Modifier.size(dimens.avatarLarge)
        )
        
        Spacer(modifier = Modifier.height(dimens.paddingLarge))
        
        Text(
            text = message,
            color = TextSecondary,
            fontSize = dimens.fontSizeMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = dimens.paddingXLarge)
        )
        
        Spacer(modifier = Modifier.height(dimens.sectionSpacing))
        
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonPrimary,
                contentColor = Surface
            ),
            modifier = Modifier.height(dimens.buttonHeightMedium)
        ) {
            Text("Grant Permission", fontSize = dimens.fontSizeMedium)
        }
    }
}

/**
 * Permission denied UI with settings redirect.
 * Responsive design with adaptive sizing.
 * Requirements: 4.6
 */
@Composable
private fun PermissionDenied(
    message: String,
    onOpenSettings: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val dimens = rememberResponsiveDimens()
    val contentHeight = if (dimens.isExpanded) 350.dp else if (dimens.isMedium) 320.dp else 300.dp
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(contentHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(dimens.avatarLarge)
        )
        
        Spacer(modifier = Modifier.height(dimens.paddingLarge))
        
        Text(
            text = message,
            color = TextSecondary,
            fontSize = dimens.fontSizeMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = dimens.paddingXLarge)
        )
        
        Spacer(modifier = Modifier.height(dimens.sectionSpacing))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.itemSpacing)
        ) {
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F1B41),
                    contentColor = TextSecondary
                ),
                modifier = Modifier.height(dimens.buttonHeightSmall)
            ) {
                Text("Try Again", fontSize = dimens.fontSizeSmall)
            }
            
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimary,
                    contentColor = Surface
                ),
                modifier = Modifier.height(dimens.buttonHeightSmall)
            ) {
                Text("Open Settings", fontSize = dimens.fontSizeSmall)
            }
        }
    }
}


/**
 * Camera preview with ML Kit barcode scanning.
 * Responsive design with adaptive sizing.
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
    val dimens = rememberResponsiveDimens()
    var cameraError by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    
    // Responsive camera preview height
    val cameraHeight = when {
        dimens.isExpanded -> 380.dp
        dimens.isMedium -> 340.dp
        else -> 300.dp
    }
    
    // Track last scanned code to prevent duplicate callbacks
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    
    if (cameraError != null) {
        // Camera error state
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(cameraHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = cameraError ?: "Camera is currently unavailable. Please try again.",
                color = TextSecondary,
                fontSize = dimens.fontSizeSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = dimens.paddingXLarge)
            )
            
            Spacer(modifier = Modifier.height(dimens.paddingLarge))
            
            Button(
                onClick = { cameraError = null },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimary,
                    contentColor = Surface
                ),
                modifier = Modifier.height(dimens.buttonHeightSmall)
            ) {
                Text("Retry", fontSize = dimens.fontSizeSmall)
            }
        }
        return
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cameraHeight)
            .clip(RoundedCornerShape(dimens.cardCornerRadius))
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
    
    Spacer(modifier = Modifier.height(dimens.paddingLarge))
    
    Text(
        text = "Point your camera at a QR code",
        color = TextSecondary.copy(alpha = 0.7f),
        fontSize = dimens.fontSizeSmall,
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
