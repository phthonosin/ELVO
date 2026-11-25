package com.elvo.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.elvo.app.data.prefs.PreferencesRepository
import com.elvo.app.domain.model.BlockMode
import com.elvo.app.lock.CodeGenerator
import com.elvo.app.ui.main.AnalyticsScreen
import com.elvo.app.ui.main.ElvoBottomNavigation
import com.elvo.app.ui.main.HomeScreen
import com.elvo.app.ui.main.SettingsActivePromptScreen
import com.elvo.app.ui.main.SettingsBlockingBehaviorScreen
import com.elvo.app.ui.main.SettingsScreen
import com.elvo.app.ui.main.SettingsTermsScreen
import com.elvo.app.ui.setup.AccessibilityPermissionScreen
import com.elvo.app.ui.setup.BatteryOptimizationScreen
import com.elvo.app.ui.setup.BlockPromptScreen
import com.elvo.app.ui.setup.ConfirmTaskScreen
import com.elvo.app.ui.setup.DirectiveIntroScreen
import com.elvo.app.ui.setup.DisplayOverAppsScreen
import com.elvo.app.ui.setup.HowToBlockScreen
import com.elvo.app.ui.setup.WarningScreen
import com.elvo.app.ui.setup.WelcomeScreen
import com.elvo.app.ui.setup.YourCodeScreen
import com.elvo.app.ui.todo.ToDoAskScreen
import com.elvo.app.ui.todo.ToDoEditScreen
import com.elvo.app.scheduler.ReminderScheduler
import com.elvo.app.ui.theme.ElvoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private lateinit var prefsRepo: PreferencesRepository
    private var openTodoFromNotification by mutableStateOf(false)

    private companion object {
        /** Starter prompts written by earlier builds. Cleared so the field starts empty. */
        val LEGACY_STARTER_PROMPTS = setOf(
            "Block adult content in browsers and limit Instagram to 30 minutes daily",
            "Keep my eyes off social media after 10 PM",
            "Clean my eyes and brain — keep impurity away from my screens"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsRepo = PreferencesRepository(this)
        openTodoFromNotification = intent.getBooleanExtra(ReminderScheduler.EXTRA_OPEN_TODO, false)

        val isSetupDoneInitial = runBlocking { prefsRepo.setupCompleted.first() }
        val storedWeekPlan = runBlocking { prefsRepo.weekPlan.first() }
        val storedPrompt = runBlocking { prefsRepo.activePromptText.first() }
        if (storedPrompt in LEGACY_STARTER_PROMPTS) {
            runBlocking { prefsRepo.setActivePromptText("") }
        }
        val startDestination = if (isSetupDoneInitial) "home" else "welcome"
        // Keeps both the daily nudges and the Saturday 6 PM planning prompt alive.
        ReminderScheduler.reschedule(this, emptyList(), storedWeekPlan)

        setContent {
            ElvoTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()

                val setupCompletedStored by prefsRepo.setupCompleted.collectAsState(initial = isSetupDoneInitial)
                val activePromptStored by prefsRepo.activePromptText.collectAsState(initial = "")
                val masterCodeStored by prefsRepo.masterCode.collectAsState(initial = "k7#Qm2\$xP9!w")
                val blockUninstallStored by prefsRepo.blockUninstall.collectAsState(initial = true)
                val weekPlanStored by prefsRepo.weekPlan.collectAsState(initial = storedWeekPlan)
                val weekDescriptionStored by prefsRepo.weekDescription.collectAsState(initial = "")

                var activePrompt by remember(activePromptStored) { mutableStateOf(activePromptStored) }
                var masterCode by remember(masterCodeStored) { mutableStateOf(masterCodeStored) }
                var blockUninstall by remember(blockUninstallStored) { mutableStateOf(blockUninstallStored) }
                var selectedBlockMode by remember { mutableStateOf(BlockMode.VIDEO) }

                // The Saturday planning notification opens the editor without going full screen.
                LaunchedEffect(openTodoFromNotification, setupCompletedStored) {
                    if (openTodoFromNotification && setupCompletedStored) {
                        navController.navigate("todo_edit") { launchSingleTop = true }
                        openTodoFromNotification = false
                    }
                }

                fun handlePopOrHome() {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate("home")
                    }
                }

                fun toggleTaskDone(day: Int, index: Int) {
                    val currentlyDone = weekPlanStored
                        .firstOrNull { it.dayOfWeek == day }
                        ?.tasks
                        ?.getOrNull(index)
                        ?.done ?: false
                    scope.launch { prefsRepo.setWeekTaskDone(day, index, !currentlyDone) }
                }

                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route
                val mainRoutes = setOf(
                    "home",
                    "analytics",
                    "settings",
                    "settings_blocking_behavior",
                    "settings_active_prompt",
                    "settings_todo",
                    "settings_terms"
                )
                val showBottomNav = currentRoute in mainRoutes
                val selectedTab = when (currentRoute) {
                    "analytics" -> 1
                    "settings",
                    "settings_blocking_behavior",
                    "settings_active_prompt",
                    "settings_todo",
                    "settings_terms" -> 2
                    else -> 0
                }

                Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    enterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            initialOffsetX = { it / 5 }
                        ) + fadeIn(tween(220))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            targetOffsetX = { -it / 8 }
                        ) + fadeOut(tween(180))
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            initialOffsetX = { -it / 5 }
                        ) + fadeIn(tween(220))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            targetOffsetX = { it / 8 }
                        ) + fadeOut(tween(180))
                    }
                ) {
                    // 1. Welcome Screen, with a one-time directive beat after Get started.
                    composable(
                        route = "welcome",
                        exitTransition = { fadeOut(tween(400)) }
                    ) {
                        var showDirective by remember { mutableStateOf(false) }

                        BackHandler(enabled = showDirective) {
                            showDirective = false
                        }

                        Crossfade(
                            targetState = showDirective,
                            animationSpec = tween(500),
                            label = "welcomeToDirective"
                        ) { directiveVisible ->
                            if (directiveVisible) {
                                DirectiveIntroScreen(
                                    onFinished = { navController.navigate("warning") }
                                )
                            } else {
                                WelcomeScreen(
                                    onGetStarted = { showDirective = true }
                                )
                            }
                        }
                    }

                    // 2. Warning Screen
                    composable(
                        route = "warning",
                        enterTransition = { fadeIn(tween(650)) }
                    ) {
                        WarningScreen(
                            onContinue = { navController.navigate("block_prompt") },
                            onNotForMe = { finish() }
                        )
                    }

                    // 3. Blocking prompt screen
                    composable("block_prompt") {
                        BlockPromptScreen(
                            initialPrompt = activePrompt,
                            onBack = { handlePopOrHome() },
                            onContinue = { prompt ->
                                activePrompt = prompt
                                scope.launch { prefsRepo.setActivePromptText(prompt) }
                                if (setupCompletedStored) {
                                    handlePopOrHome()
                                } else {
                                    navController.navigate("accessibility_perm")
                                }
                            }
                        )
                    }

                    // 7. Accessibility Permission Screen
                    composable("accessibility_perm") {
                        AccessibilityPermissionScreen(
                            onBack = { handlePopOrHome() },
                            onContinue = { navController.navigate("display_over_apps") }
                        )
                    }

                    // 8. Display Over Other Apps Screen
                    composable("display_over_apps") {
                        DisplayOverAppsScreen(
                            onBack = { handlePopOrHome() },
                            onContinue = { navController.navigate("battery_opt") }
                        )
                    }

                    // 9. Battery Optimization Screen
                    composable("battery_opt") {
                        BatteryOptimizationScreen(
                            onBack = { handlePopOrHome() },
                            onContinue = { navController.navigate("confirm_task") }
                        )
                    }

                    // 10. Confirm Task Screen
                    composable("confirm_task") {
                        ConfirmTaskScreen(
                            activePrompt = activePrompt,
                            onBack = { handlePopOrHome() },
                            onConfirm = { navController.navigate("how_to_block") },
                            onEditPrompt = { navController.navigate("block_prompt") }
                        )
                    }

                    // 11. How To Block Screen
                    composable("how_to_block") {
                        HowToBlockScreen(
                            initialMode = selectedBlockMode,
                            onBack = { handlePopOrHome() },
                            onContinue = { mode ->
                                selectedBlockMode = mode
                                val newCode = CodeGenerator.generate(12)
                                masterCode = newCode
                                scope.launch {
                                    prefsRepo.setSelectedBlockMode(mode.name)
                                    prefsRepo.setMasterCode(newCode)
                                }
                                if (setupCompletedStored) {
                                    handlePopOrHome()
                                } else {
                                    navController.navigate("your_code")
                                }
                            }
                        )
                    }

                    // 12. Your Code Screen
                    composable("your_code") {
                        YourCodeScreen(
                            generatedCode = masterCode,
                            onBack = { handlePopOrHome() },
                            onSaved = { isUninstallBlocked ->
                                scope.launch {
                                    prefsRepo.setBlockUninstall(isUninstallBlocked)
                                    prefsRepo.setSetupCompleted(true)
                                }
                                navController.navigate("todo_ask") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    // Optional step after the hardcore-mode farewell.
                    composable("todo_ask") {
                        ToDoAskScreen(
                            onBack = {
                                navController.navigate("home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onYes = { navController.navigate("todo_edit") },
                            onNo = {
                                navController.navigate("home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("todo_edit") {
                        ToDoEditScreen(
                            initialDescription = weekDescriptionStored,
                            initialPlan = weekPlanStored,
                            onBack = { handlePopOrHome() },
                            onSave = { description, plan ->
                                scope.launch {
                                    prefsRepo.saveWeekPlan(description, plan)
                                    ReminderScheduler.reschedule(this@MainActivity, weekPlanStored, plan)
                                }
                            },
                            onFinished = {
                                navController.navigate("home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onToggleTask = { day, index -> toggleTaskDone(day, index) }
                        )
                    }

                    // Main Tab 1: Home
                    composable("home") {
                        HomeScreen(
                            activePrompt = activePrompt,
                            masterCode = masterCode,
                            blockUninstall = blockUninstall,
                            weekPlan = weekPlanStored,
                            onEditPromptClicked = { navController.navigate("settings_active_prompt") },
                            onOpenToDoList = { navController.navigate("settings_todo") },
                            onToggleTask = { day, index -> toggleTaskDone(day, index) }
                        )
                    }

                    // Main Tab 2: Analytics
                    composable("analytics") {
                        AnalyticsScreen()
                    }

                    // Main Tab 3: Settings
                    composable("settings") {
                        SettingsScreen(
                            blockUninstall = blockUninstall,
                            masterCode = masterCode,
                            onToggleBlockUninstall = { enabled ->
                                blockUninstall = enabled
                                scope.launch { prefsRepo.setBlockUninstall(enabled) }
                            },
                            onNavigateToDoList = { navController.navigate("settings_todo") },
                            onNavigateHowToBlock = { navController.navigate("settings_blocking_behavior") },
                            onNavigateEditPrompt = { navController.navigate("settings_active_prompt") },
                            onOpenAccessibilitySettings = {
                                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            onOpenOverlaySettings = {
                                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                            },
                            onOpenBatterySettings = {
                                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
                            },
                            onNavigateTerms = { navController.navigate("settings_terms") }
                        )
                    }

                    // To-do list, reopened from Settings.
                    composable("settings_todo") {
                        ToDoEditScreen(
                            initialDescription = weekDescriptionStored,
                            initialPlan = weekPlanStored,
                            onBack = { navController.popBackStack() },
                            onSave = { description, plan ->
                                scope.launch {
                                    prefsRepo.saveWeekPlan(description, plan)
                                    ReminderScheduler.reschedule(this@MainActivity, weekPlanStored, plan)
                                }
                            },
                            onFinished = { navController.popBackStack() },
                            onToggleTask = { day, index -> toggleTaskDone(day, index) }
                        )
                    }

                    // Dedicated Settings Tab 2: Blocking Behavior / Alert Strategy
                    composable("settings_blocking_behavior") {
                        SettingsBlockingBehaviorScreen(
                            initialMode = selectedBlockMode,
                            onBack = { navController.popBackStack() },
                            onModeSaved = { mode ->
                                selectedBlockMode = mode
                                scope.launch { prefsRepo.setSelectedBlockMode(mode.name) }
                                navController.popBackStack()
                            }
                        )
                    }

                    // Dedicated Settings Tab 3: Active Directive & Rules
                    composable("settings_active_prompt") {
                        SettingsActivePromptScreen(
                            initialPrompt = activePrompt,
                            onBack = { navController.popBackStack() },
                            onPromptSaved = { prompt ->
                                activePrompt = prompt
                                scope.launch { prefsRepo.setActivePromptText(prompt) }
                                navController.popBackStack()
                            }
                        )
                    }

                    // Dedicated Settings Tab 4: Terms & Governance
                    composable("settings_terms") {
                        SettingsTermsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                if (showBottomNav) {
                    ElvoBottomNavigation(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            val route = when (tab) {
                                0 -> "home"
                                1 -> "analytics"
                                else -> "settings"
                            }
                            navController.navigate(route) {
                                popUpTo("home") {
                                    inclusive = false
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(ReminderScheduler.EXTRA_OPEN_TODO, false)) {
            openTodoFromNotification = true
        }
    }
}
