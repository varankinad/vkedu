package com.example.vkedu.data.mapper

import com.example.vkedu.data.remote.dto.AppDetailDto
import com.example.vkedu.data.remote.dto.CatalogAppDto
import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class AppMapperTest {
    private val catalogDto = CatalogAppDto(
        id = "app-42",
        name = "Test app",
        description = "Catalog description",
        category = "TOOLS",
        iconUrl = "https://example.test/icon.png",
    )
    private val catalogApp = App(
        id = "app-42",
        name = "Test app",
        description = "Catalog description",
        category = Category("TOOLS", "TOOLS"),
        iconUrl = "https://example.test/icon.png",
    )
    private val screenshots = listOf(
        "https://example.test/second.png",
        "https://example.test/first.png",
        "https://example.test/second.png",
    )
    private val detailDto = AppDetailDto(
        id = "app-42",
        name = "Test app",
        developer = "Test developer",
        category = "TOOLS",
        description = "Full description",
        iconUrl = "https://example.test/icon.png",
        ageRating = 16,
        size = 43.75f,
        screenshotUrlList = screenshots,
    )
    private val detailApp = App(
        id = "app-42",
        name = "Test app",
        description = "Full description",
        category = Category("TOOLS", "TOOLS"),
        iconUrl = "https://example.test/icon.png",
        developer = "Test developer",
        size = 43.75f,
        ageRating = 16,
        screenshotUrlList = screenshots,
    )

    @Test
    fun catalogDtoMapsEveryFieldAndUsesDefaultDetailFields() {
        assertEquals(catalogApp, catalogDto.toDomain())
    }

    @Test
    fun catalogDtoPreservesEmptyFields() {
        val dto = CatalogAppDto("", "", "", "", "")

        assertEquals(App("", "", "", Category("", ""), ""), dto.toDomain())
    }

    @Test
    fun emptyCatalogListMapsToEmptyDomainList() {
        assertEquals(emptyList<App>(), emptyList<CatalogAppDto>().toDomainList())
    }

    @Test
    fun catalogListPreservesOrderAndDuplicateEntries() {
        val secondDto = catalogDto.copy(id = "app-7", category = "GAMES")
        val secondApp = catalogApp.copy(id = "app-7", category = Category("GAMES", "GAMES"))

        assertEquals(
            listOf(secondApp, catalogApp, secondApp),
            listOf(secondDto, catalogDto, secondDto).toDomainList(),
        )
    }

    @Test
    fun detailDtoMapsAllFieldsAndPreservesScreenshotOrderAndDuplicates() {
        assertEquals(detailApp, detailDto.toDomain())
    }

    @Test
    fun detailDtoWithOptionalDefaultsMapsToDefaultDomainDetails() {
        val dto = AppDetailDto(
            id = "app-42",
            name = "Test app",
            category = "TOOLS",
            description = "Catalog description",
            iconUrl = "https://example.test/icon.png",
        )

        assertEquals(catalogApp, dto.toDomain())
    }

    @Test
    fun domainAppMapsToCatalogDtoUsingCategoryKeyAndOmittingDetails() {
        val app = detailApp.copy(
            description = "Catalog description",
            category = Category("TOOLS", "Displayed tools title"),
        )

        val dto = app.toCatalogDto()

        assertEquals(catalogDto, dto)
        assertEquals(catalogApp, dto.toDomain())
    }

    @Test
    fun domainAppMapsAllDetailFieldsToDtoUsingCategoryKey() {
        val app = detailApp.copy(category = Category("TOOLS", "Displayed tools title"))

        assertEquals(detailDto, app.toDetailDto())
    }

    @Test
    fun domainAppWithDefaultDetailsMapsToDefaultDetailDto() {
        val expected = AppDetailDto(
            id = "app-42",
            name = "Test app",
            category = "TOOLS",
            description = "Catalog description",
            iconUrl = "https://example.test/icon.png",
        )

        assertEquals(expected, catalogApp.toDetailDto())
    }

    @Test
    fun catalogDtoRoundTripPreservesEveryDtoField() {
        assertEquals(catalogDto, catalogDto.toDomain().toCatalogDto())
    }

    @Test
    fun detailDtoRoundTripPreservesEveryDtoField() {
        assertEquals(detailDto, detailDto.toDomain().toDetailDto())
    }

    @Test
    fun domainDetailRoundTripPreservesDetailsAndRestoresCategoryTitleFromKey() {
        val app = detailApp.copy(category = Category("TOOLS", "Displayed tools title"))

        assertEquals(detailApp, app.toDetailDto().toDomain())
    }
}
