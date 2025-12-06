package com.example.arcadia.presentation.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.arcadia.R
import com.example.arcadia.domain.model.GameListEntry
import com.example.arcadia.domain.model.ProfileSection
import com.example.arcadia.domain.model.ProfileSectionType
import com.example.arcadia.presentation.screens.searchScreen.components.SearchField
import com.example.arcadia.ui.theme.BebasNeueFont
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import androidx.compose.material3.Icon

private val CardBackground = Color(0xFF0A1F4D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSectionBottomSheet(
    libraryGames: List<GameListEntry>,
    existingSection: ProfileSection? = null,
    onDismiss: () -> Unit,
    onSave: (String, ProfileSectionType, List<Int>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(existingSection?.title ?: "") }
    var selectedType by remember { mutableStateOf(existingSection?.type ?: ProfileSectionType.SINGLE_GAME) }
    var selectedGameIds by remember { mutableStateOf(existingSection?.gameIds ?: emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter games based on search query
    val filteredGames = remember(libraryGames, searchQuery) {
        if (searchQuery.isBlank()) {
            libraryGames
        } else {
            libraryGames.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Surface) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(24.dp)
        ) {
            item {
                Text(
                    text = if (existingSection != null) "EDIT SECTION" else "ADD NEW SECTION",
                    fontSize = 28.sp,
                    fontFamily = BebasNeueFont,
                    color = TextSecondary,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Section Title") },
                    placeholder = { Text("e.g., Favorite Game of All Time") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ButtonPrimary,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                        focusedLabelColor = ButtonPrimary,
                        unfocusedLabelColor = TextSecondary.copy(alpha = 0.6f),
                        focusedTextColor = TextSecondary,
                        unfocusedTextColor = TextSecondary
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Section Type", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTypeChip(
                        text = "Single Game",
                        selected = selectedType == ProfileSectionType.SINGLE_GAME,
                        onClick = {
                            selectedType = ProfileSectionType.SINGLE_GAME
                            if (selectedGameIds.size > 1) selectedGameIds = selectedGameIds.take(1)
                        }
                    )
                    SectionTypeChip(
                        text = "Game List",
                        selected = selectedType == ProfileSectionType.GAME_LIST,
                        onClick = { selectedType = ProfileSectionType.GAME_LIST }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = "Select Games from Your Library", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Search field for filtering library games
                SearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search your library...",
                    modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (libraryGames.isEmpty()) {
                item {
                    Text(text = "Add games to your library first", fontSize = 14.sp, color = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 16.dp))
                }
            } else if (filteredGames.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    Text(
                        text = "No games found matching \"$searchQuery\"",
                        fontSize = 14.sp,
                        color = TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(filteredGames.chunked(3).size) { index ->
                    val rowGames = filteredGames.chunked(3)[index]
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        rowGames.forEach { game ->
                            val isSelected = game.rawgId in selectedGameIds
                            GameSelectionCard(
                                game = game,
                                isSelected = isSelected,
                                onClick = {
                                    selectedGameIds = if (isSelected) {
                                        selectedGameIds - game.rawgId
                                    } else {
                                        if (selectedType == ProfileSectionType.SINGLE_GAME) listOf(game.rawgId)
                                        else selectedGameIds + game.rawgId
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowGames.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { if (title.isNotBlank() && selectedGameIds.isNotEmpty()) onSave(title, selectedType, selectedGameIds) },
                    enabled = title.isNotBlank() && selectedGameIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonPrimary, disabledContainerColor = ButtonPrimary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = if (existingSection != null) "UPDATE SECTION" else "ADD SECTION", fontFamily = BebasNeueFont, fontSize = 20.sp, letterSpacing = 1.sp)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionTypeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) ButtonPrimary else Color.Transparent)
            .border(1.dp, if (selected) ButtonPrimary else TextSecondary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(text = text, fontSize = 14.sp, color = if (selected) Surface else TextSecondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GameSelectionCard(game: GameListEntry, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .aspectRatio(0.75f)
            .clickable(onClick = onClick)
            .then(if (isSelected) Modifier.border(2.dp, ButtonPrimary, RoundedCornerShape(12.dp)) else Modifier),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) ButtonPrimary.copy(alpha = 0.1f) else CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            AsyncImage(model = game.backgroundImage, contentDescription = game.name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 0f, endY = Float.POSITIVE_INFINITY)
                )
            )
            Text(
                text = game.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            )
            if (isSelected) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clip(CircleShape).background(ButtonPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painter = painterResource(id = R.drawable.finished_ic), contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
