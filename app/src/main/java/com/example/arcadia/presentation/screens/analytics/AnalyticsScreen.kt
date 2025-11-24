package com.example.arcadia.presentation.screens.analytics

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.arcadia.presentation.screens.analytics.components.PieChart
import com.example.arcadia.presentation.screens.analytics.components.SimpleBarChart
import com.example.arcadia.ui.theme.ArcadiaTheme
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = koinViewModel()
) {
    val state = viewModel.state
    val context = LocalContext.current

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Analytics Dashboard",
                        color = TextSecondary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ButtonPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        shareAnalytics(context, state)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = ButtonPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            AnalyticsContent(state = state)
        }
    }
}

@Composable
fun AnalyticsContent(
    state: AnalyticsState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Gaming DNA Section
        GamingDNACard(state = state)

        // Time & Completion Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                title = "Completion Rate",
                value = String.format(Locale.US, "%.0f%%", state.completionRate),
                subtitle = "${state.completedGames} finished",
                color = Color(0xFF4ADE80),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Total Hours",
                value = "${state.hoursPlayed}h",
                subtitle = "Avg ${(if(state.totalGames > 0) state.hoursPlayed / state.totalGames else 0)}h / game",
                color = ButtonPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // Genre Analysis
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E2A47).copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Genre Analysis",
                    color = TextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Average Rating per Genre",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (state.genreRatingAnalysis.isNotEmpty()) {
                    SimpleBarChart(
                        data = state.genreRatingAnalysis.map { it.genre to it.avgRating },
                        color = YellowAccent
                    )
                } else {
                    Text(
                        text = "Rate more games to see analysis",
                        color = TextSecondary.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            }
        }
        
        // Year in Review / Trend
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E2A47).copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Insights",
                    color = TextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                InsightItem(
                    text = state.recentTrend
                )
                Spacer(modifier = Modifier.height(12.dp))
                InsightItem(
                    text = "You rate ${if(state.genreRatingAnalysis.isNotEmpty()) state.genreRatingAnalysis.first().genre else "games"} highest on average."
                )
            }
        }
        
        // Footer for shared image
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
             Text(
                text = "Generated by Arcadia",
                color = TextSecondary.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun GamingDNACard(state: AnalyticsState) {
    // Determine background gradient based on personality
    val gradientColors = when (state.gamingPersonality) {
        GamingPersonality.Completionist -> listOf(Color(0xFF1A237E), Color(0xFF3949AB)) // Deep Blue
        GamingPersonality.Explorer -> listOf(Color(0xFF006064), Color(0xFF0097A7)) // Cyan/Teal
        GamingPersonality.Hardcore -> listOf(Color(0xFFB71C1C), Color(0xFFD32F2F)) // Red
        GamingPersonality.Casual -> listOf(Color(0xFF1B5E20), Color(0xFF388E3C)) // Green
        else -> listOf(Color(0xFF1E2A47), Color(0xFF2D3E5F)) // Default
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(gradientColors))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gaming DNA",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LVL ${(state.totalGames / 5 + 1).coerceAtMost(99)}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Personality Badge
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF62B4DA), Color(0xFF9F55FF))
                            ),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = state.gamingPersonality.title.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = state.gamingPersonality.description,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Genre Pie Chart
                if (state.topGenres.isNotEmpty()) {
                    PieChart(
                        data = state.topGenres,
                        colors = listOf(
                            Color(0xFF4ADE80),
                            Color(0xFF60A5FA),
                            Color(0xFFF472B6),
                            Color(0xFFFBB02E),
                            Color(0xFFA78BFA),
                            Color(0xFF94A3B8)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2A47).copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = color,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextSecondary.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun InsightItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(ButtonPrimary, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = TextSecondary.copy(alpha = 0.9f),
            fontSize = 14.sp
        )
    }
}

// --- Advanced Sharing Logic ---



fun shareAnalytics(context: Context, state: AnalyticsState) {

    val activity = context as? Activity ?: return

    val root = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content) ?: return



    val composeView = ComposeView(context).apply {

        // Use a specific strategy to dispose when we remove it

        setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnDetachedFromWindow)

        

        // Hide it visually but keep it enabled for drawing

        alpha = 0f

        

        setContent {

            ArcadiaTheme {

                Surface(

                    modifier = Modifier

                        .width(360.dp) // Target width for the shared image

                        .wrapContentHeight(),

                    color = Surface

                ) {

                    AnalyticsContent(state = state)

                }

            }

        }

    }



    // Add to hierarchy to get Recomposer and Lifecycle

    root.addView(composeView)



    // Wait for composition and layout

    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({

        try {

            // Manually measure to get the full height (Long Screenshot)

            // We force the width to 360dp (scaled) and let height be UNSPECIFIED

            val density = context.resources.displayMetrics.density

            val widthPx = (360 * density).toInt()

            

            val widthSpec = View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY)

            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

            

            composeView.measure(widthSpec, heightSpec)

            val heightPx = composeView.measuredHeight

            

            if (heightPx > 0) {

                // Layout at 0,0 with the measured size

                composeView.layout(0, 0, widthPx, heightPx)

                

                // Draw

                val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)

                val canvas = Canvas(bitmap)

                

                composeView.draw(canvas)

                

                // Save and Share

                val uri = saveBitmapToCache(context, bitmap)

                if (uri != null) {

                    val intent = Intent(Intent.ACTION_SEND).apply {

                        type = "image/png"

                        putExtra(Intent.EXTRA_STREAM, uri)

                        putExtra(Intent.EXTRA_TEXT, "My Gaming DNA on Arcadia! 🎮\n#ArcadiaApp")

                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    }

                    

                    val chooser = Intent.createChooser(intent, "Share Analytics")

                    context.startActivity(chooser)

                }

            }

        } catch (e: Exception) {

            e.printStackTrace()

        } finally {

            // Cleanup

            root.removeView(composeView)

        }

    }, 500) // 500ms delay to ensure composition is complete

}



fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {

    return try {

        val imagesDir = File(context.cacheDir, "images")

        imagesDir.mkdirs()

        val file = File(imagesDir, "gaming_dna_${System.currentTimeMillis()}.png")

        val stream = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

        stream.close()

        

        FileProvider.getUriForFile(

            context,

            "${context.packageName}.fileprovider",

            file

        )

    } catch (e: Exception) {

        e.printStackTrace()

        null

    }

}