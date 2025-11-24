package com.example.arcadia.presentation.screens.analytics.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PieChart(
    data: List<Pair<String, Int>>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    thickness: Dp = 24.dp
) {
    val total = data.sumOf { it.second }
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        animatedProgress.animateTo(1f, animationSpec = tween(1000))
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chart
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                var startAngle = -90f
                val strokeWidth = thickness.toPx()
                val radius = size.minDimension / 2 - strokeWidth / 2
                
                data.forEachIndexed { index, pair ->
                    val sweepAngle = (pair.second.toFloat() / total.toFloat()) * 360f * animatedProgress.value
                    
                    drawArc(
                        color = colors.getOrElse(index) { Color.Gray },
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(
                            (size.width - radius * 2) / 2,
                            (size.height - radius * 2) / 2
                        )
                    )
                    
                    startAngle += sweepAngle
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total.toString(),
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Text(
                    text = "Games",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        // Legend
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEachIndexed { index, pair ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = colors.getOrElse(index) { Color.Gray },
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(12.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${pair.first} (${(pair.second.toFloat() / total * 100).toInt()}%)",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleBarChart(
    data: List<Pair<String, Float>>, // Label, Value
    color: Color,
    modifier: Modifier = Modifier,
    maxValue: Float = 10f
) {
    val animatedProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        animatedProgress.animateTo(1f, animationSpec = tween(1000))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        data.forEach { (label, value) ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = String.format("%.1f", value),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(Color(0xFF2D3E5F), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((value / maxValue) * animatedProgress.value)
                            .height(8.dp)
                            .background(color, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
