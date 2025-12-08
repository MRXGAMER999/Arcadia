package com.example.arcadia.presentation.screens.roast.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.example.arcadia.presentation.screens.roast.RevealTiming
import kotlinx.coroutines.delay

/**
 * Text component that reveals content character by character.
 * 
 * Requirements: 2.4, 12.3
 */
@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    reduceMotion: Boolean = false,
    onComplete: () -> Unit = {}
) {
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(text, reduceMotion) {
        if (reduceMotion) {
            displayedText = text
            onComplete()
        } else {
            displayedText = ""
            text.forEachIndexed { index, char ->
                displayedText = text.substring(0, index + 1)
                delay(RevealTiming.TYPEWRITER_CHAR_DELAY_MS)
            }
            onComplete()
        }
    }

    Text(
        text = displayedText,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign
    )
}
