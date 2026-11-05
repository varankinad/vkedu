package com.example.vkedu.data.repository

import com.example.vkedu.data.mapper.toDomain
import com.example.vkedu.data.mapper.toDomainList
import com.example.vkedu.data.remote.api.CatalogApi
import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category
import com.example.vkedu.domain.repository.AppRepository
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    private val catalogApi: CatalogApi,
) : AppRepository {

    override suspend fun getApps(): Result<List<App>> {
        return try {
            val dtos = catalogApi.getCatalog()
            Result.success(dtos.toDomainList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCategories(): Result<List<Category>> {
        return try {
            val dtos = catalogApi.getCatalog()
            val categories = dtos
                .distinctBy { it.category }
                .map { Category(key = it.category, title = it.category) }
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppById(id: String): Result<App> {
        return try {
            val dto = catalogApi.getAppDetail(id)
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}