package com.example.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GroqApiService
import com.example.data.LocalAssistantEngine
import com.example.data.LocalRecipeEngine
import com.example.data.LocalRepository
import com.example.data.LocalRiskAnalyzerEngine
import com.example.data.SupabaseSyncStatus
import com.example.model.AdminUserOverview
import com.example.model.AppUser
import com.example.model.ChatMessage
import com.example.model.GroqConfigState
import com.example.model.HealthProfile
import com.example.model.ScanRecord
import com.example.model.SmartRecipe
import com.example.model.TopRatedProduct
import com.example.model.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface ScannerUiState {
    data object Idle : ScannerUiState
    data class Analyzing(val stepLabel: String) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
}

sealed interface RecipeUiState {
    data object Idle : RecipeUiState
    data object Loading : RecipeUiState
    data class Success(val recipes: List<SmartRecipe>) : RecipeUiState
    data class Error(val message: String) : RecipeUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class SmartRiskViewModel(
    private val repository: LocalRepository
) : ViewModel() {

    val currentUser: StateFlow<AppUser?> = repository.currentUser
    val supabaseStatus: StateFlow<SupabaseSyncStatus> = repository.supabaseStatus

    private val _sessionReady = MutableStateFlow(false)
    val sessionReady: StateFlow<Boolean> = _sessionReady.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthBusy = MutableStateFlow(false)
    val isAuthBusy: StateFlow<Boolean> = _isAuthBusy.asStateFlow()

    private val _passwordChangeMessage = MutableStateFlow<String?>(null)
    val passwordChangeMessage: StateFlow<String?> = _passwordChangeMessage.asStateFlow()

    private val _isSyncingSupabase = MutableStateFlow(false)
    val isSyncingSupabase: StateFlow<Boolean> = _isSyncingSupabase.asStateFlow()

    private val _groqConfig = MutableStateFlow(loadSavedGroqConfig())
    val groqConfig: StateFlow<GroqConfigState> = _groqConfig.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeAndRestoreSession()
            _sessionReady.value = true
        }
    }

    private fun loadSavedGroqConfig(): GroqConfigState {
        val prefs = repository.groqPrefs
        val savedKey = prefs.getApiKey()
        val verified = prefs.isVerified() && savedKey.isNotBlank()
        val models = if (verified) prefs.getVerifiedFreeModels() else emptyList()
        val savedModel = prefs.getSelectedModelId().ifBlank {
            models.firstOrNull { it.capability.supportsChatAndAnalysis }?.id.orEmpty()
        }
        return GroqConfigState(
            apiKey = savedKey,
            isKeyVerified = verified,
            verifiedFreeModels = models,
            selectedModelId = savedModel
        )
    }

    // Groq API Key & Free-Tier Model Picker Actions
    fun verifyGroqApiKey(apiKeyInput: String) {
        val cleanKey = apiKeyInput.trim()
        if (cleanKey.isBlank()) {
            _groqConfig.value = _groqConfig.value.copy(
                verificationError = "Please paste your Groq API key, or tap 'Create Free Groq API Key' below."
            )
            return
        }
        viewModelScope.launch {
            _groqConfig.value = _groqConfig.value.copy(
                apiKey = cleanKey,
                isVerifying = true,
                verificationError = null
            )
            val result = GroqApiService.verifyKeyAndFetchFreeModels(cleanKey)
            result.fold(
                onSuccess = { freeModels ->
                    val defaultModel = freeModels.firstOrNull { it.recommended && it.capability.supportsChatAndAnalysis }
                        ?: freeModels.firstOrNull { it.capability.supportsChatAndAnalysis }
                        ?: freeModels.firstOrNull()
                    val selectedId = defaultModel?.id.orEmpty()
                    repository.groqPrefs.saveVerifiedConfig(cleanKey, freeModels, selectedId)
                    _groqConfig.value = GroqConfigState(
                        apiKey = cleanKey,
                        isKeyVerified = true,
                        isVerifying = false,
                        verificationError = null,
                        verifiedFreeModels = freeModels,
                        selectedModelId = selectedId
                    )
                },
                onFailure = { err ->
                    _groqConfig.value = _groqConfig.value.copy(
                        isKeyVerified = false,
                        isVerifying = false,
                        verificationError = err.localizedMessage ?: "Failed to verify Groq API key.",
                        verifiedFreeModels = emptyList(),
                        selectedModelId = ""
                    )
                }
            )
        }
    }

    fun selectGroqFreeModel(modelId: String) {
        val model = _groqConfig.value.verifiedFreeModels.firstOrNull { it.id == modelId } ?: return
        if (!model.capability.supportsChatAndAnalysis) return
        repository.groqPrefs.saveSelectedModel(model.id)
        _groqConfig.value = _groqConfig.value.copy(selectedModelId = model.id)
    }

    fun clearGroqConfig() {
        repository.groqPrefs.clearConfig()
        _groqConfig.value = GroqConfigState()
    }

    fun refreshSupabaseSync() {
        viewModelScope.launch {
            _isSyncingSupabase.value = true
            repository.syncFromSupabase()
            _isSyncingSupabase.value = false
        }
    }

    val healthProfileState: StateFlow<UiState<HealthProfile>> = currentUser
        .flatMapLatest { user ->
            if (user == null) {
                flowOf<UiState<HealthProfile>>(UiState.Loading)
            } else {
                repository.observeHealthProfile(user.id, user.displayName)
                    .map<HealthProfile, UiState<HealthProfile>> { UiState.Success(it) }
                    .catch { emit(UiState.Error(it.localizedMessage ?: "Failed to load health profile")) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val scansState: StateFlow<UiState<List<ScanRecord>>> = currentUser
        .flatMapLatest { user ->
            if (user == null) {
                flowOf<UiState<List<ScanRecord>>>(UiState.Success(emptyList()))
            } else {
                repository.observeScans(user.id)
                    .map<List<ScanRecord>, UiState<List<ScanRecord>>> { UiState.Success(it) }
                    .catch { emit(UiState.Error(it.localizedMessage ?: "Failed to load scans")) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val topRatedState: StateFlow<UiState<List<TopRatedProduct>>> = repository.observeTopRatedProducts()
        .map<List<TopRatedProduct>, UiState<List<TopRatedProduct>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.localizedMessage ?: "Failed to load top-rated products")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val adminOverviewState: StateFlow<UiState<List<AdminUserOverview>>> = currentUser
        .flatMapLatest { user ->
            if (user == null || !user.isAdmin) {
                flowOf<UiState<List<AdminUserOverview>>>(UiState.Success(emptyList()))
            } else {
                repository.observeAdminOverview()
                    .map<List<AdminUserOverview>, UiState<List<AdminUserOverview>>> { UiState.Success(it) }
                    .catch { emit(UiState.Error(it.localizedMessage ?: "Failed to load admin overview")) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _scannerUiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val scannerUiState: StateFlow<ScannerUiState> = _scannerUiState.asStateFlow()

    private val _selectedScan = MutableStateFlow<ScanRecord?>(null)
    val selectedScan: StateFlow<ScanRecord?> = _selectedScan.asStateFlow()

    private val _recipeUiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Idle)
    val recipeUiState: StateFlow<RecipeUiState> = _recipeUiState.asStateFlow()

    private val _profileSaveMessage = MutableStateFlow<String?>(null)
    val profileSaveMessage: StateFlow<String?> = _profileSaveMessage.asStateFlow()

    private val _chatMessages = MutableStateFlow(defaultWelcomeMessages())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatContextScan = MutableStateFlow<ScanRecord?>(null)
    val chatContextScan: StateFlow<ScanRecord?> = _chatContextScan.asStateFlow()

    private val _isChatStreaming = MutableStateFlow(false)
    val isChatStreaming: StateFlow<Boolean> = _isChatStreaming.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    // Auth Actions
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _isAuthBusy.value = true
            _authError.value = null
            val result = repository.signIn(email, password)
            result.onFailure { _authError.value = it.localizedMessage ?: "Sign in failed." }
            _isAuthBusy.value = false
        }
    }

    fun signUp(displayName: String, email: String, password: String, confirmPassword: String) {
        if (password != confirmPassword) {
            _authError.value = "Passwords do not match."
            return
        }
        viewModelScope.launch {
            _isAuthBusy.value = true
            _authError.value = null
            val result = repository.signUp(displayName, email, password)
            result.onFailure { _authError.value = it.localizedMessage ?: "Sign up failed." }
            _isAuthBusy.value = false
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmNewPassword: String) {
        val user = currentUser.value ?: return
        if (newPassword != confirmNewPassword) {
            _passwordChangeMessage.value = "Error: New passwords do not match."
            return
        }
        viewModelScope.launch {
            _passwordChangeMessage.value = null
            val result = repository.changePassword(user.id, currentPassword, newPassword)
            _passwordChangeMessage.value = result.fold(
                onSuccess = { "Password updated successfully!" },
                onFailure = { "Error: ${it.localizedMessage ?: "Could not update password."}" }
            )
        }
    }

    fun clearPasswordChangeMessage() {
        _passwordChangeMessage.value = null
    }

    fun signOut() {
        _selectedScan.value = null
        _chatContextScan.value = null
        _chatMessages.value = defaultWelcomeMessages()
        _profileSaveMessage.value = null
        _passwordChangeMessage.value = null
        repository.signOut()
    }

    private fun currentProfileOrDefault(): HealthProfile {
        val uid = currentUser.value?.id.orEmpty()
        return (healthProfileState.value as? UiState.Success)?.data
            ?: HealthProfile(id = uid, userId = uid)
    }

    // Scanner: Uses Groq Free Tier AI when configured & verified, else falls back seamlessly to local WHO/FSSAI/EPA engine
    fun analyzeProduct(
        productNameHint: String,
        ingredientText: String,
        bitmap: Bitmap?
    ) {
        val user = currentUser.value ?: return
        val groq = _groqConfig.value
        val canUseVisionPhotoOnly = groq.isReadyForAi && (groq.selectedModel?.capability?.supportsVision == true) && bitmap != null

        if (ingredientText.isBlank() && productNameHint.isBlank() && !canUseVisionPhotoOnly) {
            _scannerUiState.value = ScannerUiState.Error(
                if (bitmap != null) {
                    "Photo attached! Enter the product name or ingredients below—or select a Groq Vision free model (like Llama 4 Scout) in Groq AI Settings to scan photos directly."
                } else {
                    "Please enter a product name or ingredient list to analyze."
                }
            )
            return
        }

        viewModelScope.launch {
            val profile = currentProfileOrDefault()
            val analyzed: ScanRecord = if (groq.isReadyForAi && groq.selectedModel != null) {
                val model = groq.selectedModel!!
                _scannerUiState.value = ScannerUiState.Analyzing("Analyzing with Groq (${model.id})...")
                GroqApiService.analyzeProductWithGroq(
                    apiKey = groq.apiKey,
                    modelId = model.id,
                    supportsVision = model.capability.supportsVision,
                    productNameHint = productNameHint,
                    ingredientText = ingredientText,
                    bitmap = bitmap,
                    profile = profile
                ).getOrElse { err ->
                    // Graceful fallback to offline engine if Groq rate limit/network occurs
                    val fallback = LocalRiskAnalyzerEngine.analyze(
                        productNameHint = productNameHint,
                        ingredientText = ingredientText,
                        hasAttachedPhoto = bitmap != null,
                        profile = profile
                    )
                    fallback.copy(
                        summary = "${fallback.summary} (Groq note: ${err.localizedMessage ?: "used local fallback"})"
                    )
                }
            } else {
                _scannerUiState.value = ScannerUiState.Analyzing("Evaluating WHO, FSSAI & EPA standards locally...")
                delay(240L)
                LocalRiskAnalyzerEngine.analyze(
                    productNameHint = productNameHint,
                    ingredientText = ingredientText,
                    hasAttachedPhoto = bitmap != null,
                    profile = profile
                )
            }

            val saved = repository.saveScan(user.id, analyzed).getOrElse { analyzed.copy(userId = user.id) }
            _scannerUiState.value = ScannerUiState.Idle
            selectScan(saved)
        }
    }

    fun clearScannerError() {
        if (_scannerUiState.value is ScannerUiState.Error) {
            _scannerUiState.value = ScannerUiState.Idle
        }
    }

    fun selectScan(scan: ScanRecord) {
        _selectedScan.value = scan
        _chatContextScan.value = scan
        val existingRecipes = scan.recipes
        _recipeUiState.value = if (!existingRecipes.isNullOrEmpty()) {
            RecipeUiState.Success(existingRecipes)
        } else {
            RecipeUiState.Idle
        }
    }

    fun clearSelectedScan() {
        _selectedScan.value = null
    }

    fun generateRecipesForSelectedScan() {
        val scan = _selectedScan.value ?: return
        val groq = _groqConfig.value
        val profile = currentProfileOrDefault()

        viewModelScope.launch {
            _recipeUiState.value = RecipeUiState.Loading
            val recipes = if (groq.isReadyForAi && groq.selectedModel != null) {
                GroqApiService.generateRecipesWithGroq(
                    apiKey = groq.apiKey,
                    modelId = groq.selectedModel!!.id,
                    scan = scan,
                    profile = profile
                ).getOrElse {
                    LocalRecipeEngine.generateThreeBalancingRecipes(scan, profile)
                }
            } else {
                delay(220L)
                LocalRecipeEngine.generateThreeBalancingRecipes(scan, profile)
            }

            _recipeUiState.value = RecipeUiState.Success(recipes)
            val updated = scan.copy(recipes = recipes)
            _selectedScan.value = updated
            if (scan.id.isNotBlank()) {
                repository.attachRecipesToScan(scan.id, recipes)
            }
        }
    }

    fun deleteScan(scanId: String) {
        if (scanId.isBlank()) return
        viewModelScope.launch {
            repository.deleteScan(scanId)
            if (_selectedScan.value?.id == scanId) {
                _selectedScan.value = null
            }
        }
    }

    fun adminDeleteUser(userId: String) {
        if (currentUser.value?.isAdmin != true) return
        viewModelScope.launch {
            repository.adminDeleteUserAndData(userId)
        }
    }

    fun saveHealthProfile(profile: HealthProfile) {
        val uid = currentUser.value?.id ?: return
        viewModelScope.launch {
            _profileSaveMessage.value = null
            val res = repository.saveHealthProfile(profile.copy(id = uid, userId = uid))
            val syncSuffix = if (supabaseStatus.value.isConnected) " & synced to Supabase" else " locally"
            _profileSaveMessage.value = res.fold(
                onSuccess = { "Health profile saved$syncSuffix" },
                onFailure = { "Save failed: ${it.localizedMessage ?: "Please try again"}" }
            )
        }
    }

    fun clearProfileSaveMessage() {
        _profileSaveMessage.value = null
    }

    fun attachScanToChat(scan: ScanRecord?) {
        _chatContextScan.value = scan
    }

    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _isChatStreaming.value) return

        val historySnapshot = _chatMessages.value
        val userMsg = ChatMessage(id = UUID.randomUUID().toString(), role = "user", content = trimmed)
        val placeholderId = UUID.randomUUID().toString()
        _chatMessages.value = historySnapshot + userMsg + ChatMessage(
            id = placeholderId,
            role = "model",
            content = "",
            streaming = true
        )
        _isChatStreaming.value = true
        _chatError.value = null

        viewModelScope.launch {
            val groq = _groqConfig.value
            val profile = currentProfileOrDefault()
            val reply = if (groq.isReadyForAi && groq.selectedModel != null) {
                GroqApiService.chatWithGroq(
                    apiKey = groq.apiKey,
                    modelId = groq.selectedModel!!.id,
                    history = historySnapshot,
                    question = trimmed,
                    activeScan = _chatContextScan.value,
                    profile = profile
                ).getOrElse { err ->
                    _chatError.value = "Groq error (${err.localizedMessage ?: "rate limit"}) — answered using built-in engine."
                    LocalAssistantEngine.generateReply(
                        question = trimmed,
                        activeScan = _chatContextScan.value,
                        profile = profile
                    )
                }
            } else {
                delay(180L)
                LocalAssistantEngine.generateReply(
                    question = trimmed,
                    activeScan = _chatContextScan.value,
                    profile = profile
                )
            }

            _chatMessages.value = _chatMessages.value.map { msg ->
                if (msg.id == placeholderId) {
                    msg.copy(content = reply, streaming = false)
                } else {
                    msg
                }
            }
            _isChatStreaming.value = false
        }
    }

    fun clearChat() {
        _chatError.value = null
        _chatMessages.value = defaultWelcomeMessages()
    }

    private fun defaultWelcomeMessages(): List<ChatMessage> = listOf(
        ChatMessage(
            id = "welcome_msg",
            role = "model",
            content = "Hello! I'm your Smart Risk AI Assistant. Connect a free Groq API key (from console.groq.com/keys) to unlock live Groq Free Tier LLMs, or chat offline using our built-in WHO/FSSAI/EPA engine!"
        )
    )
}
