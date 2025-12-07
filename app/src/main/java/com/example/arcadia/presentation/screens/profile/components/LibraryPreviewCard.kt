package com.example.arcadia.presentation.screens.profile.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.R
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent

private val CardBackground = Color(0xFF0A1F4D)
private val CardBorder = Color(0xFF1E3A8A)

@Composable
fun LibraryPreviewCard(
    games: List<GameListEntry>,
    totalGames: Int,
    onGameClick: (Int) -> Unit,
    onSeeAllClick: () -> Unit,
    expanded: Boolean,
    onExpandClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onExpandClick)
            ) {
                Icon(painter = painterResource(id = R.drawable.controller), contentDescription = null, tint = YellowAccent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "MY LIBRARY", fontSize = 16.sp, fontFamily = BebasNeueFont, color = YellowAccent, letterSpacing = 1.5.sp)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onSeeAllClick) {
                    Text(text = "See All ($totalGames)", fontSize = 13.sp, color = ButtonPrimary)
                }
            }

            AnimatedVisibility(
                visible = expanded || games.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (games.isEmpty()) {
                        Text(text = "Your library is empty", fontSize = 14.sp, color = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 12.dp))
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp), 
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(games) { game ->
                                GameCardBig(game = game, onClick = { onGameClick(game.rawgId) })
                            }
                        }
                    }
                }
            }
        }
    }
}
