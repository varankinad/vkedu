package com.example.vkedu.domain.usecase

import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category
import com.example.vkedu.domain.repository.AppRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppUseCasesTest {
    private val app = App(
        id = "selected-app",
        name = "Selected app",
        description = "Full app description",
        category = Category("tools", "Tools"),
        iconUrl = "https://example.test/icon.png",
        developer = "App developer",
        size = 43.75f,
        ageRating = 12,
        screenshotUrlList = listOf("https://example.test/second.png", "https://example.test/first.png"),
    )
    private val apps = listOf(app.copy(id = "beta"), app.copy(id = "alpha"))
    private val ids = listOf("beta", "alpha", "beta")
    private val ratings = mapOf("beta" to 3.5f, "alpha" to 4.5f)

    @Test
    fun getAppsReturnsRepositoryCatalogWithoutChangingFieldsOrOrder() = runTest {
        val repository = FakeAppRepository().apply {
            appsLoad = { Result.success(apps) }
        }

        val result = GetAppsUseCase(repository)()

        assertSame(apps, result.getOrThrow())
        assertEquals(1, repository.appsCalls)
    }

    @Test
    fun getAppsReturnsSuccessfulEmptyCatalog() = runTest {
        val repository = FakeAppRepository().apply {
            appsLoad = { Result.success(emptyList()) }
        }

        val result = GetAppsUseCase(repository)()

        assertEquals(emptyList<App>(), result.getOrThrow())
        assertEquals(1, repository.appsCalls)
    }

    @Test
    fun getAppsReturnsOriginalRepositoryFailure() = runTest {
        val failure = IOException("Catalog unavailable")
        val repository = FakeAppRepository().apply {
            appsLoad = { Result.failure(failure) }
        }

        val result = GetAppsUseCase(repository)()

        assertSame(failure, result.exceptionOrNull())
        assertEquals(1, repository.appsCalls)
    }

    @Test
    fun getAppsRethrowsRepositoryCancellation() = runTest {
        val cancellation = CancellationException("Catalog cancelled")
        val repository = FakeAppRepository().apply {
            appsLoad = { throw cancellation }
        }

        val result = runCatching { GetAppsUseCase(repository)() }

        assertSame(cancellation, result.exceptionOrNull())
        assertEquals(1, repository.appsCalls)
    }

    @Test
    fun getAppsReloadsRepositoryInsteadOfCachingPreviousResult() = runTest {
        val repository = FakeAppRepository().apply {
            appsLoad = { Result.success(apps) }
        }
        val useCase = GetAppsUseCase(repository)
        assertSame(apps, useCase().getOrThrow())
        val updatedApps = listOf(app.copy(id = "new-app"))
        repository.appsLoad = { Result.success(updatedApps) }

        assertSame(updatedApps, useCase().getOrThrow())
        assertEquals(2, repository.appsCalls)
    }

    @Test
    fun getAppByIdDelegatesExactIdOnceAndReturnsAllRepositoryFields() = runTest {
        val requestedId = "id/with spaces"
        val repository = FakeAppRepository().apply {
            detailLoad = { Result.success(app) }
        }

        val result = GetAppByIdUseCase(repository)(requestedId)

        assertSame(app, result.getOrThrow())
        assertEquals(listOf(requestedId), repository.detailIds)
    }

    @Test
    fun getAppByIdReturnsOriginalRepositoryFailure() = runTest {
        val failure = IOException("Details unavailable")
        val repository = FakeAppRepository().apply {
            detailLoad = { Result.failure(failure) }
        }

        val result = GetAppByIdUseCase(repository)(app.id)

        assertSame(failure, result.exceptionOrNull())
        assertEquals(listOf(app.id), repository.detailIds)
    }

    @Test
    fun getAppByIdRethrowsRepositoryCancellation() = runTest {
        val cancellation = CancellationException("Details cancelled")
        val repository = FakeAppRepository().apply {
            detailLoad = { throw cancellation }
        }

        val result = runCatching { GetAppByIdUseCase(repository)(app.id) }

        assertSame(cancellation, result.exceptionOrNull())
        assertEquals(listOf(app.id), repository.detailIds)
    }

    @Test
    fun getAppByIdRethrowsThrownRepositoryFailure() = runTest {
        val failure = IOException("Repository threw an exception")
        val repository = FakeAppRepository().apply {
            detailLoad = { throw failure }
        }

        val result = runCatching { GetAppByIdUseCase(repository)(app.id) }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(listOf(app.id), repository.detailIds)
    }

    @Test
    fun getAppByIdLoadsEachRequestedIdWithoutReusingPreviousApp() = runTest {
        val repository = FakeAppRepository().apply {
            detailLoad = { id -> Result.success(app.copy(id = id)) }
        }
        val useCase = GetAppByIdUseCase(repository)

        assertEquals(app.copy(id = "beta"), useCase("beta").getOrThrow())
        assertEquals(app.copy(id = "alpha"), useCase("alpha").getOrThrow())
        assertEquals(listOf("beta", "alpha"), repository.detailIds)
    }

    @Test
    fun sequentialRatingsDelegatesOriginalIdsIncludingDuplicatesAndReturnsRepositoryMap() = runTest {
        val repository = FakeAppRepository().apply {
            sequentialLoad = { ratings }
        }

        val result = LoadRatingsSequentiallyUseCase(repository)(ids)

        assertSame(ratings, result)
        assertSame(ids, repository.sequentialRequests.single())
    }

    @Test
    fun sequentialRatingsDelegatesEmptyInputAndReturnsEmptyMap() = runTest {
        val repository = FakeAppRepository().apply {
            sequentialLoad = { emptyMap() }
        }

        val result = LoadRatingsSequentiallyUseCase(repository)(emptyList())

        assertEquals(emptyMap<String, Float>(), result)
        assertEquals(listOf(emptyList<String>()), repository.sequentialRequests)
    }

    @Test
    fun sequentialRatingsRethrowsRepositoryFailure() = runTest {
        val failure = IOException("Sequential ratings unavailable")
        val repository = FakeAppRepository().apply {
            sequentialLoad = { throw failure }
        }

        val result = runCatching { LoadRatingsSequentiallyUseCase(repository)(ids) }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(listOf(ids), repository.sequentialRequests)
    }

    @Test
    fun sequentialRatingsRethrowsRepositoryCancellation() = runTest {
        val cancellation = CancellationException("Sequential ratings cancelled")
        val repository = FakeAppRepository().apply {
            sequentialLoad = { throw cancellation }
        }

        val result = runCatching { LoadRatingsSequentiallyUseCase(repository)(ids) }

        assertSame(cancellation, result.exceptionOrNull())
        assertEquals(listOf(ids), repository.sequentialRequests)
    }

    @Test
    fun sequentialRatingsWaitsForSuspendedRepositoryResult() = runTest {
        val repository = FakeAppRepository().apply {
            sequentialLoad = {
                delay(500)
                ratings
            }
        }
        val loading = async { LoadRatingsSequentiallyUseCase(repository)(ids) }

        runCurrent()
        assertFalse(loading.isCompleted)
        assertEquals(listOf(ids), repository.sequentialRequests)
        advanceTimeBy(499)
        runCurrent()
        assertFalse(loading.isCompleted)
        advanceTimeBy(1)
        runCurrent()

        assertSame(ratings, loading.await())
        assertEquals(500L, currentTime)
    }

    @Test
    fun parallelRatingsDelegatesOriginalIdsIncludingDuplicatesAndReturnsRepositoryMap() = runTest {
        val repository = FakeAppRepository().apply {
            parallelLoad = { ratings }
        }

        val result = LoadRatingsInParallelUseCase(repository)(ids)

        assertSame(ratings, result)
        assertSame(ids, repository.parallelRequests.single())
    }

    @Test
    fun parallelRatingsDelegatesEmptyInputAndReturnsEmptyMap() = runTest {
        val repository = FakeAppRepository().apply {
            parallelLoad = { emptyMap() }
        }

        val result = LoadRatingsInParallelUseCase(repository)(emptyList())

        assertEquals(emptyMap<String, Float>(), result)
        assertEquals(listOf(emptyList<String>()), repository.parallelRequests)
    }

    @Test
    fun parallelRatingsRethrowsRepositoryFailure() = runTest {
        val failure = IOException("Parallel ratings unavailable")
        val repository = FakeAppRepository().apply {
            parallelLoad = { throw failure }
        }

        val result = runCatching { LoadRatingsInParallelUseCase(repository)(ids) }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(listOf(ids), repository.parallelRequests)
    }

    @Test
    fun parallelRatingsRethrowsRepositoryCancellation() = runTest {
        val cancellation = CancellationException("Parallel ratings cancelled")
        val repository = FakeAppRepository().apply {
            parallelLoad = { throw cancellation }
        }

        val result = runCatching { LoadRatingsInParallelUseCase(repository)(ids) }

        assertSame(cancellation, result.exceptionOrNull())
        assertEquals(listOf(ids), repository.parallelRequests)
    }

    @Test
    fun parallelRatingsWaitsForSuspendedRepositoryResult() = runTest {
        val repository = FakeAppRepository().apply {
            parallelLoad = {
                delay(500)
                ratings
            }
        }
        val loading = async { LoadRatingsInParallelUseCase(repository)(ids) }

        runCurrent()
        assertFalse(loading.isCompleted)
        assertEquals(listOf(ids), repository.parallelRequests)
        advanceTimeBy(499)
        runCurrent()
        assertFalse(loading.isCompleted)
        advanceTimeBy(1)
        runCurrent()

        assertSame(ratings, loading.await())
        assertEquals(500L, currentTime)
    }

    private class FakeAppRepository : AppRepository {
        var appsLoad: suspend () -> Result<List<App>> = { error("Unexpected catalog request") }
        var detailLoad: suspend (String) -> Result<App> = { error("Unexpected details request") }
        var sequentialLoad: suspend (List<String>) -> Map<String, Float> = {
            error("Unexpected sequential ratings request")
        }
        var parallelLoad: suspend (List<String>) -> Map<String, Float> = {
            error("Unexpected parallel ratings request")
        }
        var appsCalls = 0
            private set
        val detailIds = mutableListOf<String>()
        val sequentialRequests = mutableListOf<List<String>>()
        val parallelRequests = mutableListOf<List<String>>()

        override suspend fun getApps(): Result<List<App>> {
            appsCalls++
            return appsLoad()
        }

        override suspend fun getAppById(id: String): Result<App> {
            detailIds += id
            return detailLoad(id)
        }

        override suspend fun getCategories(): Result<List<Category>> =
            error("Use cases must not request unrelated categories")

        override suspend fun loadRatingsSequentially(ids: List<String>): Map<String, Float> {
            sequentialRequests += ids
            return sequentialLoad(ids)
        }

        override suspend fun loadRatingsInParallel(ids: List<String>): Map<String, Float> {
            parallelRequests += ids
            return parallelLoad(ids)
        }
    }
}
