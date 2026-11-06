package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.screens.AlarmSettingsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PermissionsReliabilityScreen
import com.example.ui.screens.PomodoroSettingsScreen
import com.example.ui.screens.SleepSettingsScreen
import com.example.ui.theme.MyApplicationTheme

enum class ScreenDestination {
    HOME,
    SLEEP_SETTINGS,
    POMODORO_SETTINGS,
    ALARM_SETTINGS,
    PERMISSIONS_RELIABILITY
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(ScreenDestination.HOME) }

                // Request Notification Permission on Android 13+
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    // Notification permission handled
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState == ScreenDestination.HOME) {
                                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                            } else {
                                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                            }
                        },
                        label = "screen_navigation"
                    ) { destination ->
                        when (destination) {
                            ScreenDestination.HOME -> {
                                HomeScreen(
                                    onNavigateToSleepSettings = { currentScreen = ScreenDestination.SLEEP_SETTINGS },
                                    onNavigateToPomodoroSettings = { currentScreen = ScreenDestination.POMODORO_SETTINGS },
                                    onNavigateToAlarmSettings = { currentScreen = ScreenDestination.ALARM_SETTINGS },
                                    onNavigateToPermissions = { currentScreen = ScreenDestination.PERMISSIONS_RELIABILITY }
                                )
                            }
                            ScreenDestination.SLEEP_SETTINGS -> {
                                BackHandler { currentScreen = ScreenDestination.HOME }
                                SleepSettingsScreen(
                                    onNavigateBack = { currentScreen = ScreenDestination.HOME },
                                    onNavigateToAlarmSettings = { currentScreen = ScreenDestination.ALARM_SETTINGS }
                                )
                            }
                            ScreenDestination.POMODORO_SETTINGS -> {
                                BackHandler { currentScreen = ScreenDestination.HOME }
                                PomodoroSettingsScreen(
                                    onNavigateBack = { currentScreen = ScreenDestination.HOME }
                                )
                            }
                            ScreenDestination.ALARM_SETTINGS -> {
                                BackHandler { currentScreen = ScreenDestination.HOME }
                                AlarmSettingsScreen(
                                    onNavigateBack = { currentScreen = ScreenDestination.HOME }
                                )
                            }
                            ScreenDestination.PERMISSIONS_RELIABILITY -> {
                                BackHandler { currentScreen = ScreenDestination.HOME }
                                PermissionsReliabilityScreen(
                                    onNavigateBack = { currentScreen = ScreenDestination.HOME }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
