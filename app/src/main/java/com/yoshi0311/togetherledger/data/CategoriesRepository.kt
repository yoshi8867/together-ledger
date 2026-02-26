package com.yoshi0311.togetherledger.data

import kotlinx.coroutines.flow.Flow

interface CategoriesRepository {
    fun getAllCategoriesStream(): Flow<List<Category>>

    fun getCategoryStream(id: Int): Flow<Category?>

    fun getCategoriesByTypeStream(isIncome: Boolean): Flow<List<Category>>

    suspend fun insertCategory(category: Category)

    suspend fun deleteCategory(category: Category)

    suspend fun updateCategory(category: Category)
}