package com.example.vkedu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.vkedu.presentation.appslist.App
import com.example.vkedu.presentation.appslist.Category
import com.example.vkedu.ui.theme.VkeduTheme

@Composable
fun SimilarApps(
    apps: List <App>,
    modifier: Modifier = Modifier
) {
    Column (modifier) {
        Text("Вам может понравится")
        Spacer(Modifier.height (4.dp))
        LazyRow(modifier = Modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            apps.forEach { app ->
                item {
                    SimilarApp(app)
                }
            }
        }
    }
}
@Composable
private fun SimilarApp(app:App, modifier: Modifier = Modifier) {
    Column (modifier = modifier.width(132.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = app.iconUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(128.dp)
                .clip(RoundedCornerShape(16.dp)),
        )
        Text(text = app.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Button(onClick = {}, shape = RoundedCornerShape(size = 8.dp)) {
            Text("Скачать")
        }
    }
}
@Preview
@Composable
private fun Preview() {
    val apps: List<App> = buildList {
        val app = getApp ()
        repeat(times = 5) { index ->
            add(app.copy(name = "${app.name}: $index"))} }
    VkeduTheme {
        SimilarApps(apps)
    }
}

private fun getApp() = App(
    id = "1",
    name = "Название приложения",
    description = "Описание приложения",
    category = Category.UTILITIES,
    developer = "Разработчик",
    size = 100f,
    ageRating = 12,
    iconUrl = "https://example.com/icon.png",
    screenshotUrlList = emptyList(),
)