package com.example.arcadia.presentation.screens.analytics.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.R
import com.example.arcadia.domain.repository.GeminiRepository
import com.example.arcadia.presentation.screens.analytics.AnalyticsState
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent

/**
 * AI Insights section containing the main AI analysis card.
 */
@Composable
fun AIInsightsSection(state: AnalyticsState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2A47).copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box {
            // Magical gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                ButtonPrimary.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(500f, -100f),
                            radius = 600f
                        )
                    )
            )

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ai_analysis),
                        contentDescription = "AI Analysis",
                        modifier = Modifier.size(24.dp),
                        tint = ButtonPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "AI Insights",
                        color = TextSecondary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.isLoadingInsights) {
                        Spacer(modifier = Modifier.width(12.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ButtonPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    state.isLoadingInsights -> {
                        LoadingInsightsState()
                    }
                    state.insightsError != null -> {
                        ErrorInsightsState(error = state.insightsError)
                    }
                    state.aiInsights != null -> {
                        AIInsightsContent(insights = state.aiInsights)
                    }
                    state.totalGames == 0 -> {
                        EmptyInsightsState()
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingInsightsState() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha = infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    ButtonPrimary.copy(alpha = alpha.value * 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ai_controller),
                contentDescription = "AI Processing",
                modifier = Modifier.size(32.dp),
                tint = ButtonPrimary.copy(alpha = alpha.value)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Analyzing your gaming profile...",
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Our AI is discovering your unique patterns",
            color = TextSecondary.copy(alpha = 0.5f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EmptyInsightsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ai_analysis),
            contentDescription = "No Data",
            modifier = Modifier.size(48.dp),
            tint = TextSecondary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AI Insights Await",
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Add games to unlock personalized AI analysis",
            color = TextSecondary.copy(alpha = 0.5f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorInsightsState(error: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            modifier = Modifier.size(48.dp),
            tint = Color(0xFFFF9800).copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Couldn't generate insights",
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = error,
            color = TextSecondary.copy(alpha = 0.5f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AIInsightsContent(insights: GeminiRepository.GameInsights) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Personality Analysis
        MagicalInsightCard(
            icon = R.drawable.ai_analysis,
            title = "Gaming DNA",
            content = insights.personalityAnalysis,
            backgroundColor = Color(0xFF4A148C).copy(alpha = 0.3f),
            accentColor = Color(0xFFAB47BC)
        )

        // Play Style
        MagicalInsightCard(
            icon = R.drawable.ai_controller,
            title = "Play Style",
            content = insights.playStyle,
            backgroundColor = Color(0xFF1565C0).copy(alpha = 0.3f),
            accentColor = Color(0xFF42A5F5)
        )

        // Fun Facts
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE65100).copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Fun Facts",
                        modifier = Modifier.size(20.dp),
                        tint = YellowAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fun Facts",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                insights.funFacts.forEach { fact ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(6.dp)
                                .background(YellowAccent, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        StyledInsightText(
                            text = fact,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (fact != insights.funFacts.last()) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // Recommendations
        MagicalInsightCard(
            icon = R.drawable.ai_analysis,
            title = "Recommended For You",
            content = insights.recommendations,
            backgroundColor = Color(0xFF2E7D32).copy(alpha = 0.3f),
            accentColor = Color(0xFF66BB6A)
        )
    }
}

@Composable
fun MagicalInsightCard(
    icon: Int,
    title: String,
    content: String,
    backgroundColor: Color,
    accentColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            // Magical glow effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            radius = 400f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = title,
                        modifier = Modifier.size(20.dp),
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = TextSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                StyledInsightText(text = content)
            }
        }
    }
}

@Composable
fun StyledInsightText(
    text: String,
    modifier: Modifier = Modifier
) {
    val gameNameColor = ButtonPrimary
    val parts = remember(text) {
        parseGameNames(text)
    }

    androidx.compose.foundation.text.BasicText(
        text = buildAnnotatedString {
            parts.forEach { part ->
                when (part) {
                    is TextPart.Normal -> {
                        withStyle(
                            style = SpanStyle(
                                color = TextSecondary.copy(alpha = 0.9f),
                                fontSize = 13.sp
                            )
                        ) {
                            append(part.text)
                        }
                    }
                    is TextPart.GameName -> {
                        withStyle(
                            style = SpanStyle(
                                color = gameNameColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(part.text)
                        }
                    }
                }
            }
        },
        modifier = modifier,
        style = androidx.compose.ui.text.TextStyle(
            lineHeight = 20.sp
        )
    )
}

private sealed class TextPart {
    data class Normal(val text: String) : TextPart()
    data class GameName(val text: String) : TextPart()
}

private fun parseGameNames(text: String): List<TextPart> {
    val parts = mutableListOf<TextPart>()
    var currentIndex = 0
    val gamePattern = "<<GAME:(.+?)>>"
    val regex = gamePattern.toRegex()

    regex.findAll(text).forEach { match ->
        // Add text before the game name
        if (match.range.first > currentIndex) {
            parts.add(TextPart.Normal(text.substring(currentIndex, match.range.first)))
        }
        // Add the game name (without markers)
        parts.add(TextPart.GameName(match.groupValues[1]))
        currentIndex = match.range.last + 1
    }

    // Add remaining text
    if (currentIndex < text.length) {
        parts.add(TextPart.Normal(text.substring(currentIndex)))
    }

    // If no game names found, return the whole text as normal
    if (parts.isEmpty()) {
        parts.add(TextPart.Normal(text))
    }

    return parts
}
