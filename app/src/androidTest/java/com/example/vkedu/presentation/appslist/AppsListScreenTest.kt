package com.example.vkedu.presentation.appslist

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.vkedu.MainActivity
import com.example.vkedu.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppsListScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun contentAndScrollPositionSurviveRotation() {
        composeRule.onNode(
            hasScrollToNodeAction() and
                SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange),
        ).performScrollToIndex(7)
        composeRule.onNodeWithText("TinyChef").assertIsDisplayed()

        val portraitActivity = composeRule.activity
        composeRule.runOnUiThread {
            portraitActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.activity !== portraitActivity &&
                composeRule.activity.resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("TinyChef").assertIsDisplayed()
    }

    @Test
    fun toolbarSnackbarDoesNotRepeatAfterActivityRecreation() {
        val toolbarTitle = composeRule.activity.getString(R.string.apps_list_title)
        val snackbarMessage = composeRule.activity.getString(R.string.apps_list_toolbar_snackbar)
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText(toolbarTitle).performClick()
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithText(snackbarMessage).assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithText(snackbarMessage).assertDoesNotExist()

        composeRule.onNodeWithText(toolbarTitle).performClick()
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithText(snackbarMessage).assertIsDisplayed()
    }
}
