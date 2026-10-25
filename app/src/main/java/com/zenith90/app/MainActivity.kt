package com.zenith90.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenith90.app.data.local.ZenithDatabase
import com.zenith90.app.data.remote.FirebaseAuthManager
import com.zenith90.app.ui.screens.*
import com.zenith90.app.ui.theme.PixelHaptics
import com.zenith90.app.ui.theme.Zenith90Theme
import com.zenith90.app.ui.viewmodel.ZenithViewModel

enum class ZenithTab(val label: String) {
    DASHBOARD("COMMAND"),
    ANALYTICS("TRAJECTORY"),
    REWARDS("VAULT & LOGS")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                var currentTab by remember { mutableStateOf(ZenithTab.DASHBOARD) }
                var showBlueprintSheet by remember { mutableStateOf(false) }
                val snackbarHostState = remember { SnackbarHostState() }
                val view = LocalView.current

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
                            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                                ZenithTab.entries.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentTab == tab,
                                        onClick = {
                                            PixelHaptics.crispTick(view)
                                            currentTab = tab
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = when (tab) {
                                                    ZenithTab.DASHBOARD -> Icons.Outlined.Dashboard
                                                    ZenithTab.ANALYTICS -> Icons.Outlined.Analytics
                                                    ZenithTab.REWARDS -> Icons.Outlined.EmojiEvents
                                                },
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = {
                                            Text(tab.label, style = MaterialTheme.typography.labelSmall)
                                        }
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
                                    onQuickAddStudyMinutes = viewModel::commitStudySessionMinutes,
                                    onSwitchWorkoutModality = viewModel::switchWorkoutModality,
                                    onToggleWorkoutCompleted = viewModel::toggleWorkoutComplete,
                                    onAddWorkoutSet = viewModel::addWorkoutSet,
                                    onDeleteWorkoutSet = viewModel::deleteWorkoutSet,
                                    onToggleBlockCompletion = viewModel::toggleScheduleBlockCompletion,
                                    onRescheduleBlockTomorrow = viewModel::rollOverTaskToTomorrow,
                                    onOpenNightBlueprintSheet = { showBlueprintSheet = true },
                                    onTriggerCloudSync = viewModel::triggerCloudSync,
                                    onSignOut = viewModel::signOut
                                )
                                ZenithTab.ANALYTICS -> AnalyticsScreen(state = state)
                                ZenithTab.REWARDS -> RewardsScreen(
                                    state = state,
                                    onUpdateReflection = viewModel::updateDailyReflection
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
