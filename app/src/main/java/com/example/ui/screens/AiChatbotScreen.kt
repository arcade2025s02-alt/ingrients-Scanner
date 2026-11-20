package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.Key
import com.example.model.ChatMessage
import com.example.model.GroqConfigState
import com.example.model.ScanRecord
import com.example.ui.components.ScorePillBadge
import com.example.ui.theme.LocalSemanticRiskColors

private val QuickQuestions = listOf(
    "Is this safe for my health profile?",
    "Explain the highest-risk additive",
    "What does FSSAI / WHO say about this?",
    "Suggest a clean natural swap"
)

@Composable
fun AiChatbotScreen(
    messages: List<ChatMessage>,
    groqConfig: GroqConfigState,
    activeScan: ScanRecord?,
    isStreaming: Boolean,
    chatError: String?,
    onSendMessage: (String) -> Unit,
    onDetachScanContext: () -> Unit,
    onClearChat: () -> Unit,
    onOpenGroqSettings: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .border(0.5.dp, semantic.cardBorder)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Health & Ingredient Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface
                        )
                        Text(
                            text = if (groqConfig.isReadyForAi) {
                                "Groq Free AI • ${groqConfig.selectedModelId}"
                            } else {
                                "Built-in Engine • Tap key icon to add Free Groq API"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (groqConfig.isReadyForAi) colors.primary else colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenGroqSettings,
                        modifier = Modifier.testTag("chat_groq_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = "Groq API & Model Settings",
                            tint = if (groqConfig.isReadyForAi) colors.primary else colors.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear chat",
                            tint = colors.onSurfaceVariant
                        )
                    }
                }
            }

            // Active Scanned Product Context Banner
            if (activeScan != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(semantic.softEmeraldSurface)
                        .border(0.5.dp, colors.primary.copy(alpha = 0.25f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Discussing scanned product:",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primary
                        )
                        Text(
                            text = activeScan.productName,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    ScorePillBadge(score = activeScan.score, ratingLabel = activeScan.ratingLabel)
                    IconButton(
                        onClick = onDetachScanContext,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear product context",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items = messages, key = { it.id }) { msg ->
                    val fromUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 18.dp,
                                bottomStart = if (fromUser) 18.dp else 4.dp,
                                bottomEnd = if (fromUser) 4.dp else 18.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (fromUser) colors.primary else colors.surface
                            ),
                            border = if (fromUser) null else BorderStroke(1.dp, semantic.cardBorder),
                            modifier = Modifier.widthIn(max = 310.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                if (msg.content.isBlank() && msg.streaming) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Thinking...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Text(
                                        text = msg.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (fromUser) colors.onPrimary else colors.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                if (chatError != null) {
                    item(key = "chat_error") {
                        Text(
                            text = chatError,
                            style = MaterialTheme.typography.bodySmall,
                            color = semantic.bad,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // Quick Suggestion Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickQuestions.forEachIndexed { idx, prompt ->
                    AssistChip(
                        onClick = { if (!isStreaming) onSendMessage(prompt) },
                        label = { Text(prompt, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("chat_suggestion_$idx")
                    )
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .border(0.5.dp, semantic.cardBorder)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask about ingredients, additives, or alternatives...") },
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )
                IconButton(
                    onClick = {
                        val toSend = inputText.trim()
                        if (toSend.isNotBlank() && !isStreaming) {
                            inputText = ""
                            onSendMessage(toSend)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isStreaming,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && !isStreaming) colors.primary
                            else colors.surfaceVariant
                        )
                        .testTag("chat_send_button")
                ) {
                    if (isStreaming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colors.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send message",
                            tint = if (inputText.isNotBlank()) colors.onPrimary else colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
