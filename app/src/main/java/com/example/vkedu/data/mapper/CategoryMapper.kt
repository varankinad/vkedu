package com.example.vkedu.data.mapper

import com.example.vkedu.data.dto.CategoryDto
import com.example.vkedu.domain.model.Category

fun CategoryDto.toDomain(): Category {
    return Category(key = key, title = title)
}

fun List<CategoryDto>.toDomainList(): List<Category> {
    return map { it.toDomain() }
}