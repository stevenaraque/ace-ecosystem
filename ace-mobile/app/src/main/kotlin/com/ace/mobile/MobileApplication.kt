package com.ace.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MobileApplication : Application() {

    companion object {
        const val CHANNEL_SESSION_ACTIVE = "ace_session_active"
        const val CHANNEL_STREAK_REMINDER = "ace_streak_reminder"
        const val CHANNEL_SYNC_STATUS = "ace_sync_status"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val channels = listOf(
            NotificationChannel(
                CHANNEL_SESSION_ACTIVE,
                "Active Exercise Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when an exercise session is active"
                setShowBadge(false)
            },
            NotificationChannel(
                CHANNEL_STREAK_REMINDER,
                "Streak Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you to maintain your exercise streak"
            },
            NotificationChannel(
                CHANNEL_SYNC_STATUS,
                "Sync Status",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about data synchronization"
            }
        )

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannels(channels)
    }
}