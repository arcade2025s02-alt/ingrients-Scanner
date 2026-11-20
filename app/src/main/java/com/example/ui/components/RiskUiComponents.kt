package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.IngredientRiskLevel
import com.example.model.ProductCategory
import com.example.ui.theme.LocalSemanticRiskColors

@Composable
fun scoreForegroundColor(score: Long): Color {
    val risk = LocalSemanticRiskColors.current
    return when {
        score >= 75L -> risk.excellent
        score >= 40L -> risk.moderate
        else -> risk.bad
    }
}

@Composable
fun scoreBackgroundColor(score: Long): Color {
    val risk = LocalSemanticRiskColors.current
    return when {
        score >= 75L -> risk.excellentBg
        score >= 40L -> risk.moderateBg
        else -> risk.badBg
    }
}

@Composable
fun ingredientRiskForeground(level: IngredientRiskLevel): Color {
    val risk = LocalSemanticRiskColors.current
    return when (level) {
        IngredientRiskLevel.HIGH_RISK -> risk.bad
        IngredientRiskLevel.MODERATE_RISK -> risk.moderate
        IngredientRiskLevel.LOW_RISK -> risk.good
        IngredientRiskLevel.RISK_FREE -> risk.excellent
    }
}

@Composable
fun ingredientRiskBackground(level: IngredientRiskLevel): Color {
    val risk = LocalSemanticRiskColors.current
    return when (level) {
        IngredientRiskLevel.HIGH_RISK -> risk.badBg
        IngredientRiskLevel.MODERATE_RISK -> risk.moderateBg
        IngredientRiskLevel.LOW_RISK -> risk.goodBg
        IngredientRiskLevel.RISK_FREE -> risk.excellentBg
    }
}

fun categoryIcon(category: ProductCategory): ImageVector = when (category) {
    ProductCategory.PACKAGED_FOOD -> Icons.Rounded.Restaurant
    ProductCategory.AGRICULTURAL_FERTILIZER -> Icons.Rounded.Agriculture
    ProductCategory.PACKAGING_MATERIAL -> Icons.Rounded.Inventory2
}

@Composable
fun CategoryBadge(
    category: ProductCategory,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = category.displayName,
            tint = colors.primary,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RiskDot(
    color: Color,
    size: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun ScorePillBadge(
    score: Long,
    ratingLabel: String,
    modifier: Modifier = Modifier
) {
    val fg = scoreForegroundColor(score)
    val bg = scoreBackgroundColor(score)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.25f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RiskDot(color = fg, size = 8.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$score/100 • $ratingLabel",
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CircularScoreGauge(
    score: Long,
    ratingLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 104.dp
) {
    val fg = scoreForegroundColor(score)
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val animatedSweep by animateFloatAsState(
        targetValue = (score.coerceIn(0L, 100L) / 100f) * 360f,
        animationSpec = tween(durationMillis = 700),
        label = "score_gauge_sweep"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = 9.dp.toPx()
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = fg,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.displaySmall,
                color = fg,
                fontWeight = FontWeight.ExtraBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                RiskDot(color = fg, size = 7.dp)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = ratingLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
