package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Pure Android SQLiteOpenHelper database with reactive StateFlow observation.
 * Avoids KSP2 / Room annotation-processing runtime overhead while keeping full local SQLite durability.
 */
class AppDatabase private constructor(context: Context) {

    private val helper = LocalSqliteHelper(context.applicationContext)

    private val usersFlow = MutableStateFlow<List<UserEntity>>(emptyList())
    private val profilesFlow = MutableStateFlow<List<HealthProfileEntity>>(emptyList())
    private val scansFlow = MutableStateFlow<List<ScanEntity>>(emptyList())
    private val topRatedFlow = MutableStateFlow<List<TopRatedEntity>>(emptyList())

    init {
        refreshAllFlowsSync()
    }

    private fun refreshAllFlowsSync() {
        usersFlow.value = readAllUsersSync()
        profilesFlow.value = readAllProfilesSync()
        scansFlow.value = readAllScansSync()
        topRatedFlow.value = readAllTopRatedSync()
    }

    private val userDaoImpl = object : UserDao {
        override suspend fun findByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) {
            helper.readableDatabase.rawQuery(
                "SELECT * FROM users WHERE LOWER(email) = LOWER(?) LIMIT 1",
                arrayOf(email)
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.toUserEntity() else null
            }
        }

        override suspend fun findById(id: String): UserEntity? = withContext(Dispatchers.IO) {
            helper.readableDatabase.rawQuery(
                "SELECT * FROM users WHERE id = ? LIMIT 1",
                arrayOf(id)
            ).use { cursor ->
                if (cursor.moveToFirst()) cursor.toUserEntity() else null
            }
        }

        override suspend fun getAllUsersOnce(): List<UserEntity> = withContext(Dispatchers.IO) {
            readAllUsersSync()
        }

        override fun observeAllUsers(): Flow<List<UserEntity>> = usersFlow

        override suspend fun insertUser(user: UserEntity) = withContext(Dispatchers.IO) {
            helper.writableDatabase.insertWithOnConflict(
                "users",
                null,
                user.toContentValues(),
                SQLiteDatabase.CONFLICT_ABORT
            )
            usersFlow.value = readAllUsersSync()
        }

        override suspend fun upsertUser(user: UserEntity) = withContext(Dispatchers.IO) {
            helper.writableDatabase.insertWithOnConflict(
                "users",
                null,
                user.toContentValues(),
                SQLiteDatabase.CONFLICT_REPLACE
            )
            usersFlow.value = readAllUsersSync()
        }

        override suspend fun updatePassword(userId: String, hash: String, salt: String) =
            withContext(Dispatchers.IO) {
                val cv = ContentValues().apply {
                    put("passwordHash", hash)
                    put("passwordSalt", salt)
                    put("mustChangePassword", 0)
                }
                helper.writableDatabase.update("users", cv, "id = ?", arrayOf(userId))
                usersFlow.value = readAllUsersSync()
            }

        override suspend fun deleteUserById(userId: String) = withContext(Dispatchers.IO) {
            helper.writableDatabase.delete("users", "id = ?", arrayOf(userId))
            usersFlow.value = readAllUsersSync()
        }
    }

    private val healthProfileDaoImpl = object : HealthProfileDao {
        override fun observeProfile(userId: String): Flow<HealthProfileEntity?> =
            profilesFlow.map { list -> list.firstOrNull { it.userId == userId } }

        override suspend fun getProfileOnce(userId: String): HealthProfileEntity? =
            withContext(Dispatchers.IO) {
                helper.readableDatabase.rawQuery(
                    "SELECT * FROM health_profiles WHERE userId = ? LIMIT 1",
                    arrayOf(userId)
                ).use { cursor ->
                    if (cursor.moveToFirst()) cursor.toHealthProfileEntity() else null
                }
            }

        override suspend fun getAllProfilesOnce(): List<HealthProfileEntity> =
            withContext(Dispatchers.IO) { readAllProfilesSync() }

        override fun observeAllProfiles(): Flow<List<HealthProfileEntity>> = profilesFlow

        override suspend fun upsertProfile(profile: HealthProfileEntity) =
            withContext(Dispatchers.IO) {
                helper.writableDatabase.insertWithOnConflict(
                    "health_profiles",
                    null,
                    profile.toContentValues(),
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                profilesFlow.value = readAllProfilesSync()
            }

        override suspend fun deleteByUserId(userId: String) = withContext(Dispatchers.IO) {
            helper.writableDatabase.delete("health_profiles", "userId = ?", arrayOf(userId))
            profilesFlow.value = readAllProfilesSync()
        }
    }

    private val scanDaoImpl = object : ScanDao {
        override fun observeUserScans(userId: String): Flow<List<ScanEntity>> =
            scansFlow.map { list -> list.filter { it.userId == userId } }

        override fun observeAllScans(): Flow<List<ScanEntity>> = scansFlow

        override suspend fun getAllScansOnce(): List<ScanEntity> = withContext(Dispatchers.IO) {
            readAllScansSync()
        }

        override suspend fun insertScan(scan: ScanEntity) = withContext(Dispatchers.IO) {
            helper.writableDatabase.insertWithOnConflict(
                "scans",
                null,
                scan.toContentValues(),
                SQLiteDatabase.CONFLICT_REPLACE
            )
            scansFlow.value = readAllScansSync()
        }

        override suspend fun updateRecipes(scanId: String, recipesJson: String, updatedAtMillis: Long) =
            withContext(Dispatchers.IO) {
                val cv = ContentValues().apply {
                    put("recipesJson", recipesJson)
                    put("updatedAtMillis", updatedAtMillis)
                }
                helper.writableDatabase.update("scans", cv, "id = ?", arrayOf(scanId))
                scansFlow.value = readAllScansSync()
            }

        override suspend fun deleteScanById(scanId: String) = withContext(Dispatchers.IO) {
            helper.writableDatabase.delete("scans", "id = ?", arrayOf(scanId))
            scansFlow.value = readAllScansSync()
        }

        override suspend fun deleteScansByUserId(userId: String) = withContext(Dispatchers.IO) {
            helper.writableDatabase.delete("scans", "userId = ?", arrayOf(userId))
            scansFlow.value = readAllScansSync()
        }
    }

    private val topRatedDaoImpl = object : TopRatedDao {
        override fun observeTopRated(): Flow<List<TopRatedEntity>> = topRatedFlow

        override suspend fun getAllTopRatedOnce(): List<TopRatedEntity> = withContext(Dispatchers.IO) {
            readAllTopRatedSync()
        }

        override suspend fun upsertTopRated(product: TopRatedEntity) = withContext(Dispatchers.IO) {
            helper.writableDatabase.insertWithOnConflict(
                "top_rated_products",
                null,
                product.toContentValues(),
                SQLiteDatabase.CONFLICT_REPLACE
            )
            topRatedFlow.value = readAllTopRatedSync()
        }
    }

    fun userDao(): UserDao = userDaoImpl
    fun healthProfileDao(): HealthProfileDao = healthProfileDaoImpl
    fun scanDao(): ScanDao = scanDaoImpl
    fun topRatedDao(): TopRatedDao = topRatedDaoImpl

    private fun readAllUsersSync(): List<UserEntity> =
        helper.readableDatabase.rawQuery("SELECT * FROM users ORDER BY createdAtMillis DESC", null)
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.toUserEntity())
                }
            }

    private fun readAllProfilesSync(): List<HealthProfileEntity> =
        helper.readableDatabase.rawQuery("SELECT * FROM health_profiles", null)
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.toHealthProfileEntity())
                }
            }

    private fun readAllScansSync(): List<ScanEntity> =
        helper.readableDatabase.rawQuery("SELECT * FROM scans ORDER BY createdAtMillis DESC", null)
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.toScanEntity())
                }
            }

    private fun readAllTopRatedSync(): List<TopRatedEntity> =
        helper.readableDatabase.rawQuery(
            "SELECT * FROM top_rated_products ORDER BY score DESC, createdAtMillis DESC",
            null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.toTopRatedEntity())
            }
        }

    private fun UserEntity.toContentValues() = ContentValues().apply {
        put("id", id)
        put("email", email)
        put("displayName", displayName)
        put("passwordHash", passwordHash)
        put("passwordSalt", passwordSalt)
        put("role", role)
        put("mustChangePassword", if (mustChangePassword) 1 else 0)
        put("createdAtMillis", createdAtMillis)
    }

    private fun HealthProfileEntity.toContentValues() = ContentValues().apply {
        put("userId", userId)
        put("displayName", displayName)
        put("allergiesJson", allergiesJson)
        put("medicalConditionsJson", medicalConditionsJson)
        put("dietaryPreferencesJson", dietaryPreferencesJson)
        put("chemicalSensitivitiesJson", chemicalSensitivitiesJson)
        put("createdAtMillis", createdAtMillis)
        put("updatedAtMillis", updatedAtMillis)
    }

    private fun ScanEntity.toContentValues() = ContentValues().apply {
        put("id", id)
        put("userId", userId)
        put("productName", productName)
        put("brand", brand)
        put("category", category)
        put("score", score)
        put("ratingLabel", ratingLabel)
        put("summary", summary)
        put("standardsCitedJson", standardsCitedJson)
        put("personalAlertsJson", personalAlertsJson)
        put("negativesJson", negativesJson)
        put("positivesJson", positivesJson)
        put("ingredientsJson", ingredientsJson)
        put("healthierAlternativeJson", healthierAlternativeJson)
        put("rawInputText", rawInputText)
        put("recipesJson", recipesJson)
        put("createdAtMillis", createdAtMillis)
        put("updatedAtMillis", updatedAtMillis)
    }

    private fun TopRatedEntity.toContentValues() = ContentValues().apply {
        put("id", id)
        put("userId", userId)
        put("productName", productName)
        put("brand", brand)
        put("category", category)
        put("score", score)
        put("ratingLabel", ratingLabel)
        put("highlight", highlight)
        put("createdAtMillis", createdAtMillis)
    }

    private fun Cursor.toUserEntity() = UserEntity(
        id = getString(getColumnIndexOrThrow("id")),
        email = getString(getColumnIndexOrThrow("email")),
        displayName = getString(getColumnIndexOrThrow("displayName")),
        passwordHash = getString(getColumnIndexOrThrow("passwordHash")),
        passwordSalt = getString(getColumnIndexOrThrow("passwordSalt")),
        role = getString(getColumnIndexOrThrow("role")),
        mustChangePassword = getInt(getColumnIndexOrThrow("mustChangePassword")) != 0,
        createdAtMillis = getLong(getColumnIndexOrThrow("createdAtMillis"))
    )

    private fun Cursor.toHealthProfileEntity() = HealthProfileEntity(
        userId = getString(getColumnIndexOrThrow("userId")),
        displayName = getStringOrNull("displayName"),
        allergiesJson = getString(getColumnIndexOrThrow("allergiesJson")),
        medicalConditionsJson = getString(getColumnIndexOrThrow("medicalConditionsJson")),
        dietaryPreferencesJson = getString(getColumnIndexOrThrow("dietaryPreferencesJson")),
        chemicalSensitivitiesJson = getString(getColumnIndexOrThrow("chemicalSensitivitiesJson")),
        createdAtMillis = getLong(getColumnIndexOrThrow("createdAtMillis")),
        updatedAtMillis = getLong(getColumnIndexOrThrow("updatedAtMillis"))
    )

    private fun Cursor.toScanEntity() = ScanEntity(
        id = getString(getColumnIndexOrThrow("id")),
        userId = getString(getColumnIndexOrThrow("userId")),
        productName = getString(getColumnIndexOrThrow("productName")),
        brand = getString(getColumnIndexOrThrow("brand")),
        category = getString(getColumnIndexOrThrow("category")),
        score = getLong(getColumnIndexOrThrow("score")),
        ratingLabel = getString(getColumnIndexOrThrow("ratingLabel")),
        summary = getString(getColumnIndexOrThrow("summary")),
        standardsCitedJson = getString(getColumnIndexOrThrow("standardsCitedJson")),
        personalAlertsJson = getString(getColumnIndexOrThrow("personalAlertsJson")),
        negativesJson = getString(getColumnIndexOrThrow("negativesJson")),
        positivesJson = getString(getColumnIndexOrThrow("positivesJson")),
        ingredientsJson = getString(getColumnIndexOrThrow("ingredientsJson")),
        healthierAlternativeJson = getStringOrNull("healthierAlternativeJson"),
        rawInputText = getStringOrNull("rawInputText"),
        recipesJson = getStringOrNull("recipesJson"),
        createdAtMillis = getLong(getColumnIndexOrThrow("createdAtMillis")),
        updatedAtMillis = getLong(getColumnIndexOrThrow("updatedAtMillis"))
    )

    private fun Cursor.toTopRatedEntity() = TopRatedEntity(
        id = getString(getColumnIndexOrThrow("id")),
        userId = getString(getColumnIndexOrThrow("userId")),
        productName = getString(getColumnIndexOrThrow("productName")),
        brand = getString(getColumnIndexOrThrow("brand")),
        category = getString(getColumnIndexOrThrow("category")),
        score = getLong(getColumnIndexOrThrow("score")),
        ratingLabel = getString(getColumnIndexOrThrow("ratingLabel")),
        highlight = getString(getColumnIndexOrThrow("highlight")),
        createdAtMillis = getLong(getColumnIndexOrThrow("createdAtMillis"))
    )

    private fun Cursor.getStringOrNull(column: String): String? {
        val idx = getColumnIndexOrThrow(column)
        return if (isNull(idx)) null else getString(idx)
    }

    private class LocalSqliteHelper(context: Context) :
        SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS users (
                    id TEXT PRIMARY KEY NOT NULL,
                    email TEXT NOT NULL UNIQUE,
                    displayName TEXT NOT NULL,
                    passwordHash TEXT NOT NULL,
                    passwordSalt TEXT NOT NULL,
                    role TEXT NOT NULL,
                    mustChangePassword INTEGER NOT NULL,
                    createdAtMillis INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS health_profiles (
                    userId TEXT PRIMARY KEY NOT NULL,
                    displayName TEXT,
                    allergiesJson TEXT NOT NULL,
                    medicalConditionsJson TEXT NOT NULL,
                    dietaryPreferencesJson TEXT NOT NULL,
                    chemicalSensitivitiesJson TEXT NOT NULL,
                    createdAtMillis INTEGER NOT NULL,
                    updatedAtMillis INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS scans (
                    id TEXT PRIMARY KEY NOT NULL,
                    userId TEXT NOT NULL,
                    productName TEXT NOT NULL,
                    brand TEXT NOT NULL,
                    category TEXT NOT NULL,
                    score INTEGER NOT NULL,
                    ratingLabel TEXT NOT NULL,
                    summary TEXT NOT NULL,
                    standardsCitedJson TEXT NOT NULL,
                    personalAlertsJson TEXT NOT NULL,
                    negativesJson TEXT NOT NULL,
                    positivesJson TEXT NOT NULL,
                    ingredientsJson TEXT NOT NULL,
                    healthierAlternativeJson TEXT,
                    rawInputText TEXT,
                    recipesJson TEXT,
                    createdAtMillis INTEGER NOT NULL,
                    updatedAtMillis INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_scans_userId ON scans(userId)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS top_rated_products (
                    id TEXT PRIMARY KEY NOT NULL,
                    userId TEXT NOT NULL,
                    productName TEXT NOT NULL,
                    brand TEXT NOT NULL,
                    category TEXT NOT NULL,
                    score INTEGER NOT NULL,
                    ratingLabel TEXT NOT NULL,
                    highlight TEXT NOT NULL,
                    createdAtMillis INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            onCreate(db)
        }
    }

    companion object {
        private const val DB_NAME = "smart_risk_analyzer_local.db"
        private const val DB_VERSION = 2

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
