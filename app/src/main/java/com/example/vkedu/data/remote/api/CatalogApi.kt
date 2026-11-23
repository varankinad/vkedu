package com.example.vkedu.data.remote.api

import com.example.vkedu.data.remote.dto.AppDetailDto
import com.example.vkedu.data.remote.dto.CatalogAppDto
import kotlinx.coroutines.delay

interface CatalogApi {
    suspend fun getCatalog(): List<CatalogAppDto>

    suspend fun getAppDetail(id: String): AppDetailDto

    suspend fun getRating(id: String): Float {
        delay(1500)
        return (30..50).random() / 10f
    }
}
