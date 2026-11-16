package com.example.vkedu

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.vkedu.presentation.appslist.AppsListScreen
import com.example.vkedu.presentation.viewmodel.AppDetailsViewModel
import com.example.vkedu.ui.theme.VkeduTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
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
                        AppsListScreen(
                            onAppClick = { app ->
                                navController.navigate("$ROUTE_APP_DETAILS/${Uri.encode(app.id)}") {
                                    launchSingleTop = true
                                }
                            },
                        )
                    }
                    composable(
                        route = "$ROUTE_APP_DETAILS/{appId}",
                        arguments = listOf(navArgument("appId") { type = NavType.StringType }),
                    ) { backStackEntry ->
                        AppDetailsScreen(
                            viewModel = hiltViewModel<AppDetailsViewModel>(backStackEntry),
                            onBack = { navController.popBackStack() },
                            onShowcaseAppClick = { app ->
                                navController.navigate("$ROUTE_APP_DETAILS/${Uri.encode(app.id)}")
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
