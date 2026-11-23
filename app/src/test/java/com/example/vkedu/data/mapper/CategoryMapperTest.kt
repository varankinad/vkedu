package com.example.vkedu.data.mapper

import com.example.vkedu.data.dto.CategoryDto
import com.example.vkedu.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryMapperTest {
    @Test
    fun dtoMapsDistinctKeyAndTitleToDomain() {
        assertEquals(Category("tools", "Tools"), CategoryDto("tools", "Tools").toDomain())
    }

    @Test
    fun dtoPreservesEmptyKeyAndTitle() {
        assertEquals(Category("", ""), CategoryDto("", "").toDomain())
    }

    @Test
    fun emptyDtoListMapsToEmptyDomainList() {
        assertEquals(emptyList<Category>(), emptyList<CategoryDto>().toDomainList())
    }

    @Test
    fun dtoListPreservesOrderAndDuplicateCategories() {
        val dtos = listOf(
            CategoryDto("tools", "Tools"),
            CategoryDto("games", "Games"),
            CategoryDto("tools", "Another tools title"),
        )

        assertEquals(
            listOf(
                Category("tools", "Tools"),
                Category("games", "Games"),
                Category("tools", "Another tools title"),
            ),
            dtos.toDomainList(),
        )
    }

    @Test
    fun domainMapsDistinctKeyAndTitleToDto() {
        assertEquals(CategoryDto("tools", "Tools"), Category("tools", "Tools").toDto())
    }

    @Test
    fun domainPreservesEmptyKeyAndTitleInDto() {
        assertEquals(CategoryDto("", ""), Category("", "").toDto())
    }

    @Test
    fun dtoRoundTripPreservesKeyAndTitle() {
        val dto = CategoryDto("games", "Games and entertainment")

        assertEquals(dto, dto.toDomain().toDto())
    }

    @Test
    fun domainRoundTripPreservesKeyAndTitle() {
        val category = Category("education", "Learning apps")

        assertEquals(category, category.toDto().toDomain())
    }
}
