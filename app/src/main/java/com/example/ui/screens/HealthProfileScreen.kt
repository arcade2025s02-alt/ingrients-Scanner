package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Refresh
import com.example.data.SupabaseSyncStatus
import com.example.model.AppUser
import com.example.model.GroqConfigState
import com.example.model.HealthProfile
import com.example.model.UiState
import com.example.ui.components.GroqApiSetupCard
import com.example.ui.theme.LocalSemanticRiskColors

private val AllergyPresets = listOf("Peanuts", "Tree Nuts", "Dairy", "Gluten", "Soy", "Eggs", "Shellfish", "Sulfites", "Artificial Dyes")
private val MedicalPresets = listOf("Diabetes", "Hypertension", "Celiac Disease", "Heart Disease", "Kidney Disease", "Asthma", "PCOS / Thyroid")
private val DietaryPresets = listOf("Vegetarian", "Vegan", "Low Sodium", "Low Sugar", "Gluten-Free", "Organic Only", "High Protein")
private val ChemicalPresets = listOf("MSG (E621)", "Aspartame", "Palm Oil", "Sodium Benzoate", "BPA / Phthalates", "PFAS", "Synthetic Nitrates", "Parabens")

@Composable
fun HealthProfileScreen(
    currentUser: AppUser,
    profileState: UiState<HealthProfile>,
    groqConfig: GroqConfigState,
    supabaseStatus: SupabaseSyncStatus,
    isSyncingSupabase: Boolean,
    saveMessage: String?,
    passwordChangeMessage: String?,
    onSaveProfile: (HealthProfile) -> Unit,
    onClearSaveMessage: () -> Unit,
    onVerifyGroqKey: (String) -> Unit,
    onSelectGroqModel: (String) -> Unit,
    onClearGroqKey: () -> Unit,
    onRefreshSupabaseSync: () -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String, confirmNewPassword: String) -> Unit,
    onClearPasswordMessage: () -> Unit,
    onSignOut: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current

    var allergies by remember(currentUser.id) { mutableStateOf<List<String>>(emptyList()) }
    var medicalConditions by remember(currentUser.id) { mutableStateOf<List<String>>(emptyList()) }
    var dietaryPreferences by remember(currentUser.id) { mutableStateOf<List<String>>(emptyList()) }
    var chemicalSensitivities by remember(currentUser.id) { mutableStateOf<List<String>>(emptyList()) }
    var initializedFromDb by remember(currentUser.id) { mutableStateOf(false) }

    // Change Password local fields
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var passwordsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(profileState) {
        if (profileState is UiState.Success && !initializedFromDb) {
            allergies = profileState.data.allergies
            medicalConditions = profileState.data.medicalConditions
            dietaryPreferences = profileState.data.dietaryPreferences
            chemicalSensitivities = profileState.data.chemicalSensitivities
            initializedFromDb = true
        }
    }

    LaunchedEffect(passwordChangeMessage) {
        if (passwordChangeMessage != null && !passwordChangeMessage.startsWith("Error:")) {
            currentPassword = ""
            newPassword = ""
            confirmNewPassword = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            // Account Header Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentUser.isAdmin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.Person,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentUser.displayName.ifBlank { "Smart Risk Member" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.onSurface
                                )
                                if (currentUser.isAdmin) {
                                    Text(
                                        text = "ADMIN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(colors.primaryContainer)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentUser.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Sign Out",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Logout")
                    }
                }
            }

            // Groq Free Tier AI Key & Model Selector Card
            GroqApiSetupCard(
                groqConfig = groqConfig,
                onVerifyKey = onVerifyGroqKey,
                onSelectModel = onSelectGroqModel,
                onClearKey = onClearGroqKey
            )

            // Supabase Cloud Database Sync Status Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (supabaseStatus.isConnected) semantic.softEmeraldSurface else colors.surface
                ),
                border = BorderStroke(
                    1.dp,
                    if (supabaseStatus.isConnected) colors.primary.copy(alpha = 0.35f) else semantic.cardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (supabaseStatus.isConnected) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = if (supabaseStatus.isConnected) colors.primary else colors.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Supabase Backend (${supabaseStatus.projectId})",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = supabaseStatus.statusLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onRefreshSupabaseSync,
                        enabled = !isSyncingSupabase,
                        modifier = Modifier.testTag("profile_sync_supabase_button")
                    ) {
                        if (isSyncingSupabase) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Sync Supabase",
                                tint = colors.primary
                            )
                        }
                    }
                }
            }

            // Change Password Card (highlighted when default admin password is active)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentUser.mustChangePassword) semantic.moderateBg.copy(alpha = 0.45f) else colors.surface
                ),
                border = BorderStroke(
                    width = if (currentUser.mustChangePassword) 1.5.dp else 1.dp,
                    color = if (currentUser.mustChangePassword) semantic.moderate else semantic.cardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = if (currentUser.mustChangePassword) semantic.moderate else colors.primary
                        )
                        Column {
                            Text(
                                text = if (currentUser.mustChangePassword) "Change Default Password (Recommended)" else "Change Account Password",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface
                            )
                            Text(
                                text = "Update your login password securely on this device.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            onClearPasswordMessage()
                        },
                        label = { Text("Current Password") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordsVisible = !passwordsVisible }) {
                                Icon(
                                    imageVector = if (passwordsVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        },
                        visualTransformation = if (passwordsVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_password_current_input")
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            onClearPasswordMessage()
                        },
                        label = { Text("New Password (min 6 chars)") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        visualTransformation = if (passwordsVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_password_new_input")
                    )

                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = {
                            confirmNewPassword = it
                            onClearPasswordMessage()
                        },
                        label = { Text("Confirm New Password") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        visualTransformation = if (passwordsVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_password_confirm_input")
                    )

                    AnimatedVisibility(visible = passwordChangeMessage != null) {
                        val isErr = passwordChangeMessage?.startsWith("Error:") == true
                        Text(
                            text = passwordChangeMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isErr) semantic.bad else semantic.excellent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            onChangePassword(currentPassword, newPassword, confirmNewPassword)
                        },
                        enabled = currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmNewPassword.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("update_password_button")
                    ) {
                        Text("Update Password")
                    }
                }
            }

            // Personal Health Profile Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.HealthAndSafety,
                    contentDescription = null,
                    tint = colors.primary
                )
                Text(
                    text = "Personal Health Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground
                )
            }
            Text(
                text = "Synced with Supabase (and cached locally offline). Every scan cross-checks your triggers for instant red allergen & medical alerts.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            if (profileState is UiState.Loading && !initializedFromDb) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                ProfileChipEditorCard(
                    title = "Allergies",
                    subtitle = "Ingredients that trigger allergic reactions",
                    presets = AllergyPresets,
                    selected = allergies,
                    onUpdate = {
                        allergies = it
                        onClearSaveMessage()
                    }
                )

                ProfileChipEditorCard(
                    title = "Medical Conditions",
                    subtitle = "Health conditions requiring sodium, sugar, or chemical limits",
                    presets = MedicalPresets,
                    selected = medicalConditions,
                    onUpdate = {
                        medicalConditions = it
                        onClearSaveMessage()
                    }
                )

                ProfileChipEditorCard(
                    title = "Dietary Preferences",
                    subtitle = "Nutritional & lifestyle goals",
                    presets = DietaryPresets,
                    selected = dietaryPreferences,
                    onUpdate = {
                        dietaryPreferences = it
                        onClearSaveMessage()
                    }
                )

                ProfileChipEditorCard(
                    title = "Chemical & Additive Sensitivities",
                    subtitle = "Food additives, packaging plastics, or agricultural chemicals to avoid",
                    presets = ChemicalPresets,
                    selected = chemicalSensitivities,
                    onUpdate = {
                        chemicalSensitivities = it
                        onClearSaveMessage()
                    }
                )

                AnimatedVisibility(visible = saveMessage != null) {
                    val isErr = saveMessage?.startsWith("Save failed") == true
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isErr) semantic.badBg else semantic.excellentBg
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isErr) Icons.Rounded.Close else Icons.Rounded.Check,
                                contentDescription = null,
                                tint = if (isErr) semantic.bad else semantic.excellent
                            )
                            Text(
                                text = saveMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isErr) semantic.bad else semantic.excellent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onSaveProfile(
                            HealthProfile(
                                id = currentUser.id,
                                userId = currentUser.id,
                                displayName = currentUser.displayName.takeIf { it.isNotBlank() },
                                allergies = allergies,
                                medicalConditions = medicalConditions,
                                dietaryPreferences = dietaryPreferences,
                                chemicalSensitivities = chemicalSensitivities
                            )
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Save Health Profile",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileChipEditorCard(
    title: String,
    subtitle: String,
    presets: List<String>,
    selected: List<String>,
    onUpdate: (List<String>) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    var customEntry by remember { mutableStateOf("") }

    val allOptions = remember(presets, selected) {
        (presets + selected).distinct()
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allOptions.forEach { option ->
                    val isChecked = option in selected
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            val next = if (isChecked) selected - option else selected + option
                            onUpdate(next)
                        },
                        label = { Text(option) },
                        leadingIcon = if (isChecked) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = customEntry,
                    onValueChange = { customEntry = it },
                    placeholder = { Text("Add custom item...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val trimmed = customEntry.trim()
                        if (trimmed.isNotBlank() && trimmed !in selected) {
                            onUpdate(selected + trimmed)
                            customEntry = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add custom entry",
                        tint = colors.primary
                    )
                }
            }
        }
    }
}
