package com.ace.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ace.mobile.MobileApplication
import com.ace.mobile.presentation.MainActivity
import com.ace.mobile.domain.model.ExerciseSession
import com.ace.mobile.domain.model.HeartRateSample
import com.ace.mobile.domain.usecase.wear.BuildExerciseBlockUseCase
import com.ace.mobile.domain.usecase.wear.ReceiveWearDataUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExerciseSyncService : Service() {

    companion object {
        const val ACTION_START_SESSION = "com.ace.mobile.START_SESSION"
        const val ACTION_STOP_SESSION = "com.ace.mobile.STOP_SESSION"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_SPORT_TYPE = "sport_type"
        const val EXTRA_USER_ID = "user_id"

        const val BUFFER_CAPACITY = 300
        const val DISCONNECT_TIMEOUT_MS = 5000L

        // Para pruebas: 30 segundos
        const val BLOCK_DURATION_MS = 30000L
    }

    @Inject
    lateinit var receiveWearDataUseCase: ReceiveWearDataUseCase

    @Inject
    lateinit var buildExerciseBlockUseCase: BuildExerciseBlockUseCase

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private val _buffer = ArrayDeque<HeartRateSample>(BUFFER_CAPACITY)
    private var _currentSession: ExerciseSession? = null
    private var _lastSampleTime = 0L
    private var _isCollecting = false

    private val _heartRate = MutableStateFlow(0.0)
    val heartRate: StateFlow<Double> = _heartRate.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _blockCount = MutableStateFlow(0)
    val blockCount: StateFlow<Int> = _blockCount.asStateFlow()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SESSION -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return START_NOT_STICKY
                val sportType = intent.getStringExtra(EXTRA_SPORT_TYPE) ?: "RUNNING"
                val userId = intent.getStringExtra(EXTRA_USER_ID) ?: "unknown"
                startSession(sessionId, sportType, userId)
            }
            ACTION_STOP_SESSION -> {
                stopSession()
            }
        }
        return START_STICKY
    }

    private fun startSession(sessionId: String, sportType: String, userId: String) {
        startForeground(FOREGROUND_NOTIFICATION_ID, buildNotification("Starting...", sportType, 0, 0))

        _isCollecting = true
        _lastSampleTime = System.currentTimeMillis()

        // Timer de sesión
        serviceScope.launch {
            while (_isCollecting) {
                delay(1000)
                _elapsedSeconds.value += 1
                updateNotification(sportType)
            }
        }

        // Detector de desconexión
        serviceScope.launch {
            while (_isCollecting) {
                delay(1000)
                val timeSinceLastSample = System.currentTimeMillis() - _lastSampleTime
                _isConnected.value = timeSinceLastSample < DISCONNECT_TIMEOUT_MS
            }
        }

        // Recibir datos del reloj
        receiveWearDataUseCase.observeHeartRate()
            .onEach { sample ->
                _lastSampleTime = System.currentTimeMillis()
                _isConnected.value = true
                _heartRate.value = sample.bpm

                synchronized(_buffer) {
                    if (_buffer.size >= BUFFER_CAPACITY) {
                        _buffer.removeFirst()
                    }
                    _buffer.addLast(sample)
                }

                checkBlockClosure(sessionId, sportType, userId)
            }
            .catch { e ->
                android.util.Log.e("ExerciseSyncService", "Error en flow de FC", e)
                _isConnected.value = false
            }
            .launchIn(serviceScope)
    }

    private fun checkBlockClosure(sessionId: String, sportType: String, userId: String) {
        synchronized(_buffer) {
            if (_buffer.isEmpty()) return

            val firstSample = _buffer.first()
            val lastSample = _buffer.last()
            val durationMs = lastSample.timestamp - firstSample.timestamp

            if (durationMs >= BLOCK_DURATION_MS) {
                val samplesForBlock = _buffer.toList()
                _buffer.clear()

                serviceScope.launch(Dispatchers.IO) {
                    android.util.Log.d("ExerciseSyncService",
                        "BLOCK CLOSED: ${samplesForBlock.size} samples, " +
                                "${durationMs / 1000}s duration, " +
                                "avgBPM: ${samplesForBlock.map { it.bpm }.average()}")

                    _blockCount.value += 1

                    // TODO: Guardar bloque en SQLite usando BlockDao
                    // TODO: Calcular XP usando CalculateBlockXpUseCase
                    // TODO: Actualizar estadísticas usando AccumulateStatsUseCase
                }
            }
        }
    }

    private fun stopSession() {
        _isCollecting = false

        synchronized(_buffer) {
            if (_buffer.isNotEmpty()) {
                val samples = _buffer.toList()
                _buffer.clear()

                // Procesar bloque final corto
                serviceScope.launch(Dispatchers.IO) {
                    android.util.Log.d("ExerciseSyncService",
                        "Final block: ${samples.size} samples, " +
                                "${(samples.last().timestamp - samples.first().timestamp)/1000}s")
                    // TODO: Guardar bloque final en SQLite
                    // TODO: Calcular XP reducida para bloque corto
                }
            }
        }

        serviceScope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(sportType: String) {
        val notification = buildNotification(
            "FC: ${_heartRate.value.toInt()} bpm · ${_elapsedSeconds.value / 60}:${String.format("%02d", _elapsedSeconds.value % 60)}",
            sportType,
            _blockCount.value,
            _elapsedSeconds.value
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun buildNotification(content: String, sportType: String, blocks: Int, seconds: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ExerciseSyncService::class.java).apply {
            action = ACTION_STOP_SESSION
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(
            this,
            MobileApplication.CHANNEL_SESSION_ACTIVE
        )
            .setContentTitle("A.C.E — $sportType Active · $blocks blocks")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_pause, "STOP", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            MobileApplication.CHANNEL_SESSION_ACTIVE,
            "Active Exercise Session",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when an exercise session is active"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

private const val FOREGROUND_NOTIFICATION_ID = 1001