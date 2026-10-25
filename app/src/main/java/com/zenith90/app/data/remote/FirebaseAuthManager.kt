package com.zenith90.app.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.zenith90.app.data.local.DailyLogEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class ZenithUserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val isCloudConnected: Boolean,
    val lastSyncedStatus: String = "Local Room Active"
)

class FirebaseAuthManager(context: Context) {

    private val prefs = context.getSharedPreferences("zenith90_auth_prefs", Context.MODE_PRIVATE)
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var isLiveFirebaseConfigured = false

    private val _userState = MutableStateFlow<ZenithUserProfile?>(null)
    val userState: StateFlow<ZenithUserProfile?> = _userState.asStateFlow()

    init {
        try {
            val apps = FirebaseApp.getApps(context)
            val app = if (apps.isEmpty()) FirebaseApp.initializeApp(context) else FirebaseApp.getInstance()
            val apiKey = app?.options?.apiKey ?: ""
            if (app != null && !apiKey.startsWith("AIzaSyDummy")) {
                isLiveFirebaseConfigured = true
                auth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
            }
        } catch (_: Exception) {
            isLiveFirebaseConfigured = false
        }
        restoreExistingSession()
    }

    private fun restoreExistingSession() {
        val fbUser = auth?.currentUser
        if (fbUser != null) {
            _userState.value = ZenithUserProfile(
                uid = fbUser.uid,
                displayName = fbUser.displayName?.takeIf { it.isNotBlank() }
                    ?: fbUser.email?.substringBefore("@") ?: "Scholar",
                email = fbUser.email ?: "",
                isCloudConnected = true,
                lastSyncedStatus = "Firebase Cloud Connected"
            )
            return
        }

        val savedOfflineName = prefs.getString("offline_name", null)
        val savedOfflineEmail = prefs.getString("offline_email", null)
        if (savedOfflineName != null) {
            _userState.value = ZenithUserProfile(
                uid = "local_scholar",
                displayName = savedOfflineName,
                email = savedOfflineEmail ?: "offline@zenith90.local",
                isCloudConnected = false,
                lastSyncedStatus = "Offline Pixel Vault"
            )
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        return try {
            if (isLiveFirebaseConfigured && auth != null) {
                auth!!.signInWithEmailAndPassword(email.trim(), password).await()
                restoreExistingSession()
                Result.success(Unit)
            } else {
                // Graceful fallback if starter google-services.json is still in place
                continueInOfflineMode(
                    name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = email.trim()
                )
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(name: String, email: String, password: String): Result<Unit> {
        return try {
            if (isLiveFirebaseConfigured && auth != null) {
                val res = auth!!.createUserWithEmailAndPassword(email.trim(), password).await()
                val req = UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()
                res.user?.updateProfile(req)?.await()
                restoreExistingSession()
                Result.success(Unit)
            } else {
                continueInOfflineMode(name = name.trim(), email = email.trim())
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun continueInOfflineMode(name: String = "Suvankar", email: String = "scholar@pixel.local") {
        prefs.edit()
            .putString("offline_name", name)
            .putString("offline_email", email)
            .apply()
        _userState.value = ZenithUserProfile(
            uid = "local_scholar",
            displayName = name,
            email = email,
            isCloudConnected = false,
            lastSyncedStatus = "Offline Pixel Vault"
        )
    }

    suspend fun syncLogsToCloud(logs: List<DailyLogEntity>, streakDays: Int): String {
        val current = _userState.value ?: return "Not signed in"
        if (!isLiveFirebaseConfigured || firestore == null || !current.isCloudConnected) {
            return "Saved locally (${logs.size} days). Add live google-services.json for Cloud Sync."
        }
        return try {
            val userDoc = firestore!!.collection("zenith90_users").document(current.uid)
            val summary = mapOf(
                "displayName" to current.displayName,
                "email" to current.email,
                "overallStreak" to streakDays,
                "daysLogged" to logs.size,
                "lastSyncedEpochMillis" to System.currentTimeMillis()
            )
            userDoc.set(summary, SetOptions.merge()).await()

            logs.takeLast(14).forEach { log ->
                val logMap = mapOf(
                    "dateEpochDay" to log.dateEpochDay,
                    "targetWakeTimeMinutes" to log.targetWakeTimeMinutes,
                    "actualWakeTimeMinutes" to log.actualWakeTimeMinutes,
                    "totalStudyMinutes" to log.totalStudyMinutes,
                    "targetStudyMinutes" to log.targetStudyMinutes,
                    "workoutModality" to log.workoutModality.name,
                    "workoutCompleted" to log.workoutCompleted,
                    "plannedNightBeforeTimestamp" to log.plannedNightBeforeTimestamp,
                    "reflectionNote" to log.reflectionNote,
                    "completionRatio" to log.completionRatio
                )
                userDoc.collection("daily_logs")
                    .document(log.dateEpochDay.toString())
                    .set(logMap, SetOptions.merge())
                    .await()
            }
            _userState.value = current.copy(lastSyncedStatus = "Synced just now • ${logs.size} records")
            "Synced ${logs.size} days to Firebase Cloud Firestore"
        } catch (e: Exception) {
            "Local backup safe • Cloud sync deferred: ${e.localizedMessage ?: "Network offline"}"
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (_: Exception) {}
        prefs.edit().clear().apply()
        _userState.value = null
    }
}
