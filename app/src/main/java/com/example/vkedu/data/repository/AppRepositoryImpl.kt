package com.example.vkedu.data.repository

import com.example.vkedu.data.mapper.toDomain
import com.example.vkedu.data.mapper.toDomainList
import com.example.vkedu.data.remote.api.CatalogApi
import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category
import com.example.vkedu.domain.repository.AppRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class AppRepositoryImpl @Inject constructor(
    private val catalogApi: CatalogApi,
) : AppRepository {

    override suspend fun getApps(): Result<List<App>> {
        return try {
            val dtos = catalogApi.getCatalog()
            Result.success(dtos.toDomainList())
        } catch (e: CancellationException) {
            throw e
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppById(id: String): Result<App> {
        return try {
            val dto = catalogApi.getAppDetail(id)
            Result.success(dto.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadRatingsSequentially(ids: List<String>): Map<String, Float> {
        val ratings = mutableMapOf<String, Float>()
        for (id in ids) {
            ratings[id] = catalogApi.getRating(id)
        }
        return ratings
    }

    override suspend fun loadRatingsInParallel(ids: List<String>): Map<String, Float> {
        return coroutineScope {
            ids.map { id -> async { id to catalogApi.getRating(id) } }.awaitAll().toMap()
        }
    }
}
