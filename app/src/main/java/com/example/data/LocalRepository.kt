package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.HealthProfileEntity
import com.example.data.local.LocalJsonCodec
import com.example.data.local.PasswordHasher
import com.example.data.local.ScanEntity
import com.example.data.local.TopRatedEntity
import com.example.data.local.UserEntity
import com.example.model.AdminUserOverview
import com.example.model.AppUser
import com.example.model.HealthProfile
import com.example.model.ProductCategory
import com.example.model.ScanRecord
import com.example.model.SmartRecipe
import com.example.model.TopRatedProduct
import com.example.model.UserRole
import com.example.model.scoreToRatingLabel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalRepository(
    context: Context,
    private val supabase: SupabaseRestService = SupabaseRestService()
) {

    private val db = AppDatabase.getInstance(context.applicationContext)
    private val userDao = db.userDao()
    private val profileDao = db.healthProfileDao()
    private val scanDao = db.scanDao()
    private val topRatedDao = db.topRatedDao()

    val groqPrefs = GroqPreferencesStore(context.applicationContext)

    private val sessionPrefs = context.applicationContext.getSharedPreferences(
        SESSION_PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    private val _supabaseStatus = MutableStateFlow(
        SupabaseSyncStatus(
            projectId = supabase.projectId,
            isConfigured = supabase.isConfigured,
            isConnected = false,
            statusLabel = if (supabase.isConfigured) {
                "Connecting to Supabase (${supabase.projectId})..."
            } else {
                "Supabase project ${supabase.projectId} — add SUPABASE_PUBLISHABLE_KEY in Secrets to enable cloud sync"
            }
        )
    )
    val supabaseStatus: StateFlow<SupabaseSyncStatus> = _supabaseStatus.asStateFlow()

    suspend fun initializeAndRestoreSession() {
        seedDefaultAdminIfMissing()
        if (supabase.isConfigured) {
            syncFromSupabase()
        }
        val savedUserId = sessionPrefs.getString(KEY_ACTIVE_USER_ID, null)
        if (!savedUserId.isNullOrBlank()) {
            val entity = userDao.findById(savedUserId)
            _currentUser.value = entity?.toDomain()
        }
    }

    suspend fun syncFromSupabase(): Boolean {
        if (!supabase.isConfigured) {
            _supabaseStatus.value = SupabaseSyncStatus(
                projectId = supabase.projectId,
                isConfigured = false,
                isConnected = false,
                tablesMissing = false,
                statusLabel = "Local SQLite active • Add SUPABASE_PUBLISHABLE_KEY in AI Studio Secrets for cloud sync"
            )
            return false
        }

        val health = supabase.checkHealth()
        if (!health.ok) {
            _supabaseStatus.value = SupabaseSyncStatus(
                projectId = supabase.projectId,
                isConfigured = true,
                isConnected = false,
                tablesMissing = health.tablesMissing,
                statusLabel = health.message
            )
            return false
        }

        var syncedOk = true
        var lastError: String? = null

        // 1. Push any local records created offline up to Supabase (users first for foreign-key integrity)
        val localUsers = userDao.getAllUsersOnce()
        localUsers.forEach { localUser ->
            supabase.upsertUser(localUser).onFailure {
                syncedOk = false
                lastError = it.message
            }
        }
        val localProfiles = profileDao.getAllProfilesOnce()
        localProfiles.forEach { localProfile ->
            supabase.upsertHealthProfile(localProfile).onFailure {
                syncedOk = false
                lastError = it.message
            }
        }
        val localScans = scanDao.getAllScansOnce()
        localScans.forEach { localScan ->
            supabase.upsertScan(localScan).onFailure {
                syncedOk = false
                lastError = it.message
            }
        }
        val localTop = topRatedDao.getAllTopRatedOnce()
        localTop.forEach { item ->
            supabase.upsertTopRated(item).onFailure {
                syncedOk = false
                lastError = it.message
            }
        }

        // 2. Pull all remote users, health profiles, scans, and top-rated products into local SQLite
        supabase.fetchAllUsers().onSuccess { remoteUsers ->
            remoteUsers.forEach { userDao.upsertUser(it) }
        }.onFailure {
            syncedOk = false
            lastError = it.message
        }

        supabase.fetchAllProfiles().onSuccess { remoteProfiles ->
            remoteProfiles.forEach { profileDao.upsertProfile(it) }
        }

        supabase.fetchScans().onSuccess { remoteScans ->
            remoteScans.forEach { scanDao.insertScan(it) }
        }

        supabase.fetchTopRated().onSuccess { remoteTop ->
            remoteTop.forEach { topRatedDao.upsertTopRated(it) }
        }

        val totalUsers = userDao.getAllUsersOnce().size
        val totalScans = scanDao.getAllScansOnce().size
        _supabaseStatus.value = SupabaseSyncStatus(
            projectId = supabase.projectId,
            isConfigured = true,
            isConnected = syncedOk,
            tablesMissing = false,
            statusLabel = if (syncedOk) {
                "Supabase Synced (${supabase.projectId}) • $totalUsers users, $totalScans scans in cloud"
            } else {
                val detail = lastError?.take(90) ?: "check RLS / table permissions"
                "Supabase partial sync (${supabase.projectId}): $detail"
            }
        )
        return syncedOk
    }

    private suspend fun seedDefaultAdminIfMissing() {
        val existing = userDao.findByEmail(DEFAULT_ADMIN_EMAIL)
        if (existing == null) {
            val salt = PasswordHasher.generateSalt()
            val hash = PasswordHasher.hashPassword(DEFAULT_ADMIN_PASSWORD, salt)
            val adminEntity = UserEntity(
                id = DEFAULT_ADMIN_ID,
                email = DEFAULT_ADMIN_EMAIL,
                displayName = "Shivam (Admin)",
                passwordHash = hash,
                passwordSalt = salt,
                role = UserRole.ADMIN.name,
                mustChangePassword = true,
                createdAtMillis = System.currentTimeMillis()
            )
            userDao.insertUser(adminEntity)
            if (supabase.isConfigured) {
                supabase.upsertUser(adminEntity)
            }
        }
    }

    suspend fun signIn(emailInput: String, password: String): Result<AppUser> {
        seedDefaultAdminIfMissing()
        val cleanEmail = emailInput.trim().lowercase()
        if (cleanEmail.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Please enter both email/username and password."))
        }

        // Check Supabase first if configured so users created on another device can log in
        if (supabase.isConfigured) {
            supabase.findUserByEmail(cleanEmail).onSuccess { remoteUser ->
                if (remoteUser != null) {
                    userDao.upsertUser(remoteUser)
                }
            }
        }

        val entity = userDao.findByEmail(cleanEmail)
            ?: return Result.failure(IllegalArgumentException("No account found for '$cleanEmail'. Please sign up first."))

        if (!PasswordHasher.verify(password, entity.passwordSalt, entity.passwordHash)) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }

        val user = entity.toDomain()
        sessionPrefs.edit().putString(KEY_ACTIVE_USER_ID, user.id).apply()
        _currentUser.value = user
        if (supabase.isConfigured) {
            syncFromSupabase()
        }
        return Result.success(user)
    }

    suspend fun signUp(displayName: String, emailInput: String, password: String): Result<AppUser> {
        seedDefaultAdminIfMissing()
        val cleanName = displayName.trim()
        val cleanEmail = emailInput.trim().lowercase()

        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Please enter your full name (at least 2 characters)."))
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters long."))
        }

        if (supabase.isConfigured) {
            supabase.findUserByEmail(cleanEmail).onSuccess { remoteUser ->
                if (remoteUser != null) {
                    userDao.upsertUser(remoteUser)
                }
            }
        }

        if (userDao.findByEmail(cleanEmail) != null) {
            return Result.failure(IllegalArgumentException("An account with '$cleanEmail' already exists. Please sign in."))
        }

        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(password, salt)
        val now = System.currentTimeMillis()
        val newUser = UserEntity(
            id = "usr_${UUID.randomUUID()}",
            email = cleanEmail,
            displayName = cleanName,
            passwordHash = hash,
            passwordSalt = salt,
            role = UserRole.USER.name,
            mustChangePassword = false,
            createdAtMillis = now
        )
        userDao.insertUser(newUser)
        if (supabase.isConfigured) {
            supabase.upsertUser(newUser)
        }

        val domainUser = newUser.toDomain()
        sessionPrefs.edit().putString(KEY_ACTIVE_USER_ID, domainUser.id).apply()
        _currentUser.value = domainUser
        return Result.success(domainUser)
    }

    suspend fun changePassword(
        userId: String,
        currentPassword: String,
        newPassword: String
    ): Result<AppUser> {
        val entity = userDao.findById(userId)
            ?: return Result.failure(IllegalStateException("Account not found."))

        if (!PasswordHasher.verify(currentPassword, entity.passwordSalt, entity.passwordHash)) {
            return Result.failure(IllegalArgumentException("Current password is incorrect."))
        }
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters long."))
        }
        if (newPassword == currentPassword) {
            return Result.failure(IllegalArgumentException("New password must be different from your current password."))
        }

        val newSalt = PasswordHasher.generateSalt()
        val newHash = PasswordHasher.hashPassword(newPassword, newSalt)
        userDao.updatePassword(userId = userId, hash = newHash, salt = newSalt)
        if (supabase.isConfigured) {
            supabase.updateUserPassword(userId = userId, hash = newHash, salt = newSalt)
        }

        val updated = userDao.findById(userId)?.toDomain()
            ?: entity.toDomain().copy(mustChangePassword = false)
        if (_currentUser.value?.id == userId) {
            _currentUser.value = updated
        }
        return Result.success(updated)
    }

    fun signOut() {
        sessionPrefs.edit().remove(KEY_ACTIVE_USER_ID).apply()
        _currentUser.value = null
    }

    // Health Profile
    fun observeHealthProfile(userId: String, fallbackDisplayName: String? = null): Flow<HealthProfile> {
        return profileDao.observeProfile(userId).map { entity ->
            entity?.toDomain() ?: HealthProfile(
                id = userId,
                userId = userId,
                displayName = fallbackDisplayName
            )
        }
    }

    suspend fun saveHealthProfile(profile: HealthProfile): Result<Unit> {
        return try {
            val uid = profile.userId.ifBlank {
                _currentUser.value?.id ?: return Result.failure(IllegalStateException("Not signed in."))
            }
            val now = System.currentTimeMillis()
            val existing = profileDao.getProfileOnce(uid)
            val entity = HealthProfileEntity(
                userId = uid,
                displayName = profile.displayName?.trim()?.takeIf { it.isNotBlank() },
                allergiesJson = LocalJsonCodec.encodeStringList(profile.allergies.distinct()),
                medicalConditionsJson = LocalJsonCodec.encodeStringList(profile.medicalConditions.distinct()),
                dietaryPreferencesJson = LocalJsonCodec.encodeStringList(profile.dietaryPreferences.distinct()),
                chemicalSensitivitiesJson = LocalJsonCodec.encodeStringList(profile.chemicalSensitivities.distinct()),
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
            profileDao.upsertProfile(entity)
            if (supabase.isConfigured) {
                // Ensure parent app_users row exists in Supabase to satisfy foreign-key constraint
                userDao.findById(uid)?.let { supabase.upsertUser(it) }
                supabase.upsertHealthProfile(entity)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Scans
    fun observeScans(userId: String): Flow<List<ScanRecord>> {
        return scanDao.observeUserScans(userId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun saveScan(userId: String, scan: ScanRecord): Result<ScanRecord> {
        return try {
            val now = System.currentTimeMillis()
            val scanId = scan.id.ifBlank { "scan_${UUID.randomUUID()}" }
            val safeScore = scan.score.coerceIn(0L, 100L)
            val safeRating = scoreToRatingLabel(safeScore)
            val safeCategory = ProductCategory.fromRaw(scan.category).name

            val savedRecord = scan.copy(
                id = scanId,
                userId = userId,
                category = safeCategory,
                score = safeScore,
                ratingLabel = safeRating,
                createdAtMillis = now,
                updatedAtMillis = now
            )

            val scanEntity = ScanEntity(
                id = savedRecord.id,
                userId = savedRecord.userId,
                productName = savedRecord.productName,
                brand = savedRecord.brand,
                category = savedRecord.category,
                score = savedRecord.score,
                ratingLabel = savedRecord.ratingLabel,
                summary = savedRecord.summary,
                standardsCitedJson = LocalJsonCodec.encodeStringList(savedRecord.standardsCited),
                personalAlertsJson = LocalJsonCodec.encodeStringList(savedRecord.personalAlerts),
                negativesJson = LocalJsonCodec.encodeFactors(savedRecord.negatives),
                positivesJson = LocalJsonCodec.encodeFactors(savedRecord.positives),
                ingredientsJson = LocalJsonCodec.encodeIngredients(savedRecord.ingredients),
                healthierAlternativeJson = LocalJsonCodec.encodeAlternative(savedRecord.healthierAlternative),
                rawInputText = savedRecord.rawInputText,
                recipesJson = LocalJsonCodec.encodeRecipes(savedRecord.recipes),
                createdAtMillis = now,
                updatedAtMillis = now
            )
            scanDao.insertScan(scanEntity)
            if (supabase.isConfigured) {
                // Ensure parent app_users row exists in Supabase to satisfy foreign-key constraint
                userDao.findById(userId)?.let { supabase.upsertUser(it) }
                supabase.upsertScan(scanEntity)
            }

            if (safeScore >= 75L) {
                val topEntity = TopRatedEntity(
                    id = "top_${savedRecord.id}",
                    userId = userId,
                    productName = savedRecord.productName,
                    brand = savedRecord.brand.ifBlank { "Verified Scan" },
                    category = safeCategory,
                    score = safeScore,
                    ratingLabel = safeRating,
                    highlight = savedRecord.summary.take(260).ifBlank { "High-scoring clean formulation." },
                    createdAtMillis = now
                )
                topRatedDao.upsertTopRated(topEntity)
                if (supabase.isConfigured) {
                    supabase.upsertTopRated(topEntity)
                }
            }
            Result.success(savedRecord)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun attachRecipesToScan(scanId: String, recipes: List<SmartRecipe>): Result<Unit> {
        return try {
            val encoded = LocalJsonCodec.encodeRecipes(recipes) ?: "[]"
            val now = System.currentTimeMillis()
            scanDao.updateRecipes(scanId, encoded, now)
            if (supabase.isConfigured) {
                supabase.updateScanRecipes(scanId, encoded, now)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteScan(scanId: String): Result<Unit> {
        return try {
            scanDao.deleteScanById(scanId)
            if (supabase.isConfigured) {
                supabase.deleteScan(scanId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Explore Top-Rated Products (Supabase/Local High-Scores + Verified Benchmarks)
    fun observeTopRatedProducts(): Flow<List<TopRatedProduct>> {
        return topRatedDao.observeTopRated().map { entities ->
            val localTop = entities.map { it.toDomain() }
            (localTop + curatedBenchmarkProducts())
                .distinctBy { "${it.productName.lowercase()}_${it.brand.lowercase()}" }
                .sortedByDescending { it.score }
        }
    }

    // Admin Full Data Access: Observe ALL Users, ALL Health Profiles, and ALL Scans
    fun observeAdminOverview(): Flow<List<AdminUserOverview>> {
        return combine(
            userDao.observeAllUsers(),
            profileDao.observeAllProfiles(),
            scanDao.observeAllScans()
        ) { users, profiles, scans ->
            val profileByUser = profiles.associateBy { it.userId }
            val scansByUser = scans.groupBy { it.userId }
            users.map { userEntity ->
                val user = userEntity.toDomain()
                AdminUserOverview(
                    user = user,
                    profile = profileByUser[user.id]?.toDomain(),
                    scans = scansByUser[user.id]?.map { it.toDomain() } ?: emptyList()
                )
            }
        }
    }

    suspend fun adminDeleteUserAndData(userId: String): Result<Unit> {
        if (userId == DEFAULT_ADMIN_ID) {
            return Result.failure(IllegalArgumentException("The default admin account cannot be deleted."))
        }
        return try {
            scanDao.deleteScansByUserId(userId)
            profileDao.deleteByUserId(userId)
            userDao.deleteUserById(userId)
            if (supabase.isConfigured) {
                supabase.deleteUser(userId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun curatedBenchmarkProducts(): List<TopRatedProduct> = listOf(
        TopRatedProduct(
            id = "bench_oats",
            userId = "verified_standard",
            productName = "100% Organic Rolled Oats",
            brand = "WHO/FSSAI Clean Benchmark",
            category = ProductCategory.PACKAGED_FOOD.name,
            score = 96L,
            ratingLabel = "Excellent",
            highlight = "Single whole-grain ingredient, high beta-glucan fiber, zero added sugar or sodium."
        ),
        TopRatedProduct(
            id = "bench_millet_muesli",
            userId = "verified_standard",
            productName = "Sprouted Ragi & Foxtail Millet Crunch",
            brand = "Clean Label India",
            category = ProductCategory.PACKAGED_FOOD.name,
            score = 92L,
            ratingLabel = "Excellent",
            highlight = "Zero palm oil, zero refined sugar, rich in plant protein and natural iron."
        ),
        TopRatedProduct(
            id = "bench_peanut_butter",
            userId = "verified_standard",
            productName = "Unsweetened Roasted Peanut Butter",
            brand = "PureNut Organics",
            category = ProductCategory.PACKAGED_FOOD.name,
            score = 90L,
            ratingLabel = "Excellent",
            highlight = "100% roasted peanuts with no hydrogenated vegetable oil or stabilizers."
        ),
        TopRatedProduct(
            id = "bench_vermicompost",
            userId = "verified_standard",
            productName = "Enriched Organic Vermicompost & Neem Cake",
            brand = "EcoSoil Bio",
            category = ProductCategory.AGRICULTURAL_FERTILIZER.name,
            score = 95L,
            ratingLabel = "Excellent",
            highlight = "Zero synthetic nitrate runoff, boosts soil microbiome, EPA-safe organic inputs."
        ),
        TopRatedProduct(
            id = "bench_seaweed_bio",
            userId = "verified_standard",
            productName = "Ascophyllum Seaweed Bio-Stimulant",
            brand = "GreenRoot Agri",
            category = ProductCategory.AGRICULTURAL_FERTILIZER.name,
            score = 91L,
            ratingLabel = "Excellent",
            highlight = "Naturally chelated micronutrients with zero heavy metal residues."
        ),
        TopRatedProduct(
            id = "bench_bagasse_tray",
            userId = "verified_standard",
            productName = "PFAS-Free Sugarcane Bagasse Food Container",
            brand = "EarthPack Zero",
            category = ProductCategory.PACKAGING_MATERIAL.name,
            score = 97L,
            ratingLabel = "Excellent",
            highlight = "100% home-compostable plant fiber, certified PFAS-free and BPA-free."
        ),
        TopRatedProduct(
            id = "bench_glass_jar",
            userId = "verified_standard",
            productName = "Borosilicate Food-Grade Glass Jar",
            brand = "SafeStore Pure",
            category = ProductCategory.PACKAGING_MATERIAL.name,
            score = 94L,
            ratingLabel = "Excellent",
            highlight = "Zero chemical leaching at high heat, infinitely recyclable inert material."
        ),
        TopRatedProduct(
            id = "bench_curd",
            userId = "verified_standard",
            productName = "Plain Unsweetened High-Protein Curd",
            brand = "FarmFresh Dairy",
            category = ProductCategory.PACKAGED_FOOD.name,
            score = 88L,
            ratingLabel = "Excellent",
            highlight = "Live active cultures, 10g protein per serving, no artificial thickeners."
        )
    )

    companion object {
        const val DEFAULT_ADMIN_ID = "admin_shivam_root"
        const val DEFAULT_ADMIN_EMAIL = "shivam@admin.com"
        const val DEFAULT_ADMIN_PASSWORD = "shivam@Admin"
        private const val SESSION_PREFS_NAME = "smart_risk_local_session"
        private const val KEY_ACTIVE_USER_ID = "active_user_id"
    }
}
