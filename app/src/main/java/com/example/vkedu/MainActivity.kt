package com.example.vkedu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vkedu.presentation.appslist.AppsListRoute
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

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_APPS_LIST,
                ) {
                    composable(ROUTE_APPS_LIST) {
                        AppsListRoute(
                            onAppClick = {
                                navController.navigate(ROUTE_APP_DETAILS) {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    composable(ROUTE_APP_DETAILS) {
                        AppDetailsScreen(
                            onBack = { navController.popBackStack() },
                            onShowcaseAppClick = {
                                // Открываем детали выбранного приложения поверх текущих
                                navController.navigate(ROUTE_APP_DETAILS)
                            },
                        )
                    }
                }
            }
        }
    }
}

private const val ROUTE_APPS_LIST = "apps_list"
private const val ROUTE_APP_DETAILS = "app_details"
