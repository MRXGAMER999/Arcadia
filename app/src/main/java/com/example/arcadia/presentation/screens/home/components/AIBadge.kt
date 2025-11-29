package com.example.arcadia.presentation.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium

/**
 * AI Recommendation Badge Types with professional styling.
 * Based on confidence scores and AI tiers.
 * Uses Material Icons instead of emojis for consistent rendering.
 */
enum class AIBadgeType(
    val label: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color
) {
    // Top tier - 90%+ confidence / PERFECT_MATCH
    PERFECT_FOR_YOU(
        label = "Perfect For You",
        icon = Icons.Default.Diamond,
        primaryColor = Color(0xFFFFD700), // Gold
        secondaryColor = Color(0xFFFFA500), // Orange
        glowColor = Color(0xFFFFD700)
    ),
    
    // High tier - 85%+ / From favorite developers
    FROM_YOUR_FAVORITES(
        label = "From Your Favorites",
        icon = Icons.Default.Favorite,
        primaryColor = Color(0xFFFF6B9D), // Pink
        secondaryColor = Color(0xFFFF4081), // Hot Pink
        glowColor = Color(0xFFFF6B9D)
    ),
    
    // High tier - 80%+ / STRONG_MATCH
    HIGHLY_RECOMMENDED(
        label = "Highly Recommended",
        icon = Icons.Default.Verified,
        primaryColor = Color(0xFF00E5FF), // Cyan
        secondaryColor = Color(0xFF00B8D4), // Teal
        glowColor = Color(0xFF00E5FF)
    ),
    
    // Critically acclaimed games
    CRITICALLY_ACCLAIMED(
        label = "Critically Acclaimed",
        icon = Icons.Default.EmojiEvents,
        primaryColor = Color(0xFFFFD700), // Gold
        secondaryColor = Color(0xFFDAA520), // Golden Rod
        glowColor = Color(0xFFFFD700)
    ),
    
    // Mid tier - 70%+ / GOOD_MATCH
    GREAT_MATCH(
        label = "Great Match",
        icon = Icons.Default.Star,
        primaryColor = Color(0xFF4CAF50), // Green
        secondaryColor = Color(0xFF8BC34A), // Light Green
        glowColor = Color(0xFF4CAF50)
    ),
    
    // Hidden gem / underrated
    HIDDEN_GEM(
        label = "Hidden Gem",
        icon = Icons.Default.AutoAwesome,
        primaryColor = Color(0xFFE040FB), // Purple
        secondaryColor = Color(0xFF7C4DFF), // Deep Purple
        glowColor = Color(0xFFE040FB)
    ),
    
    // Trending / popular pick
    TRENDING_NOW(
        label = "Trending Now",
        icon = Icons.Default.LocalFireDepartment,
        primaryColor = Color(0xFFFF5722), // Deep Orange
        secondaryColor = Color(0xFFFF9800), // Orange
        glowColor = Color(0xFFFF5722)
    ),
    
    // Default tier
    AI_PICK(
        label = "AI Pick",
        icon = Icons.Default.Bolt,
        primaryColor = Color(0xFF62B4DA), // Blue
        secondaryColor = Color(0xFF5C6BC0), // Indigo
        glowColor = Color(0xFF62B4DA)
    )
}

/**
 * Determines the badge type based on AI data.
 */
fun determineBadgeType(
    confidence: Float?,
    tier: String?,
    reason: String?,
    metacritic: Int?,
    rating: Double?
): AIBadgeType {
    val conf = confidence?.toInt() ?: 50
    val reasonLower = reason?.lowercase() ?: ""
    
    // Check for specific keywords in reason
    val isFavoriteDevs = reasonLower.contains("favorite") || 
                         reasonLower.contains("loved") ||
                         reasonLower.contains("your top") ||
                         reasonLower.contains("developers you")
    
    val isHiddenGem = reasonLower.contains("hidden") || 
                      reasonLower.contains("gem") ||
                      reasonLower.contains("underrated") ||
                      reasonLower.contains("overlooked")
    
    val isTrending = reasonLower.contains("trending") || 
                     reasonLower.contains("popular") ||
                     reasonLower.contains("everyone") ||
                     reasonLower.contains("hot right now")
    
    // Critical acclaim based on metacritic/rating
    val isCriticallyAcclaimed = (metacritic != null && metacritic >= 90) ||
                                 (rating != null && rating >= 4.5)
    
    return when {
        // Perfect match tier
        tier == "PERFECT_MATCH" || conf >= 90 -> AIBadgeType.PERFECT_FOR_YOU
        
        // From favorite developers
        isFavoriteDevs -> AIBadgeType.FROM_YOUR_FAVORITES
        
        // Critically acclaimed
        isCriticallyAcclaimed -> AIBadgeType.CRITICALLY_ACCLAIMED
        
        // Hidden gems
        isHiddenGem -> AIBadgeType.HIDDEN_GEM
        
        // Trending
        isTrending -> AIBadgeType.TRENDING_NOW
        
        // Strong match tier
        tier == "STRONG_MATCH" || conf >= 80 -> AIBadgeType.HIGHLY_RECOMMENDED
        
        // Good match tier
        tier == "GOOD_MATCH" || conf >= 65 -> AIBadgeType.GREAT_MATCH
        
        // Default
        else -> AIBadgeType.AI_PICK
    }
}

/**
 * Premium AI Badge with gradient background and subtle animation.
 */
@Composable
fun AIRecommendationBadge(
    badgeType: AIBadgeType,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    compact: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge_shimmer")
    
    // Subtle shimmer effect for premium badges
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )
    
    val isPremium = badgeType == AIBadgeType.PERFECT_FOR_YOU || 
                    badgeType == AIBadgeType.FROM_YOUR_FAVORITES ||
                    badgeType == AIBadgeType.CRITICALLY_ACCLAIMED
    
    val backgroundBrush = Brush.horizontalGradient(
        colors = listOf(
            badgeType.primaryColor.copy(alpha = if (isPremium) 0.3f * shimmerAlpha else 0.2f),
            badgeType.secondaryColor.copy(alpha = if (isPremium) 0.25f * shimmerAlpha else 0.15f)
        )
    )
    
    val borderBrush = Brush.horizontalGradient(
        colors = listOf(
            badgeType.primaryColor.copy(alpha = if (isPremium) 0.8f else 0.5f),
            badgeType.secondaryColor.copy(alpha = if (isPremium) 0.6f else 0.4f)
        )
    )
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(
                horizontal = if (compact) 6.dp else 8.dp,
                vertical = if (compact) 3.dp else 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showIcon) {
            Icon(
                imageVector = badgeType.icon,
                contentDescription = null,
                tint = badgeType.primaryColor,
                modifier = Modifier
                    .size(if (compact) 12.dp else 14.dp)
                    .graphicsLayer {
                        if (isPremium) {
                            alpha = shimmerAlpha
                        }
                    }
            )
        }
        
        Text(
            text = badgeType.label,
            color = badgeType.primaryColor,
            fontSize = if (compact) 9.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}

/**
 * Simple AI Badge Chip for displaying AI-generated badge text.
 * Used to display dynamic badges generated by the AI model.
 */
@Composable
fun AIBadgeChip(
    text: String,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF62B4DA),
    secondaryColor: Color = Color(0xFF5C6BC0)
) {
    val backgroundBrush = Brush.horizontalGradient(
        colors = listOf(
            primaryColor.copy(alpha = 0.2f),
            secondaryColor.copy(alpha = 0.15f)
        )
    )
    
    val borderBrush = Brush.horizontalGradient(
        colors = listOf(
            primaryColor.copy(alpha = 0.5f),
            secondaryColor.copy(alpha = 0.4f)
        )
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = primaryColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * AI Reasoning Card with enhanced styling.
 * Uses icons instead of emojis for a more professional look.
 * 
 * @param reason The reasoning text to display
 * @param badgeType The badge type for icon and color theming
 * @param maxLines Maximum lines for the reasoning text (default 2, can be increased)
 * @param modifier Modifier for the composable
 */
@Composable
fun AIReasoningCard(
    reason: String,
    badgeType: AIBadgeType,
    modifier: Modifier = Modifier,
    maxLines: Int = 2
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Use icon instead of emoji for consistent styling
        Icon(
            imageVector = badgeType.icon,
            contentDescription = null,
            tint = badgeType.primaryColor.copy(alpha = 0.8f),
            modifier = Modifier
                .size(14.dp)
                .padding(top = 1.dp)
        )
        Text(
            text = reason,
            color = Color(0xFFE8E8E8),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
            letterSpacing = 0.1.sp
        )
    }
}
