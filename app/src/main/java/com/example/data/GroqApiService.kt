package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import com.example.model.ChatMessage
import com.example.model.FactorItem
import com.example.model.GroqFreeModel
import com.example.model.GroqFreeModelCatalog
import com.example.model.HealthProfile
import com.example.model.HealthierAlternative
import com.example.model.IngredientDetail
import com.example.model.ProductCategory
import com.example.model.ScanRecord
import com.example.model.SmartRecipe
import com.example.model.scoreToRatingLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GroqApiService {

    private const val BASE_URL = "https://api.groq.com/openai/v1"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    suspend fun verifyKeyAndFetchFreeModels(apiKey: String): Result<List<GroqFreeModel>> =
        withContext(Dispatchers.IO) {
            val cleanKey = apiKey.trim()
            if (cleanKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Please enter your Groq API key."))
            }
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/models")
                    .header("Authorization", "Bearer $cleanKey")
                    .header("Accept", "application/json")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.code == 401 || response.code == 403) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Invalid Groq API key. Create a free key at console.groq.com/keys and try again.")
                        )
                    }
                    if (!response.isSuccessful) {
                        val msg = extractErrorMessage(body) ?: "Groq API returned HTTP ${response.code}"
                        return@withContext Result.failure(IllegalStateException(msg))
                    }

                    val root = JSONObject(body)
                    val data = root.optJSONArray("data") ?: JSONArray()
                    val liveIds = mutableSetOf<String>()
                    for (i in 0 until data.length()) {
                        val item = data.optJSONObject(i) ?: continue
                        val id = item.optString("id").trim()
                        if (id.isNotBlank()) liveIds.add(id)
                    }

                    val freeModels = GroqFreeModelCatalog.filterFreeModelsFromLiveIds(liveIds)
                    Result.success(freeModels)
                }
            } catch (e: Exception) {
                Result.failure(IllegalStateException("Network error verifying Groq key: ${e.localizedMessage ?: "Check internet connection"}"))
            }
        }

    suspend fun analyzeProductWithGroq(
        apiKey: String,
        modelId: String,
        supportsVision: Boolean,
        productNameHint: String,
        ingredientText: String,
        bitmap: Bitmap?,
        profile: HealthProfile
    ): Result<ScanRecord> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are Smart Risk Analyzer, a scientific product safety evaluator aligned with WHO, FSSAI, and EPA standards.
                Analyze the provided product details (Packaged Food, Agricultural Fertilizer, or Packaging Material) and respond ONLY with a valid JSON object matching this exact schema:
                {
                  "productName": "string",
                  "brand": "string",
                  "category": "PACKAGED_FOOD" | "AGRICULTURAL_FERTILIZER" | "PACKAGING_MATERIAL",
                  "score": integer 0-100,
                  "ratingLabel": "Excellent" | "Good" | "Moderate" | "Bad",
                  "summary": "2 concise sentences explaining overall safety",
                  "standardsCited": ["WHO", "FSSAI", "EPA"],
                  "personalAlerts": ["specific conflict warnings against the user health profile, empty array if none"],
                  "negatives": [{"label":"string","value":"string","severity":"HIGH"|"MODERATE"|"LOW","explanation":"string","iconType":"ADDITIVE"|"SUGAR"|"CALORIES"|"SODIUM"|"PLASTIC"|"CHEMICAL"}],
                  "positives": [{"label":"string","value":"string","severity":"EXCELLENT"|"GOOD","explanation":"string","iconType":"PROTEIN"|"FIBER"|"ORGANIC"|"ECO"}],
                  "ingredients": [{"commonName":"string","chemicalName":"string","riskLevel":"HIGH_RISK"|"MODERATE_RISK"|"LOW_RISK"|"RISK_FREE","pros":"string","cons":"string","shortTermEffects":"string","longTermEffects":"string"}],
                  "healthierAlternative": {"productName":"string","brand":"string","score":integer 80-98,"reason":"string","keyBenefits":["string","string","string"]}
                }
                Do not include markdown fences or any text outside the JSON object.
            """.trimIndent()

            val userText = buildString {
                appendLine("Product Name Hint: ${productNameHint.ifBlank { "Auto-detect from ingredients/photo" }}")
                appendLine("Ingredients / Label Details: ${ingredientText.ifBlank { "Extract from attached photo" }}")
                appendLine("User Health Profile:")
                appendLine("- Allergies: ${profile.allergies.joinToString(", ").ifBlank { "None" }}")
                appendLine("- Medical Conditions: ${profile.medicalConditions.joinToString(", ").ifBlank { "None" }}")
                appendLine("- Dietary Preferences: ${profile.dietaryPreferences.joinToString(", ").ifBlank { "None" }}")
                appendLine("- Chemical Sensitivities: ${profile.chemicalSensitivities.joinToString(", ").ifBlank { "None" }}")
            }

            val messages = JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                if (supportsVision && bitmap != null) {
                    val dataUrl = "data:image/jpeg;base64,${encodeBitmapJpegBase64(bitmap)}"
                    val contentParts = JSONArray().apply {
                        put(JSONObject().put("type", "text").put("text", userText))
                        put(
                            JSONObject()
                                .put("type", "image_url")
                                .put("image_url", JSONObject().put("url", dataUrl))
                        )
                    }
                    put(JSONObject().put("role", "user").put("content", contentParts))
                } else {
                    put(JSONObject().put("role", "user").put("content", userText))
                }
            }

            val rawReply = executeChatCompletion(
                apiKey = apiKey,
                modelId = modelId,
                messages = messages,
                jsonMode = true,
                temperature = 0.2,
                maxTokens = 2400
            ).getOrElse { return@withContext Result.failure(it) }

            val jsonObj = parseCleanJsonObject(rawReply)
            val parsed = jsonToScanRecord(jsonObj, productNameHint, ingredientText)
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateRecipesWithGroq(
        apiKey: String,
        modelId: String,
        scan: ScanRecord,
        profile: HealthProfile
    ): Result<List<SmartRecipe>> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = """
                You are a clinical nutritionist. Generate exactly 3 healthy Indian/global balancing recipes that incorporate the scanned food while offsetting its nutritional negatives (e.g., lowering sodium/glycemic load, boosting fiber/protein) and respecting the user's allergies/medical conditions.
                Respond ONLY with a valid JSON object:
                {
                  "recipes": [
                    {
                      "title": "string",
                      "prepTime": "e.g. 15 mins",
                      "balancingNote": "How this recipe offsets the scanned product's negatives",
                      "ingredients": ["string"],
                      "steps": ["string"]
                    }
                  ]
                }
            """.trimIndent()

            val negativesSummary = scan.negatives.joinToString("; ") { "${it.label}: ${it.value}" }
            val userPrompt = """
                Scanned Food: ${scan.productName} (${scan.brand})
                Score: ${scan.score}/100 (${scan.ratingLabel})
                Negatives to balance: ${negativesSummary.ifBlank { "General balanced nutrition" }}
                User Allergies: ${profile.allergies.joinToString(", ").ifBlank { "None" }}
                User Medical Conditions: ${profile.medicalConditions.joinToString(", ").ifBlank { "None" }}
                Dietary Preferences: ${profile.dietaryPreferences.joinToString(", ").ifBlank { "None" }}
            """.trimIndent()

            val messages = JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", userPrompt))
            }

            val raw = executeChatCompletion(
                apiKey = apiKey,
                modelId = modelId,
                messages = messages,
                jsonMode = true,
                temperature = 0.4,
                maxTokens = 1800
            ).getOrElse { return@withContext Result.failure(it) }

            val obj = parseCleanJsonObject(raw)
            val arr = obj.optJSONArray("recipes") ?: JSONArray()
            val recipes = buildList {
                for (i in 0 until arr.length()) {
                    val r = arr.optJSONObject(i) ?: continue
                    add(
                        SmartRecipe(
                            title = r.optString("title", "Balanced Recipe ${i + 1}"),
                            prepTime = r.optString("prepTime", "15 mins"),
                            balancingNote = r.optString("balancingNote", ""),
                            ingredients = r.optJSONArray("ingredients").toStringList(),
                            steps = r.optJSONArray("steps").toStringList()
                        )
                    )
                }
            }
            if (recipes.isEmpty()) {
                Result.failure(IllegalStateException("Groq returned empty recipes list."))
            } else {
                Result.success(recipes.take(3))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chatWithGroq(
        apiKey: String,
        modelId: String,
        history: List<ChatMessage>,
        question: String,
        activeScan: ScanRecord?,
        profile: HealthProfile
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemContext = buildString {
                appendLine("You are the Smart Risk Analyzer AI Health & Ingredient Assistant backed by WHO, FSSAI, and EPA standards.")
                appendLine("Give concise, clear, practical guidance. Highlight allergen/medical risks clearly.")
                appendLine("User Profile: ${profile.summaryForDisplay()}")
                if (activeScan != null) {
                    appendLine("Active Scanned Product:")
                    appendLine("- Name: ${activeScan.productName} (${activeScan.brand})")
                    appendLine("- Category: ${activeScan.parsedCategory().displayName}")
                    appendLine("- Score: ${activeScan.score}/100 (${activeScan.ratingLabel})")
                    appendLine("- Summary: ${activeScan.summary}")
                    if (activeScan.personalAlerts.isNotEmpty()) {
                        appendLine("- Personal Alerts: ${activeScan.personalAlerts.joinToString("; ")}")
                    }
                    if (activeScan.ingredients.isNotEmpty()) {
                        appendLine(
                            "- Ingredients: " + activeScan.ingredients.joinToString(", ") {
                                "${it.commonName} (${it.riskLevel})"
                            }
                        )
                    }
                }
            }

            val messages = JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemContext))
                history.takeLast(8).forEach { msg ->
                    if (msg.content.isNotBlank() && msg.id != "welcome_msg") {
                        val role = if (msg.role == "user") "user" else "assistant"
                        put(JSONObject().put("role", role).put("content", msg.content))
                    }
                }
                put(JSONObject().put("role", "user").put("content", question))
            }

            executeChatCompletion(
                apiKey = apiKey,
                modelId = modelId,
                messages = messages,
                jsonMode = false,
                temperature = 0.4,
                maxTokens = 900
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeChatCompletion(
        apiKey: String,
        modelId: String,
        messages: JSONArray,
        jsonMode: Boolean,
        temperature: Double,
        maxTokens: Int
    ): Result<String> {
        val payload = JSONObject().apply {
            put("model", modelId)
            put("messages", messages)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            if (jsonMode) {
                put("response_format", JSONObject().put("type", "json_object"))
            }
        }

        val request = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                // Retry once without response_format if the chosen free model doesn't support strict json_object mode
                if (jsonMode && response.code == 400 && body.contains("response_format", ignoreCase = true)) {
                    return executeChatCompletion(apiKey, modelId, messages, false, temperature, maxTokens)
                }
                val errMsg = extractErrorMessage(body) ?: "Groq HTTP ${response.code}"
                return Result.failure(IllegalStateException(errMsg))
            }
            val root = JSONObject(body)
            val choices = root.optJSONArray("choices")
            val content = choices?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
                .trim()
            if (content.isBlank()) {
                return Result.failure(IllegalStateException("Groq returned an empty response."))
            }
            return Result.success(content)
        }
    }

    private fun extractErrorMessage(body: String): String? {
        return try {
            val obj = JSONObject(body)
            obj.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseCleanJsonObject(raw: String): JSONObject {
        var cleaned = raw.trim()
        // Strip <think>...</think> reasoning tags if emitted by Qwen/reasoning models
        cleaned = cleaned.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1)
        }
        return JSONObject(cleaned)
    }

    private fun jsonToScanRecord(
        obj: JSONObject,
        productNameHint: String,
        rawInputText: String
    ): ScanRecord {
        val score = obj.optLong("score", 55L).coerceIn(0L, 100L)
        val rating = obj.optString("ratingLabel").ifBlank { scoreToRatingLabel(score) }
        val category = ProductCategory.fromRaw(obj.optString("category")).name

        val negatives = obj.optJSONArray("negatives").toFactorList()
        val positives = obj.optJSONArray("positives").toFactorList()
        val ingredients = obj.optJSONArray("ingredients").toIngredientList()
        val altObj = obj.optJSONObject("healthierAlternative")
        val alternative = altObj?.let {
            HealthierAlternative(
                productName = it.optString("productName", "Clean Organic Alternative"),
                brand = it.optString("brand", "Clean Label Verified"),
                score = it.optLong("score", 88L).coerceIn(75L, 99L),
                reason = it.optString("reason", "Lower additive & risk profile."),
                keyBenefits = it.optJSONArray("keyBenefits").toStringList()
            )
        }

        return ScanRecord(
            productName = obj.optString("productName").ifBlank { productNameHint.ifBlank { "Analyzed Product" } },
            brand = obj.optString("brand", ""),
            category = category,
            score = score,
            ratingLabel = rating,
            summary = obj.optString("summary", "Analyzed via Groq Free Tier AI against WHO, FSSAI & EPA benchmarks."),
            standardsCited = obj.optJSONArray("standardsCited").toStringList().ifEmpty { listOf("WHO", "FSSAI", "EPA") },
            personalAlerts = obj.optJSONArray("personalAlerts").toStringList(),
            negatives = negatives,
            positives = positives,
            ingredients = ingredients,
            healthierAlternative = alternative,
            rawInputText = rawInputText
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val s = optString(i).trim()
                if (s.isNotBlank()) add(s)
            }
        }
    }

    private fun JSONArray?.toFactorList(): List<FactorItem> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val o = optJSONObject(i) ?: continue
                add(
                    FactorItem(
                        label = o.optString("label", "Factor"),
                        value = o.optString("value", ""),
                        severity = o.optString("severity", "MODERATE"),
                        explanation = o.optString("explanation", ""),
                        iconType = o.optString("iconType", "ADDITIVE")
                    )
                )
            }
        }
    }

    private fun JSONArray?.toIngredientList(): List<IngredientDetail> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val o = optJSONObject(i) ?: continue
                add(
                    IngredientDetail(
                        commonName = o.optString("commonName", "Ingredient"),
                        chemicalName = o.optString("chemicalName", ""),
                        riskLevel = o.optString("riskLevel", "LOW_RISK"),
                        pros = o.optString("pros", ""),
                        cons = o.optString("cons", ""),
                        shortTermEffects = o.optString("shortTermEffects", ""),
                        longTermEffects = o.optString("longTermEffects", "")
                    )
                )
            }
        }
    }

    private fun encodeBitmapJpegBase64(bitmap: Bitmap): String {
        val maxDim = 1024
        val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 78, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
