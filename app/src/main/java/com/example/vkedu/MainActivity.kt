package com.example.vkedu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.example.vkedu.presentation.appslist.AppsListScreen
import com.example.vkedu.ui.theme.VkeduTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VkeduTheme {
                val view = LocalView.current
                val useDarkStatusBarIcons =
                    MaterialTheme.colorScheme.primary.luminance() > 0.5f
                SideEffect {
                    WindowCompat.getInsetsController(window, view)
                        .isAppearanceLightStatusBars = useDarkStatusBarIcons
                }
                AppsListScreen()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    VkeduTheme {
        Greeting("Android")
    }
}