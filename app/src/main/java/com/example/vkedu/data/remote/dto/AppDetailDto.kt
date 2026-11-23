package com.example.vkedu.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AppDetailDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("developer") val developer: String = "",
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String,
    @SerializedName("iconUrl") val iconUrl: String,
    @SerializedName("ageRating") val ageRating: Int = 0,
    @SerializedName("size") val size: Float = 0f,
    @SerializedName("screenshotUrlList") val screenshotUrlList: List<String> = emptyList(),
)