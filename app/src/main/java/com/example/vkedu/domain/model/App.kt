package com.example.vkedu.domain.model

data class App(
    val id: String,
    val name: String,
    val description: String,
    val category: Category,
    val iconUrl: String,
    val developer: String = "",
    val size: Float = 0f,
    val ageRating: Int = 0,
    val screenshotUrlList: List<String> = emptyList(),
)