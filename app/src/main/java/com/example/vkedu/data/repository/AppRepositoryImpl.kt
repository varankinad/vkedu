package com.example.vkedu.data.repository

import com.example.vkedu.data.mapper.toDomain
import com.example.vkedu.data.mapper.toDomainList
import com.example.vkedu.data.source.AppsDataSource
import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category
import com.example.vkedu.domain.repository.AppRepository

class AppRepositoryImpl : AppRepository {
    override suspend fun getApps(): Result<List<App>> {
        return try {
            val categories = AppsDataSource.categories.toDomainList()
            val apps = AppsDataSource.apps.toDomainList(categories)
            Result.success(apps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCategories(): Result<List<Category>> {
        return try {
            val categories = AppsDataSource.categories.toDomainList()
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppById(id: String): Result<App> {
        return try {
            val categories = AppsDataSource.categories.toDomainList()
            val app = AppsDataSource.apps.find { it.id == id }?.toDomain(categories)
            if (app != null) {
                Result.success(app)
            } else {
                Result.failure(IllegalArgumentException("App not found: $id"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}