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
    val daysRemaining: Long = 87,
    val transformationReadinessPercent: Float = 0f,
    val currentDaySnapshot: FullDaySnapshot? = null,
    val fourMonthHistory: List<DailyLogEntity> = emptyList(),
    val allLogsWithDetails: List<FullDaySnapshot> = emptyList(),
    val badges: List<BadgeEntity> = emptyList(),
    val overallStreakDays: Int = 0,
    val studyStreakDays: Int = 0,
    val wakeStreakDays: Int = 0,
    val isFocusTimerRunning: Boolean = false,
    val activeFocusSeconds: Int = 0,
    val userProfile: ZenithUserProfile? = null,
    val authError: String? = null,
    val isAuthLoading: Boolean = false,
    val syncToastMessage: String? = null
)

class ZenithViewModel(
    private val dao: ZenithDao,
    private val authManager: FirebaseAuthManager
) : ViewModel() {

    private val todayEpoch = LocalDate.now().toEpochDay()
    private val examDate = LocalDate.of(2027, 1, 20)
    private val startWindowEpoch = LocalDate.of(2026, 10, 1).toEpochDay()
    private val endWindowEpoch = LocalDate.of(2027, 1, 31).toEpochDay()

    private var timerJob: Job? = null
    private val _timerState = MutableStateFlow(Pair(false, 0))
    private val _authMeta = MutableStateFlow(Triple<String?, Boolean, String?>(null, false, null))

    val uiState: StateFlow<ZenithUiState> = combine(
        dao.observeDaySnapshot(todayEpoch),
        dao.observeLogsInRange(startWindowEpoch, endWindowEpoch),
        dao.observeAllDaySnapshotsDesc(),
        dao.observeAllBadges(),
        combine(_timerState, authManager.userState, _authMeta) { t, u, m -> Triple(t, u, m) }
    ) { todaySnap, rangeLogs, allSnaps, badges, extra ->
        val (timer, userProfile, authMeta) = extra
        val overallStreak = calculateOverallStreak(rangeLogs)
        val wakeStreak = calculateWakeStreak(rangeLogs)
        val studyStreak = calculateStudyStreak(rangeLogs)
        val readiness = calculateReadinessPercentage(rangeLogs)

        ZenithUiState(
            todayDate = LocalDate.now(),
            targetExamDate = examDate,
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), examDate).coerceAtLeast(0),
            transformationReadinessPercent = readiness,
            currentDaySnapshot = todaySnap,
            fourMonthHistory = rangeLogs,
            allLogsWithDetails = allSnaps,
            badges = badges,
            overallStreakDays = overallStreak,
            studyStreakDays = studyStreak,
            wakeStreakDays = wakeStreak,
            isFocusTimerRunning = timer.first,
            activeFocusSeconds = timer.second,
            userProfile = userProfile,
            authError = authMeta.first,
            isAuthLoading = authMeta.second,
            syncToastMessage = authMeta.third
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
        val currentBadges = dao.observeAllBadges().firstOrNull()
        if (currentBadges.isNullOrEmpty()) {
            dao.upsertBadges(ZenithDatabase.INITIAL_BADGES)
        }
        val existingToday = dao.observeDaySnapshot(todayEpoch).firstOrNull()
        if (existingToday == null) {
            dao.upsertDailyLog(DailyLogEntity(dateEpochDay = todayEpoch))
            dao.upsertScheduleBlock(
                ScheduleBlockEntity(
                    dateEpochDay = todayEpoch,
                    startTimeMinutes = 330,
                    durationMinutes = 30,
                    title = "05:30 AM Circadian Hydration & Mobility",
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
                    title = "Quantum Mechanics: Perturbation & WKB Method",
                    examSubjectTag = "QUANTUM MECH",
                    category = BlockCategory.STUDY,
                    priority = TaskPriority.HIGH
                )
            )
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

    fun clearSyncToast() {
        _authMeta.update { it.copy(third = null) }
    }

    fun signOut() {
        authManager.signOut()
    }

    // --- PILLARS & TASKS ---
    fun logActualWakeUpNow() {
        viewModelScope.launch {
            val now = LocalTime.now()
            val minutes = now.hour * 60 + now.minute
            val currentLog = uiState.value.currentDaySnapshot?.dailyLog
                ?: DailyLogEntity(dateEpochDay = todayEpoch)
            val updated = currentLog.copy(actualWakeTimeMinutes = minutes)
            dao.upsertDailyLog(recalculateDayCompletion(updated))
            evaluateBadges()
        }
    }

    fun toggleFocusTimer() {
        val currentlyRunning = _timerState.value.first
        if (currentlyRunning) {
            timerJob?.cancel()
            val accruedMinutes = (_timerState.value.second / 60).coerceAtLeast(1)
            commitStudySessionMinutes(accruedMinutes)
            _timerState.value = Pair(false, 0)
        } else {
            _timerState.value = Pair(true, _timerState.value.second)
            timerJob = viewModelScope.launch {
                while (_timerState.value.first) {
                    delay(1000L)
                    _timerState.update { it.copy(second = it.second + 1) }
                }
            }
        }
    }

    fun commitStudySessionMinutes(minutesToAdd: Int) {
        viewModelScope.launch {
            val currentLog = uiState.value.currentDaySnapshot?.dailyLog
                ?: DailyLogEntity(dateEpochDay = todayEpoch)
            val updated = currentLog.copy(
                totalStudyMinutes = currentLog.totalStudyMinutes + minutesToAdd
            )
            dao.upsertDailyLog(recalculateDayCompletion(updated))
            evaluateBadges()
        }
    }

    fun toggleScheduleBlockCompletion(block: ScheduleBlockEntity) {
        viewModelScope.launch {
            dao.upsertScheduleBlock(block.copy(isCompleted = !block.isCompleted))
            uiState.value.currentDaySnapshot?.dailyLog?.let {
                dao.upsertDailyLog(recalculateDayCompletion(it))
            }
            evaluateBadges()
        }
    }

    fun saveOrUpdateScheduleBlock(
        id: Long = 0,
        targetEpochDay: Long = todayEpoch,
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
                dao.upsertDailyLog(
                    DailyLogEntity(
                        dateEpochDay = targetEpochDay,
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

    fun rollOverTaskToTomorrow(block: ScheduleBlockEntity) {
        viewModelScope.launch {
            val tomorrowEpoch = todayEpoch + 1
            val tomorrowSnap = dao.observeDaySnapshot(tomorrowEpoch).firstOrNull()
            if (tomorrowSnap == null) {
                dao.upsertDailyLog(DailyLogEntity(dateEpochDay = tomorrowEpoch))
            }
            dao.rescheduleBlockToNextDay(block.id, tomorrowEpoch)
        }
    }

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

    fun addWorkoutSet(exercise: String, sets: Int, repsLoad: String) {
        viewModelScope.launch {
            val modality = uiState.value.currentDaySnapshot?.dailyLog?.workoutModality
                ?: WorkoutModality.GYM
            dao.upsertWorkoutEntry(
                WorkoutEntryEntity(
                    dateEpochDay = todayEpoch,
                    modality = modality,
                    exerciseName = exercise.trim(),
                    targetSets = sets,
                    completedSets = sets,
                    repsAndLoad = repsLoad.trim(),
                    isCompleted = true
                )
            )
            toggleWorkoutComplete(true)
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
                "5am_club" -> badge.copy(
                    progressCurrent = wakeStreak.coerceAtMost(7),
                    isUnlocked = badge.isUnlocked || wakeStreak >= 7
                )
                "iron_and_ink" -> badge.copy(
                    progressCurrent = if (ironAndInkDone || badge.isUnlocked) 1 else 0,
                    isUnlocked = badge.isUnlocked || ironAndInkDone
                )
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
