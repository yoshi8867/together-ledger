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
    suspend fun delete(category: Category)

    @Query("SELECT * FROM categories WHERE id = :id")
    fun getCategory(id: Int): Flow<Category>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE isIncome = :isIncome ORDER BY name ASC")
    fun getCategoriesByType(isIncome: Boolean): Flow<List<Category>>

    @Query("SELECT id FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryIdByName(name: String): Int?

    @Insert
    suspend fun insertCategory(category: Category): Long
}