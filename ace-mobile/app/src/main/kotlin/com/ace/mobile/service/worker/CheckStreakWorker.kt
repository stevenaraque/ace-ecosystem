package com.ace.mobile.service.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import sena.adso.ace_mobile.R
import com.ace.mobile.domain.usecase.streak.CheckStreakUseCase
import com.ace.shared.constants.StreakConstants
import com.ace.shared.enums.NotificationChannelId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "CheckStreakWorker"

@HiltWorker
class CheckStreakWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val checkStreakUseCase: CheckStreakUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "CheckStreakWorker started at ${System.currentTimeMillis()}")

        val shouldRemind = checkStreakUseCase()

        if (shouldRemind) {
            val streakState = checkStreakUseCase.getStreakState()
            showNotification(
                currentStreak = streakState.currentStreak,
                bestStreak = streakState.bestStreak
            )
            Log.i(TAG, "Streak reminder shown: current=${streakState.currentStreak}")
        } else {
            Log.d(TAG, "User trained today, no reminder needed")
        }

        return Result.success()
    }

    private fun showNotification(currentStreak: Int, bestStreak: Int) {
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal si es necesario (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationChannelId.STREAK_REMINDER.channelId,
                "Recordatorio de Racha",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para mantener tu racha de ejercicio"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val message = if (currentStreak > 0) {
            "¡No rompas tu racha de $currentStreak días! 🔥 Entrena hoy para mantenerla."
        } else {
            "¡Empieza tu racha hoy! 💪 Entrena para comenzar a acumular días."
        }

        val notification = NotificationCompat.Builder(
            context,
            NotificationChannelId.STREAK_REMINDER.channelId
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("A.C.E — Recordatorio de Racha")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(STREAK_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val STREAK_NOTIFICATION_ID = 2001
    }
}