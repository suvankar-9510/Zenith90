package com.zenith90.app

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenith90.app.data.local.ZenithDatabase
import com.zenith90.app.data.remote.FirebaseAuthManager
import com.zenith90.app.notifications.ZenithNotificationManager
import com.zenith90.app.ui.screens.*
import com.zenith90.app.ui.theme.PixelHaptics
import com.zenith90.app.ui.theme.ZenithColors
import com.zenith90.app.ui.theme.Zenith90Theme
import com.zenith90.app.ui.viewmodel.ZenithViewModel
import kotlinx.coroutines.launch

enum class ZenithTab(val label: String) {
    DASHBOARD("COMMAND"),
    BLUEPRINT("BLUEPRINT"),
    ANALYTICS("TRAJECTORY"),
    REWARDS("VAULT & LOGS")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Notification Channels
        ZenithNotificationManager.initChannels(applicationContext)

        val database = ZenithDatabase.getDatabase(applicationContext)
        val authManager = FirebaseAuthManager(applicationContext)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ZenithViewModel(database.zenithDao(), authManager) as T
            }
        }

        setContent {
            Zenith90Theme {
                val viewModel: ZenithViewModel = viewModel(factory = factory)
                val state by viewModel.uiState.collectAsState()
                val coroutineScope = rememberCoroutineScope()
                var currentTab by remember { mutableStateOf(ZenithTab.DASHBOARD) }
                var showBlueprintSheet by remember { mutableStateOf(false) }
                val snackbarHostState = remember { SnackbarHostState() }
                val view = LocalView.current

                // Android 13+ Notification Permission Request Launcher
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        ZenithNotificationManager.sendTestNotification(applicationContext)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("🔔 Notifications enabled! Test notification dispatched.")
                        }
                    }
                }

                // Request notification permission once at startup on Android 13+
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            applicationContext,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!hasPerm) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                // Dispatch system notification when Pomodoro session concludes!
                LaunchedEffect(state.pendingPomodoroMinutes) {
                    state.pendingPomodoroMinutes?.let { minutes ->
                        ZenithNotificationManager.sendFocusCompleteNotification(
                            context = applicationContext,
                            elapsedMinutes = minutes,
                            subject = state.userSettings.examTitle
                        )
                    }
                }

                LaunchedEffect(state.syncToastMessage) {
                    state.syncToastMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearSyncToast()
                    }
                }

                if (state.userProfile == null) {
                    AuthScreen(
                        authError = state.authError,
                        isLoading = state.isAuthLoading,
                        onSignIn = viewModel::signIn,
                        onSignUp = viewModel::signUp,
                        onSignInWithGoogle = { viewModel.signInWithGoogle(this@MainActivity) },
                        onContinueAsGuest = viewModel::signInAsGuest,
                        onContinueOffline = viewModel::continueOffline
                    )
                } else {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                ZenithTab.entries.forEach { tab ->
                                    val isSelected = currentTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            PixelHaptics.crispTick(view)
                                            currentTab = tab
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = when (tab) {
                                                    ZenithTab.DASHBOARD -> Icons.Outlined.Dashboard
                                                    ZenithTab.BLUEPRINT -> Icons.Outlined.CalendarMonth
                                                    ZenithTab.ANALYTICS -> Icons.Outlined.Analytics
                                                    ZenithTab.REWARDS -> Icons.Outlined.EmojiEvents
                                                },
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            when (currentTab) {
                                ZenithTab.DASHBOARD -> DashboardScreen(
                                    state = state,
                                    onLogWakeUpNow = viewModel::logActualWakeUpNow,
                                    onToggleFocusTimer = viewModel::toggleFocusTimer,
                                    onConfirmPomodoro = viewModel::confirmPomodoroSession,
                                    onDiscardPomodoro = viewModel::discardPomodoroSession,
                                    onQuickAddStudyMinutes = viewModel::commitStudySessionMinutes,
                                    onUpdateExactStudyMinutes = viewModel::updateLoggedStudyMinutes,
                                    onSwitchWorkoutModality = viewModel::switchWorkoutModality,
                                    onToggleWorkoutCompleted = viewModel::toggleWorkoutComplete,
                                    onAddWorkoutExercise = viewModel::addWorkoutExercise,
                                    onIncrementWorkoutSet = viewModel::incrementWorkoutSet,
                                    onDeleteWorkoutSet = viewModel::deleteWorkoutSet,
                                    onToggleBlockCompletion = viewModel::toggleScheduleBlockCompletion,
                                    onRescheduleBlockTomorrow = viewModel::rollOverTaskToTomorrow,
                                    onDeleteScheduleBlock = viewModel::deleteScheduleBlock,
                                    onSaveBlock = { id, epochDay, startMins, durMins, title, tag, cat, prio ->
                                        viewModel.saveOrUpdateScheduleBlock(
                                            id = id,
                                            targetEpochDay = epochDay,
                                            startTimeMinutes = startMins,
                                            durationMinutes = durMins,
                                            title = title,
                                            examSubjectTag = tag,
                                            category = cat,
                                            priority = prio
                                        )
                                    },
                                    onOpenNightBlueprintSheet = { showBlueprintSheet = true },
                                    onUpdateUserSettings = viewModel::updateUserSettings,
                                    onUpdateTodayTargets = viewModel::updateTodayTargets,
                                    onTriggerCloudSync = viewModel::triggerCloudSync,
                                    onSignOut = viewModel::signOut,
                                    onNavigateToBlueprint = { currentTab = ZenithTab.BLUEPRINT },
                                    onSendTestNotification = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(
                                                applicationContext,
                                                android.Manifest.permission.POST_NOTIFICATIONS
                                            ) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            val sent = ZenithNotificationManager.sendTestNotification(applicationContext)
                                            coroutineScope.launch {
                                                if (sent) {
                                                    snackbarHostState.showSnackbar("🔔 Notification sent! Check your notification shade.")
                                                } else {
                                                    snackbarHostState.showSnackbar("⚠️ Please enable notifications in Android Settings.")
                                                }
                                            }
                                        }
                                    }
                                )
                                ZenithTab.BLUEPRINT -> BlueprintScreen(
                                    state = state,
                                    onToggleBlockCompletion = viewModel::toggleScheduleBlockCompletion,
                                    onRescheduleBlockTomorrow = viewModel::rollOverTaskToTomorrow,
                                    onDeleteScheduleBlock = viewModel::deleteScheduleBlock,
                                    onSaveBlock = { id, epochDay, startMins, durMins, title, tag, cat, prio ->
                                        viewModel.saveOrUpdateScheduleBlock(
                                            id = id,
                                            targetEpochDay = epochDay,
                                            startTimeMinutes = startMins,
                                            durationMinutes = durMins,
                                            title = title,
                                            examSubjectTag = tag,
                                            category = cat,
                                            priority = prio
                                        )
                                    }
                                )
                                ZenithTab.ANALYTICS -> AnalyticsScreen(state = state)
                                ZenithTab.REWARDS -> RewardsScreen(
                                    state = state,
                                    onUpdateReflection = viewModel::updateDailyReflection,
                                    onClaimBadge = viewModel::claimBadge
                                )
                            }
                        }

                        if (showBlueprintSheet) {
                            PlannerBottomSheet(
                                onDismiss = { showBlueprintSheet = false },
                                onSaveBlock = { epochDay, startMins, durMins, title, tag, cat, prio ->
                                    viewModel.saveOrUpdateScheduleBlock(
                                        targetEpochDay = epochDay,
                                        startTimeMinutes = startMins,
                                        durationMinutes = durMins,
                                        title = title,
                                        examSubjectTag = tag,
                                        category = cat,
                                        priority = prio
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
