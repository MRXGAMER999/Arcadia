package com.example.arcadia.presentation.screens.analytics

import android.content.Context
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arcadia.presentation.screens.analytics.components.AIInsightsSection
import com.example.arcadia.presentation.screens.analytics.components.GamingDNACard
import com.example.arcadia.presentation.screens.analytics.components.SimpleBarChart
import com.example.arcadia.presentation.screens.analytics.components.StatCard
import com.example.arcadia.presentation.screens.analytics.util.AnalyticsShareHelper
import com.example.arcadia.ui.theme.ButtonPrimary
import com.example.arcadia.ui.theme.Surface
import com.example.arcadia.ui.theme.TextSecondary
import com.example.arcadia.ui.theme.YellowAccent
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSearch: (String) -> Unit = {},
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
                        AnalyticsShareHelper.shareAnalytics(context, state)
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
            AnalyticsContent(
                state = state,
                onGameClick = onNavigateToSearch
            )
        }
    }
}

@Composable
fun AnalyticsContent(
    state: AnalyticsState,
    onGameClick: (String) -> Unit = {},
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

        // AI Insights Section
        AIInsightsSection(
            state = state,
            onGameClick = onGameClick
        )

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
private fun InsightItem(text: String) {
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