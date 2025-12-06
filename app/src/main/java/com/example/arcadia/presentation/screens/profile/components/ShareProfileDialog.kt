package com.example.arcadia.presentation.screens.profile.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.ProfileSection
import com.example.arcadia.presentation.screens.profile.ProfileState
import com.example.arcadia.presentation.screens.profile.ProfileStatsState
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary

private val CardBackground = Color(0xFF0A1F4D)

@Composable
fun ShareProfileDialog(
    profileState: ProfileState,
    statsState: ProfileStatsState,
    libraryGames: List<GameListEntry>,
    customSections: List<ProfileSection>,
    onDismiss: () -> Unit,
    onShareText: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = {
            Text(text = "SHARE PROFILE", fontFamily = BebasNeueFont, fontSize = 24.sp, color = TextSecondary, letterSpacing = 2.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Share your gaming profile with friends:", color = TextSecondary.copy(alpha = 0.8f))
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "• ${statsState.totalGames} games in library", color = TextSecondary.copy(alpha = 0.7f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "• ${customSections.size} custom sections", color = TextSecondary.copy(alpha = 0.7f), fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "• Profile Link included", color = ButtonPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onShareText, 
                colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Profile Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel", color = TextSecondary) }
        }
    )
}

fun shareProfileAsText(
    context: Context,
    profileState: ProfileState,
    statsState: ProfileStatsState,
    libraryGames: List<GameListEntry>,
    customSections: List<ProfileSection>
) {
    val shareText = buildString {
        appendLine("🎮 Check out ${profileState.name}'s Gaming Profile on Arcadia!")
        appendLine("https://mrxgamer999.github.io/Arcadia/profile?id=${profileState.id}")
        appendLine()
        
        if (profileState.username.isNotEmpty()) appendLine("@${profileState.username}")
        appendLine()

        appendLine("📊 Gaming Stats:")
        appendLine("• Total Games: ${statsState.totalGames}")
        appendLine("• Finished: ${statsState.finishedGames}")
        appendLine("• Hours Played: ${statsState.hoursPlayed}h")
        if (statsState.avgRating > 0) appendLine("• Avg Rating: ${String.format("%.1f", statsState.avgRating)}/10")
        appendLine()

        if (customSections.isNotEmpty()) {
            customSections.forEach { section ->
                val sectionGames = libraryGames.filter { it.rawgId in section.gameIds }
                appendLine("⭐ ${section.title}:")
                sectionGames.take(3).forEach { game ->
                    val ratingStr = game.rating?.let { " (${String.format("%.1f", it)}/10)" } ?: ""
                    appendLine("  • ${game.name}$ratingStr")
                }
                if (sectionGames.size > 3) appendLine("  ...and ${sectionGames.size - 3} more")
                appendLine()
            }
        }

        val topGames = libraryGames.filter { it.rating != null && it.rating > 0 }.sortedByDescending { it.rating }.take(3)
        if (topGames.isNotEmpty()) {
            appendLine("🏆 Top Rated Games:")
            topGames.forEach { game -> appendLine("  • ${game.name} (${String.format("%.1f", game.rating)}/10)") }
            appendLine()
        }

        appendLine("📱 Download Arcadia to view full profile and library!")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share your gaming profile"))
}
