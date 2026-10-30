package com.example.vkedu.data.mapper

import com.example.vkedu.data.dto.AppDto
import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category

fun AppDto.toDomain(categories: List<Category>): App {
    val category = categories.find { it.key == category } ?: Category(key = category, title = category)
    return App(
        id = id,
        name = name,
        description = description,
        category = category,
        iconUrl = iconUrl,
        developer = developer,
        size = size,
        ageRating = ageRating,
        screenshotUrlList = screenshotUrlList,
    )
}

fun List<AppDto>.toDomainList(categories: List<Category>): List<App> {
    return map { it.toDomain(categories) }
}