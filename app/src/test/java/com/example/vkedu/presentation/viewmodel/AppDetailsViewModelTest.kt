package com.example.vkedu.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category
import com.example.vkedu.domain.repository.AppRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppDetailsViewModelTest {
    private lateinit var scheduler: TestCoroutineScheduler
    private lateinit var mainDispatcher: TestDispatcher
    private lateinit var viewModelStore: ViewModelStore
    private var nextViewModelId = 0

    private val app = App(
        id = "selected-app",
        name = "Selected app",
        description = "Full description of the selected app",
        category = Category(key = "education", title = "Education"),
        iconUrl = "https://example.com/selected-app/icon.png",
        developer = "Selected developer",
        size = 43.75f,
        ageRating = 12,
        screenshotUrlList = listOf(
            "https://example.com/selected-app/screenshot-1.png",
            "https://example.com/selected-app/screenshot-2.png",
        ),
    )

    @Before
    fun setUp() {
        scheduler = TestCoroutineScheduler()
        mainDispatcher = StandardTestDispatcher(scheduler)
        viewModelStore = ViewModelStore()
        nextViewModelId = 0
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        try {
            viewModelStore.clear()
            scheduler.runCurrent()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun initLoadsRequestedIdAndPreservesAllAppFields() = runTest(mainDispatcher) {
        val repository = FakeAppRepository { Result.success(app) }
        val viewModel = createViewModel(repository)

        assertEquals(AppDetailsState.Loading, viewModel.state.value)
        assertTrue(repository.requestedIds.isEmpty())

        runCurrent()

        assertEquals(listOf(app.id), repository.requestedIds)
        assertContent(viewModel, InstallState.NotInstalled)
    }

    @Test
    fun missingOrBlankIdShowsErrorWithoutLoadingOrInstalling() = runTest(mainDispatcher) {
        val repository = FakeAppRepository { Result.success(app) }
        val viewModels = listOf(null, "", " \t\n").map { id ->
            createViewModel(repository, appId = id).also { viewModel ->
                assertEquals(AppDetailsState.Error, viewModel.state.value)
                repeat(3) {
                    viewModel.onInstallClick()
                    viewModel.retry()
                    assertEquals(AppDetailsState.Error, viewModel.state.value)
                }
            }
        }

        advanceTimeBy(6_000)
        runCurrent()

        assertTrue(repository.requestedIds.isEmpty())
        viewModels.forEach { assertEquals(AppDetailsState.Error, it.state.value) }
    }

    @Test
    fun installAndRetryClicksAreIgnoredWhileLoading() = runTest(mainDispatcher) {
        val repository = FakeAppRepository {
            delay(1_000)
            Result.success(app)
        }
        val viewModel = createViewModel(repository)

        repeat(3) {
            viewModel.onInstallClick()
            viewModel.retry()
        }
        assertEquals(AppDetailsState.Loading, viewModel.state.value)
        runCurrent()
        assertEquals(1, repository.activeRequests)

        repeat(3) {
            viewModel.onInstallClick()
            viewModel.retry()
        }
        advanceTimeBy(999)
        runCurrent()
        assertEquals(AppDetailsState.Loading, viewModel.state.value)

        advanceTimeBy(1)
        runCurrent()
        assertContent(viewModel, InstallState.NotInstalled)
        assertEquals(listOf(app.id), repository.requestedIds)

        advanceTimeBy(3_000)
        runCurrent()
        assertContent(viewModel, InstallState.NotInstalled)
    }

    @Test
    fun failedResultShowsErrorAndInstallClicksAreIgnored() = runTest(mainDispatcher) {
        val repository = FakeAppRepository {
            Result.failure(IllegalStateException("Details unavailable"))
        }
        val viewModel = createViewModel(repository)
        runCurrent()

        assertEquals(AppDetailsState.Error, viewModel.state.value)
        repeat(5) { viewModel.onInstallClick() }
        advanceTimeBy(6_000)
        runCurrent()

        assertEquals(AppDetailsState.Error, viewModel.state.value)
        assertEquals(listOf(app.id), repository.requestedIds)
    }

    @Test
    fun retryIsGuardedAndRecoversFromReturnedAndThrownFailures() = runTest(mainDispatcher) {
        var attempts = 0
        val repository = FakeAppRepository {
            attempts++
            delay(600)
            when (attempts) {
                1 -> Result.failure(IllegalStateException("Failed result"))
                2 -> throw IllegalStateException("Thrown failure")
                else -> Result.success(app)
            }
        }
        val viewModel = createViewModel(repository)
        runCurrent()
        advanceTimeBy(600)
        runCurrent()
        assertEquals(AppDetailsState.Error, viewModel.state.value)

        repeat(2) { retryIndex ->
            viewModel.retry()
            assertEquals(AppDetailsState.Loading, viewModel.state.value)
            repeat(5) {
                viewModel.retry()
                viewModel.onInstallClick()
            }
            runCurrent()
            assertEquals(List(retryIndex + 2) { app.id }, repository.requestedIds)
            assertEquals(1, repository.activeRequests)

            advanceTimeBy(599)
            runCurrent()
            assertEquals(AppDetailsState.Loading, viewModel.state.value)
            advanceTimeBy(1)
            runCurrent()

            if (retryIndex == 0) {
                assertEquals(AppDetailsState.Error, viewModel.state.value)
            } else {
                assertContent(viewModel, InstallState.NotInstalled)
            }
        }

        repeat(5) { viewModel.retry() }
        runCurrent()
        assertEquals(List(3) { app.id }, repository.requestedIds)
        assertContent(viewModel, InstallState.NotInstalled)
    }

    @Test
    fun thrownOrReturnedCancellationDoesNotBecomeError() = runTest(mainDispatcher) {
        for (returnFailure in listOf(false, true)) {
            val repository = FakeAppRepository {
                val cancellation = CancellationException("Cancelled details request")
                if (returnFailure) Result.failure(cancellation) else throw cancellation
            }
            val viewModel = createViewModel(repository)
            runCurrent()

            assertEquals(AppDetailsState.Loading, viewModel.state.value)
            assertEquals(listOf(app.id), repository.requestedIds)
            assertEquals(0, repository.activeRequests)

            viewModel.onInstallClick()
            viewModel.retry()
            advanceTimeBy(3_000)
            runCurrent()
            assertEquals(AppDetailsState.Loading, viewModel.state.value)
            assertEquals(listOf(app.id), repository.requestedIds)
        }
    }

    @Test
    fun installStartsSynchronouslyAtZeroAndWaitsForFirstStep() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onInstallClick()
        assertContent(viewModel, InstallState.Installing(0))

        runCurrent()
        assertContent(viewModel, InstallState.Installing(0))
        advanceTimeBy(149)
        runCurrent()
        assertContent(viewModel, InstallState.Installing(0))

        advanceTimeBy(1)
        runCurrent()
        assertContent(viewModel, InstallState.Installing(5))
    }

    @Test
    fun installEmitsEveryProgressStepInRangeAndFinishesAtThreeSeconds() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        val observed = mutableListOf<AppDetailsState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect { observed += it }
        }

        viewModel.onInstallClick()
        assertContent(viewModel, InstallState.Installing(0))
        runCurrent()

        for (progress in 5..100 step 5) {
            advanceTimeBy(149)
            runCurrent()
            assertContent(viewModel, InstallState.Installing(progress - 5))

            advanceTimeBy(1)
            runCurrent()
            assertContent(
                viewModel,
                if (progress == 100) InstallState.Installed else InstallState.Installing(progress),
            )
        }
        assertEquals(3_000L, currentTime)

        advanceTimeBy(1_000)
        runCurrent()
        assertContent(viewModel, InstallState.Installed)

        val installStates = observed.map { (it as AppDetailsState.Content).installState }
        val expected = listOf<InstallState>(InstallState.NotInstalled) +
            (0..100 step 5).map { InstallState.Installing(it) } + InstallState.Installed
        assertEquals(expected, installStates)
        assertTrue(installStates.filterIsInstance<InstallState.Installing>().all {
            it.progress in 0..100
        })
        observed.forEach { assertEquals(app, (it as AppDetailsState.Content).app) }
    }

    @Test
    fun installedAppCanBeEmulatedAgainFromZero() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onInstallClick()
        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()
        assertContent(viewModel, InstallState.Installed)

        viewModel.onInstallClick()
        assertContent(viewModel, InstallState.Installing(0))
        runCurrent()
        advanceTimeBy(149)
        runCurrent()
        assertContent(viewModel, InstallState.Installing(0))
        advanceTimeBy(1)
        runCurrent()
        assertContent(viewModel, InstallState.Installing(5))

        advanceTimeBy(2_850)
        runCurrent()
        assertContent(viewModel, InstallState.Installed)
        assertEquals(6_000L, currentTime)
    }

    @Test
    fun cancellationImmediatelyMidwayAndJustBeforeCompletionStopsInstallation() = runTest(mainDispatcher) {
        for (elapsedMillis in listOf(0L, 1_500L, 2_999L)) {
            val viewModel = createViewModel()
            runCurrent()
            viewModel.onInstallClick()
            runCurrent()
            advanceTimeBy(elapsedMillis)
            runCurrent()
            assertContent(viewModel, InstallState.Installing((elapsedMillis / 150 * 5).toInt()))

            viewModel.onInstallClick()
            assertContent(viewModel, InstallState.NotInstalled)
            runCurrent()
            assertContent(viewModel, InstallState.NotInstalled)

            advanceTimeBy(6_000)
            runCurrent()
            assertContent(viewModel, InstallState.NotInstalled)
        }
    }

    @Test
    fun immediateRestartIsNotResetOrUpdatedByCancelledJob() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onInstallClick()
        runCurrent()
        advanceTimeBy(725)
        runCurrent()
        assertContent(viewModel, InstallState.Installing(20))

        viewModel.onInstallClick()
        assertContent(viewModel, InstallState.NotInstalled)
        viewModel.onInstallClick()
        assertContent(viewModel, InstallState.Installing(0))
        val restartTime = currentTime
        runCurrent()
        assertContent(viewModel, InstallState.Installing(0))

        for (progress in 5..100 step 5) {
            advanceTimeBy(149)
            runCurrent()
            assertContent(viewModel, InstallState.Installing(progress - 5))
            advanceTimeBy(1)
            runCurrent()
            assertContent(
                viewModel,
                if (progress == 100) InstallState.Installed else InstallState.Installing(progress),
            )
        }

        assertEquals(restartTime + 3_000, currentTime)
    }

    @Test
    fun fastClicksBeforeDispatchDoNotLeaveMultipleInstallJobs() = runTest(mainDispatcher) {
        val viewModel = createViewModel()
        runCurrent()

        repeat(5) {
            viewModel.onInstallClick()
            assertContent(viewModel, InstallState.Installing(0))
            viewModel.onInstallClick()
            assertContent(viewModel, InstallState.NotInstalled)
        }
        viewModel.onInstallClick()
        assertContent(viewModel, InstallState.Installing(0))
        runCurrent()
        advanceTimeBy(300)
        runCurrent()
        assertContent(viewModel, InstallState.Installing(10))

        viewModel.onInstallClick()
        assertContent(viewModel, InstallState.NotInstalled)
        advanceTimeBy(6_000)
        runCurrent()
        assertContent(viewModel, InstallState.NotInstalled)
    }

    @Test
    fun newViewModelLoadsFreshContentWithInstallationReset() = runTest(mainDispatcher) {
        val repository = FakeAppRepository { Result.success(app) }
        val previousViewModel = createViewModel(repository)
        runCurrent()
        previousViewModel.onInstallClick()
        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()
        assertContent(previousViewModel, InstallState.Installed)
        viewModelStore.clear()

        val newViewModel = createViewModel(repository)
        assertEquals(AppDetailsState.Loading, newViewModel.state.value)
        runCurrent()
        assertContent(newViewModel, InstallState.NotInstalled)
        assertEquals(listOf(app.id, app.id), repository.requestedIds)

        advanceTimeBy(3_000)
        runCurrent()
        assertContent(newViewModel, InstallState.NotInstalled)
        assertContent(previousViewModel, InstallState.Installed)
    }

    @Test
    fun clearingStoreCancelsLoadingWithoutPublishingErrorOrContent() = runTest(mainDispatcher) {
        val repository = FakeAppRepository {
            delay(10_000)
            Result.success(app)
        }
        val viewModel = createViewModel(repository)
        runCurrent()
        assertEquals(AppDetailsState.Loading, viewModel.state.value)
        assertEquals(1, repository.activeRequests)

        viewModelStore.clear()
        runCurrent()
        assertEquals(1, repository.cancelledRequests)
        assertEquals(0, repository.activeRequests)
        assertEquals(AppDetailsState.Loading, viewModel.state.value)

        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(AppDetailsState.Loading, viewModel.state.value)
        assertEquals(listOf(app.id), repository.requestedIds)
    }

    @Test
    fun clearingStoreBeforeLoadingIsDispatchedPreventsRequest() = runTest(mainDispatcher) {
        val repository = FakeAppRepository { Result.success(app) }
        val viewModel = createViewModel(repository)

        viewModelStore.clear()
        advanceTimeBy(3_000)
        runCurrent()

        assertTrue(repository.requestedIds.isEmpty())
        assertEquals(AppDetailsState.Loading, viewModel.state.value)
    }

    @Test
    fun clearingStoreCancelsQueuedAndRunningInstallationsWithoutFurtherUpdates() = runTest(mainDispatcher) {
        for (elapsedMillis in listOf(0L, 1_050L, 2_999L)) {
            val viewModel = createViewModel()
            runCurrent()
            viewModel.onInstallClick()
            if (elapsedMillis > 0) {
                runCurrent()
                advanceTimeBy(elapsedMillis)
                runCurrent()
            }
            assertContent(viewModel, InstallState.Installing((elapsedMillis / 150 * 5).toInt()))
            val beforeClear = viewModel.state.value

            viewModelStore.clear()
            runCurrent()
            assertEquals(beforeClear, viewModel.state.value)
            advanceTimeBy(6_000)
            runCurrent()
            assertEquals(beforeClear, viewModel.state.value)
        }
    }

    private fun createViewModel(
        repository: AppRepository = FakeAppRepository { Result.success(app) },
        appId: String? = app.id,
    ): AppDetailsViewModel {
        val initialState = if (appId == null) emptyMap() else mapOf("appId" to appId)
        return AppDetailsViewModel(repository, SavedStateHandle(initialState)).also {
            viewModelStore.put("app-details-${nextViewModelId++}", it)
        }
    }

    private fun assertContent(viewModel: AppDetailsViewModel, installState: InstallState) {
        assertEquals(AppDetailsState.Content(app, installState), viewModel.state.value)
    }

    private class FakeAppRepository(
        private val loadApp: suspend (String) -> Result<App>,
    ) : AppRepository {
        val requestedIds = mutableListOf<String>()
        var activeRequests = 0
            private set
        var cancelledRequests = 0
            private set

        override suspend fun getAppById(id: String): Result<App> {
            requestedIds += id
            activeRequests++
            try {
                return loadApp(id)
            } catch (exception: CancellationException) {
                cancelledRequests++
                throw exception
            } finally {
                activeRequests--
            }
        }

        override suspend fun getApps(): Result<List<App>> =
            error("App details must not request the catalog")

        override suspend fun getCategories(): Result<List<Category>> =
            error("App details must not request categories")

        override suspend fun loadRatingsSequentially(ids: List<String>): Map<String, Float> =
            error("App details must not load ratings")

        override suspend fun loadRatingsInParallel(ids: List<String>): Map<String, Float> =
            error("App details must not load ratings")
    }
}
