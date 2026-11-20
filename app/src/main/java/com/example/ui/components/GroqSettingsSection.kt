package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.GroqConfigState
import com.example.model.GroqFreeModel
import com.example.model.GroqFreeModelCatalog
import com.example.ui.theme.LocalSemanticRiskColors

@Composable
fun GroqApiSetupCard(
    groqConfig: GroqConfigState,
    onVerifyKey: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onClearKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current

    var keyInput by remember { mutableStateOf(groqConfig.apiKey) }
    var showKey by remember { mutableStateOf(false) }

    LaunchedEffect(groqConfig.apiKey) {
        if (groqConfig.apiKey.isNotBlank() && keyInput.isBlank()) {
            keyInput = groqConfig.apiKey
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(
            width = if (groqConfig.isReadyForAi) 1.5.dp else 1.dp,
            color = if (groqConfig.isReadyForAi) colors.primary.copy(alpha = 0.45f) else semantic.cardBorder
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Groq Free Tier AI Setup",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (groqConfig.isReadyForAi) {
                                "Active Model: ${groqConfig.selectedModelId}"
                            } else {
                                "Enter your free Groq API key to unlock Free Plan models"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (groqConfig.isReadyForAi) colors.primary else colors.onSurfaceVariant
                        )
                    }
                }
                if (groqConfig.isKeyVerified) {
                    TextButton(
                        onClick = {
                            keyInput = ""
                            onClearKey()
                        },
                        modifier = Modifier.testTag("groq_disconnect_button")
                    ) {
                        Text("Remove", color = semantic.bad, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // API Key Text Field
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("Groq API Key (gsk_...)") },
                placeholder = { Text("Paste your free Groq API key here") },
                leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (keyInput.isNotBlank()) {
                            IconButton(onClick = { keyInput = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear key")
                            }
                        }
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Toggle API key visibility"
                            )
                        }
                    }
                },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                enabled = !groqConfig.isVerifying,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("groq_api_key_input")
            )

            // Verify Key Button + Create Free Key URL Button
            Button(
                onClick = { onVerifyKey(keyInput) },
                enabled = !groqConfig.isVerifying && keyInput.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("groq_verify_key_button")
            ) {
                if (groqConfig.isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = colors.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Verifying Key & Loading Free Models...")
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (groqConfig.isKeyVerified) "Re-Verify & Refresh Free Models" else "Verify Key & Show Free Models")
                }
            }

            // Create Free API Key link (https://console.groq.com/keys)
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GroqFreeModelCatalog.CONSOLE_KEYS_URL))
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, colors.primary),
                contentPadding = PaddingValues(vertical = 11.dp, horizontal = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("groq_create_free_key_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Don't have a key? Create Free API Key",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = GroqFreeModelCatalog.CONSOLE_KEYS_URL,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            // Error Banner
            AnimatedVisibility(visible = groqConfig.verificationError != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(semantic.badBg)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = semantic.bad,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = groqConfig.verificationError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.bad
                    )
                }
            }

            // ONLY show Free Plan Models AFTER the user enters and verifies a valid Groq API key
            AnimatedVisibility(visible = groqConfig.isKeyVerified && groqConfig.verifiedFreeModels.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = semantic.cardBorder)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(semantic.excellentBg)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = semantic.excellent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Key Verified • Showing Free Plan Models Only (${groqConfig.verifiedFreeModels.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = semantic.excellent,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Rate Limit Abbreviations Full-Form Legend
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(semantic.softEmeraldSurface)
                            .border(1.dp, colors.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Free Plan Rate Limits — Full Forms",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• RPM: Requests Per Minute\n• RPD: Requests Per Day\n• TPM: Tokens Per Minute\n• TPD: Tokens Per Day\n• ASH / ASD: Audio Seconds Per Hour / Day (Whisper)",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurface
                        )
                    }

                    Text(
                        text = "Choose a Free Tier Model:",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    groqConfig.verifiedFreeModels.forEach { model ->
                        FreeModelOptionCard(
                            model = model,
                            selected = groqConfig.selectedModelId == model.id,
                            onSelect = { onSelectModel(model.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FreeModelOptionCard(
    model: GroqFreeModel,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    val selectable = model.capability.supportsChatAndAnalysis

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                when {
                    selected -> semantic.softEmeraldSurface
                    selectable -> colors.background
                    else -> colors.surfaceVariant.copy(alpha = 0.45f)
                }
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.primary else semantic.cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .then(
                if (selectable) Modifier.clickable { onSelect() } else Modifier
            )
            .padding(12.dp)
            .testTag("groq_model_card_${model.id.replace("/", "_")}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = if (selected) "Selected model" else "Select model",
                    tint = when {
                        selected -> colors.primary
                        selectable -> colors.onSurfaceVariant
                        else -> colors.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = model.id,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = model.capability.badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectable) colors.primary else colors.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selectable) colors.primaryContainer else colors.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        if (model.recommended) {
                            Text(
                                text = "Recommended",
                                style = MaterialTheme.typography.labelSmall,
                                color = semantic.excellent,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(semantic.excellentBg)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2x2 Grid showing RPM, RPD, TPM, TPD with full forms
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RateLimitPill(
                code = "RPM",
                fullForm = "Requests Per Minute",
                value = model.rpm,
                modifier = Modifier.weight(1f)
            )
            RateLimitPill(
                code = "RPD",
                fullForm = "Requests Per Day",
                value = model.rpd,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RateLimitPill(
                code = "TPM",
                fullForm = "Tokens Per Minute",
                value = model.tpm,
                modifier = Modifier.weight(1f)
            )
            RateLimitPill(
                code = "TPD",
                fullForm = "Tokens Per Day",
                value = model.tpd,
                modifier = Modifier.weight(1f)
            )
        }

        if (model.ash != "-" || model.asd != "-") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RateLimitPill(
                    code = "ASH",
                    fullForm = "Audio Secs / Hour",
                    value = model.ash,
                    modifier = Modifier.weight(1f)
                )
                RateLimitPill(
                    code = "ASD",
                    fullForm = "Audio Secs / Day",
                    value = model.asd,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (model.notes.isNotBlank()) {
            Text(
                text = model.notes,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RateLimitPill(
    code: String,
    fullForm: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .border(0.75.dp, semantic.cardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = code,
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = fullForm,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
}
