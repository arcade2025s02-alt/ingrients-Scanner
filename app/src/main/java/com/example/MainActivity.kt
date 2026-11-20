package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.LocalRepository
import com.example.data.SupabaseSyncStatus
import com.example.model.AppUser
import com.example.model.GroqConfigState
import com.example.model.UiState
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AiChatbotScreen
import com.example.ui.screens.AnalyticsHistoryScreen
import com.example.ui.screens.ExploreTopRatedScreen
import com.example.ui.screens.HealthProfileScreen
import com.example.ui.screens.LandingAuthScreen
import com.example.ui.screens.ScanResultDashboard
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.LocalSemanticRiskColors
import com.example.ui.theme.SmartRiskAnalyzerTheme
import com.example.viewmodel.SmartRiskViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartRiskAnalyzerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmartRiskRootApp()
                }
            }
        }
    }
}

private enum class AppTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    SCAN(R.string.nav_scan, Icons.Filled.DocumentScanner, Icons.Outlined.DocumentScanner, "nav_tab_scan"),
    EXPLORE(R.string.nav_explore, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_tab_explore"),
    ANALYTICS(R.string.nav_analytics, Icons.Filled.Analytics, Icons.Outlined.Analytics, "nav_tab_analytics"),
    CHAT(R.string.nav_chat, Icons.Filled.SmartToy, Icons.Outlined.SmartToy, "nav_tab_chat"),
    ADMIN(R.string.nav_admin, Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings, "nav_tab_admin"),
    PROFILE(R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person, "nav_tab_profile")
}

@Composable
fun SmartRiskRootApp() {
    val viewModel: SmartRiskViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                SmartRiskViewModel(LocalRepository(app))
            }
        }
    )

    val sessionReady by viewModel.sessionReady.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAuthBusy by viewModel.isAuthBusy.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()

    if (!sessionReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val user = currentUser
    if (user == null) {
        LandingAuthScreen(
            isAuthBusy = isAuthBusy,
            authError = authError,
            onSignIn = { email, pwd -> viewModel.signIn(email, pwd) },
            onSignUp = { name, email, pwd, confirm -> viewModel.signUp(name, email, pwd, confirm) },
            onClearError = { viewModel.clearAuthError() }
        )
    } else {
        AuthenticatedAppShell(
            currentUser = user,
            viewModel = viewModel
        )
    }
}

@Composable
private fun AuthenticatedAppShell(
    currentUser: AppUser,
    viewModel: SmartRiskViewModel
) {
    val healthProfileState by viewModel.healthProfileState.collectAsStateWithLifecycle()
    val scansState by viewModel.scansState.collectAsStateWithLifecycle()
    val topRatedState by viewModel.topRatedState.collectAsStateWithLifecycle()
    val adminOverviewState by viewModel.adminOverviewState.collectAsStateWithLifecycle()
    val scannerUiState by viewModel.scannerUiState.collectAsStateWithLifecycle()
    val selectedScan by viewModel.selectedScan.collectAsStateWithLifecycle()
    val recipeUiState by viewModel.recipeUiState.collectAsStateWithLifecycle()
    val profileSaveMessage by viewModel.profileSaveMessage.collectAsStateWithLifecycle()
    val passwordChangeMessage by viewModel.passwordChangeMessage.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chatContextScan by viewModel.chatContextScan.collectAsStateWithLifecycle()
    val isChatStreaming by viewModel.isChatStreaming.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val groqConfig by viewModel.groqConfig.collectAsStateWithLifecycle()
    val supabaseStatus by viewModel.supabaseStatus.collectAsStateWithLifecycle()
    val isSyncingSupabase by viewModel.isSyncingSupabase.collectAsStateWithLifecycle()

    val availableTabs = if (currentUser.isAdmin) {
        listOf(AppTab.ADMIN, AppTab.SCAN, AppTab.ANALYTICS, AppTab.CHAT, AppTab.PROFILE)
    } else {
        listOf(AppTab.SCAN, AppTab.EXPLORE, AppTab.ANALYTICS, AppTab.CHAT, AppTab.PROFILE)
    }

    var currentTab by rememberSaveable(currentUser.id) {
        mutableStateOf(if (currentUser.isAdmin) AppTab.ADMIN else AppTab.SCAN)
    }

    LaunchedEffect(currentUser.id) {
        if (!currentUser.isAdmin && currentTab == AppTab.ADMIN) {
            currentTab = AppTab.SCAN
        }
    }

    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current

    if (selectedScan != null) {
        ScanResultDashboard(
            scan = selectedScan!!,
            similarGoodProducts = (topRatedState as? UiState.Success)?.data.orEmpty(),
            recipeUiState = recipeUiState,
            onGenerateRecipes = { viewModel.generateRecipesForSelectedScan() },
            onAskAiAboutScan = { scan ->
                viewModel.attachScanToChat(scan)
                viewModel.clearSelectedScan()
                currentTab = AppTab.CHAT
            },
            onBack = { viewModel.clearSelectedScan() }
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        if (useNavigationRail) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = colors.surface,
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                ) {
                    val railTabs = if (currentUser.isAdmin) {
                        listOf(AppTab.ADMIN, AppTab.SCAN, AppTab.EXPLORE, AppTab.ANALYTICS, AppTab.CHAT, AppTab.PROFILE)
                    } else {
                        availableTabs
                    }
                    railTabs.forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.labelRes)
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AppBrandTopBar(isAdmin = currentUser.isAdmin, groqConfig = groqConfig, supabaseConnected = supabaseStatus.isConnected)
                    Box(modifier = Modifier.weight(1f)) {
                        ActiveTabContent(
                            currentTab = currentTab,
                            currentUser = currentUser,
                            healthProfileState = healthProfileState,
                            scansState = scansState,
                            topRatedState = topRatedState,
                            adminOverviewState = adminOverviewState,
                            scannerUiState = scannerUiState,
                            groqConfig = groqConfig,
                            supabaseStatus = supabaseStatus,
                            isSyncingSupabase = isSyncingSupabase,
                            profileSaveMessage = profileSaveMessage,
                            passwordChangeMessage = passwordChangeMessage,
                            chatMessages = chatMessages,
                            chatContextScan = chatContextScan,
                            isChatStreaming = isChatStreaming,
                            chatError = chatError,
                            viewModel = viewModel,
                            onNavigateTab = { currentTab = it }
                        )
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = colors.background,
                topBar = { AppBrandTopBar(isAdmin = currentUser.isAdmin, groqConfig = groqConfig, supabaseConnected = supabaseStatus.isConnected) },
                bottomBar = {
                    NavigationBar(
                        containerColor = colors.surface,
                        windowInsets = WindowInsets.navigationBars,
                        modifier = Modifier.border(0.5.dp, semantic.cardBorder)
                    ) {
                        availableTabs.forEach { tab ->
                            val isSelected = currentTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ActiveTabContent(
                        currentTab = currentTab,
                        currentUser = currentUser,
                        healthProfileState = healthProfileState,
                        scansState = scansState,
                        topRatedState = topRatedState,
                        adminOverviewState = adminOverviewState,
                        scannerUiState = scannerUiState,
                        groqConfig = groqConfig,
                        supabaseStatus = supabaseStatus,
                        isSyncingSupabase = isSyncingSupabase,
                        profileSaveMessage = profileSaveMessage,
                        passwordChangeMessage = passwordChangeMessage,
                        chatMessages = chatMessages,
                        chatContextScan = chatContextScan,
                        isChatStreaming = isChatStreaming,
                        chatError = chatError,
                        viewModel = viewModel,
                        onNavigateTab = { currentTab = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppBrandTopBar(
    isAdmin: Boolean,
    groqConfig: GroqConfigState,
    supabaseConnected: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val semantic = LocalSemanticRiskColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface)
            .statusBarsPadding()
            .border(0.5.dp, semantic.cardBorder)
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
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp)),
                contentScale = ContentScale.Crop
            )
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                val engineLabel = if (groqConfig.isReadyForAi) "Groq Free AI" else "Offline Engine"
                val dbLabel = if (supabaseConnected) "Supabase Cloud" else "Supabase + Local"
                Text(
                    text = "$engineLabel • $dbLabel • WHO/FSSAI/EPA",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary
                )
            }
        }
        if (isAdmin) {
            Text(
                text = "ADMIN",
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ActiveTabContent(
    currentTab: AppTab,
    currentUser: AppUser,
    healthProfileState: UiState<com.example.model.HealthProfile>,
    scansState: UiState<List<com.example.model.ScanRecord>>,
    topRatedState: UiState<List<com.example.model.TopRatedProduct>>,
    adminOverviewState: UiState<List<com.example.model.AdminUserOverview>>,
    scannerUiState: com.example.viewmodel.ScannerUiState,
    groqConfig: GroqConfigState,
    supabaseStatus: SupabaseSyncStatus,
    isSyncingSupabase: Boolean,
    profileSaveMessage: String?,
    passwordChangeMessage: String?,
    chatMessages: List<com.example.model.ChatMessage>,
    chatContextScan: com.example.model.ScanRecord?,
    isChatStreaming: Boolean,
    chatError: String?,
    viewModel: SmartRiskViewModel,
    onNavigateTab: (AppTab) -> Unit
) {
    when (currentTab) {
        AppTab.ADMIN -> AdminPanelScreen(
            adminUser = currentUser,
            overviewState = adminOverviewState,
            supabaseStatus = supabaseStatus,
            isSyncingSupabase = isSyncingSupabase,
            onRefreshSupabaseSync = { viewModel.refreshSupabaseSync() },
            onSelectScan = { viewModel.selectScan(it) },
            onDeleteScan = { viewModel.deleteScan(it) },
            onDeleteUser = { viewModel.adminDeleteUser(it) },
            onOpenChangePassword = { onNavigateTab(AppTab.PROFILE) }
        )

        AppTab.SCAN -> ScannerScreen(
            scannerUiState = scannerUiState,
            groqConfig = groqConfig,
            healthProfile = (healthProfileState as? UiState.Success)?.data,
            recentScans = (scansState as? UiState.Success)?.data.orEmpty(),
            onAnalyze = { nameHint, text, bitmap ->
                viewModel.analyzeProduct(
                    productNameHint = nameHint,
                    ingredientText = text,
                    bitmap = bitmap
                )
            },
            onDismissError = { viewModel.clearScannerError() },
            onSelectScan = { viewModel.selectScan(it) },
            onOpenProfile = { onNavigateTab(AppTab.PROFILE) },
            onOpenGroqSettings = { onNavigateTab(AppTab.PROFILE) }
        )

        AppTab.EXPLORE -> ExploreTopRatedScreen(
            topRatedState = topRatedState
        )

        AppTab.ANALYTICS -> AnalyticsHistoryScreen(
            scansState = scansState,
            onSelectScan = { viewModel.selectScan(it) },
            onDeleteScan = { viewModel.deleteScan(it) },
            onStartNewScan = { onNavigateTab(AppTab.SCAN) }
        )

        AppTab.CHAT -> AiChatbotScreen(
            messages = chatMessages,
            groqConfig = groqConfig,
            activeScan = chatContextScan,
            isStreaming = isChatStreaming,
            chatError = chatError,
            onSendMessage = { viewModel.sendChatMessage(it) },
            onDetachScanContext = { viewModel.attachScanToChat(null) },
            onClearChat = { viewModel.clearChat() },
            onOpenGroqSettings = { onNavigateTab(AppTab.PROFILE) }
        )

        AppTab.PROFILE -> HealthProfileScreen(
            currentUser = currentUser,
            profileState = healthProfileState,
            groqConfig = groqConfig,
            supabaseStatus = supabaseStatus,
            isSyncingSupabase = isSyncingSupabase,
            saveMessage = profileSaveMessage,
            passwordChangeMessage = passwordChangeMessage,
            onSaveProfile = { viewModel.saveHealthProfile(it) },
            onClearSaveMessage = { viewModel.clearProfileSaveMessage() },
            onVerifyGroqKey = { viewModel.verifyGroqApiKey(it) },
            onSelectGroqModel = { viewModel.selectGroqFreeModel(it) },
            onClearGroqKey = { viewModel.clearGroqConfig() },
            onRefreshSupabaseSync = { viewModel.refreshSupabaseSync() },
            onChangePassword = { current, next, confirm ->
                viewModel.changePassword(current, next, confirm)
            },
            onClearPasswordMessage = { viewModel.clearPasswordChangeMessage() },
            onSignOut = { viewModel.signOut() }
        )
    }
}
