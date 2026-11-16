package com.example.vkedu

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.vkedu.domain.model.App
import com.example.vkedu.domain.model.Category
import com.example.vkedu.presentation.appslist.AppListItem
import com.example.vkedu.presentation.viewmodel.AppDetailsState
import com.example.vkedu.presentation.viewmodel.AppDetailsViewModel
import com.example.vkedu.presentation.viewmodel.InstallState
import com.example.vkedu.ui.theme.VkeduTheme

@Composable
fun AppDetailsScreen(
    modifier: Modifier = Modifier,
    viewModel: AppDetailsViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onShowcaseAppClick: (App) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppDetailsContent(
        state = state,
        onInstallClick = viewModel::onInstallClick,
        onRetry = viewModel::retry,
        onBack = onBack,
        onShowcaseAppClick = onShowcaseAppClick,
        modifier = modifier,
    )
}

@Composable
internal fun AppDetailsContent(
    state: AppDetailsState,
    onInstallClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onShowcaseAppClick: (App) -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            Row(modifier = Modifier.statusBarsPadding().padding(horizontal = 8.dp)) {
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.back))
                }
            }
        },
    ) { innerPadding ->
        when (state) {
            AppDetailsState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.app_details_loading))
                }
            }
            AppDetailsState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.app_details_load_error),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
                }
            }
            is AppDetailsState.Content -> {
                val showcaseApps = rememberShowcaseApps()
                val pagerState = rememberPagerState(pageCount = { showcaseApps.size })
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        AsyncImage(
                            model = state.app.iconUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                            error = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(22.dp)),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(state.app.name, style = MaterialTheme.typography.headlineSmall)
                            Text(
                                state.app.category.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(state.app.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(20.dp))
                    InstallButton(
                        installState = state.installState,
                        onInstallClick = onInstallClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(24.dp))
                    if (state.app.screenshotUrlList.isNotEmpty()) {
                        ScreenshotBlock(screenshotList = state.app.screenshotUrlList)
                        Spacer(Modifier.height(24.dp))
                    }
                    Text(
                        text = stringResource(R.string.showcase_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                    ) { page ->
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            showcaseApps[page].forEach { app ->
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
        }
    }
}

@Composable
internal fun InstallButton(
    installState: InstallState,
    onInstallClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onInstallClick, modifier = Modifier.weight(1f)) {
            Text(
                stringResource(
                    if (installState is InstallState.Installing) R.string.cancel_install
                    else R.string.install_app,
                ),
            )
        }
        if (installState is InstallState.Installing) {
            Text(
                stringResource(R.string.install_progress, installState.progress),
                style = MaterialTheme.typography.labelLarge,
            )
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
        id = "fa2e31b8-1234-4cf7-9914-108a170a1b01",
        name = "QuickNote",
        description = "Быстрые заметки с синхронизацией.",
        category = Category(key = "UTILITIES", title = "Утилиты"),
        iconUrl = "",
    ),
    App(
        id = "82ea6f63-2b2a-43e1-b54c-ec62e6e27229",
        name = "Calmalist",
        description = "Планировщик задач и привычек.",
        category = Category(key = "PRODUCTIVITY", title = "Производительность"),
        iconUrl = "",
    ),
    App(
        id = "c6f64035-c432-4aad-a550-a1dac2d3b047",
        name = "FitRiver",
        description = "Тренировки, шагомер и дневник.",
        category = Category(key = "HEALTH", title = "Здоровье"),
        iconUrl = "",
    ),
    App(
        id = "d22b7cda-2d36-4ebd-bfa3-be58770fa455",
        name = "TaskLoop",
        description = "Канбан-доски и таймер фокуса.",
        category = Category(key = "PRODUCTIVITY", title = "Производительность"),
        iconUrl = "",
    ),
    App(
        id = "f830e414-42a9-4fe6-af16-555a85f6f6c2",
        name = "Pixtrix",
        description = "Редактор фото с пресетами.",
        category = Category(key = "PHOTOGRAPHY", title = "Фотография"),
        iconUrl = "",
    ),
    App(
        id = "ba31ce5d-b214-46e9-bf7c-35e99e70af14",
        name = "TinyChef",
        description = "Простые рецепты и списки покупок.",
        category = Category(key = "FOOD", title = "Еда"),
        iconUrl = "",
    ),
    App(
        id = "204a939c-39fb-4c9d-bed9-daeeba8a465e",
        name = "GymMate",
        description = "Личный тренер: программы и прогресс.",
        category = Category(key = "SPORTS", title = "Спорт"),
        iconUrl = "",
    ),
    App(
        id = "1ec98719-1d86-4cdc-b99d-11c6c23d981e",
        name = "FlowCard",
        description = "Учёт бюджетов и отчёты.",
        category = Category(key = "FINANCE", title = "Финансы"),
        iconUrl = "",
    ),
    App(
        id = "d8180b87-56de-4737-b5ca-953d8c15ec80",
        name = "DreamSafe",
        description = "Дневник снов и медитации.",
        category = Category(key = "HEALTH", title = "Здоровье"),
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
            screenshotList.forEachIndexed { index, screenshot ->
                AsyncImage(
                    model = screenshot,
                    contentDescription = stringResource(R.string.app_screenshot, index + 1),
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                    error = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
                    modifier = Modifier
                        .size(width = 280.dp, height = 150.dp)
                        .clip(shape = RoundedCornerShape(8.dp)),
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    VkeduTheme {
        AppDetailsContent(
            state = AppDetailsState.Content(showcaseApps.first(), InstallState.Installing(35)),
            onInstallClick = {},
            onRetry = {},
        )
    }
}
