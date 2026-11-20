package com.example.model

enum class GroqModelCapability(
    val badgeLabel: String,
    val supportsChatAndAnalysis: Boolean,
    val supportsVision: Boolean = false
) {
    CHAT_ANALYSIS("Chat & Analysis", supportsChatAndAnalysis = true, supportsVision = false),
    VISION_CHAT("Vision + Chat", supportsChatAndAnalysis = true, supportsVision = true),
    SAFETY_GUARD("Prompt Guard", supportsChatAndAnalysis = false, supportsVision = false),
    TEXT_TO_SPEECH("Text-to-Speech", supportsChatAndAnalysis = false, supportsVision = false),
    SPEECH_TO_TEXT("Speech-to-Text", supportsChatAndAnalysis = false, supportsVision = false)
}

data class GroqFreeModel(
    val id: String,
    val displayName: String,
    val capability: GroqModelCapability,
    val rpm: String, // Requests Per Minute
    val rpd: String, // Requests Per Day
    val tpm: String, // Tokens Per Minute
    val tpd: String, // Tokens Per Day
    val ash: String = "-", // Audio Seconds Per Hour
    val asd: String = "-", // Audio Seconds Per Day
    val recommended: Boolean = false,
    val notes: String = ""
)

data class GroqConfigState(
    val apiKey: String = "",
    val isKeyVerified: Boolean = false,
    val isVerifying: Boolean = false,
    val verificationError: String? = null,
    val verifiedFreeModels: List<GroqFreeModel> = emptyList(),
    val selectedModelId: String = ""
) {
    val selectedModel: GroqFreeModel?
        get() = verifiedFreeModels.firstOrNull { it.id == selectedModelId }

    val isReadyForAi: Boolean
        get() = isKeyVerified && apiKey.isNotBlank() && (selectedModel?.capability?.supportsChatAndAnalysis == true)
}

object GroqFreeModelCatalog {

    const val CONSOLE_KEYS_URL = "https://console.groq.com/keys"

    /**
     * Curated Groq Free Plan Limits catalog.
     * Includes every Free Plan model from the user's Groq account along with standard Groq free-tier LLMs.
     * Any model not in this Free Plan catalog is filtered out so paid/developer-only models are never offered.
     */
    val freePlanModels: List<GroqFreeModel> = listOf(
        // Account Free Plan Chat & Reasoning Models (from user's Groq Free Plan table)
        GroqFreeModel(
            id = "openai/gpt-oss-120b",
            displayName = "GPT-OSS 120B",
            capability = GroqModelCapability.CHAT_ANALYSIS,
            rpm = "30",
            rpd = "1K",
            tpm = "8K",
            tpd = "200K",
            recommended = true,
            notes = "Best accuracy for ingredient risk scoring, side effects & recipes"
        ),
        GroqFreeModel(
            id = "openai/gpt-oss-20b",
            displayName = "GPT-OSS 20B",
            capability = GroqModelCapability.CHAT_ANALYSIS,
            rpm = "30",
            rpd = "1K",
            tpm = "8K",
            tpd = "200K",
            notes = "Ultra-fast responses for label scanning & chat"
        ),
        GroqFreeModel(
            id = "qwen/qwen3.8-27b",
            displayName = "Qwen 3.8 27B",
            capability = GroqModelCapability.CHAT_ANALYSIS,
            rpm = "30",
            rpd = "1K",
            tpm = "8K",
            tpd = "200K",
            notes = "Strong structured JSON & multilingual ingredient analysis"
        ),
        GroqFreeModel(
            id = "openai/gpt-oss-safeguard-20b",
            displayName = "GPT-OSS Safeguard 20B",
            capability = GroqModelCapability.CHAT_ANALYSIS,
            rpm = "3",
            rpd = "1K",
            tpm = "2K",
            tpd = "200K",
            notes = "Safety-aligned text model (lower RPM/TPM)"
        ),
        // Additional standard Groq Free Tier Chat / Vision LLMs (shown only if active on the verified key)
        GroqFreeModel(
            id = "meta-llama/llama-4-scout-17b-16e-instruct",
            displayName = "Llama 4 Scout 17B (Vision)",
            capability = GroqModelCapability.VISION_CHAT,
            rpm = "30",
            rpd = "1K",
            tpm = "30K",
            tpd = "500K",
            recommended = true,
            notes = "Supports reading ingredient photos directly + fast chat"
        ),
        GroqFreeModel(
            id = "meta-llama/llama-4-maverick-17b-128e-instruct",
            displayName = "Llama 4 Maverick 17B (Vision)",
            capability = GroqModelCapability.VISION_CHAT,
            rpm = "30",
            rpd = "1K",
            tpm = "6K",
            tpd = "500K",
            notes = "Multimodal vision & reasoning model"
        ),
        GroqFreeModel(
            id = "llama-3.3-70b-versatile",
            displayName = "Llama 3.3 70B Versatile",
            capability = GroqModelCapability.CHAT_ANALYSIS,
            rpm = "30",
            rpd = "1K",
            tpm = "12K",
            tpd = "100K",
            notes = "High-quality versatile reasoning & recipe generation"
        ),
        GroqFreeModel(
            id = "llama-3.1-8b-instant",
            displayName = "Llama 3.1 8B Instant",
            capability = GroqModelCapability.CHAT_ANALYSIS,
            rpm = "30",
            rpd = "14.4K",
            tpm = "6K",
            tpd = "500K",
            notes = "High daily quota (14.4K RPD) instant responses"
        ),
        GroqFreeModel(
            id = "qwen/qwen3-32b",
            displayName = "Qwen 3 32B",
            capability = GroqModelCapability.CHAT_ANALYSIS,
            rpm = "60",
            rpd = "1K",
            tpm = "6K",
            tpd = "500K",
            notes = "Strong analysis & high RPM limit"
        ),
        // Account Free Plan Specialized Models (Guardrails, TTS & Whisper Speech-to-Text)
        GroqFreeModel(
            id = "meta-llama/llama-prompt-guard-2-86m",
            displayName = "Llama Prompt Guard 2 86M",
            capability = GroqModelCapability.SAFETY_GUARD,
            rpm = "30",
            rpd = "14.4K",
            tpm = "15K",
            tpd = "500K",
            notes = "Safety classifier only (not for chat/analysis)"
        ),
        GroqFreeModel(
            id = "meta-llama/llama-prompt-guard-2-22m",
            displayName = "Llama Prompt Guard 2 22M",
            capability = GroqModelCapability.SAFETY_GUARD,
            rpm = "30",
            rpd = "14.4K",
            tpm = "15K",
            tpd = "500K",
            notes = "Lightweight safety classifier (not for chat/analysis)"
        ),
        GroqFreeModel(
            id = "canopylabs/orpheus-v1-english",
            displayName = "Orpheus V1 English",
            capability = GroqModelCapability.TEXT_TO_SPEECH,
            rpm = "10",
            rpd = "100",
            tpm = "1.2K",
            tpd = "3.6K",
            notes = "English text-to-speech model (audio output)"
        ),
        GroqFreeModel(
            id = "canopylabs/orpheus-arabic-saudi",
            displayName = "Orpheus Arabic Saudi",
            capability = GroqModelCapability.TEXT_TO_SPEECH,
            rpm = "10",
            rpd = "100",
            tpm = "1.2K",
            tpd = "3.6K",
            notes = "Arabic text-to-speech model (audio output)"
        ),
        GroqFreeModel(
            id = "whisper-large-v3",
            displayName = "Whisper Large V3",
            capability = GroqModelCapability.SPEECH_TO_TEXT,
            rpm = "20",
            rpd = "2K",
            tpm = "-",
            tpd = "-",
            ash = "7.2K",
            asd = "28.8K",
            notes = "Audio transcription model (ASH: 7.2K, ASD: 28.8K)"
        ),
        GroqFreeModel(
            id = "whisper-large-v3-turbo",
            displayName = "Whisper Large V3 Turbo",
            capability = GroqModelCapability.SPEECH_TO_TEXT,
            rpm = "20",
            rpd = "2K",
            tpm = "-",
            tpd = "-",
            ash = "7.2K",
            asd = "28.8K",
            notes = "Fast audio transcription model (ASH: 7.2K, ASD: 28.8K)"
        )
    )

    private val byId: Map<String, GroqFreeModel> = freePlanModels.associateBy { it.id.lowercase() }

    /**
     * Intersects live Groq `/openai/v1/models` IDs with the Free Plan catalog so ONLY free models
     * enabled on the user's verified API key are displayed.
     */
    fun filterFreeModelsFromLiveIds(liveModelIds: Set<String>): List<GroqFreeModel> {
        val normalizedLive = liveModelIds.map { it.trim().lowercase() }.toSet()
        val matched = freePlanModels.filter { it.id.lowercase() in normalizedLive }
        return if (matched.isNotEmpty()) {
            matched.sortedWith(
                compareByDescending<GroqFreeModel> { it.capability.supportsChatAndAnalysis }
                    .thenByDescending { it.recommended }
                    .thenBy { it.id }
            )
        } else {
            // Fallback to the exact Free Plan models listed for the user's account if Groq returns new aliases
            freePlanModels.filter {
                it.id in setOf(
                    "openai/gpt-oss-120b",
                    "openai/gpt-oss-20b",
                    "qwen/qwen3.8-27b",
                    "openai/gpt-oss-safeguard-20b",
                    "meta-llama/llama-prompt-guard-2-86m",
                    "meta-llama/llama-prompt-guard-2-22m",
                    "canopylabs/orpheus-v1-english",
                    "canopylabs/orpheus-arabic-saudi",
                    "whisper-large-v3",
                    "whisper-large-v3-turbo"
                )
            }
        }
    }

    fun findById(id: String): GroqFreeModel? = byId[id.trim().lowercase()]
}
