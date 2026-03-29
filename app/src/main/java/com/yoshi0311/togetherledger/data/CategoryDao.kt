package com.yoshi0311.togetherledger.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun hardDelete(category: Category)

    @Query("UPDATE categories SET isDeleted = 1, syncStatus = 'PENDING', localUpdatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Int, now: Long)

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategory(id: Int): Flow<Category>

    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE isIncome = :isIncome AND isDeleted = 0 ORDER BY name ASC")
    fun getCategoriesByType(isIncome: Boolean): Flow<List<Category>>

    @Query("SELECT id FROM categories WHERE name = :name AND isDeleted = 0 LIMIT 1")
    suspend fun getCategoryIdByName(name: String): Int?

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Query("SELECT * FROM categories WHERE syncStatus = 'PENDING'")
    suspend fun getPendingCategories(): List<Category>

    @Query("UPDATE categories SET serverId = :serverId, syncStatus = :status, syncedAt = :syncedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: Int, serverId: String, status: String, syncedAt: Long)

    @Query("SELECT * FROM categories WHERE serverId = :serverId LIMIT 1")
    suspend fun getCategoryByServerId(serverId: String): Category?

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Int)
}
