package com.example.data.local

import kotlinx.coroutines.flow.Flow

interface UserDao {
    suspend fun findByEmail(email: String): UserEntity?
    suspend fun findById(id: String): UserEntity?
    suspend fun getAllUsersOnce(): List<UserEntity>
    fun observeAllUsers(): Flow<List<UserEntity>>
    suspend fun insertUser(user: UserEntity)
    suspend fun upsertUser(user: UserEntity)
    suspend fun updatePassword(userId: String, hash: String, salt: String)
    suspend fun deleteUserById(userId: String)
}

interface HealthProfileDao {
    fun observeProfile(userId: String): Flow<HealthProfileEntity?>
    suspend fun getProfileOnce(userId: String): HealthProfileEntity?
    suspend fun getAllProfilesOnce(): List<HealthProfileEntity>
    fun observeAllProfiles(): Flow<List<HealthProfileEntity>>
    suspend fun upsertProfile(profile: HealthProfileEntity)
    suspend fun deleteByUserId(userId: String)
}

interface ScanDao {
    fun observeUserScans(userId: String): Flow<List<ScanEntity>>
    fun observeAllScans(): Flow<List<ScanEntity>>
    suspend fun getAllScansOnce(): List<ScanEntity>
    suspend fun insertScan(scan: ScanEntity)
    suspend fun updateRecipes(scanId: String, recipesJson: String, updatedAtMillis: Long)
    suspend fun deleteScanById(scanId: String)
    suspend fun deleteScansByUserId(userId: String)
}

interface TopRatedDao {
    fun observeTopRated(): Flow<List<TopRatedEntity>>
    suspend fun getAllTopRatedOnce(): List<TopRatedEntity>
    suspend fun upsertTopRated(product: TopRatedEntity)
}
