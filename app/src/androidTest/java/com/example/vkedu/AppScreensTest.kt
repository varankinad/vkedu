package com.example.vkedu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category
import com.example.vkedu.domain.repository.AppRepository
import com.example.vkedu.presentation.appslist.AppListItem
import com.example.vkedu.presentation.appslist.AppsListContent
import com.example.vkedu.presentation.appslist.FeaturedAppCard
import com.example.vkedu.presentation.viewmodel.AppDetailsState
import com.example.vkedu.presentation.viewmodel.AppDetailsViewModel
import com.example.vkedu.presentation.viewmodel.InstallState
import com.example.vkedu.presentation.viewmodel.RatingsLoadingMode
import com.example.vkedu.presentation.viewmodel.RatingsState
import com.example.vkedu.ui.theme.VkeduTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val app = App(
        id = "chosen-app",
        name = "Chosen app",
        description = "Selected app description",
        category = Category("education", "Education"),
        iconUrl = "",
        ageRating = 12,
        screenshotUrlList = listOf("", ""),
    )

    @Test
    fun installButtonCancelsAndRestartsActualViewModel() {
        val store = ViewModelStore()
        lateinit var viewModel: AppDetailsViewModel
        compose.runOnUiThread {
            viewModel = AppDetailsViewModel(DetailsRepository(app), SavedStateHandle(mapOf("appId" to app.id)))
            store.put("details", viewModel)
        }
        try {
            compose.setContent { VkeduTheme { AppDetailsScreen(viewModel = viewModel) } }

            compose.onNodeWithText(text(R.string.install_app)).assertIsEnabled().performClick()
            compose.onNodeWithText(text(R.string.cancel_install)).assertIsEnabled().performClick()
            compose.runOnIdle {
                assertEquals(InstallState.NotInstalled, (viewModel.state.value as AppDetailsState.Content).installState)
            }

            compose.onNodeWithText(text(R.string.install_app)).performClick()
            compose.waitUntil(timeoutMillis = 10_000) {
                (viewModel.state.value as? AppDetailsState.Content)?.installState == InstallState.Installed
            }
            compose.onNodeWithText(text(R.string.install_app)).assertIsEnabled().performClick()
            compose.onNodeWithText(text(R.string.cancel_install)).assertIsEnabled().performClick()
            compose.runOnIdle {
                assertEquals(InstallState.NotInstalled, (viewModel.state.value as AppDetailsState.Content).installState)
            }
        } finally {
            compose.runOnUiThread { store.clear() }
        }
    }

    @Test
    fun detailsShowSelectedAppScreenshotsProgressAndBackAction() {
        var backClicks = 0
        var showcaseSelection: App? = null
        compose.setContent {
            VkeduTheme {
                AppDetailsContent(
                    state = AppDetailsState.Content(app, InstallState.Installing(35)),
                    onInstallClick = {},
                    onRetry = {},
                    onBack = { backClicks++ },
                    onShowcaseAppClick = { showcaseSelection = it },
                )
            }
        }

        compose.onNodeWithText(app.name).assertIsDisplayed()
        compose.onNodeWithText(app.description).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.install_progress, 35)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.cancel_install)).assertIsEnabled()
        compose.onNodeWithContentDescription(text(R.string.app_screenshot, 1)).assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.app_screenshot, 2)).assertExists()
        compose.onNodeWithText(text(R.string.back)).performClick()
        compose.runOnIdle { assertEquals(1, backClicks) }
        compose.onNode(hasText("Calmalist") and hasClickAction()).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("82ea6f63-2b2a-43e1-b54c-ec62e6e27229", showcaseSelection?.id)
        }
    }

    @Test
    fun loadingAndErrorHideInstallAndRetryRestoresContent() {
        var state by mutableStateOf<AppDetailsState>(AppDetailsState.Loading)
        var retryClicks = 0
        var backClicks = 0
        compose.setContent {
            VkeduTheme {
                AppDetailsContent(
                    state = state,
                    onInstallClick = {},
                    onRetry = {
                        retryClicks++
                        state = AppDetailsState.Content(app)
                    },
                    onBack = { backClicks++ },
                )
            }
        }

        compose.onNodeWithText(text(R.string.app_details_loading)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.install_app)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.cancel_install)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.back)).performClick()
        compose.runOnIdle { state = AppDetailsState.Error }
        compose.onNodeWithText(text(R.string.app_details_load_error)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.install_app)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.cancel_install)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.retry)).performClick()
        compose.onNodeWithText(text(R.string.install_app)).assertIsEnabled()
        compose.runOnIdle {
            assertEquals(1, retryClicks)
            assertEquals(1, backClicks)
        }
    }

    @Test
    fun normalAndFeaturedCardsOnlyShowReturnedLocalizedRatings() {
        var rating by mutableStateOf<Float?>(null)
        compose.setContent {
            VkeduTheme {
                Column {
                    AppListItem(app = app, rating = rating)
                    FeaturedAppCard(app = app, rating = rating)
                }
            }
        }

        compose.onNodeWithText(text(R.string.app_user_rating, 0f)).assertDoesNotExist()
        compose.runOnIdle { rating = 4.76f }
        compose.onAllNodesWithText(text(R.string.app_user_rating, 4.76f)).assertCountEquals(2)
        compose.onNodeWithText(text(R.string.app_user_rating, 0f)).assertDoesNotExist()
        compose.runOnIdle { rating = null }
        compose.onAllNodesWithText(text(R.string.app_user_rating, 4.76f)).assertCountEquals(0)
    }

    @Test
    fun catalogRemainsNavigableDuringBothPhasesAndShowsBothTimes() {
        val apps = catalogApps()
        var state by mutableStateOf(RatingsState(loadingMode = RatingsLoadingMode.Sequential, idCount = apps.size))
        var clickedApp: App? = null
        var compareClicks = 0
        compose.setContent {
            VkeduTheme {
                AppsListContent(
                    appList = apps,
                    isLoading = false,
                    hasError = false,
                    onRetry = {},
                    onAppClick = { clickedApp = it },
                    ratingsState = state,
                    onCompareRatings = { compareClicks++ },
                )
            }
        }

        for (mode in RatingsLoadingMode.entries) {
            compose.runOnIdle {
                state = state.copy(
                    loadingMode = mode,
                    sequentialTimeMillis = if (mode == RatingsLoadingMode.Parallel) 7_500 else null,
                )
            }
            compose.onNodeWithText(text(R.string.compare_ratings)).assertIsNotEnabled()
            compose.onNodeWithText(text(R.string.ratings_id_count, apps.size)).assertIsDisplayed()
            compose.onNodeWithText(text(
                if (mode == RatingsLoadingMode.Sequential) R.string.ratings_loading_sequential
                else R.string.ratings_loading_parallel,
            )).assertIsDisplayed()
            compose.onAllNodes(hasScrollToIndexAction()).onFirst()
                .performScrollToNode(hasText(apps.last().name) and hasClickAction())
            compose.onNodeWithText(apps.last().name).performClick()
            compose.runOnIdle { assertEquals(apps.last(), clickedApp) }
            compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToIndex(0)
        }

        compose.runOnIdle { state = state.copy(loadingMode = null, parallelTimeMillis = 1_500) }
        compose.onNodeWithText(text(R.string.ratings_sequential_time, 7_500L)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.ratings_parallel_time, 1_500L)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.compare_ratings)).assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, compareClicks) }
    }

    @Test
    fun ratingsErrorKeepsCatalogAndFirstResultAndAllowsRetry() {
        val apps = catalogApps()
        var retries = 0
        var clickedApp: App? = null
        compose.setContent {
            VkeduTheme {
                AppsListContent(
                    appList = apps,
                    isLoading = false,
                    hasError = false,
                    onRetry = {},
                    onAppClick = { clickedApp = it },
                    ratingsState = RatingsState(
                        ratings = mapOf(apps.last().id to 4.1f),
                        idCount = apps.size,
                        sequentialTimeMillis = 7_500,
                        hasError = true,
                    ),
                    onCompareRatings = { retries++ },
                )
            }
        }

        compose.onNodeWithText(text(R.string.ratings_load_error)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.ratings_sequential_time, 7_500L)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.ratings_parallel_pending)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.compare_ratings)).assertIsEnabled().performClick()
        compose.onAllNodes(hasScrollToIndexAction()).onFirst()
            .performScrollToNode(hasText(apps.last().name) and hasClickAction())
        compose.onNodeWithText(text(R.string.app_user_rating, 4.1f)).assertIsDisplayed()
        compose.onNodeWithText(apps.last().name).performClick()
        compose.runOnIdle {
            assertEquals(1, retries)
            assertEquals(apps.last(), clickedApp)
        }
    }

    @Test
    fun comparisonIsDisabledForEmptyOrReloadingCatalog() {
        var apps by mutableStateOf(emptyList<App>())
        var loading by mutableStateOf(false)
        compose.setContent {
            VkeduTheme {
                AppsListContent(
                    appList = apps,
                    isLoading = loading,
                    hasError = false,
                    onRetry = {},
                    onAppClick = {},
                )
            }
        }

        compose.onNodeWithText(text(R.string.compare_ratings)).assertIsNotEnabled()
        compose.runOnIdle {
            apps = catalogApps()
            loading = true
        }
        compose.onNodeWithText(text(R.string.compare_ratings)).assertIsNotEnabled()
        compose.runOnIdle { loading = false }
        compose.onNodeWithText(text(R.string.compare_ratings)).assertIsEnabled()
    }

    @Test
    fun comparisonControlsWrapAndRemainScrollableInNarrowLandscapeViewport() {
        val labels = listOf(
            text(R.string.compare_ratings),
            text(R.string.ratings_loading_parallel),
            text(R.string.ratings_sequential_time, 21_000L),
            text(R.string.ratings_parallel_pending),
        )
        compose.setContent {
            VkeduTheme {
                Box(Modifier.width(280.dp).height(300.dp)) {
                    AppsListContent(
                        appList = catalogApps(),
                        isLoading = false,
                        hasError = false,
                        onRetry = {},
                        onAppClick = {},
                        ratingsState = RatingsState(
                            loadingMode = RatingsLoadingMode.Parallel,
                            idCount = 5,
                            sequentialTimeMillis = 21_000,
                        ),
                    )
                }
            }
        }

        for (label in labels) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(label, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                    assertTrue(action(layouts))
                }
            val layout = layouts.single()
            assertFalse(
                "$label overflows: lines=${layout.lineCount}, size=${layout.size}, " +
                    "width=${layout.didOverflowWidth}, height=${layout.didOverflowHeight}",
                layout.hasVisualOverflow,
            )
        }
    }

    private fun catalogApps(): List<App> = List(5) { index ->
        app.copy(id = "catalog-$index", name = "Catalog app $index", screenshotUrlList = emptyList())
    }

    private fun text(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private class DetailsRepository(private val app: App) : AppRepository {
        override suspend fun getAppById(id: String): Result<App> = Result.success(app)
        override suspend fun getApps(): Result<List<App>> = error("Details must not load the catalog")
        override suspend fun getCategories(): Result<List<Category>> = error("Details must not load categories")
        override suspend fun loadRatingsSequentially(ids: List<String>): Map<String, Float> =
            error("Details must not load ratings")
        override suspend fun loadRatingsInParallel(ids: List<String>): Map<String, Float> =
            error("Details must not load ratings")
    }
}
