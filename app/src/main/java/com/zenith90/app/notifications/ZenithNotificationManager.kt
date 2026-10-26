package com.zenith90.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.zenith90.app.MainActivity
import com.zenith90.app.R

object ZenithNotificationManager {

    const val CHANNEL_FOCUS = "zenith_focus_channel"
    const val CHANNEL_CIRCADIAN = "zenith_circadian_channel"
    const val CHANNEL_BLUEPRINT = "zenith_blueprint_channel"

    private const val NOTIF_ID_TEST = 1001
    private const val NOTIF_ID_FOCUS = 1002
    private const val NOTIF_ID_CIRCADIAN = 1003
    private const val NOTIF_ID_BLUEPRINT = 1004

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS,
                "Focus & Pomodoro Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when deep study focus sessions and Pomodoro intervals finish"
                enableVibration(true)
                setShowBadge(true)
            }

            val circadianChannel = NotificationChannel(
                CHANNEL_CIRCADIAN,
                "Circadian Wake Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily morning wake anchor notifications"
                enableVibration(true)
                setShowBadge(true)
            }

            val blueprintChannel = NotificationChannel(
                CHANNEL_BLUEPRINT,
                "Daily Blueprint & Schedule",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for time blocks, tomorrow's night planner, and daily tasks"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(focusChannel, circadianChannel, blueprintChannel))
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun getLaunchPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    fun sendTestNotification(context: Context, title: String = "Zenith90 Notifications Active", message: String = "You will receive alerts for focus intervals, wake calls, and blueprint tasks."): Boolean {
        if (!hasNotificationPermission(context)) return false

        val builder = NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🌿 $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getLaunchPendingIntent(context))
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_TEST, builder.build())
            return true
        } catch (_: SecurityException) {
            return false
        }
    }

    fun sendFocusCompleteNotification(context: Context, elapsedMinutes: Int, subject: String): Boolean {
        if (!hasNotificationPermission(context)) return false

        val builder = NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎯 Focus Session Complete! ($elapsedMinutes min)")
            .setContentText("Great job on $subject. Log your session to verify trajectory points.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Completed $elapsedMinutes minutes on $subject. Open Zenith90 to validate and update your 3-Pillar Equilibrium score.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getLaunchPendingIntent(context))
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_FOCUS, builder.build())
            return true
        } catch (_: SecurityException) {
            return false
        }
    }

    fun sendCircadianWakeNotification(context: Context, targetTime: String): Boolean {
        if (!hasNotificationPermission(context)) return false

        val builder = NotificationCompat.Builder(context, CHANNEL_CIRCADIAN)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("☀️ Circadian Anchor: $targetTime")
            .setContentText("Target wake time reached. Tap to log your wake-up and preserve your circadian streak.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getLaunchPendingIntent(context))
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_CIRCADIAN, builder.build())
            return true
        } catch (_: SecurityException) {
            return false
        }
    }

    fun sendBlueprintReminderNotification(context: Context): Boolean {
        if (!hasNotificationPermission(context)) return false

        val builder = NotificationCompat.Builder(context, CHANNEL_BLUEPRINT)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🌙 Night Blueprint Protocol")
            .setContentText("Lock in tomorrow's time blocks tonight for zero morning friction.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(getLaunchPendingIntent(context))
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_BLUEPRINT, builder.build())
            return true
        } catch (_: SecurityException) {
            return false
        }
    }
}
