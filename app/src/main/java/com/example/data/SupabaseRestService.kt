package com.example.data

import com.example.BuildConfig
import com.example.data.local.HealthProfileEntity
import com.example.data.local.ScanEntity
import com.example.data.local.TopRatedEntity
import com.example.data.local.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SupabaseSyncStatus(
    val projectId: String,
    val isConfigured: Boolean,
    val isConnected: Boolean,
    val tablesMissing: Boolean = false,
    val statusLabel: String
)

class SupabaseRestService(
    private val baseUrl: String = BuildConfig.SUPABASE_URL.trim().trimEnd('/'),
    private val publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
) {

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val projectId: String
        get() = baseUrl.removePrefix("https://").substringBefore(".").ifBlank { "wrqpnupqlucbgyiapugx" }

    val isConfigured: Boolean
        get() = baseUrl.startsWith("https://") &&
            publishableKey.isNotBlank() &&
            !publishableKey.equals("YOUR_SUPABASE_PUBLISHABLE_KEY", ignoreCase = true)

    /**
     * New Supabase publishable keys (`sb_publishable_...`) are NOT JWTs and must ONLY be passed
     * in the `apikey` header. Passing them in `Authorization: Bearer ...` causes PostgREST to reject
     * the request with HTTP 401 Invalid JWT. Legacy JWT anon keys (`eyJ...`) support both headers.
     */
    private fun baseRequest(pathAndQuery: String): Request.Builder {
        val builder = Request.Builder()
            .url("$baseUrl/rest/v1/$pathAndQuery")
            .header("apikey", publishableKey)
            .header("Accept", "application/json")
            .header("Accept-Profile", "public")
            .header("Content-Profile", "public")
        if (publishableKey.startsWith("eyJ")) {
            builder.header("Authorization", "Bearer $publishableKey")
        }
        return builder
    }

    data class HealthCheckResult(
        val ok: Boolean,
        val tablesMissing: Boolean = false,
        val message: String
    )

    suspend fun checkHealth(): HealthCheckResult = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext HealthCheckResult(
                ok = false,
                message = "Add SUPABASE_PUBLISHABLE_KEY in AI Studio Secrets to enable cloud sync"
            )
        }
        try {
            val req = baseRequest("app_users?select=id&limit=1").get().build()
            client.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                when {
                    res.isSuccessful -> HealthCheckResult(
                        ok = true,
                        message = "Connected to Supabase ($projectId)"
                    )
                    res.code == 404 || body.contains("PGRST205", ignoreCase = true) ||
                        body.contains("does not exist", ignoreCase = true) ||
                        body.contains("Could not find the table", ignoreCase = true) -> {
                        val rawDetail = extractErrorMessage(body)?.take(110).orEmpty()
                        HealthCheckResult(
                            ok = false,
                            tablesMissing = true,
                            message = "Supabase ($projectId) API cache/grants pending ($rawDetail) — Copy & Run the updated SQL script with GRANT + NOTIFY pgrst, then tap Refresh"
                        )
                    }
                    res.code == 401 || res.code == 403 -> {
                        val detail = extractErrorMessage(body) ?: "Check SUPABASE_PUBLISHABLE_KEY & table RLS policies"
                        HealthCheckResult(
                            ok = false,
                            message = "Supabase auth error (HTTP ${res.code}): $detail"
                        )
                    }
                    else -> {
                        val detail = extractErrorMessage(body) ?: "HTTP ${res.code}"
                        HealthCheckResult(
                            ok = false,
                            message = "Supabase error ($detail) — using local SQLite cache"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            HealthCheckResult(
                ok = false,
                message = "Supabase unreachable (${e.localizedMessage ?: "offline"}) — using local SQLite cache"
            )
        }
    }

    private fun extractErrorMessage(body: String): String? {
        return try {
            val obj = JSONObject(body)
            obj.optString("message").ifBlank { obj.optString("hint") }.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchAllUsers(): Result<List<UserEntity>> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase key not configured"))
        try {
            val req = baseRequest("app_users?select=*&order=created_at_millis.desc").get().build()
            client.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) return@withContext Result.failure(IllegalStateException("HTTP ${res.code}: $body"))
                val arr = JSONArray(body)
                val list = buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        add(
                            UserEntity(
                                id = o.optString("id"),
                                email = o.optString("email"),
                                displayName = o.optString("display_name"),
                                passwordHash = o.optString("password_hash"),
                                passwordSalt = o.optString("password_salt"),
                                role = o.optString("role", "USER"),
                                mustChangePassword = o.optBoolean("must_change_password", false),
                                createdAtMillis = o.optLong("created_at_millis", System.currentTimeMillis())
                            )
                        )
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findUserByEmail(email: String): Result<UserEntity?> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase key not configured"))
        try {
            val encoded = URLEncoder.encode(email.trim().lowercase(), "UTF-8")
            val req = baseRequest("app_users?email=eq.$encoded&select=*&limit=1").get().build()
            client.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) return@withContext Result.failure(IllegalStateException("HTTP ${res.code}: $body"))
                val arr = JSONArray(body)
                if (arr.length() == 0) return@withContext Result.success(null)
                val o = arr.getJSONObject(0)
                Result.success(
                    UserEntity(
                        id = o.optString("id"),
                        email = o.optString("email"),
                        displayName = o.optString("display_name"),
                        passwordHash = o.optString("password_hash"),
                        passwordSalt = o.optString("password_salt"),
                        role = o.optString("role", "USER"),
                        mustChangePassword = o.optBoolean("must_change_password", false),
                        createdAtMillis = o.optLong("created_at_millis", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertUser(user: UserEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase key not configured"))
        try {
            val payload = JSONObject().apply {
                put("id", user.id)
                put("email", user.email)
                put("display_name", user.displayName)
                put("password_hash", user.passwordHash)
                put("password_salt", user.passwordSalt)
                put("role", user.role)
                put("must_change_password", user.mustChangePassword)
                put("created_at_millis", user.createdAtMillis)
            }
            val req = baseRequest("app_users?on_conflict=id")
                .header("Prefer", "resolution=merge-duplicates")
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) Result.success(Unit)
                else Result.failure(IllegalStateException("HTTP ${res.code}: ${res.body?.string().orEmpty()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserPassword(userId: String, hash: String, salt: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase key not configured"))
            try {
                val encoded = URLEncoder.encode(userId, "UTF-8")
                val payload = JSONObject().apply {
                    put("password_hash", hash)
                    put("password_salt", salt)
                    put("must_change_password", false)
                }
                val req = baseRequest("app_users?id=eq.$encoded")
                    .patch(payload.toString().toRequestBody(jsonMedia))
                    .build()
                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) Result.success(Unit)
                    else Result.failure(IllegalStateException("HTTP ${res.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteUser(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase key not configured"))
        try {
            val encoded = URLEncoder.encode(userId, "UTF-8")
            val req = baseRequest("app_users?id=eq.$encoded").delete().build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) Result.success(Unit)
                else Result.failure(IllegalStateException("HTTP ${res.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchAllProfiles(): Result<List<HealthProfileEntity>> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase key not configured"))
        try {
            val req = baseRequest("health_profiles?select=*").get().build()
            client.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) return@withContext Result.failure(IllegalStateException("HTTP ${res.code}"))
                val arr = JSONArray(body)
                val list = buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        add(
                            HealthProfileEntity(
                                userId = o.optString("user_id"),
                                displayName = o.optString("display_name").takeIf { it.isNotBlank() && it != "null" },
                                allergiesJson = o.optString("allergies_json", "[]"),
                                medicalConditionsJson = o.optString("medical_conditions_json", "[]"),
                                dietaryPreferencesJson = o.optString("dietary_preferences_json", "[]"),
                                chemicalSensitivitiesJson = o.optString("chemical_sensitivities_json", "[]"),
                                createdAtMillis = o.optLong("created_at_millis", System.currentTimeMillis()),
                                updatedAtMillis = o.optLong("updated_at_millis", System.currentTimeMillis())
                            )
                        )
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertHealthProfile(entity: HealthProfileEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase key not configured"))
        try {
            val payload = JSONObject().apply {
                put("user_id", entity.userId)
                put("display_name", entity.displayName ?: JSONObject.NULL)
                put("allergies_json", entity.allergiesJson)
                put("medical_conditions_json", entity.medicalConditionsJson)
                put("dietary_preferences_json", entity.dietaryPreferencesJson)
                put("chemical_sensitivities_json", entity.chemicalSensitivitiesJson)
                put("created_at_millis", entity.createdAtMillis)
                put("updated_at_millis", entity.updatedAtMillis)
            }
            val req = baseRequest("health_profiles?on_conflict=user_id")
                .header("Prefer", "resolution=merge-duplicates")
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) Result.success(Unit)
                else Result.failure(IllegalStateException("HTTP ${res.code}: ${extractErrorMessage(res.body?.string().orEmpty()) ?: "write failed"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchScans(userId: String? = null): Result<List<ScanEntity>> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase key not configured"))
        try {
            val filter = if (userId != null) {
                "scans?user_id=eq.${URLEncoder.encode(userId, "UTF-8")}&select=*&order=created_at_millis.desc"
            } else {
                "scans?select=*&order=created_at_millis.desc"
            }
            val req = baseRequest(filter).get().build()
            client.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) return@withContext Result.failure(IllegalStateException("HTTP ${res.code}"))
                val arr = JSONArray(body)
                val list = buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        add(
                            ScanEntity(
                                id = o.optString("id"),
                                userId = o.optString("user_id"),
                                productName = o.optString("product_name"),
                                brand = o.optString("brand", ""),
                                category = o.optString("category"),
                                score = o.optLong("score", 50L),
                                ratingLabel = o.optString("rating_label", "Moderate"),
                                summary = o.optString("summary", ""),
                                standardsCitedJson = o.optString("standards_cited_json", "[]"),
                                personalAlertsJson = o.optString("personal_alerts_json", "[]"),
                                negativesJson = o.optString("negatives_json", "[]"),
                                positivesJson = o.optString("positives_json", "[]"),
                                ingredientsJson = o.optString("ingredients_json", "[]"),
                                healthierAlternativeJson = o.optString("healthier_alternative_json").takeIf { it.isNotBlank() && it != "null" },
                                rawInputText = o.optString("raw_input_text", ""),
                                recipesJson = o.optString("recipes_json").takeIf { it.isNotBlank() && it != "null" },
                                createdAtMillis = o.optLong("created_at_millis", System.currentTimeMillis()),
                                updatedAtMillis = o.optLong("updated_at_millis", System.currentTimeMillis())
                            )
                        )
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertScan(entity: ScanEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase key not configured"))
        try {
            val payload = JSONObject().apply {
                put("id", entity.id)
                put("user_id", entity.userId)
                put("product_name", entity.productName)
                put("brand", entity.brand)
                put("category", entity.category)
                put("score", entity.score)
                put("rating_label", entity.ratingLabel)
                put("summary", entity.summary)
                put("standards_cited_json", entity.standardsCitedJson)
                put("personal_alerts_json", entity.personalAlertsJson)
                put("negatives_json", entity.negativesJson)
                put("positives_json", entity.positivesJson)
                put("ingredients_json", entity.ingredientsJson)
                put("healthier_alternative_json", entity.healthierAlternativeJson ?: JSONObject.NULL)
                put("raw_input_text", entity.rawInputText ?: "")
                put("recipes_json", entity.recipesJson ?: JSONObject.NULL)
                put("created_at_millis", entity.createdAtMillis)
                put("updated_at_millis", entity.updatedAtMillis)
            }
            val req = baseRequest("scans?on_conflict=id")
                .header("Prefer", "resolution=merge-duplicates")
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) Result.success(Unit)
                else Result.failure(IllegalStateException("HTTP ${res.code}: ${extractErrorMessage(res.body?.string().orEmpty()) ?: "write failed"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateScanRecipes(scanId: String, recipesJson: String, updatedAt: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase key not configured"))
            try {
                val encoded = URLEncoder.encode(scanId, "UTF-8")
                val payload = JSONObject().apply {
                    put("recipes_json", recipesJson)
                    put("updated_at_millis", updatedAt)
                }
                val req = baseRequest("scans?id=eq.$encoded")
                    .patch(payload.toString().toRequestBody(jsonMedia))
                    .build()
                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) Result.success(Unit)
                    else Result.failure(IllegalStateException("HTTP ${res.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteScan(scanId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase key not configured"))
        try {
            val encoded = URLEncoder.encode(scanId, "UTF-8")
            val req = baseRequest("scans?id=eq.$encoded").delete().build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) Result.success(Unit)
                else Result.failure(IllegalStateException("HTTP ${res.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchTopRated(): Result<List<TopRatedEntity>> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase key not configured"))
        try {
            val req = baseRequest("top_rated_products?select=*&order=score.desc").get().build()
            client.newCall(req).execute().use { res ->
                val body = res.body?.string().orEmpty()
                if (!res.isSuccessful) return@withContext Result.failure(IllegalStateException("HTTP ${res.code}"))
                val arr = JSONArray(body)
                val list = buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        add(
                            TopRatedEntity(
                                id = o.optString("id"),
                                userId = o.optString("user_id"),
                                productName = o.optString("product_name"),
                                brand = o.optString("brand", ""),
                                category = o.optString("category"),
                                score = o.optLong("score", 80L),
                                ratingLabel = o.optString("rating_label", "Excellent"),
                                highlight = o.optString("highlight", ""),
                                createdAtMillis = o.optLong("created_at_millis", System.currentTimeMillis())
                            )
                        )
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertTopRated(entity: TopRatedEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext Result.failure(IllegalStateException("Supabase key not configured"))
        try {
            val payload = JSONObject().apply {
                put("id", entity.id)
                put("user_id", entity.userId)
                put("product_name", entity.productName)
                put("brand", entity.brand)
                put("category", entity.category)
                put("score", entity.score)
                put("rating_label", entity.ratingLabel)
                put("highlight", entity.highlight)
                put("created_at_millis", entity.createdAtMillis)
            }
            val req = baseRequest("top_rated_products?on_conflict=id")
                .header("Prefer", "resolution=merge-duplicates")
                .post(payload.toString().toRequestBody(jsonMedia))
                .build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) Result.success(Unit)
                else Result.failure(IllegalStateException("HTTP ${res.code}: ${extractErrorMessage(res.body?.string().orEmpty()) ?: "write failed"}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        val SQL_SCHEMA_SCRIPT: String = """
create table if not exists public.app_users (
    id text primary key,
    email text not null unique,
    display_name text not null,
    password_hash text not null,
    password_salt text not null,
    role text not null default 'USER',
    must_change_password boolean not null default false,
    created_at_millis bigint not null
);

create table if not exists public.health_profiles (
    user_id text primary key references public.app_users(id) on delete cascade,
    display_name text,
    allergies_json text not null default '[]',
    medical_conditions_json text not null default '[]',
    dietary_preferences_json text not null default '[]',
    chemical_sensitivities_json text not null default '[]',
    created_at_millis bigint not null,
    updated_at_millis bigint not null
);

create table if not exists public.scans (
    id text primary key,
    user_id text not null references public.app_users(id) on delete cascade,
    product_name text not null,
    brand text not null default '',
    category text not null,
    score bigint not null,
    rating_label text not null,
    summary text not null default '',
    standards_cited_json text not null default '[]',
    personal_alerts_json text not null default '[]',
    negatives_json text not null default '[]',
    positives_json text not null default '[]',
    ingredients_json text not null default '[]',
    healthier_alternative_json text,
    raw_input_text text not null default '',
    recipes_json text,
    created_at_millis bigint not null,
    updated_at_millis bigint not null
);

create table if not exists public.top_rated_products (
    id text primary key,
    user_id text not null,
    product_name text not null,
    brand text not null default '',
    category text not null,
    score bigint not null,
    rating_label text not null,
    highlight text not null default '',
    created_at_millis bigint not null
);

alter table public.app_users disable row level security;
alter table public.health_profiles disable row level security;
alter table public.scans disable row level security;
alter table public.top_rated_products disable row level security;

grant usage on schema public to anon, authenticated;
grant select, insert, update, delete on table public.app_users to anon, authenticated;
grant select, insert, update, delete on table public.health_profiles to anon, authenticated;
grant select, insert, update, delete on table public.scans to anon, authenticated;
grant select, insert, update, delete on table public.top_rated_products to anon, authenticated;

notify pgrst, 'reload schema';
        """.trimIndent()
    }
}
