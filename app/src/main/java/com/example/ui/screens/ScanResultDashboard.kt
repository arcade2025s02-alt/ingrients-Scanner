package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Recycling
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material3.FilterChip
import com.example.model.FactorItem
import com.example.model.HealthierAlternative
import com.example.model.IngredientDetail
import com.example.model.ProductCategory
import com.example.model.ScanRecord
import com.example.model.SmartRecipe
import com.example.model.TopRatedProduct
import com.example.ui.components.CategoryBadge
import com.example.ui.components.CircularScoreGauge
import com.example.ui.components.RiskDot
import com.example.ui.components.categoryIcon
import com.example.ui.components.ingredientRiskBackground
import com.example.ui.components.ingredientRiskForeground
import com.example.ui.components.scoreBackgroundColor
import com.example.ui.components.scoreForegroundColor
import com.example.ui.theme.LocalSemanticRiskColors
import com.example.viewmodel.RecipeUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScanResultDashboard(
    scan: ScanRecord,
    similarGoodProducts: List<TopRatedProduct> = emptyList(),
    recipeUiState: RecipeUiState,
    onGenerateRecipes: () -> Unit,
    onAskAiAboutScan: (ScanRecord) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    var activeIngredient by remember { mutableStateOf<IngredientDetail?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Risk Analysis Report",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("result_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { onAskAiAboutScan(scan) },
                    modifier = Modifier.testTag("result_ask_ai_icon")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SmartToy,
                        contentDescription = "Ask Health Assistant",
                        tint = colors.primary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Yuka-Style Header Card with 0-100 Score Gauge
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = BorderStroke(1.dp, semantic.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(scoreBackgroundColor(scan.score)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = categoryIcon(scan.parsedCategory()),
                                    contentDescription = scan.parsedCategory().displayName,
                                    tint = scoreForegroundColor(scan.score),
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                CategoryBadge(category = scan.parsedCategory())
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = scan.productName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = colors.onSurface
                                )
                                if (scan.brand.isNotBlank()) {
                                    Text(
                                        text = scan.brand,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }

                            CircularScoreGauge(
                                score = scan.score,
                                ratingLabel = scan.ratingLabel,
                                size = 94.dp
                            )
                        }

                        if (scan.summary.isNotBlank()) {
                            Text(
                                text = scan.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant
                            )
                        }

                        if (scan.standardsCited.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                scan.standardsCited.forEach { std ->
                                    Text(
                                        text = "Standard: $std",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(semantic.softEmeraldSurface)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Allergen & Medical Alert Banner (Immediate Red Alert if conflicts exist)
                if (scan.personalAlerts.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = semantic.badBg),
                        border = BorderStroke(1.5.dp, semantic.bad),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("personal_alert_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ReportProblem,
                                    contentDescription = "Allergen and Medical Alert",
                                    tint = semantic.bad,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Personal Health & Allergen Alert",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = semantic.bad,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            scan.personalAlerts.forEach { alert ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    RiskDot(
                                        color = semantic.bad,
                                        size = 7.dp,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                    Text(
                                        text = alert,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = semantic.bad
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Negatives Breakdown (Expandable Yuka-Style Rows)
                if (scan.negatives.isNotEmpty()) {
                    FactorBreakdownCard(
                        title = "Negatives",
                        items = scan.negatives,
                        positive = false
                    )
                }

                // 4. Positives Breakdown
                if (scan.positives.isNotEmpty()) {
                    FactorBreakdownCard(
                        title = "Positives",
                        items = scan.positives,
                        positive = true
                    )
                }

                // 5. Interactive Ingredient List (Tap to open Ingredient Detail Modal)
                if (scan.ingredients.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Detected Ingredients (${scan.ingredients.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.onSurface
                            )
                            Text(
                                text = "Tap any ingredient to view its chemical name, pros, cons, and side effects.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                            )

                            scan.ingredients.forEachIndexed { idx, ingredient ->
                                val riskLevel = ingredient.parsedRiskLevel()
                                val fg = ingredientRiskForeground(riskLevel)
                                val bg = ingredientRiskBackground(riskLevel)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { activeIngredient = ingredient }
                                        .padding(vertical = 10.dp, horizontal = 6.dp)
                                        .testTag("ingredient_row_$idx"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RiskDot(color = fg, size = 11.dp)
                                        Column {
                                            Text(
                                                text = ingredient.commonName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = colors.onSurface
                                            )
                                            if (ingredient.chemicalName.isNotBlank()) {
                                                Text(
                                                    text = ingredient.chemicalName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = riskLevel.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = fg,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(bg)
                                            .padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                                if (idx < scan.ingredients.lastIndex) {
                                    HorizontalDivider(color = semantic.cardBorder.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }

                // 6. Similar Good Products Suggestion & Head-to-Head Comparison
                val comparisonCandidates = remember(scan.id, scan.healthierAlternative, similarGoodProducts) {
                    buildComparisonCandidates(scan, similarGoodProducts)
                }
                if (comparisonCandidates.isNotEmpty()) {
                    SimilarGoodProductsComparisonCard(
                        scanned = scan,
                        candidates = comparisonCandidates
                    )
                }

                // 7. Smart Recipe Generator (for Packaged Food Scans)
                if (scan.parsedCategory() == ProductCategory.PACKAGED_FOOD) {
                    SmartRecipeSection(
                        recipeUiState = recipeUiState,
                        onGenerateRecipes = onGenerateRecipes
                    )
                }

                // 8. Follow-up AI Health Assistant CTA
                OutlinedButton(
                    onClick = { onAskAiAboutScan(scan) },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, colors.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("ask_ai_followup_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Ask Health Assistant About This Product",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Ingredient Detail Modal Bottom Sheet
    if (activeIngredient != null) {
        val ing = activeIngredient!!
        val risk = ing.parsedRiskLevel()
        val fg = ingredientRiskForeground(risk)
        val bg = ingredientRiskBackground(risk)
        ModalBottomSheet(
            onDismissRequest = { activeIngredient = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ing.commonName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = colors.onSurface
                        )
                        if (ing.chemicalName.isNotBlank()) {
                            Text(
                                text = "Chemical / Scientific: ${ing.chemicalName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(bg)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        RiskDot(color = fg, size = 8.dp)
                        Text(
                            text = risk.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = fg,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IngredientInfoBlock(
                    title = "Pros (Functional / Nutritional Benefits)",
                    body = ing.pros,
                    accentColor = semantic.excellent,
                    containerColor = semantic.softEmeraldSurface
                )
                IngredientInfoBlock(
                    title = "Cons (Health / Environmental Risks)",
                    body = ing.cons,
                    accentColor = semantic.bad,
                    containerColor = semantic.badBg.copy(alpha = 0.55f)
                )
                IngredientInfoBlock(
                    title = "Short-Term Side Effects",
                    body = ing.shortTermEffects,
                    accentColor = semantic.moderate,
                    containerColor = semantic.moderateBg.copy(alpha = 0.5f)
                )
                IngredientInfoBlock(
                    title = "Long-Term Side Effects",
                    body = ing.longTermEffects,
                    accentColor = colors.onSurface,
                    containerColor = colors.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun IngredientInfoBlock(
    title: String,
    body: String,
    accentColor: Color,
    containerColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = body.ifBlank { "No significant concerns reported." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FactorBreakdownCard(
    title: String,
    items: List<FactorItem>,
    positive: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = if (positive) semantic.excellent else semantic.bad
            )
            Spacer(Modifier.height(8.dp))
            items.forEachIndexed { idx, item ->
                var expanded by remember(item.label, idx) { mutableStateOf(false) }
                val dotColor = if (positive) {
                    if (item.severity.equals("EXCELLENT", true)) semantic.excellent else semantic.good
                } else {
                    when (item.severity.uppercase()) {
                        "HIGH" -> semantic.bad
                        "MODERATE" -> semantic.moderate
                        else -> semantic.good
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(dotColor.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = factorIcon(item.iconType),
                                    contentDescription = item.label,
                                    tint = dotColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.onSurface
                                )
                                Text(
                                    text = item.value,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RiskDot(color = dotColor, size = 10.dp)
                            Icon(
                                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = colors.onSurfaceVariant
                            )
                        }
                    }
                    AnimatedVisibility(visible = expanded) {
                        Text(
                            text = item.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(start = 46.dp, top = 8.dp, end = 8.dp)
                        )
                    }
                }
                if (idx < items.lastIndex) {
                    HorizontalDivider(color = semantic.cardBorder.copy(alpha = 0.6f))
                }
            }
        }
    }
}

private fun factorIcon(iconType: String): ImageVector = when (iconType.uppercase()) {
    "SUGAR" -> Icons.Rounded.Fastfood
    "CALORIES" -> Icons.Rounded.LocalFireDepartment
    "SODIUM" -> Icons.Rounded.WaterDrop
    "PROTEIN" -> Icons.Rounded.FitnessCenter
    "FIBER", "ORGANIC" -> Icons.Rounded.Eco
    "PLASTIC", "ECO" -> Icons.Rounded.Recycling
    else -> Icons.Rounded.Science
}

private data class GoodProductCandidate(
    val id: String,
    val productName: String,
    val brand: String,
    val score: Long,
    val ratingLabel: String,
    val highlight: String,
    val keyBenefits: List<String>,
    val sourceTag: String
)

private fun buildComparisonCandidates(
    scanned: ScanRecord,
    topRatedProducts: List<TopRatedProduct>
): List<GoodProductCandidate> {
    val scannedCat = scanned.parsedCategory()
    val scannedNameLower = scanned.productName.trim().lowercase()
    val keywords = scannedNameLower.split(Regex("[^a-z0-9]+")).filter { it.length >= 3 }.toSet()

    val result = mutableListOf<GoodProductCandidate>()

    // 1. Always prioritize the tailored healthier alternative from the scan analysis
    scanned.healthierAlternative?.let { alt ->
        if (alt.productName.isNotBlank() && !alt.productName.equals(scanned.productName, ignoreCase = true)) {
            result.add(
                GoodProductCandidate(
                    id = "alt_primary",
                    productName = alt.productName,
                    brand = alt.brand.ifBlank { "Clean Label Pick" },
                    score = alt.score.coerceIn(75L, 99L),
                    ratingLabel = if (alt.score >= 75L) "Excellent" else "Good",
                    highlight = alt.reason,
                    keyBenefits = alt.keyBenefits.ifEmpty {
                        listOf("Cleaner ingredient list", "Lower risk profile", "Aligned with WHO/FSSAI/EPA standards")
                    },
                    sourceTag = "Best Match"
                )
            )
        }
    }

    // 2. Find similar high-scoring good products (score >= 75) from the same category in Supabase / benchmarks
    val sameCategoryTop = topRatedProducts
        .filter {
            it.parsedCategory() == scannedCat &&
                it.score >= 75L &&
                !it.productName.equals(scanned.productName, ignoreCase = true)
        }
        .sortedWith(
            compareByDescending<TopRatedProduct> { candidate ->
                val text = "${candidate.productName} ${candidate.highlight}".lowercase()
                keywords.count { kw -> text.contains(kw) }
            }.thenByDescending { it.score }
        )

    sameCategoryTop.forEach { top ->
        val alreadyPresent = result.any { it.productName.equals(top.productName, ignoreCase = true) }
        if (!alreadyPresent) {
            result.add(
                GoodProductCandidate(
                    id = top.id.ifBlank { top.productName },
                    productName = top.productName,
                    brand = top.brand.ifBlank { "Top Rated" },
                    score = top.score,
                    ratingLabel = top.ratingLabel.ifBlank { "Excellent" },
                    highlight = top.highlight.ifBlank { "Top-rated safe product in ${scannedCat.shortLabel}." },
                    keyBenefits = listOf(
                        top.highlight.ifBlank { "Verified high safety rating (${top.score}/100)" },
                        "Category: ${scannedCat.displayName}",
                        if (top.score > scanned.score) "+${top.score - scanned.score} points safer than scanned product"
                        else "Comparable clean benchmark product"
                    ),
                    sourceTag = "Top Rated"
                )
            )
        }
    }

    return result.take(4)
}

@Composable
private fun SimilarGoodProductsComparisonCard(
    scanned: ScanRecord,
    candidates: List<GoodProductCandidate>
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    val scannedIsRisky = scanned.score < 75L

    var selectedIndex by remember(scanned.id, candidates.size) { mutableStateOf(0) }
    val selected = candidates.getOrElse(selectedIndex) { candidates.first() }
    val scoreDelta = selected.score - scanned.score

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.2.dp, colors.primary.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("similar_products_comparison_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CompareArrows,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (scannedIsRisky) "Similar Good Products & Comparison" else "Compare With Top-Rated Similar Products",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Select a similar safe product below to compare side-by-side.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            // Similar Good Product Selector Chips
            if (candidates.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    candidates.forEachIndexed { idx, item ->
                        FilterChip(
                            selected = selectedIndex == idx,
                            onClick = { selectedIndex = idx },
                            label = {
                                Text(
                                    text = "${item.productName.take(22)}${if (item.productName.length > 22) "…" else ""} (${item.score})",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.testTag("compare_candidate_chip_$idx")
                        )
                    }
                }
            }

            // Visual Side-by-Side Header Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Scanned product side
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (scannedIsRisky) semantic.badBg.copy(alpha = 0.55f) else semantic.softEmeraldSurface)
                        .border(
                            1.dp,
                            if (scannedIsRisky) semantic.bad.copy(alpha = 0.3f) else semantic.excellent.copy(alpha = 0.3f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "SCANNED",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (scannedIsRisky) Icons.Rounded.Cancel else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (scannedIsRisky) semantic.bad else semantic.excellent,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = scanned.productName,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${scanned.score}/100 • ${scanned.ratingLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = scoreForegroundColor(scanned.score),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Arrow + Score Improvement Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Compare with",
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (scoreDelta > 0) {
                        Text(
                            text = "+$scoreDelta pts",
                            style = MaterialTheme.typography.labelSmall,
                            color = semantic.excellent,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Selected Good Alternative side
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(semantic.excellentBg.copy(alpha = 0.72f))
                        .border(1.2.dp, semantic.excellent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = selected.sourceTag.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = semantic.excellent,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Suggested Good Product",
                        tint = semantic.excellent,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = selected.productName,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${selected.score}/100 • ${selected.ratingLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = semantic.excellent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Head-to-Head Comparison Table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.background)
                    .border(1.dp, semantic.cardBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Head-to-Head Comparison",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(color = semantic.cardBorder)

                ComparisonRow(
                    metric = "Safety Score",
                    leftValue = "${scanned.score}/100 (${scanned.ratingLabel})",
                    rightValue = "${selected.score}/100 (${selected.ratingLabel})",
                    leftAccent = scoreForegroundColor(scanned.score),
                    rightAccent = semantic.excellent
                )
                HorizontalDivider(color = semantic.cardBorder.copy(alpha = 0.5f))

                val highRiskCount = scanned.ingredients.count { it.riskLevel.equals("HIGH_RISK", true) }
                val scannedNegText = when {
                    highRiskCount > 0 -> "$highRiskCount high-risk ingredient(s), ${scanned.negatives.size} concern(s)"
                    scanned.negatives.isNotEmpty() -> "${scanned.negatives.size} concern(s): ${scanned.negatives.first().label}"
                    else -> "Minimal concerns detected"
                }
                ComparisonRow(
                    metric = "Risk & Additives",
                    leftValue = scannedNegText,
                    rightValue = "Clean formulation • Zero harsh additives",
                    leftAccent = if (highRiskCount > 0) semantic.bad else colors.onSurfaceVariant,
                    rightAccent = semantic.excellent
                )
                HorizontalDivider(color = semantic.cardBorder.copy(alpha = 0.5f))

                ComparisonRow(
                    metric = "Profile Alerts",
                    leftValue = if (scanned.personalAlerts.isEmpty()) "0 conflicts" else "${scanned.personalAlerts.size} alert(s) triggered",
                    rightValue = "Cleaner swap aligned with WHO/FSSAI/EPA",
                    leftAccent = if (scanned.personalAlerts.isEmpty()) semantic.excellent else semantic.bad,
                    rightAccent = semantic.excellent
                )
                HorizontalDivider(color = semantic.cardBorder.copy(alpha = 0.5f))

                ComparisonRow(
                    metric = "Brand / Benchmark",
                    leftValue = scanned.brand.ifBlank { "Scanned Item" },
                    rightValue = selected.brand,
                    leftAccent = colors.onSurface,
                    rightAccent = colors.primary
                )
            }

            if (selected.highlight.isNotBlank()) {
                Text(
                    text = selected.highlight,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (selected.keyBenefits.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    selected.keyBenefits.forEach { benefit ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = semantic.excellent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = benefit,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonRow(
    metric: String,
    leftValue: String,
    rightValue: String,
    leftAccent: Color,
    rightAccent: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = metric,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = leftValue,
                style = MaterialTheme.typography.bodySmall,
                color = leftAccent,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = rightValue,
                style = MaterialTheme.typography.bodySmall,
                color = rightAccent,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SmartRecipeSection(
    recipeUiState: RecipeUiState,
    onGenerateRecipes: () -> Unit
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestaurantMenu,
                    contentDescription = null,
                    tint = colors.primary
                )
                Text(
                    text = "Smart Balancing Recipes",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface
                )
            }
            Text(
                text = "Generate 3 balanced healthy recipes incorporating this food while offsetting its nutritional negatives (e.g., high sodium or sugar).",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            when (recipeUiState) {
                is RecipeUiState.Idle -> {
                    Button(
                        onClick = onGenerateRecipes,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("generate_recipes_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Generate 3 Healthy Recipes")
                    }
                }
                is RecipeUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Crafting 3 personalized balancing recipes...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is RecipeUiState.Error -> {
                    Text(
                        text = recipeUiState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.bad
                    )
                    Button(
                        onClick = onGenerateRecipes,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retry Recipe Generation")
                    }
                }
                is RecipeUiState.Success -> {
                    recipeUiState.recipes.forEachIndexed { index, recipe ->
                        ExpandableRecipeCard(index = index + 1, recipe = recipe)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableRecipeCard(
    index: Int,
    recipe: SmartRecipe
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    var expanded by remember(recipe.title) { mutableStateOf(index == 1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(semantic.softEmeraldSurface)
            .border(1.dp, colors.primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .clickable { expanded = !expanded }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$index. ${recipe.title}",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Prep: ${recipe.prepTime}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = colors.primary
            )
        }
        Text(
            text = recipe.balancingNote,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HorizontalDivider(color = colors.primary.copy(alpha = 0.15f))
                Text(
                    text = "Ingredients",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold
                )
                recipe.ingredients.forEach { ing ->
                    Text(
                        text = "• $ing",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurface
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Steps",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold
                )
                recipe.steps.forEachIndexed { sIdx, step ->
                    Text(
                        text = "${sIdx + 1}. $step",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurface
                    )
                }
            }
        }
    }
}
