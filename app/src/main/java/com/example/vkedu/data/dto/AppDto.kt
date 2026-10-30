package com.example.vkedu.data.dto

data class AppDto(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val iconUrl: String,
    val developer: String = "",
    val size: Float = 0f,
    val ageRating: Int = 0,
    val screenshotUrlList: List<String> = emptyList(),
)