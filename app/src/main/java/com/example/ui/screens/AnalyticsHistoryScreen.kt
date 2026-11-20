package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ScanRecord
import com.example.model.UiState
import com.example.ui.components.CategoryBadge
import com.example.ui.components.ScorePillBadge
import com.example.ui.components.scoreForegroundColor
import com.example.ui.theme.LocalSemanticRiskColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsHistoryScreen(
    scansState: UiState<List<ScanRecord>>,
    onSelectScan: (ScanRecord) -> Unit,
    onDeleteScan: (String) -> Unit,
    onStartNewScan: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Analytics,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "Scan History & Analytics",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground
                )
            }
            Text(
                text = "Your local saved scans and average safety score trajectory over time.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )

            when (scansState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = semantic.badBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = scansState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = semantic.bad,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val scans = scansState.data
                    if (scans.isEmpty()) {
                        EmptyHistoryCard(onStartNewScan = onStartNewScan)
                    } else {
                        val avgScore = remember(scans) {
                            scans.map { it.score }.average().toLong()
                        }
                        val excellentCount = remember(scans) {
                            scans.count { it.score >= 75L }
                        }
                        val chronologicalScans = remember(scans) {
                            scans.sortedBy { it.createdAtMillis }.takeLast(12)
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item(key = "kpi_row") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    KpiStatCard(
                                        label = "Total Scans",
                                        value = "${scans.size}",
                                        accent = colors.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    KpiStatCard(
                                        label = "Avg Score",
                                        value = "$avgScore/100",
                                        accent = scoreForegroundColor(avgScore),
                                        modifier = Modifier.weight(1f)
                                    )
                                    KpiStatCard(
                                        label = "Safe (75+)",
                                        value = "$excellentCount",
                                        accent = semantic.excellent,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            item(key = "trend_chart") {
                                ScoreTrendChartCard(chronologicalScans = chronologicalScans)
                            }

                            item(key = "history_heading") {
                                Text(
                                    text = "Saved Scans (${scans.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.onBackground,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            items(items = scans, key = { it.id.ifBlank { it.productName } }) { scan ->
                                val dateStr = remember(scan.createdAtMillis) {
                                    dateFormatter.format(Date(scan.createdAtMillis))
                                }
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                                    border = BorderStroke(1.dp, semantic.cardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectScan(scan) }
                                        .testTag("history_scan_item_${scan.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            CategoryBadge(category = scan.parsedCategory())
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                ScorePillBadge(score = scan.score, ratingLabel = scan.ratingLabel)
                                                IconButton(
                                                    onClick = { onDeleteScan(scan.id) },
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .testTag("delete_scan_${scan.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DeleteOutline,
                                                        contentDescription = "Delete scan",
                                                        tint = colors.onSurfaceVariant,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = scan.productName,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = colors.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = scan.brand.ifBlank { "Generic" },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.onSurfaceVariant
                                            )
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun ScoreTrendChartCard(
    chronologicalScans: List<ScanRecord>
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = colors.primary
                    )
                    Text(
                        text = "Safety Score Over Time",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface
                    )
                }
                Text(
                    text = "Last ${chronologicalScans.size} scans",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.background)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                val w = size.width
                val h = size.height

                val y75 = h * (1f - 0.75f)
                val y40 = h * (1f - 0.40f)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))

                drawLine(
                    color = semantic.excellent.copy(alpha = 0.4f),
                    start = Offset(0f, y75),
                    end = Offset(w, y75),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = dashEffect
                )
                drawLine(
                    color = semantic.moderate.copy(alpha = 0.4f),
                    start = Offset(0f, y40),
                    end = Offset(w, y40),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = dashEffect
                )

                val points = chronologicalScans.mapIndexed { idx, scan ->
                    val x = if (chronologicalScans.size == 1) w / 2f
                    else (idx.toFloat() / (chronologicalScans.size - 1).toFloat()) * w
                    val y = h * (1f - (scan.score.coerceIn(0L, 100L) / 100f))
                    Offset(x, y) to scan.score
                }

                if (points.size >= 2) {
                    val linePath = Path().apply {
                        moveTo(points.first().first.x, points.first().first.y)
                        points.drop(1).forEach { (pt, _) -> lineTo(pt.x, pt.y) }
                    }
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(points.last().first.x, h)
                        lineTo(points.first().first.x, h)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.25f),
                                colors.primary.copy(alpha = 0.02f)
                            )
                        )
                    )
                    drawPath(
                        path = linePath,
                        color = colors.primary,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                points.forEach { (pt, score) ->
                    val dotColor = when {
                        score >= 75L -> semantic.excellent
                        score >= 40L -> semantic.moderate
                        else -> semantic.bad
                    }
                    drawCircle(color = colors.surface, radius = 7.dp.toPx(), center = pt)
                    drawCircle(color = dotColor, radius = 5.dp.toPx(), center = pt)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "— 75+ Excellent",
                    style = MaterialTheme.typography.labelSmall,
                    color = semantic.excellent
                )
                Text(
                    text = "— 40–74 Moderate",
                    style = MaterialTheme.typography.labelSmall,
                    color = semantic.moderate
                )
                Text(
                    text = "— 0–39 High Risk",
                    style = MaterialTheme.typography.labelSmall,
                    color = semantic.bad
                )
            }
        }
    }
}

@Composable
private fun EmptyHistoryCard(onStartNewScan: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DocumentScanner,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "No Scans Saved Yet",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface
            )
            Text(
                text = "Scan your first packaged food, fertilizer, or packaging label to see your safety score trend chart and history here.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Button(
                onClick = onStartNewScan,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("empty_history_scan_button")
            ) {
                Text("Scan First Product")
            }
        }
    }
}
