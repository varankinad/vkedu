package com.example.vkedu

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.vkedu.presentation.appslist.App
import com.example.vkedu.presentation.appslist.AppListItem
import com.example.vkedu.ui.theme.VkeduTheme

@Composable
fun AppDetailsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onShowcaseAppClick: (App) -> Unit = {},
) {
    val showcaseApps = rememberShowcaseApps()
    val pagerState = rememberPagerState(pageCount = { showcaseApps.size })

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ScreenshotBlock(screenshotList = listOf("", "", ""))
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(id = R.string.showcase_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(12.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val pageApps = showcaseApps[page]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pageApps.forEach { app ->
                    AppListItem(
                        app = app,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onShowcaseAppClick(app) },
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberShowcaseApps(): List<List<App>> {
    return remember {
        showcaseApps.chunked(3)
    }
}

private val showcaseApps = listOf(
    App(
        id = "showcase-1",
        name = "QuickNote",
        description = "Быстрые заметки с синхронизацией.",
        category = com.example.vkedu.presentation.appslist.Category.UTILITIES,
        iconUrl = "",
    ),
    App(
        id = "showcase-2",
        name = "Calmalist",
        description = "Планировщик задач и привычек.",
        category = com.example.vkedu.presentation.appslist.Category.PRODUCTIVITY,
        iconUrl = "",
    ),
    App(
        id = "showcase-3",
        name = "FitRiver",
        description = "Тренировки, шагомер и дневник.",
        category = com.example.vkedu.presentation.appslist.Category.HEALTH,
        iconUrl = "",
    ),
    App(
        id = "showcase-4",
        name = "TaskLoop",
        description = "Канбан-доски и таймер фокуса.",
        category = com.example.vkedu.presentation.appslist.Category.PRODUCTIVITY,
        iconUrl = "",
    ),
    App(
        id = "showcase-5",
        name = "Pixtrix",
        description = "Редактор фото с пресетами.",
        category = com.example.vkedu.presentation.appslist.Category.PHOTOGRAPHY,
        iconUrl = "",
    ),
    App(
        id = "showcase-6",
        name = "TinyChef",
        description = "Простые рецепты и списки покупок.",
        category = com.example.vkedu.presentation.appslist.Category.FOOD,
        iconUrl = "",
    ),
    App(
        id = "showcase-7",
        name = "GymMate",
        description = "Личный тренер: программы и прогресс.",
        category = com.example.vkedu.presentation.appslist.Category.SPORTS,
        iconUrl = "",
    ),
    App(
        id = "showcase-8",
        name = "FlowCard",
        description = "Учёт бюджетов и отчёты.",
        category = com.example.vkedu.presentation.appslist.Category.FINANCE,
        iconUrl = "",
    ),
    App(
        id = "showcase-9",
        name = "DreamSafe",
        description = "Дневник снов и медитации.",
        category = com.example.vkedu.presentation.appslist.Category.HEALTH,
        iconUrl = "",
    ),
)

@Composable
private fun ScreenshotBlock(
    screenshotList: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = stringResource(id = R.string.screenshot_title))
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(state = rememberScrollState()),
        ) {
            screenshotList.forEach { screenshot ->
                Box(
                    Modifier
                        .size(width = 280.dp, height = 150.dp)
                        .clip(shape = RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    VkeduTheme {
        AppDetailsScreen()
    }
}