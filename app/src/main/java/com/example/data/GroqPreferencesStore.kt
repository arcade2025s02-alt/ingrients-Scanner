package com.example.data

import android.content.Context
import com.example.model.GroqFreeModel
import com.example.model.GroqFreeModelCatalog

class GroqPreferencesStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getApiKey(): String = prefs.getString(KEY_GROQ_API_KEY, "")?.trim().orEmpty()

    fun isVerified(): Boolean = prefs.getBoolean(KEY_GROQ_VERIFIED, false)

    fun getSelectedModelId(): String = prefs.getString(KEY_SELECTED_MODEL, "")?.trim().orEmpty()

    fun getVerifiedFreeModels(): List<GroqFreeModel> {
        val raw = prefs.getString(KEY_VERIFIED_MODELS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        val ids = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        return GroqFreeModelCatalog.filterFreeModelsFromLiveIds(ids)
    }

    fun saveVerifiedConfig(
        apiKey: String,
        freeModels: List<GroqFreeModel>,
        selectedModelId: String
    ) {
        prefs.edit()
            .putString(KEY_GROQ_API_KEY, apiKey.trim())
            .putBoolean(KEY_GROQ_VERIFIED, true)
            .putString(KEY_VERIFIED_MODELS, freeModels.joinToString(",") { it.id })
            .putString(KEY_SELECTED_MODEL, selectedModelId.trim())
            .apply()
    }

    fun saveSelectedModel(modelId: String) {
        prefs.edit().putString(KEY_SELECTED_MODEL, modelId.trim()).apply()
    }

    fun clearConfig() {
        prefs.edit()
            .remove(KEY_GROQ_API_KEY)
            .remove(KEY_GROQ_VERIFIED)
            .remove(KEY_VERIFIED_MODELS)
            .remove(KEY_SELECTED_MODEL)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "smart_risk_groq_prefs"
        private const val KEY_GROQ_API_KEY = "groq_api_key"
        private const val KEY_GROQ_VERIFIED = "groq_verified"
        private const val KEY_VERIFIED_MODELS = "groq_verified_models"
        private const val KEY_SELECTED_MODEL = "groq_selected_model"
    }
}
