package com.example.vkedu.data.repository

import com.example.vkedu.data.remote.api.CatalogApi
import com.example.vkedu.data.remote.api.CatalogHttpApi
import com.example.vkedu.data.remote.dto.AppDetailDto
import com.example.vkedu.data.remote.dto.CatalogAppDto
import com.example.vkedu.di.NetworkModule
import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category
import java.io.IOException
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppRepositoryImplTest {
    private val ids = List(14) { "app-$it" }
    private val ratings = ids.mapIndexed { index, id -> id to (30 + index) / 10f }.toMap()
    private val catalog = listOf(
        CatalogAppDto(
            id = "beta",
            name = "Beta app",
            description = "Beta description",
            category = "TOOLS",
            iconUrl = "https://example.test/beta.png",
        ),
        CatalogAppDto(
            id = "alpha",
            name = "Alpha app",
            description = "Alpha description",
            category = "GAMES",
            iconUrl = "https://example.test/alpha.png",
        ),
        CatalogAppDto(
            id = "gamma",
            name = "Gamma app",
            description = "Gamma description",
            category = "TOOLS",
            iconUrl = "https://example.test/gamma.png",
        ),
    )

    @Test
    fun getAppsMapsCatalogAndPreservesApiOrder() = runTest {
        val api = StubCatalogApi(catalog = catalog)

        val result = AppRepositoryImpl(api).getApps()

        assertEquals(
            listOf(
                App(
                    id = "beta",
                    name = "Beta app",
                    description = "Beta description",
                    category = Category("TOOLS", "TOOLS"),
                    iconUrl = "https://example.test/beta.png",
                ),
                App(
                    id = "alpha",
                    name = "Alpha app",
                    description = "Alpha description",
                    category = Category("GAMES", "GAMES"),
                    iconUrl = "https://example.test/alpha.png",
                ),
                App(
                    id = "gamma",
                    name = "Gamma app",
                    description = "Gamma description",
                    category = Category("TOOLS", "TOOLS"),
                    iconUrl = "https://example.test/gamma.png",
                ),
            ),
            result.getOrThrow(),
        )
        assertEquals(1, api.catalogCalls)
        assertTrue(api.detailIds.isEmpty())
    }

    @Test
    fun getAppsReturnsSuccessfulEmptyListForEmptyCatalog() = runTest {
        val api = StubCatalogApi()

        val result = AppRepositoryImpl(api).getApps()

        assertEquals(emptyList<App>(), result.getOrThrow())
        assertEquals(1, api.catalogCalls)
    }

    @Test
    fun getCategoriesRemovesDuplicatesAndPreservesFirstAppearanceOrder() = runTest {
        val api = StubCatalogApi(catalog = catalog)

        val result = AppRepositoryImpl(api).getCategories()

        assertEquals(
            listOf(Category("TOOLS", "TOOLS"), Category("GAMES", "GAMES")),
            result.getOrThrow(),
        )
        assertEquals(1, api.catalogCalls)
        assertTrue(api.detailIds.isEmpty())
    }

    @Test
    fun getCategoriesReturnsSuccessfulEmptyListForEmptyCatalog() = runTest {
        val api = StubCatalogApi()

        val result = AppRepositoryImpl(api).getCategories()

        assertEquals(emptyList<Category>(), result.getOrThrow())
        assertEquals(1, api.catalogCalls)
    }

    @Test
    fun getAppByIdDelegatesOriginalIdAndMapsAllDetailFields() = runTest {
        val requestedId = "app/with spaces"
        val detail = AppDetailDto(
            id = "returned-app",
            name = "Detailed app",
            developer = "App developer",
            category = "EDUCATION",
            description = "Full description",
            iconUrl = "https://example.test/icon.png",
            ageRating = 12,
            size = 43.75f,
            screenshotUrlList = listOf("https://example.test/second.png", "https://example.test/first.png"),
        )
        val api = StubCatalogApi(detail = detail)

        val result = AppRepositoryImpl(api).getAppById(requestedId)

        assertEquals(
            App(
                id = "returned-app",
                name = "Detailed app",
                description = "Full description",
                category = Category("EDUCATION", "EDUCATION"),
                iconUrl = "https://example.test/icon.png",
                developer = "App developer",
                size = 43.75f,
                ageRating = 12,
                screenshotUrlList = listOf("https://example.test/second.png", "https://example.test/first.png"),
            ),
            result.getOrThrow(),
        )
        assertEquals(listOf(requestedId), api.detailIds)
        assertEquals(0, api.catalogCalls)
    }

    @Test
    fun sequentialRequestsFollowInputOrderWithOneActiveRequestAndAccumulateDelay() = runTest {
        val api = RecordingCatalogApi(now = { currentTime }) { id ->
            delay(1500)
            ratings.getValue(id)
        }
        val start = currentTime

        val result = AppRepositoryImpl(api).loadRatingsSequentially(ids)

        assertEquals(ratings, result)
        assertEquals(ids, api.startedIds)
        assertEquals(ids, api.completedIds)
        assertEquals(ids.indices.map { start + it * 1500L }, api.startedAt)
        assertEquals(1, api.maxActiveRequests)
        assertEquals(0, api.activeRequests)
        assertEquals(ids.size * 1500L, currentTime - start)
    }

    @Test
    fun parallelRequestsAllStartTogetherAndFinishAfterOneDelay() = runTest {
        val api = RecordingCatalogApi(now = { currentTime }) { id ->
            delay(1500)
            ratings.getValue(id)
        }
        val start = currentTime
        val loading = async { AppRepositoryImpl(api).loadRatingsInParallel(ids) }

        runCurrent()
        assertEquals(ids.toSet(), api.startedIds.toSet())
        assertEquals(ids.size, api.startedIds.size)
        assertEquals(List(ids.size) { start }, api.startedAt)
        assertEquals(ids.size, api.activeRequests)
        assertEquals(ids.size, api.maxActiveRequests)
        assertTrue(api.completedIds.isEmpty())

        advanceTimeBy(1499)
        runCurrent()
        assertFalse(loading.isCompleted)
        assertTrue(api.completedIds.isEmpty())

        advanceTimeBy(1)
        runCurrent()
        assertEquals(ratings, loading.await())
        assertEquals(ids.toSet(), api.completedIds.toSet())
        assertEquals(0, api.activeRequests)
        assertEquals(1500L, currentTime - start)
    }

    @Test
    fun parallelResultsKeepIdAssociationsWhenRequestsFinishInReverseOrder() = runTest {
        val api = RecordingCatalogApi(now = { currentTime }) { id ->
            delay((ids.size - ids.indexOf(id)) * 100L)
            ratings.getValue(id)
        }

        val result = AppRepositoryImpl(api).loadRatingsInParallel(ids)

        assertEquals(ids.reversed(), api.completedIds)
        assertEquals(ratings, result)
        assertEquals(ids.size, api.maxActiveRequests)
        assertEquals(0, api.activeRequests)
    }

    @Test
    fun emptyInputsReturnEmptyMapsWithoutApiCallsOrDelay() = runTest {
        val api = RecordingCatalogApi(now = { currentTime }) {
            error("Empty input must not request ratings")
        }
        val repository = AppRepositoryImpl(api)

        assertTrue(repository.loadRatingsSequentially(emptyList()).isEmpty())
        assertTrue(repository.loadRatingsInParallel(emptyList()).isEmpty())
        assertTrue(api.startedIds.isEmpty())
        assertEquals(0, api.maxActiveRequests)
        assertEquals(0L, currentTime)
    }

    @Test
    fun cancellingSequentialLoadCancelsCurrentRequestAndDoesNotStartRemainingIds() = runTest {
        val api = RecordingCatalogApi(now = { currentTime }) { id ->
            delay(1500)
            ratings.getValue(id)
        }
        val loading = async { AppRepositoryImpl(api).loadRatingsSequentially(ids) }

        runCurrent()
        assertEquals(listOf(ids.first()), api.startedIds)
        assertEquals(1, api.activeRequests)
        advanceTimeBy(750)
        loading.cancelAndJoin()
        advanceUntilIdle()

        assertTrue(loading.isCancelled)
        assertTrue(runCatching { loading.await() }.exceptionOrNull() is CancellationException)
        assertEquals(listOf(ids.first()), api.startedIds)
        assertEquals(listOf(ids.first()), api.cancelledIds)
        assertTrue(api.completedIds.isEmpty())
        assertEquals(0, api.activeRequests)
    }

    @Test
    fun cancellingParallelLoadCancelsEveryActiveRequest() = runTest {
        val api = RecordingCatalogApi(now = { currentTime }) { id ->
            delay(1500)
            ratings.getValue(id)
        }
        val loading = async { AppRepositoryImpl(api).loadRatingsInParallel(ids) }

        runCurrent()
        assertEquals(ids.size, api.activeRequests)
        advanceTimeBy(750)
        loading.cancelAndJoin()
        advanceUntilIdle()

        assertTrue(loading.isCancelled)
        assertTrue(runCatching { loading.await() }.exceptionOrNull() is CancellationException)
        assertEquals(ids.toSet(), api.startedIds.toSet())
        assertEquals(ids.size, api.startedIds.size)
        assertEquals(ids.toSet(), api.cancelledIds.toSet())
        assertEquals(ids.size, api.cancelledIds.size)
        assertTrue(api.completedIds.isEmpty())
        assertEquals(0, api.activeRequests)
    }

    @Test
    fun parallelFailureIsRethrownAndCancelsAllSiblingRequests() = runTest {
        val failingId = ids[5]
        val failure = IOException("Rating request failed")
        val api = RecordingCatalogApi(now = { currentTime }) { id ->
            if (id == failingId) {
                delay(100)
                throw failure
            }
            delay(1500)
            ratings.getValue(id)
        }

        supervisorScope {
            val loading = async { AppRepositoryImpl(api).loadRatingsInParallel(ids) }
            runCurrent()
            assertEquals(ids.toSet(), api.startedIds.toSet())
            assertEquals(ids.size, api.activeRequests)

            advanceTimeBy(100)
            runCurrent()
            val thrown = runCatching { loading.await() }.exceptionOrNull()
            assertTrue(thrown is IOException)
            assertEquals(failure.message, thrown?.message)
        }
        advanceUntilIdle()

        assertEquals(ids.filterNot { it == failingId }.toSet(), api.cancelledIds.toSet())
        assertEquals(ids.size - 1, api.cancelledIds.size)
        assertTrue(api.completedIds.isEmpty())
        assertEquals(0, api.activeRequests)
    }

    @Test
    fun sequentialFailureIsRethrownWithoutRequestingRemainingIds() = runTest {
        val failingIndex = 3
        val failure = IOException("Rating request failed")
        val api = RecordingCatalogApi(now = { currentTime }) { id ->
            delay(1500)
            if (id == ids[failingIndex]) throw failure
            ratings.getValue(id)
        }

        val result = runCatching { AppRepositoryImpl(api).loadRatingsSequentially(ids) }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(ids.take(failingIndex + 1), api.startedIds)
        assertEquals(ids.take(failingIndex), api.completedIds)
        assertEquals(0, api.activeRequests)
    }

    @Test
    fun defaultRatingSimulationWaits1500MillisecondsAndReturnsTenthsInRange() = runTest {
        val api = NoHttpCatalogApi()
        val requests = ids.map { id -> async { api.getRating(id) } }

        runCurrent()
        assertTrue(requests.none { it.isCompleted })
        advanceTimeBy(1499)
        runCurrent()
        assertTrue(requests.none { it.isCompleted })

        advanceTimeBy(1)
        runCurrent()
        assertTrue(requests.all { it.isCompleted })
        requests.forEach { assertSimulatedRating(it.await()) }
        assertEquals(1500L, currentTime)
    }

    @Test
    fun catalogLoadMethodsRethrowCancellationInsteadOfReturningResultFailure() = runTest {
        val cancellation = CancellationException("Catalog request cancelled")
        val repository = AppRepositoryImpl(FailingCatalogApi(cancellation))

        assertSame(
            "getApps must rethrow cancellation",
            cancellation,
            runCatching { repository.getApps() }.exceptionOrNull(),
        )
        assertSame(
            "getCategories must rethrow cancellation",
            cancellation,
            runCatching { repository.getCategories() }.exceptionOrNull(),
        )
        assertSame(
            "getAppById must rethrow cancellation",
            cancellation,
            runCatching { repository.getAppById(ids.first()) }.exceptionOrNull(),
        )
    }

    @Test
    fun catalogLoadMethodsStillReturnResultFailureForOtherExceptions() = runTest {
        val failure = IOException("Catalog request failed")
        val repository = AppRepositoryImpl(FailingCatalogApi(failure))

        assertSame("getApps must return Result.failure", failure, repository.getApps().exceptionOrNull())
        assertSame(
            "getCategories must return Result.failure",
            failure,
            repository.getCategories().exceptionOrNull(),
        )
        assertSame(
            "getAppById must return Result.failure",
            failure,
            repository.getAppById(ids.first()).exceptionOrNull(),
        )
    }

    @Test
    fun kotlinCatalogAdapterDelegatesHttpMethodsAndInheritsRatingSimulation() = runTest {
        val detailId = ids.last()
        val catalog = listOf(
            CatalogAppDto(
                id = detailId,
                name = "Test app",
                description = "Test description",
                category = "UTILITIES",
                iconUrl = "https://example.com/icon.png",
            ),
        )
        val detail = AppDetailDto(
            id = detailId,
            name = "Test app",
            category = "UTILITIES",
            description = "Test description",
            iconUrl = "https://example.com/icon.png",
        )
        var catalogCalls = 0
        val detailIds = mutableListOf<String>()
        val httpApi = object : CatalogHttpApi {
            override suspend fun getCatalog(): List<CatalogAppDto> {
                catalogCalls++
                return catalog
            }

            override suspend fun getAppDetail(id: String): AppDetailDto {
                detailIds += id
                return detail
            }
        }
        val api = NetworkModule.provideCatalogApi(httpApi)

        assertSame(catalog, api.getCatalog())
        assertSame(detail, api.getAppDetail(detailId))
        val rating = async { api.getRating("rating-only-id") }
        runCurrent()
        advanceTimeBy(1499)
        runCurrent()
        assertFalse(rating.isCompleted)

        advanceTimeBy(1)
        runCurrent()
        assertTrue(rating.isCompleted)
        assertSimulatedRating(rating.await())
        assertEquals(1500L, currentTime)
        assertEquals(1, catalogCalls)
        assertEquals(listOf(detailId), detailIds)
    }

    private fun assertSimulatedRating(rating: Float) {
        assertTrue("Rating must be in 3.0..5.0, but was $rating", rating in 3f..5f)
        assertEquals("Rating must have one decimal place", (rating * 10).roundToInt() / 10f, rating, 0f)
    }

    private open class NoHttpCatalogApi : CatalogApi {
        override suspend fun getCatalog(): List<CatalogAppDto> =
            error("Ratings must not fetch the HTTP catalog")

        override suspend fun getAppDetail(id: String): AppDetailDto =
            error("Ratings must not fetch HTTP app details")
    }

    private class FailingCatalogApi(private val failure: Exception) : NoHttpCatalogApi() {
        override suspend fun getCatalog(): List<CatalogAppDto> = throw failure

        override suspend fun getAppDetail(id: String): AppDetailDto = throw failure
    }

    private class StubCatalogApi(
        private val catalog: List<CatalogAppDto> = emptyList(),
        private val detail: AppDetailDto? = null,
    ) : NoHttpCatalogApi() {
        var catalogCalls = 0
            private set
        val detailIds = mutableListOf<String>()

        override suspend fun getCatalog(): List<CatalogAppDto> {
            catalogCalls++
            return catalog
        }

        override suspend fun getAppDetail(id: String): AppDetailDto {
            detailIds += id
            return checkNotNull(detail)
        }
    }

    private class RecordingCatalogApi(
        private val now: () -> Long,
        private val respond: suspend (String) -> Float,
    ) : NoHttpCatalogApi() {
        val startedIds = mutableListOf<String>()
        val startedAt = mutableListOf<Long>()
        val completedIds = mutableListOf<String>()
        val cancelledIds = mutableListOf<String>()
        var activeRequests = 0
            private set
        var maxActiveRequests = 0
            private set

        override suspend fun getRating(id: String): Float {
            startedIds += id
            startedAt += now()
            activeRequests++
            maxActiveRequests = maxOf(maxActiveRequests, activeRequests)
            try {
                val rating = respond(id)
                completedIds += id
                return rating
            } catch (e: CancellationException) {
                cancelledIds += id
                throw e
            } finally {
                activeRequests--
            }
        }
    }
}
