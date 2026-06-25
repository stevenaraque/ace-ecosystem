package com.ace.mobile.feature.streak.domain

import android.util.Log
import com.ace.mobile.core.database.dao.UserDao
import com.ace.mobile.core.database.entity.LocalUserEntity
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

private const val TAG = "CheckStreakUC"

class CheckStreakUseCase @Inject constructor(
    private val userDao: UserDao
) {

    /**
     * Verifica si el usuario ha entrenado hoy.
     * Retorna true si DEBE enviarse recordatorio (no entrenó hoy).
     */
    suspend operator fun invoke(): Boolean {
        val user = userDao.getCurrentUser() ?: run {
            Log.d(TAG, "No user logged in, skipping streak check")
            return false
        }

        val lastDate = user.lastExerciseDate
        if (lastDate == null) {
            Log.d(TAG, "No exercise recorded yet")
            return true // Debe recordar que empiece racha
        }

        val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
            .toEpochDay()

        val lastExerciseDay = Instant.ofEpochMilli(lastDate)
            .atZone(ZoneId.systemDefault()).toLocalDate()
            .toEpochDay()

        val hasTrainedToday = lastExerciseDay == today

        Log.d(TAG, "Streak check: today=$today, last=$lastExerciseDay, trainedToday=$hasTrainedToday")

        return !hasTrainedToday
    }

    /**
     * Obtiene el estado actual de racha para mostrar en UI.
     */
    suspend fun getStreakState(): StreakState {
        val user = userDao.getCurrentUser()
        return StreakState(
            currentStreak = user?.currentStreak ?: 0,
            bestStreak = user?.bestStreak ?: 0,
            lastExerciseDate = user?.lastExerciseDate,
            isAtRisk = user?.let { isStreakAtRisk(it) } ?: false
        )
    }

    private fun isStreakAtRisk(user: LocalUserEntity): Boolean {
        val lastDate = user.lastExerciseDate ?: return true
        val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        val lastDay = Instant.ofEpochMilli(lastDate).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        return today - lastDay >= 1 // No entrenó hoy
    }

    data class StreakState(
        val currentStreak: Int,
        val bestStreak: Int,
        val lastExerciseDate: Long?,
        val isAtRisk: Boolean
    )
}