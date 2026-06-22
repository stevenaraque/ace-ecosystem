package com.ace.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ace.mobile.service.worker.SyncBlockWorker
import com.ace.shared.enums.NotificationChannelId
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "ExerciseSyncService"
private const val FOREGROUND_NOTIFICATION_ID = 1

/**
 * Foreground Service que mantiene viva la sesión de ejercicio.
 *
 * Responsabilidades:
 * 1. Mostrar notificación persistente durante sesión activa (S8)
 * 2. Disparar SyncBlockWorker cuando se cierra un bloque (S3)
 * 3. Escuchar comandos START/STOP del wear (S1)
 *
 * NOTA: Usa FOREGROUND_SERVICE_TYPE_DATA_SYNC para evitar permisos de health en Android 14+.
 *
 * @see Apéndice S2 (Sesión) · Apéndice S8 (Notificaciones)
 */
@AndroidEntryPoint
class ExerciseSyncService : Service() {

    @Inject
    lateinit var blockRepository: com.ace.mobile.data.repository.BlockRepository

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "ExerciseSyncService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        Log.i(TAG, "onStartCommand: action=$action")

        when (action) {
            ACTION_START_SESSION -> {
                startForegroundService()
            }

            ACTION_STOP_SESSION -> {
                stopForegroundService()
            }

            ACTION_BLOCK_CLOSED -> {
                val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID)
                Log.i(TAG, "Block closed, triggering sync for session=$sessionId")
                SyncBlockWorker.enqueue(this, sessionId)
            }

            else -> {
                Log.w(TAG, "Unknown action: $action")
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val notification = buildForegroundNotification("Sesión activa")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC  // ← CAMBIADO: SHORT_SERVICE → DATA_SYNC
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }

        Log.i(TAG, "Foreground service started (dataSync)")
    }

    private fun stopForegroundService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.i(TAG, "Foreground service stopped")
    }

    /**
     * Construye la notificación persistente de sesión activa.
     */
    private fun buildForegroundNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, NotificationChannelId.SESSION_ACTIVE.channelId)
            .setContentTitle("A.C.E — Entrenamiento en curso")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationChannelId.SESSION_ACTIVE.channelId,
                "Sesión activa",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación persistente durante el entrenamiento"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START_SESSION = "com.ace.mobile.ACTION_START_SESSION"
        const val ACTION_STOP_SESSION = "com.ace.mobile.ACTION_STOP_SESSION"
        const val ACTION_BLOCK_CLOSED = "com.ace.mobile.ACTION_BLOCK_CLOSED"
        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_SPORT_TYPE = "extra_sport_type"
        const val EXTRA_USER_ID = "extra_user_id"

        fun startSession(context: android.content.Context) {
            val intent = Intent(context, ExerciseSyncService::class.java).apply {
                action = ACTION_START_SESSION
            }
            context.startForegroundService(intent)
        }

        fun stopSession(context: android.content.Context) {
            val intent = Intent(context, ExerciseSyncService::class.java).apply {
                action = ACTION_STOP_SESSION
            }
            context.startService(intent)
        }

        fun notifyBlockClosed(context: android.content.Context, sessionId: String) {
            val intent = Intent(context, ExerciseSyncService::class.java).apply {
                action = ACTION_BLOCK_CLOSED
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            context.startService(intent)
        }
    }
}