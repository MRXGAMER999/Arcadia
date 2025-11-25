package com.example.arcadia.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.R
import com.example.arcadia.domain.model.Game
import com.example.arcadia.domain.model.GameStatus
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickStatusSheet(
    game: Game,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onStatusSelected: (GameStatus) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (isOpen) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = Color(0xFF0A1929),
            contentColor = TextSecondary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Game name title
                Text(
                    text = game.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = "Choose a status",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Status chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GameStatus.entries.forEach { status ->
                        StatusChip(
                            status = status,
                            onClick = {
                                onStatusSelected(status)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    status: GameStatus,
    onClick: () -> Unit
) {
    val statusColor = getStatusColor(status)
    val statusIcon = getStatusIcon(status)
    
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "ChipScale_${status.name}"
    )

    FilterChip(
        selected = false,
        onClick = onClick,
        label = {
            Text(
                text = status.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(id = statusIcon),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = statusColor.copy(alpha = 0.15f),
            labelColor = statusColor,
            iconColor = statusColor
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = false,
            borderColor = statusColor.copy(alpha = 0.5f),
            borderWidth = 1.dp
        ),
        modifier = Modifier.scale(scale)
    )
}

private fun getStatusIcon(status: GameStatus): Int {
    return when (status) {
        GameStatus.FINISHED -> R.drawable.finished_ic
        GameStatus.PLAYING -> R.drawable.playing_ic
        GameStatus.DROPPED -> R.drawable.dropped_ic
        GameStatus.ON_HOLD -> R.drawable.on_hold_ic
        GameStatus.WANT -> R.drawable.want_ic
    }
}

private fun getStatusColor(status: GameStatus): Color {
    return when (status) {
        GameStatus.FINISHED -> Color(0xFFFBB02E)
        GameStatus.PLAYING -> Color(0xFFD34ECE)
        GameStatus.DROPPED -> Color(0xFFBA5C3E)
        GameStatus.ON_HOLD -> Color(0xFF62B4DA)
        GameStatus.WANT -> Color(0xFF3F77CC)
    }
}
