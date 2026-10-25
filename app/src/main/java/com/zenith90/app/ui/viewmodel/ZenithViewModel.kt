package com.zenith90.app.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenith90.app.data.auth.AuthResult
import com.zenith90.app.data.auth.ZenithAuthRepository
import com.zenith90.app.data.auth.ZenithUser
import com.zenith90.app.data.local.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class ZenithUiState(
    val currentUser: ZenithUser? = null,
    val isAuthLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val authSuccessMessage: String? = null,
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
    val focusModeLabel: String = "DEEP WORK • 50:00"
)

@OptIn(ExperimentalCoroutinesApi::class)
class ZenithViewModel(
    private val dao: ZenithDao,
    private val authRepository: ZenithAuthRepository
) : ViewModel() {

    private val todayEpoch = LocalDate.now().toEpochDay()
    private val examDate = LocalDate.of(2027, 1, 20)
    private val startWindowEpoch = LocalDate.of(2026, 10, 1).toEpochDay()
    private val endWindowEpoch = LocalDate.of(2027, 1, 31).toEpochDay()

    private var timerJob: Job? = null
    private val _timerState = MutableStateFlow(Pair(false, 0))
    private val _isAuthLoading = MutableStateFlow(false)
    private val _authErrorMessage = MutableStateFlow<String?>(null)
    private val _authSuccessMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ZenithUiState> = authRepository.currentUser.flatMapLatest { user ->
        if (user == null) {
            combine(_isAuthLoading, _authErrorMessage, _authSuccessMessage) { loading, error, success ->
                ZenithUiState(
                    currentUser = null,
                    isAuthLoading = loading,
                    authErrorMessage = error,
                    authSuccessMessage = success
                )
            }
        } else {
            ensureUserInitialized(user.uid)
            combine(
                dao.observeDaySnapshot(user.uid, todayEpoch),
                dao.observeLogsInRange(user.uid, startWindowEpoch, endWindowEpoch),
                dao.observeAllDaySnapshotsDesc(user.uid),
                dao.observeAllBadges(user.uid),
                _timerState,
                _isAuthLoading,
                _authErrorMessage,
                _authSuccessMessage
            ) { todaySnap, rangeLogs, allSnaps, badges, timer, loading, error, success ->
                val overallStreak = calculateOverallStreak(rangeLogs)
                val wakeStreak = calculateWakeStreak(rangeLogs)
                val studyStreak = calculateStudyStreak(rangeLogs)
                val readiness = calculateReadinessPercentage(rangeLogs)

                ZenithUiState(
                    currentUser = user,
                    isAuthLoading = loading,
                    authErrorMessage = error,
                    authSuccessMessage = success,
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
                    activeFocusSeconds = timer.second
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ZenithUiState()
    )

    private val currentUserId: String
        get() = uiState.value.currentUser?.uid ?: "guest"

    private fun ensureUserInitialized(userId: String) {
        viewModelScope.launch {
            val currentBadges = dao.observeAllBadges(userId).firstOrNull()
            if (currentBadges.isNullOrEmpty()) {
                dao.upsertBadges(ZenithDatabase.initialBadgesForUser(userId))
            }
            val existingToday = dao.getDailyLog(userId, todayEpoch)
            if (existingToday == null) {
                dao.upsertDailyLog(
                    DailyLogEntity(
                        logKey = DailyLogEntity.createKey(userId, todayEpoch),
                        userId = userId,
                        dateEpochDay = todayEpoch
                    )
                )
            }
        }
    }

    // --- Authentication Actions ---

    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            when (val result = authRepository.signInWithEmail(email, pass)) {
                is AuthResult.Success -> {
                    _isAuthLoading.value = false
                    ensureUserInitialized(result.user.uid)
                }
                is AuthResult.Error -> {
                    _isAuthLoading.value = false
                    _authErrorMessage.value = result.message
                }
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, displayName: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            when (val result = authRepository.signUpWithEmail(email, pass, displayName)) {
                is AuthResult.Success -> {
                    _isAuthLoading.value = false
                    ensureUserInitialized(result.user.uid)
                }
                is AuthResult.Error -> {
                    _isAuthLoading.value = false
                    _authErrorMessage.value = result.message
                }
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            when (val result = authRepository.signInAnonymously()) {
                is AuthResult.Success -> {
                    _isAuthLoading.value = false
                    ensureUserInitialized(result.user.uid)
                }
                is AuthResult.Error -> {
                    _isAuthLoading.value = false
                    _authErrorMessage.value = result.message
                }
            }
        }
    }

    fun signInWithGoogle(activity: Activity, webClientId: String? = null) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            when (val result = authRepository.signInWithGoogleCredential(activity, webClientId)) {
                is AuthResult.Success -> {
                    _isAuthLoading.value = false
                    ensureUserInitialized(result.user.uid)
                }
                is AuthResult.Error -> {
                    _isAuthLoading.value = false
                    _authErrorMessage.value = result.message
                }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            when (val result = authRepository.sendPasswordReset(email)) {
                is AuthResult.Success -> {
                    _isAuthLoading.value = false
                    _authSuccessMessage.value = "Password reset instructions sent to $email."
                }
                is AuthResult.Error -> {
                    _isAuthLoading.value = false
                    _authErrorMessage.value = result.message
                }
            }
        }
    }

    fun signOut() {
        timerJob?.cancel()
        _timerState.value = Pair(false, 0)
        authRepository.signOut()
    }

    fun clearAuthMessages() {
        _authErrorMessage.value = null
        _authSuccessMessage.value = null
    }

    // --- Habit & Time-Block Actions (Partitioned by Current User) ---

    fun logActualWakeUpNow() {
        viewModelScope.launch {
            val userId = currentUserId
            val now = LocalTime.now()
            val minutes = now.hour * 60 + now.minute
            val currentLog = uiState.value.currentDaySnapshot?.dailyLog
                ?: DailyLogEntity(
                    logKey = DailyLogEntity.createKey(userId, todayEpoch),
                    userId = userId,
                    dateEpochDay = todayEpoch
                )
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
            val userId = currentUserId
            val currentLog = uiState.value.currentDaySnapshot?.dailyLog
                ?: DailyLogEntity(
                    logKey = DailyLogEntity.createKey(userId, todayEpoch),
                    userId = userId,
                    dateEpochDay = todayEpoch
                )
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
            val userId = currentUserId
            val targetKey = DailyLogEntity.createKey(userId, targetEpochDay)
            val targetSnap = dao.observeDaySnapshot(userId, targetEpochDay).firstOrNull()

            if (targetSnap == null) {
                dao.upsertDailyLog(
                    DailyLogEntity(
                        logKey = targetKey,
                        userId = userId,
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
                    logKey = targetKey,
                    userId = userId,
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
            val userId = currentUserId
            val tomorrowEpoch = todayEpoch + 1
            val tomorrowKey = DailyLogEntity.createKey(userId, tomorrowEpoch)
            val tomorrowSnap = dao.observeDaySnapshot(userId, tomorrowEpoch).firstOrNull()
            if (tomorrowSnap == null) {
                dao.upsertDailyLog(
                    DailyLogEntity(
                        logKey = tomorrowKey,
                        userId = userId,
                        dateEpochDay = tomorrowEpoch
                    )
                )
            }
            dao.rescheduleBlockToNextDay(block.id, tomorrowEpoch, tomorrowKey)
        }
    }

    fun switchWorkoutModality(modality: WorkoutModality) {
        viewModelScope.launch {
            val userId = currentUserId
            val currentLog = uiState.value.currentDaySnapshot?.dailyLog
                ?: DailyLogEntity(
                    logKey = DailyLogEntity.createKey(userId, todayEpoch),
                    userId = userId,
                    dateEpochDay = todayEpoch
                )
            dao.upsertDailyLog(currentLog.copy(workoutModality = modality))
        }
    }

    fun toggleWorkoutComplete(completed: Boolean) {
        viewModelScope.launch {
            val userId = currentUserId
            val currentLog = uiState.value.currentDaySnapshot?.dailyLog
                ?: DailyLogEntity(
                    logKey = DailyLogEntity.createKey(userId, todayEpoch),
                    userId = userId,
                    dateEpochDay = todayEpoch
                )
            val updated = currentLog.copy(workoutCompleted = completed)
            dao.upsertDailyLog(recalculateDayCompletion(updated))
            evaluateBadges()
        }
    }

    fun updateDailyReflection(note: String) {
        viewModelScope.launch {
            val currentLog = uiState.value.currentDaySnapshot?.dailyLog ?: return@launch
            dao.upsertDailyLog(currentLog.copy(reflectionNote = note))
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
        val userId = currentUserId
        val logs = dao.observeLogsInRange(userId, startWindowEpoch, endWindowEpoch).firstOrNull() ?: return
        val currentBadges = dao.observeAllBadges(userId).firstOrNull() ?: return
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
