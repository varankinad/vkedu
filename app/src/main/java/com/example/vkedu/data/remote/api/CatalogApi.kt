package com.example.vkedu.data.remote.api

import com.example.vkedu.data.remote.dto.AppDetailDto
import com.example.vkedu.data.remote.dto.CatalogAppDto
import retrofit2.http.GET
import retrofit2.http.Path

interface CatalogApi {
    @GET("catalog")
    suspend fun getCatalog(): List<CatalogAppDto>

    @GET("catalog/{id}")
    suspend fun getAppDetail(@Path("id") id: String): AppDetailDto
}