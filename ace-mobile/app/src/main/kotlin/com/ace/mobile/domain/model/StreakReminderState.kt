package com.ace.mobile.domain.model

/**
 * Estado del recordatorio de racha para la UI.
 */
sealed class StreakReminderState {
    data object NoUser : StreakReminderState()
    data object TrainedToday : StreakReminderState()
    data class ReminderDue(
        val currentStreak: Int,
        val bestStreak: Int,
        val hoursUntilReset: Int
    ) : StreakReminderState()
}