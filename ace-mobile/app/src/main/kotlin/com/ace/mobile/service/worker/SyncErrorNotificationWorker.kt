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
import com.ace.mobile.data.local.database.dao.BlockDao
import com.ace.shared.enums.BlockStatus
import com.ace.shared.enums.NotificationChannelId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "SyncErrorNotifWorker"

@HiltWorker
class SyncErrorNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val blockDao: BlockDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "SyncErrorNotificationWorker started")

        val errorCount = blockDao.countByStatus(BlockStatus.ERROR.name)

        if (errorCount > 0) {
            showNotification(errorCount)
            Log.i(TAG, "Sync error notification shown: $errorCount blocks")
        } else {
            Log.d(TAG, "No sync errors, skipping notification")
        }

        return Result.success()
    }

    private fun showNotification(errorCount: Int) {
        val context = applicationContext
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationChannelId.SYNC_ERROR.channelId,
                "Errores de Sincronización",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones cuando hay errores al sincronizar bloques"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(
            context,
            NotificationChannelId.SYNC_ERROR.channelId
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("A.C.E — Sincronización")
            .setContentText("$errorCount bloques no pudieron sincronizarse. Toca para revisar.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(SYNC_ERROR_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val SYNC_ERROR_NOTIFICATION_ID = 3001
    }
}