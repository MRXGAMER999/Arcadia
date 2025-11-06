package com.example.arcadia.presentation.screens.searchScreen

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.R
import kotlinx.coroutines.delay

// 🎨 Colors
val Background = Color(0xFF00123B)
val FieldTxt = Color(0xFFDCDCDC)
val FieldBg = Color(0xFF00123B)
val ButtonBlue = Color(0xFF62B4DA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBackClick: (() -> Unit)? = null // optional back callback
) {
    var query by remember { mutableStateOf("") }

    val mockGames = listOf(
        Game("Ori and the Blind Forest", "Platform, Puzzle, Adventure", "2015", R.drawable.sample_cover),
        Game("Ori and the Will of the Wisps", "Platform, Adventure", "2020", R.drawable.sample_cover),
        Game("Celeste", "Platform, Indie", "2018", R.drawable.sample_cover),
        Game("Hollow Knight", "Metroidvania, Adventure", "2017", R.drawable.sample_cover),
        Game("Hollow Knight:SilkSOng", "Metroidvania, Adventure", "2017", R.drawable.sample_cover)
    )

    var addedGames by remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {

        // 🔙 Back + Search Field Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { onBackClick?.invoke() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = FieldTxt
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search games...", color = FieldTxt.copy(alpha = 0.5f)) },
                singleLine = true,
                shape = RoundedCornerShape(50.dp), // Rounded pill shape
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ButtonBlue,
                    unfocusedBorderColor = FieldTxt.copy(alpha = 0.4f),
                    focusedContainerColor = FieldBg,
                    unfocusedContainerColor = FieldBg,
                    cursorColor = ButtonBlue
                ),
                textStyle = TextStyle(color = FieldTxt)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔎 Filter results
        val filteredGames = remember(query) {
            mockGames.filter { it.title.contains(query, ignoreCase = true) }
        }

        LazyColumn {
            items(filteredGames) { game ->
                SearchResultCard(
                    title = game.title,
                    genre = game.genre,
                    year = game.year,
                    imageRes = game.imageRes,
                    isAdded = addedGames.contains(game.title),
                    onToggle = {
                        addedGames = if (addedGames.contains(game.title)) {
                            addedGames - game.title
                        } else {
                            addedGames + game.title
                        }
                    }
                )

                Divider(
                    color = Color.White.copy(alpha = 0.2f),
                    thickness = 0.6.dp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}



@Composable
fun SearchResultCard(
    title: String,
    genre: String,
    year: String,
    imageRes: Int,
    isAdded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 🎨 Game image
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp))
        )

        Spacer(modifier = Modifier.width(10.dp))

        // 📝 Game info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = title, color = FieldTxt, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(text = genre, color = FieldTxt.copy(alpha = 0.6f), fontSize = 14.sp)
            Text(text = year, color = FieldTxt.copy(alpha = 0.4f), fontSize = 12.sp)
        }

        // ➕ / ✅ Toggle button
        IconButton(
            onClick = { onToggle() },
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = if (isAdded) ButtonBlue.copy(alpha = 0.3f)
                    else Color.Transparent,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = if (isAdded) Icons.Default.Check else Icons.Default.Add,
                contentDescription = "Add or Remove",
                tint = ButtonBlue,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// 🧩 Game model
data class Game(
    val title: String,
    val genre: String,
    val year: String,
    val imageRes: Int
)


@Preview(
    showBackground = true,
    showSystemUi = true,
    name = "Search Screen Preview"
)
@Composable
fun SearchScreenPreview() {
    SearchScreen()
}


