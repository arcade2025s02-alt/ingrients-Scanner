package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import com.example.data.SupabaseSyncStatus
import com.example.model.AdminUserOverview
import com.example.model.AppUser
import com.example.model.ProductCategory
import com.example.model.ScanRecord
import com.example.model.UiState
import com.example.ui.components.CategoryBadge
import com.example.ui.components.ScorePillBadge
import com.example.ui.components.scoreForegroundColor
import com.example.ui.theme.LocalSemanticRiskColors
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.data.SupabaseRestService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminPanelScreen(
    adminUser: AppUser,
    overviewState: UiState<List<AdminUserOverview>>,
    supabaseStatus: SupabaseSyncStatus,
    isSyncingSupabase: Boolean,
    onRefreshSupabaseSync: () -> Unit,
    onSelectScan: (ScanRecord) -> Unit,
    onDeleteScan: (String) -> Unit,
    onDeleteUser: (String) -> Unit,
    onOpenChangePassword: () -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    var searchQuery by remember { mutableStateOf("") }
    var viewAllScansMode by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var sqlCopiedNotice by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onRefreshSupabaseSync()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
        ) {
            Spacer(Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AdminPanelSettings,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Admin Console",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onBackground
                    )
                    Text(
                        text = "Full visibility into all registered users, health profiles & scan data",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Supabase Cloud Sync Bar
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (supabaseStatus.isConnected) semantic.softEmeraldSurface else colors.surface
                ),
                border = BorderStroke(
                    1.dp,
                    if (supabaseStatus.isConnected) colors.primary.copy(alpha = 0.3f) else semantic.cardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (supabaseStatus.isConnected) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                            contentDescription = null,
                            tint = if (supabaseStatus.isConnected) colors.primary else colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = supabaseStatus.statusLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurface
                            )
                            Text(
                                text = "Tables: app_users • health_profiles • scans • top_rated_products",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onRefreshSupabaseSync,
                            enabled = !isSyncingSupabase,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("admin_sync_supabase_button")
                        ) {
                            if (isSyncingSupabase) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Sync Supabase",
                                    tint = colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (supabaseStatus.tablesMissing) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("Supabase SQL Schema", SupabaseRestService.SQL_SCHEMA_SCRIPT)
                                )
                                sqlCopiedNotice = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_copy_sql_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (sqlCopiedNotice) "SQL Copied! Paste in Supabase SQL Editor & Run" else "Copy Supabase SQL Setup Script",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Default First-Time Admin Password Security Banner
            if (adminUser.mustChangePassword) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = semantic.moderateBg),
                    border = BorderStroke(1.2.dp, semantic.moderate),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("admin_change_default_password_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = semantic.moderate,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Default Admin Password Active",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Update your first-time default password for better account security.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onOpenChangePassword,
                            colors = ButtonDefaults.buttonColors(containerColor = semantic.moderate),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("admin_banner_change_password_button")
                        ) {
                            Text("Change", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            when (overviewState) {
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
                            text = overviewState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = semantic.bad,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val allOverviews = overviewState.data
                    val allScansWithOwner = remember(allOverviews) {
                        allOverviews.flatMap { ov -> ov.scans.map { it to ov.user } }
                            .sortedByDescending { it.first.createdAtMillis }
                    }
                    val totalUsers = allOverviews.size
                    val totalScans = allScansWithOwner.size
                    val avgScore = if (totalScans == 0) 0L
                    else allScansWithOwner.map { it.first.score }.average().toLong()
                    val highRiskCount = allScansWithOwner.count { it.first.score < 40L }

                    val query = searchQuery.trim().lowercase(Locale.ROOT)
                    val filteredUsers = remember(allOverviews, query) {
                        if (query.isBlank()) allOverviews
                        else allOverviews.filter { ov ->
                            ov.user.displayName.lowercase(Locale.ROOT).contains(query) ||
                                ov.user.email.lowercase(Locale.ROOT).contains(query) ||
                                ov.scans.any { it.productName.lowercase(Locale.ROOT).contains(query) }
                        }
                    }
                    val filteredAllScans = remember(allScansWithOwner, query, selectedCategory) {
                        allScansWithOwner.filter { (scan, owner) ->
                            val matchesCat = selectedCategory == null || scan.parsedCategory() == selectedCategory
                            val matchesQuery = query.isBlank() ||
                                scan.productName.lowercase(Locale.ROOT).contains(query) ||
                                scan.brand.lowercase(Locale.ROOT).contains(query) ||
                                owner.email.lowercase(Locale.ROOT).contains(query) ||
                                owner.displayName.lowercase(Locale.ROOT).contains(query)
                            matchesCat && matchesQuery
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // System-Wide KPIs
                        item(key = "admin_kpis") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AdminKpiTile("Users", "$totalUsers", colors.primary, Modifier.weight(1f))
                                AdminKpiTile("Scans", "$totalScans", colors.primary, Modifier.weight(1f))
                                AdminKpiTile(
                                    "Avg Score",
                                    if (totalScans > 0) "$avgScore" else "—",
                                    if (totalScans > 0) scoreForegroundColor(avgScore) else colors.onSurfaceVariant,
                                    Modifier.weight(1f)
                                )
                                AdminKpiTile("High Risk", "$highRiskCount", semantic.bad, Modifier.weight(1f))
                            }
                        }

                        // View Mode Switch + Search
                        item(key = "admin_controls") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    SegmentedButton(
                                        selected = !viewAllScansMode,
                                        onClick = { viewAllScansMode = false },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                        modifier = Modifier.testTag("admin_mode_users")
                                    ) {
                                        Text("All Users ($totalUsers)")
                                    }
                                    SegmentedButton(
                                        selected = viewAllScansMode,
                                        onClick = { viewAllScansMode = true },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                        modifier = Modifier.testTag("admin_mode_all_scans")
                                    ) {
                                        Text("All Scans ($totalScans)")
                                    }
                                }

                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search by user email, name, or scanned product...") },
                                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_search_input")
                                )

                                if (viewAllScansMode) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = selectedCategory == null,
                                            onClick = { selectedCategory = null },
                                            label = { Text("All") }
                                        )
                                        ProductCategory.entries.forEach { cat ->
                                            FilterChip(
                                                selected = selectedCategory == cat,
                                                onClick = { selectedCategory = cat },
                                                label = { Text(cat.shortLabel) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!viewAllScansMode) {
                            items(items = filteredUsers, key = { it.user.id }) { overview ->
                                AdminUserExpandableCard(
                                    overview = overview,
                                    dateFormatter = dateFormatter,
                                    onSelectScan = onSelectScan,
                                    onDeleteScan = onDeleteScan,
                                    onDeleteUser = onDeleteUser
                                )
                            }
                        } else {
                            if (filteredAllScans.isEmpty()) {
                                item(key = "no_admin_scans") {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                                        border = BorderStroke(1.dp, semantic.cardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "No user scans match the current filter yet.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.onSurfaceVariant,
                                            modifier = Modifier.padding(20.dp)
                                        )
                                    }
                                }
                            } else {
                                items(items = filteredAllScans, key = { it.first.id }) { (scan, owner) ->
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                                        border = BorderStroke(1.dp, semantic.cardBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectScan(scan) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.DeleteOutline,
                                                            contentDescription = "Delete scan",
                                                            tint = colors.onSurfaceVariant,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = scan.productName,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = colors.onSurface
                                            )
                                            Text(
                                                text = "User: ${owner.displayName} (${owner.email}) • ${dateFormatter.format(Date(scan.createdAtMillis))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.primary
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
private fun AdminKpiTile(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AdminUserExpandableCard(
    overview: AdminUserOverview,
    dateFormatter: SimpleDateFormat,
    onSelectScan: (ScanRecord) -> Unit,
    onDeleteScan: (String) -> Unit,
    onDeleteUser: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    var expanded by remember(overview.user.id) { mutableStateOf(true) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, if (overview.user.isAdmin) colors.primary.copy(alpha = 0.35f) else semantic.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("admin_user_card_${overview.user.email}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (overview.user.isAdmin) colors.primaryContainer else colors.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (overview.user.isAdmin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Person,
                            contentDescription = null,
                            tint = if (overview.user.isAdmin) colors.primary else colors.onSurfaceVariant
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = overview.user.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = overview.user.role.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (overview.user.isAdmin) colors.primary else colors.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (overview.user.isAdmin) colors.primaryContainer else colors.surfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = overview.user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = "${overview.scans.size} scans • Avg: ${overview.averageScore?.toString() ?: "—"} • Joined ${dateFormatter.format(Date(overview.user.createdAtMillis))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = colors.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = semantic.cardBorder)

                    // User Health Profile Detailed Breakdown
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(semantic.softEmeraldSurface)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Saved Health Profile",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                        val p = overview.profile
                        if (p == null || (p.allergies.isEmpty() && p.medicalConditions.isEmpty() && p.dietaryPreferences.isEmpty() && p.chemicalSensitivities.isEmpty())) {
                            Text(
                                text = "No allergies, conditions, or sensitivities saved yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        } else {
                            if (p.allergies.isNotEmpty()) {
                                Text("• Allergies: ${p.allergies.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
                            }
                            if (p.medicalConditions.isNotEmpty()) {
                                Text("• Medical Conditions: ${p.medicalConditions.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
                            }
                            if (p.dietaryPreferences.isNotEmpty()) {
                                Text("• Dietary Preferences: ${p.dietaryPreferences.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
                            }
                            if (p.chemicalSensitivities.isNotEmpty()) {
                                Text("• Chemical Sensitivities: ${p.chemicalSensitivities.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
                            }
                        }
                    }

                    // User's Scans
                    Text(
                        text = "User Scans (${overview.scans.size}) — Tap any scan for full report",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurface
                    )
                    if (overview.scans.isEmpty()) {
                        Text(
                            text = "This user has not scanned any products yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    } else {
                        overview.scans.forEach { scan ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.background)
                                    .clickable { onSelectScan(scan) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = scan.productName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${scan.parsedCategory().shortLabel} • ${scan.ingredients.size} ingredients • ${dateFormatter.format(Date(scan.createdAtMillis))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                ScorePillBadge(score = scan.score, ratingLabel = scan.ratingLabel)
                                IconButton(
                                    onClick = { onDeleteScan(scan.id) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete scan",
                                        tint = colors.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!overview.user.isAdmin) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { onDeleteUser(overview.user.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = semantic.badBg,
                                    contentColor = semantic.bad
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Remove User & Data", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
