package com.yoshi0311.togetherledger.data

import kotlinx.coroutines.flow.Flow

interface CategoriesRepository {
    fun getAllCategoriesStream(): Flow<List<Category>>

    fun getCategoryStream(id: Int): Flow<Category?>

    fun getCategoriesByTypeStream(isIncome: Boolean): Flow<List<Category>>

    suspend fun insertCategory(category: Category): Long

    suspend fun deleteCategory(category: Category)  // serverId 유무에 따라 소프트/하드 딜리트

    suspend fun updateCategory(category: Category)

    suspend fun getOrCreateCategoryId(categoryName: String): Int

    suspend fun hardDeleteById(id: Int)
    suspend fun getPendingCategories(): List<Category>
    suspend fun syncUpdateStatus(id: Int, serverId: String, status: String, syncedAt: Long)
    suspend fun getCategoryByServerId(serverId: String): Category?
}