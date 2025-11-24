package com.example.arcadia.presentation.screens.analytics.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.atan2

@Composable
fun PieChart(
    data: List<Pair<String, Int>>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    thickness: Dp = 24.dp
) {
    val total = data.sumOf { it.second }
    val animatedProgress = remember { Animatable(0f) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    
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
            Canvas(
                modifier = Modifier
                    .size(160.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val centerX = size.width / 2
                            val centerY = size.height / 2
                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            
                            // Calculate angle in degrees [-180, 180]
                            var angle = (atan2(dy, dx) * 180 / PI).toFloat()
                            // Normalize to [0, 360] matching startAngle -90 offset logic
                            // Our drawing starts at -90. So 0 degrees in atan2 (3 o'clock) corresponds to 90 degrees in our sweep logic relative to -90?
                            // Let's simplify: 
                            // atan2 0 is East. -90 is North.
                            // Standard angles: 0=E, 90=S, 180=W, -90=N.
                            // We draw starting at -90 (North).
                            
                            // Shift angle so North is 0, going clockwise.
                            // Current: E=0, S=90, W=180, N=-90
                            // Target: N=0, E=90, S=180, W=270
                            angle += 90f 
                            if (angle < 0) angle += 360f
                            
                            // Find which slice contains this angle
                            var currentAngle = 0f
                            var foundIndex = -1
                            
                            for ((index, pair) in data.withIndex()) {
                                val sweep = (pair.second.toFloat() / total.toFloat()) * 360f
                                if (angle >= currentAngle && angle < currentAngle + sweep) {
                                    foundIndex = index
                                    break
                                }
                                currentAngle += sweep
                            }
                            
                            selectedIndex = if (selectedIndex == foundIndex) -1 else foundIndex
                        }
                    }
            ) {
                var startAngle = -90f
                val strokeWidth = thickness.toPx()
                // Base radius
                val baseRadius = size.minDimension / 2 - strokeWidth / 2
                
                data.forEachIndexed { index, pair ->
                    val sweepAngle = (pair.second.toFloat() / total.toFloat()) * 360f * animatedProgress.value
                    val isSelected = index == selectedIndex
                    
                    // Scale radius if selected
                    val currentStrokeWidth = if (isSelected) strokeWidth * 1.3f else strokeWidth
                    val currentRadius = if (isSelected) baseRadius + 2.dp.toPx() else baseRadius
                    
                    drawArc(
                        color = colors.getOrElse(index) { Color.Gray },
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Butt),
                        size = Size(currentRadius * 2, currentRadius * 2),
                        topLeft = Offset(
                            (size.width - currentRadius * 2) / 2,
                            (size.height - currentRadius * 2) / 2
                        )
                    )
                    
                    startAngle += sweepAngle
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (selectedIndex != -1) {
                    val selectedData = data[selectedIndex]
                    Text(
                        text = selectedData.first,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${selectedData.second} games",
                        color = TextSecondary.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                } else {
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
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        // Legend
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEachIndexed { index, pair ->
                val isSelected = index == selectedIndex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent, 
                            RoundedCornerShape(4.dp)
                        )
                        .padding(4.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { 
                                selectedIndex = if (selectedIndex == index) -1 else index
                            }
                        }
                ) {
                    Surface(
                        color = colors.getOrElse(index) { Color.Gray },
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(12.dp)
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${pair.first} (${(pair.second.toFloat() / total * 100).toInt()}%)",
                        color = if (isSelected) Color.White else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
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
