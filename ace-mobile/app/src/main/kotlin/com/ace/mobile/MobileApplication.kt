package com.ace.mobile

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class MobileApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    companion object {
        const val CHANNEL_SESSION_ACTIVE = "ace_session_active"
        const val CHANNEL_STREAK_REMINDER = "ace_streak_reminder"
        const val CHANNEL_SYNC_STATUS = "ace_sync_status"

        private const val PREFS_BOOTSTRAP = "ace_bootstrap"
        private const val KEY_LAST_INSTALL = "last_install_time"
    }

    override fun onCreate() {
        // Detecta debug sin BuildConfig: si el package contiene "debug" es build debug


        super.onCreate()
        createNotificationChannels()
        Log.i("MobileApplication", "A.C.E Mobile initialized")

        if (isDebugBuild() && isNewInstallation()) {
            wipeAllAppData()
        }

    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    /**
     * Detecta si es build de debug sin depender de BuildConfig.
     * En debug, el applicationIdSuffix añade ".debug" o el package contiene "debug".
     */
    private fun isDebugBuild(): Boolean {
        return packageName.contains("debug", ignoreCase = true) ||
                applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private fun isNewInstallation(): Boolean {
        val currentInstallTime = packageManager.getPackageInfo(packageName, 0).lastUpdateTime
        val prefs = getSharedPreferences(PREFS_BOOTSTRAP, Context.MODE_PRIVATE)
        val lastKnownTime = prefs.getLong(KEY_LAST_INSTALL, 0)

        return if (currentInstallTime > lastKnownTime) {
            prefs.edit().putLong(KEY_LAST_INSTALL, currentInstallTime).apply()
            true
        } else {
            false
        }
    }

    private fun wipeAllAppData() {
        Log.w("ACE_WIPE", ">>> NUEVA INSTALACION DETECTADA — BORRANDO DATOS <<<")

        WorkManager.getInstance(this).cancelAllWork()

        databaseList().forEach { dbName ->
            deleteDatabase(dbName)
            Log.d("ACE_WIPE", "DB eliminada: $dbName")
        }

        val prefsDir = File(applicationInfo.dataDir, "shared_prefs")
        prefsDir.listFiles()?.forEach { file ->
            if (!file.name.contains(PREFS_BOOTSTRAP)) {
                file.delete()
                Log.d("ACE_WIPE", "Prefs eliminadas: ${file.name}")
            }
        }

        File(filesDir, "datastore").deleteRecursively()
        cacheDir?.deleteRecursively()
        codeCacheDir?.deleteRecursively()
        externalCacheDir?.deleteRecursively()

        Log.w("ACE_WIPE", ">>> DATOS BORRADOS — ARRANQUE LIMPIO <<<")
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