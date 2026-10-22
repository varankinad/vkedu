package com.example.vkedu.presentation.appslist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vkedu.R
import com.example.vkedu.ui.theme.VkeduTheme
import kotlinx.coroutines.launch

@Composable
fun AppsListScreen(
    modifier: Modifier = Modifier,
    appList: List<App> = apps,
    onAppClick: (App) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val featuredApps = appList.take(4)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = { AppsToolbar(count = appList.size) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            item(key = "featured_row") {
                FeaturedAppsRow(
                    apps = featuredApps,
                )
            }
            item(key = "all_header") {
                SectionHeader(
                    title = stringResource(R.string.apps_list_all),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
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
        AppsListScreen()
    }
}
