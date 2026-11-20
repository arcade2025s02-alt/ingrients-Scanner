package com.example.data.local

import com.example.model.AppUser
import com.example.model.FactorItem
import com.example.model.HealthProfile
import com.example.model.HealthierAlternative
import com.example.model.IngredientDetail
import com.example.model.ScanRecord
import com.example.model.SmartRecipe
import com.example.model.TopRatedProduct
import com.example.model.UserRole
import org.json.JSONArray
import org.json.JSONObject

data class UserEntity(
    val id: String,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val passwordSalt: String,
    val role: String,
    val mustChangePassword: Boolean,
    val createdAtMillis: Long
) {
    fun toDomain(): AppUser = AppUser(
        id = id,
        email = email,
        displayName = displayName,
        role = UserRole.fromRaw(role),
        mustChangePassword = mustChangePassword,
        createdAtMillis = createdAtMillis
    )
}

data class HealthProfileEntity(
    val userId: String,
    val displayName: String?,
    val allergiesJson: String,
    val medicalConditionsJson: String,
    val dietaryPreferencesJson: String,
    val chemicalSensitivitiesJson: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): HealthProfile = HealthProfile(
        id = userId,
        userId = userId,
        displayName = displayName,
        allergies = LocalJsonCodec.decodeStringList(allergiesJson),
        medicalConditions = LocalJsonCodec.decodeStringList(medicalConditionsJson),
        dietaryPreferences = LocalJsonCodec.decodeStringList(dietaryPreferencesJson),
        chemicalSensitivities = LocalJsonCodec.decodeStringList(chemicalSensitivitiesJson),
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

data class ScanEntity(
    val id: String,
    val userId: String,
    val productName: String,
    val brand: String,
    val category: String,
    val score: Long,
    val ratingLabel: String,
    val summary: String,
    val standardsCitedJson: String,
    val personalAlertsJson: String,
    val negativesJson: String,
    val positivesJson: String,
    val ingredientsJson: String,
    val healthierAlternativeJson: String?,
    val rawInputText: String?,
    val recipesJson: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): ScanRecord = ScanRecord(
        id = id,
        userId = userId,
        productName = productName,
        brand = brand,
        category = category,
        score = score,
        ratingLabel = ratingLabel,
        summary = summary,
        standardsCited = LocalJsonCodec.decodeStringList(standardsCitedJson),
        personalAlerts = LocalJsonCodec.decodeStringList(personalAlertsJson),
        negatives = LocalJsonCodec.decodeFactors(negativesJson),
        positives = LocalJsonCodec.decodeFactors(positivesJson),
        ingredients = LocalJsonCodec.decodeIngredients(ingredientsJson),
        healthierAlternative = LocalJsonCodec.decodeAlternative(healthierAlternativeJson),
        rawInputText = rawInputText,
        recipes = LocalJsonCodec.decodeRecipes(recipesJson),
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

data class TopRatedEntity(
    val id: String,
    val userId: String,
    val productName: String,
    val brand: String,
    val category: String,
    val score: Long,
    val ratingLabel: String,
    val highlight: String,
    val createdAtMillis: Long
) {
    fun toDomain(): TopRatedProduct = TopRatedProduct(
        id = id,
        userId = userId,
        productName = productName,
        brand = brand,
        category = category,
        score = score,
        ratingLabel = ratingLabel,
        highlight = highlight,
        createdAtMillis = createdAtMillis
    )
}

object LocalJsonCodec {
    fun encodeStringList(list: List<String>): String = JSONArray(list).toString()

    fun decodeStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeFactors(items: List<FactorItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("label", item.label)
                    put("value", item.value)
                    put("severity", item.severity)
                    put("explanation", item.explanation)
                    put("iconType", item.iconType)
                }
            )
        }
        return arr.toString()
    }

    fun decodeFactors(json: String?): List<FactorItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                FactorItem(
                    label = o.optString("label"),
                    value = o.optString("value"),
                    severity = o.optString("severity", "MODERATE"),
                    explanation = o.optString("explanation"),
                    iconType = o.optString("iconType", "ADDITIVES")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeIngredients(items: List<IngredientDetail>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("commonName", item.commonName)
                    put("chemicalName", item.chemicalName)
                    put("riskLevel", item.riskLevel)
                    put("pros", item.pros)
                    put("cons", item.cons)
                    put("shortTermEffects", item.shortTermEffects)
                    put("longTermEffects", item.longTermEffects)
                }
            )
        }
        return arr.toString()
    }

    fun decodeIngredients(json: String?): List<IngredientDetail> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                IngredientDetail(
                    commonName = o.optString("commonName"),
                    chemicalName = o.optString("chemicalName"),
                    riskLevel = o.optString("riskLevel", "LOW_RISK"),
                    pros = o.optString("pros"),
                    cons = o.optString("cons"),
                    shortTermEffects = o.optString("shortTermEffects"),
                    longTermEffects = o.optString("longTermEffects")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeAlternative(alt: HealthierAlternative?): String? {
        if (alt == null) return null
        return JSONObject().apply {
            put("productName", alt.productName)
            put("brand", alt.brand)
            put("score", alt.score)
            put("reason", alt.reason)
            put("keyBenefits", JSONArray(alt.keyBenefits))
        }.toString()
    }

    fun decodeAlternative(json: String?): HealthierAlternative? {
        if (json.isNullOrBlank()) return null
        return try {
            val o = JSONObject(json)
            val benefitsArr = o.optJSONArray("keyBenefits") ?: JSONArray()
            val benefits = (0 until benefitsArr.length()).map { benefitsArr.optString(it) }.filter { it.isNotBlank() }
            HealthierAlternative(
                productName = o.optString("productName"),
                brand = o.optString("brand"),
                score = o.optLong("score", 88L),
                reason = o.optString("reason"),
                keyBenefits = benefits
            )
        } catch (_: Exception) {
            null
        }
    }

    fun encodeRecipes(recipes: List<SmartRecipe>?): String? {
        if (recipes == null) return null
        val arr = JSONArray()
        recipes.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("title", r.title)
                    put("prepTime", r.prepTime)
                    put("balancingNote", r.balancingNote)
                    put("ingredients", JSONArray(r.ingredients))
                    put("steps", JSONArray(r.steps))
                }
            )
        }
        return arr.toString()
    }

    fun decodeRecipes(json: String?): List<SmartRecipe>? {
        if (json.isNullOrBlank()) return null
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ingArr = o.optJSONArray("ingredients") ?: JSONArray()
                val stepsArr = o.optJSONArray("steps") ?: JSONArray()
                SmartRecipe(
                    title = o.optString("title"),
                    prepTime = o.optString("prepTime"),
                    balancingNote = o.optString("balancingNote"),
                    ingredients = (0 until ingArr.length()).map { ingArr.optString(it) },
                    steps = (0 until stepsArr.length()).map { stepsArr.optString(it) }
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
