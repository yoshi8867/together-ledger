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

    override suspend fun getOrCreateCategoryId(categoryName: String): Int {
        // 1. 이름으로 카테고리 ID 조회
        val existingId = categoryDao.getCategoryIdByName(categoryName)

        // 2. 존재하면 바로 반환
        if (existingId != null) {
            return existingId
        }

        // 3. 존재하지 않으면 새로 삽입하고 새 ID 반환
        val newCategory = Category(name = categoryName)
        return categoryDao.insertCategory(newCategory).toInt()
    }
}