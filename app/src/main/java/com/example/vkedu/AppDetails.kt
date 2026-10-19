package com.example.vkedu

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun AppDetailsScreen (modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        ScreenshotBlock(screenshotList = listOf("", "", ""))
}}

@Composable
private fun ScreenshotBlock (screenshotList: List<String>, modifier: Modifier = Modifier ) {
    Column (modifier = modifier) {
        Text(text = stringResource(id = R.string.screenshot_title))
        Spacer (Modifier.height(8.dp))
        Row (horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(state = rememberScrollState())){
            screenshotList.forEach { screenshot ->
                Box(
                    Modifier.size(width = 280.dp, height = 150.dp)
                        .clip(shape= RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary)

                )

            }
        }
    }
}

@Preview
@Composable
private fun Preview () {
    ScreenshotBlock(screenshotList = listOf("","",""))
}