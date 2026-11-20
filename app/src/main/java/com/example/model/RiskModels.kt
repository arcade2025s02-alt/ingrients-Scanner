package com.example.model

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class UserRole {
    ADMIN,
    USER;

    companion object {
        fun fromRaw(raw: String?): UserRole =
            if (raw.equals("ADMIN", ignoreCase = true)) ADMIN else USER
    }
}

data class AppUser(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: UserRole = UserRole.USER,
    val mustChangePassword: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val isAdmin: Boolean get() = role == UserRole.ADMIN
}

enum class ProductCategory(val displayName: String, val shortLabel: String) {
    PACKAGED_FOOD("Packaged Food", "Food"),
    AGRICULTURAL_FERTILIZER("Agricultural Fertilizer", "Fertilizer"),
    PACKAGING_MATERIAL("Packaging Material", "Packaging");

    companion object {
        fun fromRaw(raw: String?): ProductCategory {
            val normalized = raw?.trim()?.uppercase()?.replace(" ", "_") ?: ""
            return entries.firstOrNull { it.name == normalized } ?: PACKAGED_FOOD
        }
    }
}

enum class IngredientRiskLevel(val label: String) {
    HIGH_RISK("High-Risk"),
    MODERATE_RISK("Moderate Risk"),
    LOW_RISK("Low Risk"),
    RISK_FREE("Risk-Free");

    companion object {
        fun fromRaw(raw: String?): IngredientRiskLevel {
            val normalized = raw?.trim()?.uppercase()?.replace("-", "_")?.replace(" ", "_") ?: ""
            return entries.firstOrNull { it.name == normalized } ?: LOW_RISK
        }
    }
}

data class HealthProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String? = null,
    val allergies: List<String> = emptyList(),
    val medicalConditions: List<String> = emptyList(),
    val dietaryPreferences: List<String> = emptyList(),
    val chemicalSensitivities: List<String> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun hasAnyRestrictions(): Boolean =
        allergies.isNotEmpty() || medicalConditions.isNotEmpty() ||
            dietaryPreferences.isNotEmpty() || chemicalSensitivities.isNotEmpty()

    fun summaryForDisplay(): String {
        if (!hasAnyRestrictions()) return "No allergies, medical conditions, or sensitivities recorded."
        val parts = mutableListOf<String>()
        if (allergies.isNotEmpty()) parts.add("Allergies: ${allergies.joinToString(", ")}")
        if (medicalConditions.isNotEmpty()) parts.add("Medical: ${medicalConditions.joinToString(", ")}")
        if (dietaryPreferences.isNotEmpty()) parts.add("Diet: ${dietaryPreferences.joinToString(", ")}")
        if (chemicalSensitivities.isNotEmpty()) parts.add("Sensitivities: ${chemicalSensitivities.joinToString(", ")}")
        return parts.joinToString(" • ")
    }
}

data class FactorItem(
    val label: String = "",
    val value: String = "",
    val severity: String = "MODERATE",
    val explanation: String = "",
    val iconType: String = "ADDITIVES"
)

data class IngredientDetail(
    val commonName: String = "",
    val chemicalName: String = "",
    val riskLevel: String = "LOW_RISK",
    val pros: String = "",
    val cons: String = "",
    val shortTermEffects: String = "",
    val longTermEffects: String = ""
) {
    fun parsedRiskLevel(): IngredientRiskLevel = IngredientRiskLevel.fromRaw(riskLevel)
}

data class HealthierAlternative(
    val productName: String = "",
    val brand: String = "",
    val score: Long = 85L,
    val reason: String = "",
    val keyBenefits: List<String> = emptyList()
)

data class SmartRecipe(
    val title: String = "",
    val prepTime: String = "",
    val balancingNote: String = "",
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList()
)

data class ScanRecord(
    val id: String = "",
    val userId: String = "",
    val productName: String = "",
    val brand: String = "",
    val category: String = ProductCategory.PACKAGED_FOOD.name,
    val score: Long = 0L,
    val ratingLabel: String = "Moderate",
    val summary: String = "",
    val standardsCited: List<String> = emptyList(),
    val personalAlerts: List<String> = emptyList(),
    val negatives: List<FactorItem> = emptyList(),
    val positives: List<FactorItem> = emptyList(),
    val ingredients: List<IngredientDetail> = emptyList(),
    val healthierAlternative: HealthierAlternative? = null,
    val rawInputText: String? = null,
    val recipes: List<SmartRecipe>? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun parsedCategory(): ProductCategory = ProductCategory.fromRaw(category)
}

data class TopRatedProduct(
    val id: String = "",
    val userId: String = "",
    val productName: String = "",
    val brand: String = "",
    val category: String = ProductCategory.PACKAGED_FOOD.name,
    val score: Long = 85L,
    val ratingLabel: String = "Excellent",
    val highlight: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun parsedCategory(): ProductCategory = ProductCategory.fromRaw(category)
}

data class AdminUserOverview(
    val user: AppUser,
    val profile: HealthProfile?,
    val scans: List<ScanRecord>
) {
    val averageScore: Long?
        get() = if (scans.isEmpty()) null else scans.map { it.score }.average().toLong()
}

data class ChatMessage(
    val id: String,
    val role: String, // "user" or "model"
    val content: String,
    val streaming: Boolean = false
)

fun scoreToRatingLabel(score: Long): String = when {
    score >= 75L -> "Excellent"
    score >= 60L -> "Good"
    score >= 40L -> "Moderate"
    else -> "Bad"
}
