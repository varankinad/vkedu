package com.example.vkedu.presentation.appslist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.vkedu.R
import com.example.vkedu.domain.model.App
import com.example.vkedu.presentation.viewmodel.AppsViewModel
import com.example.vkedu.ui.theme.VkeduTheme

@Composable
fun AppsListScreen(
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = hiltViewModel(),
    onAppClick: (App) -> Unit = {},
) {
    val appsResult by viewModel.apps.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    AppsListContent(
        appList = appsResult.getOrNull().orEmpty(),
        isLoading = isLoading,
        hasError = appsResult.isFailure,
        onRetry = viewModel::loadData,
        onAppClick = onAppClick,
        modifier = modifier,
    )
}

@Composable
private fun AppsListContent(
    appList: List<App>,
    isLoading: Boolean,
    hasError: Boolean,
    onRetry: () -> Unit,
    onAppClick: (App) -> Unit,
    modifier: Modifier = Modifier,
) {
    val featuredApps = appList.take(4)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = { AppsToolbar(count = appList.size) },
    ) { innerPadding ->
        when {
            isLoading && appList.isEmpty() -> {
                LoadingIndicator(modifier = Modifier.padding(innerPadding))
            }
            hasError && appList.isEmpty() -> {
                LoadError(
                    onRetry = onRetry,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 28.dp),
                ) {
                    item(key = "featured_row") {
                        FeaturedAppsRow(apps = featuredApps)
                    }
                    item(key = "all_header") {
                        SectionHeader(
                            title = stringResource(R.string.apps_list_all),
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = 28.dp,
                                bottom = 12.dp,
                            ),
                        )
                    }
                    items(appList, key = { app -> app.id }) { app ->
                        AppListItem(
                            app = app,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                .animateItem(),
                            onClick = { onAppClick(app) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp,
        )
    }
}

@Composable
private fun LoadError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.apps_load_error),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = stringResource(R.string.retry))
        }
    }
}

@Composable
private fun FeaturedAppsRow(
    apps: List<App>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = stringResource(R.string.apps_list_featured),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(apps, key = { app -> app.id }) { app ->
                FeaturedAppCard(app = app)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun AppsListScreenPreview() {
    VkeduTheme {
        AppsListContent(
            appList = emptyList(),
            isLoading = false,
            hasError = false,
            onRetry = {},
            onAppClick = {},
        )
    }
}
