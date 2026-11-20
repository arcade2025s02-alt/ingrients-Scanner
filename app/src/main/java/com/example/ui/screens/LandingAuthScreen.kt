package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.LocalSemanticRiskColors
import kotlinx.coroutines.launch

private data class FeatureCardItem(
    val title: String,
    val description: String,
    val icon: ImageVector
)

private val LandingFeatures = listOf(
    FeatureCardItem(
        title = "Smart Scanner",
        description = "Analyze packaged foods, fertilizers & packaging labels 100% offline.",
        icon = Icons.Rounded.DocumentScanner
    ),
    FeatureCardItem(
        title = "Risk Analysis",
        description = "Instant 0–100 scores grounded in WHO, FSSAI & EPA standards.",
        icon = Icons.Rounded.HealthAndSafety
    ),
    FeatureCardItem(
        title = "Ingredient Assistant",
        description = "Ask questions about additives, INS codes & cleaner swaps at zero cost.",
        icon = Icons.Rounded.SmartToy
    ),
    FeatureCardItem(
        title = "Personal Profile",
        description = "Save allergies, medical conditions & sensitivities for live alerts.",
        icon = Icons.Rounded.PersonOutline
    ),
    FeatureCardItem(
        title = "Visual Analytics",
        description = "Track your average safety scores and healthier choices over time.",
        icon = Icons.Rounded.Analytics
    ),
    FeatureCardItem(
        title = "Scan History",
        description = "Private on-device database of every scanned product and recipe.",
        icon = Icons.Rounded.History
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingAuthScreen(
    isAuthBusy: Boolean,
    authError: String?,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (displayName: String, email: String, password: String, confirmPassword: String) -> Unit,
    onClearError: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current

    var showAuthSheet by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(false) }

    fun openAuth(signUp: Boolean) {
        onClearError()
        isSignUpMode = signUp
        showAuthSheet = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Sticky Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .border(width = 0.5.dp, color = semantic.cardBorder)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo_clean),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                }

                Button(
                    onClick = { openAuth(signUp = false) },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("landing_top_get_started_button")
                ) {
                    Text(
                        text = stringResource(R.string.cta_get_started),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Scrollable Landing Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(22.dp))

                    // Movement Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.primaryContainer)
                            .border(1.dp, colors.primary.copy(alpha = 0.25f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Eco,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.movement_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = stringResource(R.string.hero_headline),
                        style = MaterialTheme.typography.displayLarge,
                        color = colors.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.hero_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    // Primary & Secondary CTA buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { openAuth(signUp = true) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.25f)
                                .height(52.dp)
                                .testTag("hero_get_started_free_button")
                        ) {
                            Text(
                                text = stringResource(R.string.cta_get_started_free),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        OutlinedButton(
                            onClick = { openAuth(signUp = false) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, colors.primary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .weight(0.95f)
                                .height(52.dp)
                                .testTag("hero_sign_in_button")
                        ) {
                            Text(
                                text = "Sign In",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    // Generated Editorial Hero Artwork
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_banner_1795079060187),
                            contentDescription = "Smart Risk Analyzer Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(195.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Trust strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        listOf("100% Free", "WHO & FSSAI", "EPA Safety").forEach { std ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surface)
                                    .border(1.dp, semantic.cardBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = std,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(34.dp))

                    // 6-Card Feature Grid
                    Text(
                        text = "Everything You Need to Decode Labels",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Zero subscription, zero API cost — works privately on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))

                    LandingFeatures.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { item ->
                                FeatureGridCard(
                                    item = item,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(26.dp))

                    // How It Works 3-Step Section
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "How It Works",
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.onSurface
                            )
                            HowItWorksStep(
                                number = "1",
                                title = "Scan or Enter",
                                description = "Attach a label photo or type ingredients for food, fertilizer, or packaging."
                            )
                            HowItWorksStep(
                                number = "2",
                                title = "Instant Offline Risk Analysis",
                                description = "Built-in WHO, FSSAI & EPA ingredient engine checks additives, chemical codes & your health profile."
                            )
                            HowItWorksStep(
                                number = "3",
                                title = "Get Insights",
                                description = "Review your 0–100 score, allergen alerts, healthier swaps, balancing recipes, and analytics."
                            )
                        }
                    }

                    Spacer(Modifier.height(26.dp))

                    // Green Bottom CTA Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(EmeraldPrimary, EmeraldPrimaryDark)
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Start Scanning Free Today",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Sign up or log in with email & password to save your health profile, scan history, and safety analytics locally at zero cost.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = { openAuth(signUp = false) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = EmeraldPrimaryDark
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("bottom_cta_login_button")
                            ) {
                                Text(
                                    text = "Login / Create Account",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    // Email & Password Login / Sign Up Bottom Sheet
    if (showAuthSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showAuthSheet = false
                onClearError()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface
        ) {
            EmailPasswordAuthSheetContent(
                isSignUpMode = isSignUpMode,
                onModeChange = {
                    isSignUpMode = it
                    onClearError()
                },
                isAuthBusy = isAuthBusy,
                authError = authError,
                onSignIn = onSignIn,
                onSignUp = onSignUp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailPasswordAuthSheetContent(
    isSignUpMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    isAuthBusy: Boolean,
    authError: String?,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (displayName: String, email: String, password: String, confirmPassword: String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current

    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (isSignUpMode) "Create Free Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onSurface
        )
        Text(
            text = if (isSignUpMode) {
                "Sign up with email & password. Stored securely on your device at zero cost."
            } else {
                "Sign in with your user or admin email & password."
            },
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !isSignUpMode,
                onClick = { onModeChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                modifier = Modifier.testTag("auth_tab_sign_in")
            ) {
                Text("Login")
            }
            SegmentedButton(
                selected = isSignUpMode,
                onClick = { onModeChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                modifier = Modifier.testTag("auth_tab_sign_up")
            ) {
                Text("Sign Up")
            }
        }

        if (authError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = semantic.badBg),
                border = BorderStroke(1.dp, semantic.bad.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = authError,
                    style = MaterialTheme.typography.bodySmall,
                    color = semantic.bad,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (isSignUpMode) {
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_name_input")
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email / Username") },
            placeholder = { Text("you@example.com") },
            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_email_input")
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_password_input")
        )

        if (isSignUpMode) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_confirm_password_input")
            )
        }

        Button(
            onClick = {
                if (isSignUpMode) {
                    onSignUp(displayName, email, password, confirmPassword)
                } else {
                    onSignIn(email, password)
                }
            },
            enabled = !isAuthBusy,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("auth_submit_button")
        ) {
            if (isAuthBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = colors.onPrimary
                )
            } else {
                Text(
                    text = if (isSignUpMode) "Create Account" else "Login",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun FeatureGridCard(
    item: FeatureCardItem,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HowItWorksStep(
    number: String,
    title: String,
    description: String
) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}
