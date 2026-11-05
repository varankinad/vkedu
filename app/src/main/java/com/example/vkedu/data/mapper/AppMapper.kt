package com.example.vkedu.data.mapper

import com.example.vkedu.data.remote.dto.AppDetailDto
import com.example.vkedu.data.remote.dto.CatalogAppDto
import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category

fun CatalogAppDto.toDomain(): App {
    return App(
        id = id,
        name = name,
        description = description,
        category = Category(key = category, title = category),
        iconUrl = iconUrl,
    )
}

fun List<CatalogAppDto>.toDomainList(): List<App> {
    return map { it.toDomain() }
}

fun AppDetailDto.toDomain(): App {
    return App(
        id = id,
        name = name,
        description = description,
        category = Category(key = category, title = category),
        iconUrl = iconUrl,
        developer = developer,
        size = size,
        ageRating = ageRating,
        screenshotUrlList = screenshotUrlList,
    )
}