package com.zenith90.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenith90.app.data.local.*
import com.zenith90.app.data.remote.FirebaseAuthManager
import com.zenith90.app.data.remote.ZenithUserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class ZenithUiState(
    val todayDate: LocalDate = LocalDate.now(),
    val targetExamDate: LocalDate = LocalDate.of(2027, 1, 20),
    val examTitle: String = "CSIR-NET Physical Sciences",
    val daysRemaining: Long = 87,
    val transformationReadinessPercent: Float = 0f,
    val userSettings: UserSettingsEntity = UserSettingsEntity(),
    val currentDaySnapshot: FullDaySnapshot? = null,
    val tomorrowSnapshot: FullDaySnapshot? = null,
    val fourMonthHistory: List<DailyLogEntity> = emptyList(),
    val allLogsWithDetails: List<FullDaySnapshot> = emptyList(),
    val badges: List<BadgeEntity> = emptyList(),
    val overallStreakDays: Int = 0,
    val studyStreakDays: Int = 0,
    val wakeStreakDays: Int = 0,
    val isFocusTimerRunning: Boolean = false,
    val activeFocusSeconds: Int = 0,
    val pendingPomodoroMinutes: Int? = null,
    val userProfile: ZenithUserProfile? = null,
    val authError: String? = null,
    val isAuthLoading: Boolean = false,
    val syncToastMessage: String? = null
)

private data class QuadState(
    val timer: Pair<Boolean, Int>,
    val pomodoro: Int?,
    val user: ZenithUserProfile?,
    val meta: Triple<String?, Boolean, String?>
)

private data class ExtraStreamState(
    val quad: QuadState,
    val settings: UserSettingsEntity,
    val allSnaps: List<FullDaySnapshot>,
    val tomorrowSnap: FullDaySnapshot?
)

class ZenithViewModel(
    private val dao: ZenithDao,
    private val authManager: FirebaseAuthManager
) : ViewModel() {

    private val todayEpoch = LocalDate.now().toEpochDay()
    private val tomorrowEpoch = todayEpoch + 1
    private val startWindowEpoch = LocalDate.of(2026, 10, 1).toEpochDay()
    private val endWindowEpoch = LocalDate.of(2027, 1, 31).toEpochDay()

    private var timerJob: Job? = null
    private val _timerState = MutableStateFlow(Pair(false, 0))
    private val _pendingPomodoro = MutableStateFlow<Int?>(null)
    private val _authMeta = MutableStateFlow(Triple<String?, Boolean, String?>(null, false, null))

    private val quadFlow = combine(
        _timerState,
        _pendingPomodoro,
        authManager.userState,
        _authMeta
    ) { timer, pomodoro, user, meta ->
        QuadState(timer, pomodoro, user, meta)
    }

    private val extraStreamFlow = combine(
        quadFlow,
        dao.observeUserSettings(),
        dao.observeAllDaySnapshotsDesc(),
        dao.observeDaySnapshot(tomorrowEpoch)
    ) { quad, settings, allSnaps, tomorrowSnap ->
        ExtraStreamState(
            quad = quad,
            settings = settings ?: UserSettingsEntity(),
            allSnaps = allSnaps,
            tomorrowSnap = tomorrowSnap
        )
    }

    val uiState: StateFlow<ZenithUiState> = combine(
        dao.observeDaySnapshot(todayEpoch),
        dao.observeLogsInRange(startWindowEpoch, endWindowEpoch),
        dao.observeAllBadges(),
        extraStreamFlow
    ) { todaySnap, rangeLogs, badges, extra ->
        val examDate = LocalDate.ofEpochDay(extra.settings.targetExamEpochDay)
        val daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), examDate).coerceAtLeast(0)
        val overallStreak = calculateOverallStreak(rangeLogs)
        val wakeStreak = calculateWakeStreak(rangeLogs)
        val studyStreak = calculateStudyStreak(rangeLogs)
        val readiness = calculateReadinessPercentage(rangeLogs)

        ZenithUiState(
            todayDate = LocalDate.now(),
            targetExamDate = examDate,
            examTitle = extra.settings.examTitle,
            daysRemaining = daysRemaining,
            transformationReadinessPercent = readiness,
            userSettings = extra.settings,
            currentDaySnapshot = todaySnap,
            tomorrowSnapshot = extra.tomorrowSnap,
            fourMonthHistory = rangeLogs,
            allLogsWithDetails = extra.allSnaps,
            badges = badges,
            overallStreakDays = overallStreak,
            studyStreakDays = studyStreak,
            wakeStreakDays = wakeStreak,
            isFocusTimerRunning = extra.quad.timer.first,
            activeFocusSeconds = extra.quad.timer.second,
            pendingPomodoroMinutes = extra.quad.pomodoro,
            userProfile = extra.quad.user,
            authError = extra.quad.meta.first,
            isAuthLoading = extra.quad.meta.second,
            syncToastMessage = extra.quad.meta.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ZenithUiState(userProfile = authManager.userState.value)
    )

    init {
        viewModelScope.launch { ensureTodayInitialized() }
    }

    private suspend fun ensureTodayInitialized() {
        val currentSettings = dao.getUserSettings()
        if (currentSettings == null) {
            dao.upsertUserSettings(UserSettingsEntity())
        }

        val currentBadges = dao.observeAllBadges().firstOrNull() ?: emptyList()
        val existingIds = currentBadges.map { it.badgeId }.toSet()
        val missingBadges = ZenithDatabase.INITIAL_BADGES.filter { it.badgeId !in existingIds }
        if (missingBadges.isNotEmpty()) {
            dao.upsertBadges(missingBadges)
        }

        val settings = dao.getUserSettings() ?: UserSettingsEntity()
        val existingToday = dao.observeDaySnapshot(todayEpoch).firstOrNull()
        if (existingToday == null) {
            dao.upsertDailyLog(
                DailyLogEntity(
                    dateEpochDay = todayEpoch,
                    targetWakeTimeMinutes = settings.targetWakeMinutes,
                    targetStudyMinutes = settings.targetStudyMinutes
                )
            )
            dao.upsertScheduleBlock(
                ScheduleBlockEntity(
                    dateEpochDay = todayEpoch,
                    startTimeMinutes = settings.targetWakeMinutes,
                    durationMinutes = 30,
                    title = "Circadian Hydration, Sunlight & Mobility",
                    examSubjectTag = "ROUTINE",
                    category = BlockCategory.WAKE_UP,
                    priority = TaskPriority.HIGH
                )
            )
            dao.upsertScheduleBlock(
                ScheduleBlockEntity(
                    dateEpochDay = todayEpoch,
                    startTimeMinutes = 510,
                    durationMinutes = 120,
                    title = "Quantum Mechanics: Core Formalism",
                    examSubjectTag = "QUANTUM MECH",
                    category = BlockCategory.STUDY,
                    priority = TaskPriority.HIGH
                )
            )
        }

        val existingTomorrow = dao.observeDaySnapshot(tomorrowEpoch).firstOrNull()
        if (existingTomorrow == null) {
            dao.upsertDailyLog(
                DailyLogEntity(
                    dateEpochDay = tomorrowEpoch,
                    targetWakeTimeMinutes = settings.targetWakeMinutes,
                    targetStudyMinutes = settings.targetStudyMinutes
                )
            )
        }
    }

    // --- ONBOARDING & TARGET CUSTOMIZATION ---
    fun updateUserSettings(
        examTitle: String,
        targetDate: LocalDate,
        wakeTargetMinutes: Int,
        studyTargetMinutes: Int
    ) {
        viewModelScope.launch {
            val updated = UserSettingsEntity(
                id = 1,
                examTitle = examTitle.trim().ifBlank { "January Examination" },
                targetExamEpochDay = targetDate.toEpochDay(),
                targetWakeMinutes = wakeTargetMinutes,
                targetStudyMinutes = studyTargetMinutes,
                hasCompletedOnboarding = true
            )
            dao.upsertUserSettings(updated)
            dao.updateDailyTargets(todayEpoch, wakeTargetMinutes, studyTargetMinutes)
            dao.updateDailyTargets(tomorrowEpoch, wakeTargetMinutes, studyTargetMinutes)
            _authMeta.value = Triple(null, false, "Exam goals locked in: \"$examTitle\"")
        }
    }

    fun updateTodayTargets(wakeTargetMinutes: Int, studyTargetMinutes: Int) {
        viewModelScope.launch {
            dao.updateDailyTargets(todayEpoch, wakeTargetMinutes, studyTargetMinutes)
            _authMeta.value = Triple(null, false, "Updated today's wake & study targets")
        }
    }

    // --- AUTHENTICATION & CLOUD SYNC ---
    fun signIn(email: String, pass: String) {
        viewModelScope.launch {
            _authMeta.value = Triple(null, true, null)
            val res = authManager.signInWithEmail(email, pass)
            _authMeta.value = Triple(res.exceptionOrNull()?.localizedMessage, false, null)
        }
    }

    fun signUp(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _authMeta.value = Triple(null, true, null)
            val res = authManager.signUpWithEmail(name, email, pass)
            _authMeta.value = Triple(res.exceptionOrNull()?.localizedMessage, false, null)
        }
    }

    fun continueOffline(name: String) {
        viewModelScope.launch {
            authManager.continueInOfflineMode(name = name.ifBlank { "Scholar" })
        }
    }

    fun signInAsGuest() {
        viewModelScope.launch {
            _authMeta.value = Triple(null, true, null)
            val res = authManager.signInAsGuest()
            _authMeta.value = Triple(res.exceptionOrNull()?.localizedMessage, false, null)
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authMeta.value = Triple(null, true, null)
            val res = authManager.signInWithGoogle(context)
            _authMeta.value = Triple(res.exceptionOrNull()?.localizedMessage, false, null)
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            val status = authManager.syncLogsToCloud(
                logs = uiState.value.fourMonthHistory,
                streakDays = uiState.value.overallStreakDays
            )
            _authMeta.value = Triple(null, false, status)
        }
    }

    fun signOut() {
        authManager.signOut()
    }

    fun clearSyncToast() {
        _authMeta.value = Triple(_authMeta.value.first, _authMeta.value.second, null)
    }

    // --- PILLAR I: CIRCADIAN WAKE LOGGING ---
    fun logActualWakeUpNow() {
        viewModelScope.launch {
            val nowMinutes = LocalTime.now().hour * 60 + LocalTime.now().minute
            val currentLog = uiState.value.currentDaySnapshot?.dailyLog
                ?: DailyLogEntity(dateEpochDay = todayEpoch)
            val updated = currentLog.copy(actualWakeTimeMinutes = nowMinutes)
            dao.upsertDailyLog(recalculateDayCompletion(updated))
            evaluateBadges()
        }
    }

    // --- PILLAR II: DEEP STUDY ENGINE & POMODORO VALIDATION ---
    fun toggleFocusTimer() {
        if (_timerState.value.first) {
            // Stopping timer: if user spent >= 60 seconds, prompt to confirm rather than blindly logging!
            val elapsedSecs = _timerState.value.second
            val elapsedMins = elapsedSecs / 60
            timerJob?.cancel()
            _timerState.value = Pair(false, 0)
            if (elapsedMins >= 1) {
                _pendingPomodoro.value = elapsedMins
            }
        } else {
            // Starting stopwatch
            _timerState.value = Pair(true, 0)
            timerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    val nextSec = _timerState.value.second + 1
                    _timerState.value = Pair(true, nextSec)
                    // If a standard 25-minute Pomodoro completes (1500 sec)
                    if (nextSec == 1500) {
                        _timerState.value = Pair(false, 0)
                        _pendingPomodoro.value = 25
                        break
                    }
                }
            }
        }
    }

    fun confirmPomodoroSession(minutes: Int, subjectTag: String) {
        viewModelScope.launch {
            _pendingPomodoro.value = null
            commitStudySessionMinutes(minutes)
            _authMeta.value = Triple(null, false, "Validated & logged $minutes mins of study ($subjectTag)")
        }
    }

    fun discardPomodoroSession() {
        _pendingPomodoro.value = null
        _authMeta.value = Triple(null, false, "Idle Pomodoro session discarded")
    }

    fun verifyAndCommitStudyBoost(minutes: Int, subjectTag: String) {
        viewModelScope.launch {
            commitStudySessionMinutes(minutes)
            _authMeta.value = Triple(null, false, "Verified Pillar II Boost: +$minutes mins ($subjectTag)")
        }
    }

    fun claimBadge(badgeId: String) {
        viewModelScope.launch {
            val currentBadges = dao.observeAllBadges().firstOrNull() ?: return@launch
            val badge = currentBadges.find { it.badgeId == badgeId } ?: return@launch
            if (badge.progressCurrent >= badge.progressTarget) {
                val updated = badge.copy(
                    isUnlocked = true,
                    unlockedEpochMillis = System.currentTimeMillis()
                )
                dao.upsertBadges(listOf(updated))
                _authMeta.value = Triple(null, false, "★ RANK CLAIMED! ${badge.title} ★")
                triggerCloudSync()
            }
        }
    }

    fun commitStudySessionMinutes(minutes: Int) {
        viewModelScope.launch {
            val currentLog = uiState.value.currentDaySnapshot?.dailyLog
                ?: DailyLogEntity(dateEpochDay = todayEpoch)
            val updated = currentLog.copy(totalStudyMinutes = currentLog.totalStudyMinutes + minutes)
            dao.upsertDailyLog(recalculateDayCompletion(updated))
            evaluateBadges()
        }
    }

    // --- TIME-BLOCKED BLUEPRINT & SCHEDULE MANAGEMENT ---
    fun saveOrUpdateScheduleBlock(
        id: Long = 0,
        targetEpochDay: Long,
        startTimeMinutes: Int,
        durationMinutes: Int,
        title: String,
        examSubjectTag: String?,
        category: BlockCategory,
        priority: TaskPriority
    ) {
        viewModelScope.launch {
            val targetSnap = dao.observeDaySnapshot(targetEpochDay).firstOrNull()
            if (targetSnap == null) {
                val settings = dao.getUserSettings() ?: UserSettingsEntity()
                dao.upsertDailyLog(
                    DailyLogEntity(
                        dateEpochDay = targetEpochDay,
                        targetWakeTimeMinutes = settings.targetWakeMinutes,
                        targetStudyMinutes = settings.targetStudyMinutes,
                        plannedNightBeforeTimestamp = System.currentTimeMillis()
                    )
                )
            } else if (targetEpochDay > todayEpoch) {
                dao.upsertDailyLog(
                    targetSnap.dailyLog.copy(plannedNightBeforeTimestamp = System.currentTimeMillis())
                )
            }

            dao.upsertScheduleBlock(
                ScheduleBlockEntity(
                    id = id,
                    dateEpochDay = targetEpochDay,
                    startTimeMinutes = startTimeMinutes,
                    durationMinutes = durationMinutes,
                    title = title,
                    examSubjectTag = examSubjectTag?.uppercase()?.trim(),
                    category = category,
                    priority = priority
                )
            )
            evaluateBadges()
        }
    }

    fun toggleScheduleBlockCompletion(block: ScheduleBlockEntity) {
        viewModelScope.launch {
            dao.upsertScheduleBlock(block.copy(isCompleted = !block.isCompleted))
            evaluateBadges()
        }
    }

    fun rollOverTaskToTomorrow(block: ScheduleBlockEntity) {
        viewModelScope.launch {
            val targetTomorrow = todayEpoch + 1
            val tomorrowSnap = dao.observeDaySnapshot(targetTomorrow).firstOrNull()
            if (tomorrowSnap == null) {
                val settings = dao.getUserSettings() ?: UserSettingsEntity()
                dao.upsertDailyLog(
                    DailyLogEntity(
                        dateEpochDay = targetTomorrow,
                        targetWakeTimeMinutes = settings.targetWakeMinutes,
                        targetStudyMinutes = settings.targetStudyMinutes
                    )
                )
            }
            dao.rescheduleBlockToNextDay(block.id, targetTomorrow)
            _authMeta.value = Triple(null, false, "Rolled over \"${block.title}\" to Tomorrow's Blueprint")
        }
    }

    fun deleteScheduleBlock(block: ScheduleBlockEntity) {
        viewModelScope.launch {
            dao.deleteScheduleBlock(block)
            _authMeta.value = Triple(null, false, "Task \"${block.title}\" removed")
        }
    }

    // --- PILLAR III: PHYSICAL CONDITIONING & EXERCISE MANAGER ---
    fun switchWorkoutModality(modality: WorkoutModality) {
        viewModelScope.launch {
            val currentLog = uiState.value.currentDaySnapshot?.dailyLog
                ?: DailyLogEntity(dateEpochDay = todayEpoch)
            dao.upsertDailyLog(currentLog.copy(workoutModality = modality))
        }
    }

    fun toggleWorkoutComplete(completed: Boolean) {
        viewModelScope.launch {
            val currentLog = uiState.value.currentDaySnapshot?.dailyLog
                ?: DailyLogEntity(dateEpochDay = todayEpoch)
            val updated = currentLog.copy(workoutCompleted = completed)
            dao.upsertDailyLog(recalculateDayCompletion(updated))
            evaluateBadges()
        }
    }

    fun addWorkoutExercise(exerciseName: String, targetSets: Int, repsAndLoad: String) {
        viewModelScope.launch {
            val modality = uiState.value.currentDaySnapshot?.dailyLog?.workoutModality
                ?: WorkoutModality.GYM
            dao.upsertWorkoutEntry(
                WorkoutEntryEntity(
                    dateEpochDay = todayEpoch,
                    modality = modality,
                    exerciseName = exerciseName.trim(),
                    targetSets = targetSets.coerceAtLeast(1),
                    completedSets = 0,
                    repsAndLoad = repsAndLoad.trim(),
                    isCompleted = false
                )
            )
        }
    }

    fun incrementWorkoutSet(entry: WorkoutEntryEntity) {
        viewModelScope.launch {
            val nextCompleted = (entry.completedSets + 1).coerceAtMost(entry.targetSets)
            val isDone = nextCompleted >= entry.targetSets
            dao.updateWorkoutEntryProgress(entry.id, nextCompleted, isDone)

            val currentEntries = uiState.value.currentDaySnapshot?.workoutEntries ?: emptyList()
            val allComplete = currentEntries.all { if (it.id == entry.id) isDone else it.isCompleted }
            if (allComplete && isDone) {
                toggleWorkoutComplete(true)
            }
        }
    }

    fun deleteWorkoutSet(entry: WorkoutEntryEntity) {
        viewModelScope.launch {
            dao.deleteWorkoutEntry(entry)
        }
    }

    fun updateDailyReflection(note: String) {
        viewModelScope.launch {
            val currentLog = uiState.value.currentDaySnapshot?.dailyLog ?: return@launch
            dao.upsertDailyLog(currentLog.copy(reflectionNote = note.trim()))
        }
    }

    private fun recalculateDayCompletion(log: DailyLogEntity): DailyLogEntity {
        val wakeScore = if (log.isWakeGoalMet) 0.25f else 0f
        val studyScore = (log.totalStudyMinutes.toFloat() / log.targetStudyMinutes.toFloat())
            .coerceIn(0f, 1f) * 0.50f
        val workoutScore = if (log.workoutCompleted) 0.25f else 0f
        return log.copy(completionRatio = (wakeScore + studyScore + workoutScore).coerceIn(0f, 1f))
    }

    private fun calculateOverallStreak(logs: List<DailyLogEntity>): Int {
        var streak = 0
        for (log in logs.sortedByDescending { it.dateEpochDay }) {
            if (log.isOverallStreakDay) streak++ else if (log.dateEpochDay < todayEpoch) break
        }
        return streak
    }

    private fun calculateWakeStreak(logs: List<DailyLogEntity>): Int {
        var streak = 0
        for (log in logs.sortedByDescending { it.dateEpochDay }) {
            if (log.isWakeGoalMet) streak++ else if (log.dateEpochDay < todayEpoch) break
        }
        return streak
    }

    private fun calculateStudyStreak(logs: List<DailyLogEntity>): Int {
        var streak = 0
        for (log in logs.sortedByDescending { it.dateEpochDay }) {
            if (log.totalStudyMinutes >= 240) streak++ else if (log.dateEpochDay < todayEpoch) break
        }
        return streak
    }

    private fun calculateReadinessPercentage(logs: List<DailyLogEntity>): Float {
        if (logs.isEmpty()) return 0f
        val sumRatios = logs.sumOf { it.completionRatio.toDouble() }.toFloat()
        return ((sumRatios / 90f) * 100f).coerceIn(0f, 100f)
    }

    private suspend fun evaluateBadges() {
        val logs = dao.observeLogsInRange(startWindowEpoch, endWindowEpoch).firstOrNull() ?: return
        val currentBadges = dao.observeAllBadges().firstOrNull() ?: return
        val wakeStreak = calculateWakeStreak(logs)
        val overallStreak = calculateOverallStreak(logs)
        val todayLog = logs.find { it.dateEpochDay == todayEpoch }
        val ironAndInkDone = todayLog != null && todayLog.workoutCompleted && todayLog.totalStudyMinutes >= 360
        val nightPlannedCount = logs.count { it.plannedNightBeforeTimestamp != null }

        val updatedBadges = currentBadges.map { badge ->
            when (badge.badgeId) {
                "first_blood" -> {
                    val touched = logs.any { it.completionRatio > 0.2f || it.totalStudyMinutes > 0 }
                    badge.copy(
                        progressCurrent = if (touched || badge.isUnlocked) 1 else 0,
                        isUnlocked = badge.isUnlocked || touched
                    )
                }
                "5am_club" -> badge.copy(
                    progressCurrent = wakeStreak.coerceAtMost(7),
                    isUnlocked = badge.isUnlocked || wakeStreak >= 7
                )
                "quad_pomodoro" -> {
                    val pomos = (todayLog?.totalStudyMinutes ?: 0) / 25
                    badge.copy(
                        progressCurrent = pomos.coerceAtMost(4),
                        isUnlocked = badge.isUnlocked || pomos >= 4
                    )
                }
                "iron_and_ink" -> badge.copy(
                    progressCurrent = if (ironAndInkDone || badge.isUnlocked) 1 else 0,
                    isUnlocked = badge.isUnlocked || ironAndInkDone
                )
                "discipline_100" -> {
                    val perfect = logs.any { it.completionRatio >= 0.99f }
                    badge.copy(
                        progressCurrent = if (perfect || badge.isUnlocked) 1 else 0,
                        isUnlocked = badge.isUnlocked || perfect
                    )
                }
                "night_architect" -> badge.copy(
                    progressCurrent = nightPlannedCount.coerceAtMost(7),
                    isUnlocked = badge.isUnlocked || nightPlannedCount >= 7
                )
                "relentless_30" -> badge.copy(
                    progressCurrent = overallStreak.coerceAtMost(30),
                    isUnlocked = badge.isUnlocked || overallStreak >= 30
                )
                "relentless_60" -> badge.copy(
                    progressCurrent = overallStreak.coerceAtMost(60),
                    isUnlocked = badge.isUnlocked || overallStreak >= 60
                )
                "relentless_90" -> badge.copy(
                    progressCurrent = overallStreak.coerceAtMost(90),
                    isUnlocked = badge.isUnlocked || overallStreak >= 90
                )
                else -> badge
            }
        }
        dao.upsertBadges(updatedBadges)
    }
}
