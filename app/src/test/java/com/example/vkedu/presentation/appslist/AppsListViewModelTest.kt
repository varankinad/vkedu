package com.example.vkedu.presentation.appslist

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppsListViewModelTest {
    @Test
    fun retainedViewModelLoadsAppsOnlyOnce() {
        var loadCount = 0
        val expectedApps = listOf(
            App(
                id = "test-app",
                name = "Test App",
                description = "Test description",
                category = Category.UTILITIES,
                iconUrl = "",
            ),
        )
        val store = ViewModelStore()
        val factory = viewModelFactory {
            initializer {
                AppsListViewModel {
                    loadCount++
                    expectedApps
                }
            }
        }

        val first = ViewModelProvider(store, factory)[AppsListViewModel::class.java]
        val recreated = ViewModelProvider(store, factory)[AppsListViewModel::class.java]

        assertSame(first, recreated)
        assertSame(first.uiState.value, recreated.uiState.value)
        assertEquals(expectedApps, recreated.uiState.value.apps)
        assertEquals(1, loadCount)
        store.clear()
    }

    @Test
    fun toolbarSnackbarIsNotReplayedToNewCollector() = runTest {
        val viewModel = AppsListViewModel { emptyList() }
        val events = mutableListOf<Unit>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.snackbarEvents.collect { events.add(it) }
        }

        viewModel.onToolbarClick()
        runCurrent()
        assertEquals(1, events.size)
        collector.cancelAndJoin()

        val recreatedEvents = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.snackbarEvents.collect { recreatedEvents.add(it) }
        }
        runCurrent()
        assertTrue(recreatedEvents.isEmpty())

        viewModel.onToolbarClick()
        runCurrent()
        assertEquals(1, recreatedEvents.size)
    }

    @Test
    fun toolbarSnackbarIsNotQueuedWithoutAnActiveScreen() = runTest {
        val viewModel = AppsListViewModel { emptyList() }
        viewModel.onToolbarClick()

        val events = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.snackbarEvents.collect { events.add(it) }
        }
        runCurrent()

        assertTrue(events.isEmpty())
    }
}
