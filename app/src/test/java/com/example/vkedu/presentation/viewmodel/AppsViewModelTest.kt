package com.example.vkedu.presentation.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category
import com.example.vkedu.domain.repository.AppRepository
import com.example.vkedu.domain.usecase.GetAppsUseCase
import com.example.vkedu.domain.usecase.LoadRatingsInParallelUseCase
import com.example.vkedu.domain.usecase.LoadRatingsSequentiallyUseCase
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppsViewModelTest {
    private lateinit var dispatcher: TestDispatcher
    private lateinit var viewModelStore: ViewModelStore

    private val category = Category("tools", "Tools")
    private val gamesCategory = Category("games", "Games")
    private val catalog = listOf(app("beta"), app("alpha", gamesCategory), app("gamma"))
    private val ids = catalog.map { it.id }
    private val sequentialRatings = ids.associateWith { 3.5f }
    private val parallelRatings = ids.associateWith { 4.5f }

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher(TestCoroutineScheduler())
        Dispatchers.setMain(dispatcher)
        viewModelStore = ViewModelStore()
    }

    @After
    fun tearDown() {
        try {
            viewModelStore.clear()
            dispatcher.scheduler.advanceUntilIdle()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun catalogLoadingDoesNotAutomaticallyCompareRatings() = runTest(dispatcher) {
        val repository = FakeAppRepository(catalog)
        val viewModel = createViewModel(repository)

        assertTrue(viewModel.isLoading.value)
        assertEquals(RatingsState(), viewModel.ratingsState.value)
        assertTrue(repository.ratingRequests.isEmpty())

        advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
        assertEquals(catalog, viewModel.apps.value.getOrThrow())
        assertEquals(listOf(category, gamesCategory), viewModel.categories.value.getOrThrow())
        assertEquals(1, repository.catalogCalls)
        assertEquals(RatingsState(), viewModel.ratingsState.value)
        assertTrue(repository.ratingRequests.isEmpty())
    }

    @Test
    fun bothPhasesUseOneOrderedDistinctIdSnapshot() = runTest(dispatcher) {
        val mutableCatalog = mutableListOf(
            catalog[1],
            catalog[0],
            catalog[1].copy(name = "Duplicate alpha"),
            catalog[2],
            catalog[0].copy(name = "Duplicate beta"),
        )
        val expectedIds = listOf("alpha", "beta", "gamma")
        val finishSequential = CompletableDeferred<Unit>()
        val repository = FakeAppRepository(mutableCatalog).apply {
            sequentialLoad = { snapshot ->
                finishSequential.await()
                snapshot.associateWith { 3.5f }
            }
        }
        val viewModel = createViewModel(repository)
        runCurrent()

        viewModel.compareRatings()
        assertEquals(expectedIds.size, viewModel.ratingsState.value.idCount)
        mutableCatalog += app("added-before-launch")
        runCurrent()
        assertEquals(
            listOf(RatingsLoadingMode.Sequential to expectedIds),
            repository.ratingRequests,
        )

        mutableCatalog.clear()
        mutableCatalog += app("added-before-parallel")
        finishSequential.complete(Unit)
        runCurrent()

        assertEquals(
            listOf(
                RatingsLoadingMode.Sequential to expectedIds,
                RatingsLoadingMode.Parallel to expectedIds,
            ),
            repository.ratingRequests,
        )
        assertSame(repository.ratingRequests[0].second, repository.ratingRequests[1].second)
        assertSuccessfulComparison(viewModel.ratingsState.value, parallelRatings)
    }

    @Test
    fun comparisonPublishesSequentialThenParallelRatingsAndKeepsBothMeasuredTimes() =
        runTest(dispatcher) {
            val repository = FakeAppRepository(catalog).apply {
                sequentialLoad = {
                    delay(1_000)
                    sequentialRatings
                }
                parallelLoad = {
                    delay(500)
                    parallelRatings
                }
            }
            val viewModel = createViewModel(repository)
            runCurrent()
            val observedStates = mutableListOf<RatingsState>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.ratingsState.collect { observedStates += it }
            }

            viewModel.compareRatings()
            val firstStage = RatingsState(
                loadingMode = RatingsLoadingMode.Sequential,
                idCount = ids.size,
            )
            assertEquals(firstStage, viewModel.ratingsState.value)
            assertTrue(repository.ratingRequests.isEmpty())
            runCurrent()
            advanceTimeBy(999)
            runCurrent()
            assertEquals(firstStage, viewModel.ratingsState.value)
            assertEquals(listOf(RatingsLoadingMode.Sequential to ids), repository.ratingRequests)

            advanceTimeBy(1)
            runCurrent()
            val secondStage = viewModel.ratingsState.value
            assertEquals(RatingsLoadingMode.Parallel, secondStage.loadingMode)
            assertEquals(sequentialRatings, secondStage.ratings)
            assertEquals(ids.size, secondStage.idCount)
            assertFalse(secondStage.hasError)
            assertNull(secondStage.parallelTimeMillis)
            val sequentialTime = requireNotNull(secondStage.sequentialTimeMillis)
            assertTrue(sequentialTime >= 0L)
            assertEquals(
                listOf(RatingsLoadingMode.Sequential to ids, RatingsLoadingMode.Parallel to ids),
                repository.ratingRequests,
            )

            advanceTimeBy(499)
            runCurrent()
            assertEquals(secondStage, viewModel.ratingsState.value)
            advanceTimeBy(1)
            runCurrent()

            val completed = viewModel.ratingsState.value
            assertSuccessfulComparison(completed, parallelRatings)
            assertEquals(sequentialTime, completed.sequentialTimeMillis)
            assertEquals(
                listOf(
                    null,
                    RatingsLoadingMode.Sequential,
                    RatingsLoadingMode.Parallel,
                    RatingsLoadingMode.Parallel,
                    null,
                ),
                observedStates.map { it.loadingMode },
            )
            assertEquals(
                listOf(emptyMap(), emptyMap(), sequentialRatings, parallelRatings, parallelRatings),
                observedStates.map { it.ratings },
            )
        }

    @Test
    fun comparisonClaimsLoadingSynchronouslyAndIgnoresClicksInEitherPhase() = runTest(dispatcher) {
        val finishSequential = CompletableDeferred<Map<String, Float>>()
        val finishParallel = CompletableDeferred<Map<String, Float>>()
        val repository = FakeAppRepository(catalog).apply {
            sequentialLoad = { finishSequential.await() }
            parallelLoad = { finishParallel.await() }
        }
        val viewModel = createViewModel(repository)
        runCurrent()

        viewModel.compareRatings()
        val firstStage = RatingsState(
            loadingMode = RatingsLoadingMode.Sequential,
            idCount = ids.size,
        )
        assertEquals(firstStage, viewModel.ratingsState.value)
        repeat(3) { viewModel.compareRatings() }
        assertEquals(firstStage, viewModel.ratingsState.value)
        assertTrue(repository.ratingRequests.isEmpty())

        runCurrent()
        repeat(3) { viewModel.compareRatings() }
        runCurrent()
        assertEquals(firstStage, viewModel.ratingsState.value)
        assertEquals(listOf(RatingsLoadingMode.Sequential to ids), repository.ratingRequests)

        finishSequential.complete(sequentialRatings)
        runCurrent()
        val secondStage = viewModel.ratingsState.value
        assertEquals(RatingsLoadingMode.Parallel, secondStage.loadingMode)
        repeat(3) { viewModel.compareRatings() }
        runCurrent()
        assertEquals(secondStage, viewModel.ratingsState.value)
        assertEquals(
            listOf(RatingsLoadingMode.Sequential to ids, RatingsLoadingMode.Parallel to ids),
            repository.ratingRequests,
        )

        finishParallel.complete(parallelRatings)
        runCurrent()
        assertSuccessfulComparison(viewModel.ratingsState.value, parallelRatings)
    }

    @Test
    fun nextComparisonResetsBothTimesAndKeepsPreviousRatingsUntilFirstPhaseCompletes() =
        runTest(dispatcher) {
            val repository = FakeAppRepository(catalog)
            val viewModel = createViewModel(repository)
            runCurrent()
            viewModel.compareRatings()
            runCurrent()
            assertSuccessfulComparison(viewModel.ratingsState.value, parallelRatings)

            val nextSequentialRatings = ids.associateWith { 3.8f }
            val nextParallelRatings = ids.associateWith { 4.8f }
            val finishSequential = CompletableDeferred<Unit>()
            val finishParallel = CompletableDeferred<Unit>()
            repository.sequentialLoad = {
                finishSequential.await()
                nextSequentialRatings
            }
            repository.parallelLoad = {
                finishParallel.await()
                nextParallelRatings
            }

            viewModel.compareRatings()
            val reset = RatingsState(
                ratings = parallelRatings,
                loadingMode = RatingsLoadingMode.Sequential,
                idCount = ids.size,
            )
            assertEquals(reset, viewModel.ratingsState.value)
            runCurrent()
            assertEquals(reset, viewModel.ratingsState.value)

            finishSequential.complete(Unit)
            runCurrent()
            assertEquals(nextSequentialRatings, viewModel.ratingsState.value.ratings)
            assertEquals(RatingsLoadingMode.Parallel, viewModel.ratingsState.value.loadingMode)
            assertNotNull(viewModel.ratingsState.value.sequentialTimeMillis)
            assertNull(viewModel.ratingsState.value.parallelTimeMillis)

            finishParallel.complete(Unit)
            runCurrent()
            assertSuccessfulComparison(viewModel.ratingsState.value, nextParallelRatings)
            assertEquals(
                listOf(
                    RatingsLoadingMode.Sequential to ids, RatingsLoadingMode.Parallel to ids,
                    RatingsLoadingMode.Sequential to ids, RatingsLoadingMode.Parallel to ids,
                ),
                repository.ratingRequests,
            )
        }

    @Test
    fun sequentialFailurePreservesCatalogAndPreviousRatingsAndAllowsRetry() = runTest(dispatcher) {
        val repository = FakeAppRepository(catalog)
        val viewModel = createViewModel(repository)
        runCurrent()
        viewModel.compareRatings()
        runCurrent()
        assertSuccessfulComparison(viewModel.ratingsState.value, parallelRatings)
        val appsBefore = viewModel.apps.value
        val categoriesBefore = viewModel.categories.value
        repository.sequentialLoad = { throw IOException("Sequential ratings failed") }

        viewModel.compareRatings()
        runCurrent()

        assertEquals(
            RatingsState(ratings = parallelRatings, idCount = ids.size, hasError = true),
            viewModel.ratingsState.value,
        )
        assertEquals(appsBefore, viewModel.apps.value)
        assertEquals(categoriesBefore, viewModel.categories.value)
        assertFalse(viewModel.isLoading.value)
        assertEquals(1, repository.catalogCalls)
        assertEquals(
            listOf(
                RatingsLoadingMode.Sequential to ids, RatingsLoadingMode.Parallel to ids,
                RatingsLoadingMode.Sequential to ids,
            ),
            repository.ratingRequests,
        )

        val finishRetry = CompletableDeferred<Unit>()
        val retryRatings = ids.associateWith { 4.9f }
        repository.sequentialLoad = {
            finishRetry.await()
            sequentialRatings
        }
        repository.parallelLoad = { retryRatings }
        viewModel.compareRatings()
        val retryState = RatingsState(
            ratings = parallelRatings,
            loadingMode = RatingsLoadingMode.Sequential,
            idCount = ids.size,
        )
        assertEquals(retryState, viewModel.ratingsState.value)
        runCurrent()
        assertEquals(retryState, viewModel.ratingsState.value)
        finishRetry.complete(Unit)
        runCurrent()

        assertSuccessfulComparison(viewModel.ratingsState.value, retryRatings)
        assertEquals(appsBefore, viewModel.apps.value)
        assertEquals(categoriesBefore, viewModel.categories.value)
        assertEquals(1, repository.catalogCalls)
        assertEquals(
            listOf(
                RatingsLoadingMode.Sequential to ids, RatingsLoadingMode.Parallel to ids,
                RatingsLoadingMode.Sequential to ids,
                RatingsLoadingMode.Sequential to ids, RatingsLoadingMode.Parallel to ids,
            ),
            repository.ratingRequests,
        )
    }

    @Test
    fun parallelFailurePreservesCatalogAndCompletedPhaseAndAllowsRetry() = runTest(dispatcher) {
        val failParallel = CompletableDeferred<Unit>()
        val repository = FakeAppRepository(catalog).apply {
            parallelLoad = {
                failParallel.await()
                throw IOException("Parallel ratings failed")
            }
        }
        val viewModel = createViewModel(repository)
        runCurrent()
        val appsBefore = viewModel.apps.value
        val categoriesBefore = viewModel.categories.value
        viewModel.compareRatings()
        runCurrent()
        val completedFirstPhase = viewModel.ratingsState.value
        assertEquals(sequentialRatings, completedFirstPhase.ratings)
        assertEquals(RatingsLoadingMode.Parallel, completedFirstPhase.loadingMode)
        assertTrue(requireNotNull(completedFirstPhase.sequentialTimeMillis) >= 0L)
        assertNull(completedFirstPhase.parallelTimeMillis)

        failParallel.complete(Unit)
        runCurrent()

        assertEquals(
            completedFirstPhase.copy(loadingMode = null, hasError = true),
            viewModel.ratingsState.value,
        )
        assertEquals(appsBefore, viewModel.apps.value)
        assertEquals(categoriesBefore, viewModel.categories.value)
        assertFalse(viewModel.isLoading.value)
        assertEquals(1, repository.catalogCalls)

        val finishRetry = CompletableDeferred<Unit>()
        val retryRatings = ids.associateWith { 4.9f }
        repository.sequentialLoad = {
            finishRetry.await()
            ids.associateWith { 3.9f }
        }
        repository.parallelLoad = { retryRatings }
        viewModel.compareRatings()
        val retryState = RatingsState(
            ratings = sequentialRatings,
            loadingMode = RatingsLoadingMode.Sequential,
            idCount = ids.size,
        )
        assertEquals(retryState, viewModel.ratingsState.value)
        runCurrent()
        assertEquals(retryState, viewModel.ratingsState.value)
        finishRetry.complete(Unit)
        runCurrent()

        assertSuccessfulComparison(viewModel.ratingsState.value, retryRatings)
        assertEquals(appsBefore, viewModel.apps.value)
        assertEquals(categoriesBefore, viewModel.categories.value)
        assertEquals(1, repository.catalogCalls)
        assertEquals(
            listOf(
                RatingsLoadingMode.Sequential to ids, RatingsLoadingMode.Parallel to ids,
                RatingsLoadingMode.Sequential to ids, RatingsLoadingMode.Parallel to ids,
            ),
            repository.ratingRequests,
        )
    }

    @Test
    fun comparisonIsIgnoredDuringInitialLoadingAndNonemptyCatalogReload() = runTest(dispatcher) {
        val finishInitialLoad = CompletableDeferred<Result<List<App>>>()
        val repository = FakeAppRepository(catalog).apply {
            catalogLoad = { finishInitialLoad.await() }
        }
        val viewModel = createViewModel(repository)
        viewModel.compareRatings()
        assertTrue(viewModel.isLoading.value)
        assertEquals(RatingsState(), viewModel.ratingsState.value)
        runCurrent()
        viewModel.compareRatings()
        assertEquals(RatingsState(), viewModel.ratingsState.value)
        assertTrue(repository.ratingRequests.isEmpty())

        finishInitialLoad.complete(Result.success(catalog))
        runCurrent()
        assertFalse(viewModel.isLoading.value)
        assertTrue(repository.ratingRequests.isEmpty())

        val finishReload = CompletableDeferred<Result<List<App>>>()
        repository.catalogLoad = { finishReload.await() }
        viewModel.loadData()
        assertTrue(viewModel.isLoading.value)
        assertEquals(catalog, viewModel.apps.value.getOrThrow())
        viewModel.compareRatings()
        assertEquals(RatingsState(), viewModel.ratingsState.value)
        runCurrent()
        viewModel.compareRatings()
        assertEquals(RatingsState(), viewModel.ratingsState.value)
        assertTrue(repository.ratingRequests.isEmpty())

        finishReload.complete(Result.success(catalog))
        runCurrent()
        assertFalse(viewModel.isLoading.value)
        assertEquals(2, repository.catalogCalls)
        assertTrue(repository.ratingRequests.isEmpty())
        viewModel.compareRatings()
        runCurrent()
        assertSuccessfulComparison(viewModel.ratingsState.value, parallelRatings)
    }

    @Test
    fun emptyCatalogCannotStartComparison() = runTest(dispatcher) {
        val repository = FakeAppRepository(emptyList())
        val viewModel = createViewModel(repository)
        runCurrent()

        repeat(3) { viewModel.compareRatings() }
        advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
        assertTrue(viewModel.apps.value.getOrThrow().isEmpty())
        assertEquals(RatingsState(), viewModel.ratingsState.value)
        assertTrue(repository.ratingRequests.isEmpty())
    }

    @Test
    fun failedCatalogCannotStartComparisonButSuccessfulReloadEnablesIt() = runTest(dispatcher) {
        val repository = FakeAppRepository(catalog).apply {
            catalogResult = Result.failure(IOException("Catalog failed"))
        }
        val viewModel = createViewModel(repository)
        runCurrent()

        viewModel.compareRatings()
        runCurrent()

        assertFalse(viewModel.isLoading.value)
        assertTrue(viewModel.apps.value.isFailure)
        assertTrue(viewModel.categories.value.isFailure)
        assertEquals(RatingsState(), viewModel.ratingsState.value)
        assertTrue(repository.ratingRequests.isEmpty())

        repository.catalogResult = Result.success(catalog)
        viewModel.loadData()
        viewModel.compareRatings()
        assertEquals(RatingsState(), viewModel.ratingsState.value)
        runCurrent()
        assertEquals(catalog, viewModel.apps.value.getOrThrow())
        assertTrue(repository.ratingRequests.isEmpty())
        viewModel.compareRatings()
        runCurrent()

        assertSuccessfulComparison(viewModel.ratingsState.value, parallelRatings)
        assertEquals(2, repository.catalogCalls)
    }

    @Test
    fun reloadCancelsSequentialPhaseAndResetsRatingsWithoutError() = runTest(dispatcher) {
        assertReloadCancelsPhase(RatingsLoadingMode.Sequential)
    }

    @Test
    fun reloadCancelsParallelPhaseAndResetsRatingsWithoutError() = runTest(dispatcher) {
        assertReloadCancelsPhase(RatingsLoadingMode.Parallel)
    }

    @Test
    fun cancelledSequentialResponseCannotPublishOrClearANewerComparison() = runTest(dispatcher) {
        assertOldComparisonCannotAffectNewOne(RatingsLoadingMode.Sequential, false)
    }

    @Test
    fun cancelledParallelResponseCannotPublishOrClearANewerComparison() = runTest(dispatcher) {
        assertOldComparisonCannotAffectNewOne(RatingsLoadingMode.Parallel, false)
    }

    @Test
    fun cancelledSequentialFailureCannotSetErrorOrClearANewerComparison() = runTest(dispatcher) {
        assertOldComparisonCannotAffectNewOne(RatingsLoadingMode.Sequential, true)
    }

    @Test
    fun cancelledParallelFailureCannotSetErrorOrClearANewerComparison() = runTest(dispatcher) {
        assertOldComparisonCannotAffectNewOne(RatingsLoadingMode.Parallel, true)
    }

    @Test
    fun immediateRepositoriesFinishAndCanRunAgainUnderUnconfinedMain() = runTest(dispatcher) {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repository = FakeAppRepository(catalog)
        val viewModel = createViewModel(repository)
        assertFalse(viewModel.isLoading.value)
        assertEquals(RatingsState(), viewModel.ratingsState.value)
        assertTrue(repository.ratingRequests.isEmpty())

        viewModel.compareRatings()
        assertSuccessfulComparison(viewModel.ratingsState.value, parallelRatings)
        viewModel.compareRatings()
        assertSuccessfulComparison(viewModel.ratingsState.value, parallelRatings)

        assertEquals(
            listOf(
                RatingsLoadingMode.Sequential to ids, RatingsLoadingMode.Parallel to ids,
                RatingsLoadingMode.Sequential to ids, RatingsLoadingMode.Parallel to ids,
            ),
            repository.ratingRequests,
        )
    }

    @Test
    fun clearingViewModelCancelsSequentialComparisonWithoutError() = runTest(dispatcher) {
        assertClearCancelsPhase(RatingsLoadingMode.Sequential)
    }

    @Test
    fun clearingViewModelCancelsParallelComparisonWithoutError() = runTest(dispatcher) {
        assertClearCancelsPhase(RatingsLoadingMode.Parallel)
    }

    private fun TestScope.assertReloadCancelsPhase(phase: RatingsLoadingMode) {
        val repository = FakeAppRepository(catalog)
        val delayedLoad: suspend (List<String>) -> Map<String, Float> = { snapshot ->
            delay(1_000)
            snapshot.associateWith { 4.9f }
        }
        if (phase == RatingsLoadingMode.Sequential) {
            repository.sequentialLoad = delayedLoad
        } else {
            repository.parallelLoad = delayedLoad
        }
        val viewModel = createViewModel(repository)
        runCurrent()
        viewModel.compareRatings()
        runCurrent()
        assertEquals(phase, viewModel.ratingsState.value.loadingMode)

        val reloadedCatalog = listOf(app("reloaded"))
        val finishReload = CompletableDeferred<Result<List<App>>>()
        repository.catalogLoad = { finishReload.await() }
        viewModel.loadData()
        assertEquals(RatingsState(), viewModel.ratingsState.value)
        assertTrue(viewModel.isLoading.value)
        runCurrent()
        assertEquals(listOf(phase), repository.cancelledModes)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(RatingsState(), viewModel.ratingsState.value)
        assertEquals(catalog, viewModel.apps.value.getOrThrow())

        finishReload.complete(Result.success(reloadedCatalog))
        runCurrent()
        assertFalse(viewModel.isLoading.value)
        assertEquals(reloadedCatalog, viewModel.apps.value.getOrThrow())
        assertEquals(listOf(category), viewModel.categories.value.getOrThrow())
        assertEquals(RatingsState(), viewModel.ratingsState.value)
        assertEquals(2, repository.catalogCalls)
        val expectedRequests = if (phase == RatingsLoadingMode.Sequential) {
            listOf(RatingsLoadingMode.Sequential to ids)
        } else {
            listOf(RatingsLoadingMode.Sequential to ids, RatingsLoadingMode.Parallel to ids)
        }
        assertEquals(expectedRequests, repository.ratingRequests)
    }

    private fun TestScope.assertOldComparisonCannotAffectNewOne(
        phase: RatingsLoadingMode,
        throwAfterCancellation: Boolean,
    ) {
        val originalCatalog = listOf(app("old"))
        val originalIds = originalCatalog.map { it.id }
        val replacementCatalog = listOf(app("new-beta"), app("new-alpha"))
        val replacementIds = replacementCatalog.map { it.id }
        val obsoleteRatings = originalIds.associateWith { 3.1f }
        val replacementSequentialRatings = replacementIds.associateWith { 3.9f }
        val replacementParallelRatings = replacementIds.associateWith { 4.9f }
        var obsoleteResponseFinished = false
        val obsoleteLoad: suspend () -> Map<String, Float> = {
            withContext(NonCancellable) { delay(1_000) }
            obsoleteResponseFinished = true
            if (throwAfterCancellation) throw IOException("Obsolete ratings failed")
            obsoleteRatings
        }
        val repository = FakeAppRepository(originalCatalog).apply {
            sequentialLoad = { snapshot ->
                if (snapshot == originalIds) {
                    if (phase == RatingsLoadingMode.Sequential) {
                        obsoleteLoad()
                    } else {
                        snapshot.associateWith { 3.5f }
                    }
                } else {
                    if (phase == RatingsLoadingMode.Sequential) delay(3_000)
                    replacementSequentialRatings
                }
            }
            parallelLoad = { snapshot ->
                if (snapshot == originalIds) {
                    obsoleteLoad()
                } else {
                    if (phase == RatingsLoadingMode.Parallel) delay(3_000)
                    replacementParallelRatings
                }
            }
        }
        val viewModel = createViewModel(repository)
        runCurrent()
        viewModel.compareRatings()
        runCurrent()
        assertEquals(phase, viewModel.ratingsState.value.loadingMode)

        repository.catalogResult = Result.success(replacementCatalog)
        viewModel.loadData()
        assertEquals(RatingsState(), viewModel.ratingsState.value)
        runCurrent()
        assertEquals(replacementCatalog, viewModel.apps.value.getOrThrow())
        viewModel.compareRatings()
        runCurrent()
        val newRunningState = viewModel.ratingsState.value
        assertEquals(phase, newRunningState.loadingMode)
        assertEquals(replacementIds.size, newRunningState.idCount)
        assertFalse(newRunningState.hasError)
        val runningRequests = repository.ratingRequests.toList()

        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(obsoleteResponseFinished)
        assertEquals(newRunningState, viewModel.ratingsState.value)
        assertEquals(runningRequests, repository.ratingRequests)
        viewModel.compareRatings()
        runCurrent()
        assertEquals(newRunningState, viewModel.ratingsState.value)
        assertEquals(runningRequests, repository.ratingRequests)

        advanceUntilIdle()
        assertSuccessfulComparison(viewModel.ratingsState.value, replacementParallelRatings)
        if (phase == RatingsLoadingMode.Parallel) {
            assertEquals(
                newRunningState.sequentialTimeMillis,
                viewModel.ratingsState.value.sequentialTimeMillis,
            )
        }
        val originalRequests = if (phase == RatingsLoadingMode.Sequential) {
            listOf(RatingsLoadingMode.Sequential to originalIds)
        } else {
            listOf(
                RatingsLoadingMode.Sequential to originalIds,
                RatingsLoadingMode.Parallel to originalIds,
            )
        }
        assertEquals(
            originalRequests + listOf(
                RatingsLoadingMode.Sequential to replacementIds,
                RatingsLoadingMode.Parallel to replacementIds,
            ),
            repository.ratingRequests,
        )
    }

    private fun TestScope.assertClearCancelsPhase(phase: RatingsLoadingMode) {
        val repository = FakeAppRepository(catalog)
        val delayedLoad: suspend (List<String>) -> Map<String, Float> = { snapshot ->
            delay(1_000)
            snapshot.associateWith { 4.9f }
        }
        if (phase == RatingsLoadingMode.Sequential) {
            repository.sequentialLoad = delayedLoad
        } else {
            repository.parallelLoad = delayedLoad
        }
        val viewModel = createViewModel(repository)
        runCurrent()
        viewModel.compareRatings()
        runCurrent()
        val runningState = viewModel.ratingsState.value
        assertEquals(phase, runningState.loadingMode)
        val runningRequests = repository.ratingRequests.toList()

        viewModelStore.clear()
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(listOf(phase), repository.cancelledModes)
        assertEquals(runningState.copy(loadingMode = null), viewModel.ratingsState.value)
        assertEquals(runningRequests, repository.ratingRequests)
        assertFalse(viewModel.ratingsState.value.hasError)
        assertEquals(catalog, viewModel.apps.value.getOrThrow())
        assertEquals(1, repository.catalogCalls)
    }

    private fun createViewModel(repository: AppRepository): AppsViewModel =
        AppsViewModel(
            GetAppsUseCase(repository),
            LoadRatingsSequentiallyUseCase(repository),
            LoadRatingsInParallelUseCase(repository),
        ).also { viewModelStore.put("apps", it) }

    private fun assertSuccessfulComparison(state: RatingsState, expectedRatings: Map<String, Float>) {
        assertEquals(expectedRatings, state.ratings)
        assertEquals(expectedRatings.size, state.idCount)
        assertNull(state.loadingMode)
        assertFalse(state.hasError)
        assertNotNull(state.sequentialTimeMillis)
        assertNotNull(state.parallelTimeMillis)
        assertTrue(state.sequentialTimeMillis!! >= 0L)
        assertTrue(state.parallelTimeMillis!! >= 0L)
    }

    private fun app(id: String, category: Category = this.category) = App(
        id = id,
        name = "App $id",
        description = "Description $id",
        category = category,
        iconUrl = "https://example.test/$id.png",
    )

    private class FakeAppRepository(initialApps: List<App>) : AppRepository {
        var catalogResult: Result<List<App>> = Result.success(initialApps)
        var catalogLoad: suspend () -> Result<List<App>> = { catalogResult }
        var sequentialLoad: suspend (List<String>) -> Map<String, Float> = { snapshot ->
            snapshot.associateWith { 3.5f }
        }
        var parallelLoad: suspend (List<String>) -> Map<String, Float> = { snapshot ->
            snapshot.associateWith { 4.5f }
        }
        var catalogCalls = 0
            private set
        val ratingRequests = mutableListOf<Pair<RatingsLoadingMode, List<String>>>()
        val cancelledModes = mutableListOf<RatingsLoadingMode>()

        override suspend fun getApps(): Result<List<App>> {
            catalogCalls++
            return catalogLoad()
        }

        override suspend fun getCategories(): Result<List<Category>> = catalogResult.map { apps ->
            apps.map { it.category }.distinctBy { it.key }
        }

        override suspend fun getAppById(id: String): Result<App> = catalogResult.mapCatching { apps ->
            apps.first { it.id == id }
        }

        override suspend fun loadRatingsSequentially(ids: List<String>): Map<String, Float> {
            ratingRequests += RatingsLoadingMode.Sequential to ids
            try {
                return sequentialLoad(ids)
            } catch (e: CancellationException) {
                cancelledModes += RatingsLoadingMode.Sequential
                throw e
            }
        }

        override suspend fun loadRatingsInParallel(ids: List<String>): Map<String, Float> {
            ratingRequests += RatingsLoadingMode.Parallel to ids
            try {
                return parallelLoad(ids)
            } catch (e: CancellationException) {
                cancelledModes += RatingsLoadingMode.Parallel
                throw e
            }
        }
    }
}
