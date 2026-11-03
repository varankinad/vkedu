package com.example.vkedu.domain.repository

import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category

interface AppRepository {
    suspend fun getApps(): Result<List<App>>
    suspend fun getCategories(): Result<List<Category>>
    suspend fun getAppById(id: String): Result<App>
}