package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AyhaViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var viewModel: AyhaViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize databases and ViewModel
        database = AppDatabase.getInstance(applicationContext)
        val factory = AyhaViewModel.Factory(applicationContext, database)
        viewModel = ViewModelProvider(this, factory)[AyhaViewModel::class.java]

        setContent {
            MyApplicationTheme {
                AyhaCompanionApp(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up text-to-speech engine to prevent memory leaks
        viewModel.cleanup()
    }
}

@Composable
fun AyhaCompanionApp(viewModel: AyhaViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Determine current active route for the sidebar highlights
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route ?: "tap_to_wake"

    // Only allow swipe/interaction of the drawer on major application screens (home, chat, etc.)
    val isDrawerGestureEnabled = when (currentRoute) {
        "home", "chat", "voice", "image_gen", "translator", "notes", "reminders", "history", "settings", "about" -> true
        else -> false
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isDrawerGestureEnabled,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    activeRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCloseDrawer = {
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = "tap_to_wake",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("tap_to_wake") {
                TapToWakeScreen(onWake = { navController.navigate("splash") })
            }
            composable("splash") {
                SplashScreen(onNavigateNext = { navController.navigate("welcome") })
            }
            composable("welcome") {
                WelcomeScreen(onNavigateNext = { navController.navigate("permission") })
            }
            composable("permission") {
                PermissionScreen(onNavigateNext = {
                    navController.navigate("home") {
                        popUpTo("welcome") { inclusive = true }
                    }
                })
            }
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigateToChat = { navController.navigate("chat") },
                    onNavigateToVoice = { navController.navigate("voice") },
                    onNavigateToDashboard = { navController.navigate("dashboard") },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable("dashboard") {
                DashboardScreen(
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable("chat") {
                ChatScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable("voice") {
                VoiceAssistantScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable("image_gen") {
                ImageGeneratorScreen(onBack = { navController.popBackStack() })
            }
            composable("translator") {
                TranslatorScreen(onBack = { navController.popBackStack() })
            }
            composable("notes") {
                NotesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable("reminders") {
                RemindersScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable("history") {
                HistoryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable("settings") {
                SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable("about") {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
