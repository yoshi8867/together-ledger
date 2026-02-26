package com.yoshi0311.togetherledger.data

import kotlinx.coroutines.flow.Flow

class OfflineCategoriesRepository(private val categoryDao: CategoryDao) : CategoriesRepository {
    override fun getAllCategoriesStream(): Flow<List<Category>> =
        categoryDao.getAllCategories()

    override fun getCategoryStream(id: Int): Flow<Category?> =
        categoryDao.getCategory(id)

    override fun getCategoriesByTypeStream(isIncome: Boolean): Flow<List<Category>> =
        categoryDao.getCategoriesByType(isIncome)

    override suspend fun insertCategory(category: Category) =
        categoryDao.insert(category)

    override suspend fun deleteCategory(category: Category) =
        categoryDao.delete(category)

    override suspend fun updateCategory(category: Category) =
        categoryDao.update(category)
}