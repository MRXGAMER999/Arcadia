package com.example.arcadia.presentation.screens.roast.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.domain.model.ai.RoastInsights
import com.example.arcadia.presentation.screens.roast.RevealPhase
import com.example.arcadia.presentation.screens.roast.RoastTheme

/**
 * Displays the roast result with sequential reveal animations.
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnimatedRoastResultCard(
    roast: RoastInsights,
    revealPhase: RevealPhase,
    reduceMotion: Boolean,
    onRegenerate: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    badgesContent: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Title Badge (Bounce In)
        AnimatedVisibility(
            visible = revealPhase >= RevealPhase.REVEALING_TITLE,
            enter = if (reduceMotion) fadeIn() else slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
            ) + fadeIn()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                RoastTheme.emberRed,
                                RoastTheme.fireOrange
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "${roast.roastTitleEmoji} ${roast.roastTitle}",
                    style = MaterialTheme.typography.titleLargeEmphasized.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }

        // 2. Headline (Typewriter)
        if (revealPhase >= RevealPhase.REVEALING_HEADLINE) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = roast.headline,
                    style = MaterialTheme.typography.titleMediumEmphasized.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }

        // 3. Could Have List (Slide In)
        if (revealPhase >= RevealPhase.REVEALING_COULD_HAVE) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A0A0A)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            RoastTheme.fireOrange.copy(alpha = 0.6f),
                            RoastTheme.emberRed.copy(alpha = 0.6f)
                        )
                    )
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "⏰",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "TIME WASTED - YOU COULD HAVE:",
                            style = MaterialTheme.typography.titleSmallEmphasized.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = RoastTheme.fireOrange
                        )
                    }
                    
                    roast.couldHaveList.forEachIndexed { index, item ->
                        AnimatedVisibility(
                            visible = true,
                            enter = if (reduceMotion) fadeIn() else slideInVertically { it } + fadeIn()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            color = RoastTheme.fireOrange.copy(alpha = 0.2f),
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = RoastTheme.fireOrange,
                                        style = MaterialTheme.typography.labelLargeEmphasized.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item.trim().trimStart('.', '-', '•', '*', ' ').trim(),
                                    color = Color.White.copy(alpha = 0.95f),
                                    style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                                        lineHeight = 24.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Prediction (Fade In with Glow)
        AnimatedVisibility(
            visible = revealPhase >= RevealPhase.REVEALING_PREDICTION,
            enter = fadeIn(animationSpec = tween(1000))
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4A148C).copy(alpha = 0.3f),
                                Color(0xFF6A1B9A).copy(alpha = 0.2f),
                                Color(0xFF4A148C).copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF9C27B0).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🔮",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "YOUR GAMING FUTURE",
                            style = MaterialTheme.typography.titleSmallEmphasized.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            ),
                            color = Color(0xFFCE93D8)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = roast.prediction,
                        style = MaterialTheme.typography.bodyLargeEmphasized.copy(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            lineHeight = 26.sp
                        ),
                        color = Color.White.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 5. Wholesome (Gentle Fade)
        AnimatedVisibility(
            visible = revealPhase >= RevealPhase.REVEALING_WHOLESOME,
            enter = fadeIn(animationSpec = tween(1500))
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF00695C).copy(alpha = 0.3f),
                                Color(0xFF00897B).copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF4DB6AC).copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "💚",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BUT SERIOUSLY THOUGH...",
                            style = MaterialTheme.typography.titleSmallEmphasized.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFF80CBC4)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = roast.wholesomeCloser,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 26.sp
                        ),
                        color = Color.White.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 6. Buttons (Fade In)
        AnimatedVisibility(
            visible = revealPhase >= RevealPhase.COMPLETE,
            enter = fadeIn()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Badges
                badgesContent()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onShare,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4DB6AC)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = Color(0xFF4DB6AC)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = "📤 Share",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    
                    Button(
                        onClick = onRegenerate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoastTheme.fireOrange
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = "🔥 New Roast",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}
