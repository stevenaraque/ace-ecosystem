package com.ace.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ace.mobile.MobileApplication
import com.ace.mobile.presentation.MainActivity
import com.ace.mobile.domain.model.HeartRateSample
import com.ace.mobile.domain.usecase.wear.BuildExerciseBlockUseCase
import com.ace.mobile.domain.usecase.wear.ReceiveWearDataUseCase
import com.ace.mobile.data.local.database.dao.BlockDao
import com.ace.mobile.data.local.database.entity.LocalBlockEntity
import com.ace.shared.enums.BlockStatus
import com.ace.shared.enums.SportType
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class ExerciseSyncService : Service(), MessageClient.OnMessageReceivedListener {

    companion object {
        const val ACTION_START_SESSION = "com.ace.mobile.START_SESSION"
        const val ACTION_STOP_SESSION = "com.ace.mobile.STOP_SESSION"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_SPORT_TYPE = "sport_type"
        const val EXTRA_USER_ID = "user_id"

        const val BUFFER_CAPACITY = 300
        const val DISCONNECT_TIMEOUT_MS = 5000L
        const val BLOCK_DURATION_MS = 30000L

        private const val TAG = "ExerciseSyncService"
        private const val PATH_SESSION_STATUS = "/ace/session/"
        private const val KEY_COMMAND = "command"
        private const val KEY_SESSION_ID = "sessionId"
        private const val FOREGROUND_NOTIFICATION_ID = 1001
    }

    // ─── Binder para clientes bound (ViewModel) ───
    @Suppress("unused")
    inner class LocalBinder : Binder() {
        fun getService(): ExerciseSyncService = this@ExerciseSyncService
    }
    private val binder = LocalBinder()

    // ─── Inyeccion Hilt ───
    @Inject
    lateinit var receiveWearDataUseCase: ReceiveWearDataUseCase

    @Inject
    lateinit var buildExerciseBlockUseCase: BuildExerciseBlockUseCase

    @Inject
    lateinit var blockDao: BlockDao

    // ─── MessageClient (declarado a nivel de clase, no en onCreate) ───
    private var messageClient: MessageClient? = null

    // ─── Scopes ───
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var sessionJob: Job? = null
    private var sessionScope: CoroutineScope? = null

    // ─── Estado expuesto via StateFlow ───
    private val _heartRate = MutableStateFlow(0.0)
    val heartRate: StateFlow<Double> = _heartRate.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    private val _blockCount = MutableStateFlow(0)
    val blockCount: StateFlow<Int> = _blockCount.asStateFlow()

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _sportType = MutableStateFlow("RUNNING")
    val sportType: StateFlow<String> = _sportType.asStateFlow()

    // ─── Eventos puntuales via SharedFlow ───
    private val _sessionStopped = MutableSharedFlow<SessionStoppedEvent>(extraBufferCapacity = 1)
    val sessionStopped: SharedFlow<SessionStoppedEvent> = _sessionStopped.asSharedFlow()

    // ─── Interno ───
    private val _buffer = ArrayDeque<HeartRateSample>(BUFFER_CAPACITY)
    private var _currentSessionId: String? = null
    private var _currentUserId: String = "unknown"
    private var _lastSampleTime = 0L
    private var _stopRequestedFromMobile = false

    data class SessionStoppedEvent(
        val sessionId: String,
        val reason: StopReason,
        val totalBlocks: Int,
        val totalSamples: Int,
        val elapsedSeconds: Int
    )

    enum class StopReason {
        USER_STOPPED_WATCH,
        USER_STOPPED_MOBILE,
        DISCONNECTED
    }

    // ─── Service Lifecycle ───

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        messageClient = Wearable.getMessageClient(this)
        android.util.Log.i(TAG, "ExerciseSyncService creado, MessageClient listo")
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SESSION -> {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return START_STICKY
                val sportType = intent.getStringExtra(EXTRA_SPORT_TYPE) ?: "RUNNING"
                val userId = intent.getStringExtra(EXTRA_USER_ID) ?: "unknown"
                startSession(sessionId, sportType, userId)
            }
            ACTION_STOP_SESSION -> {
                handleStopFromMobile()
            }
        }
        return START_STICKY
    }

    // ─── MessageClient: recibir STOPPED del reloj ───

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val data = messageEvent.data

        android.util.Log.d(TAG, "onMessageReceived: path=$path, dataSize=${data.size}")

        if (path.startsWith(PATH_SESSION_STATUS)) {
            handleSessionStatusMessage(path, data)
        } else {
            android.util.Log.w(TAG, "Mensaje con path desconocido: $path")
        }
    }

    private fun handleSessionStatusMessage(path: String, data: ByteArray) {
        try {
            val dataMap = DataMap.fromByteArray(data)
            val command = dataMap.getString(KEY_COMMAND, "")
            val sessionId = dataMap.getString(KEY_SESSION_ID, "")

            android.util.Log.d(TAG, "Comando recibido: command=$command, sessionId=$sessionId")

            when (command) {
                "STOPPED" -> {
                    android.util.Log.i(TAG, "STOPPED recibido de reloj: sessionId=$sessionId")
                    stopSessionInternal(StopReason.USER_STOPPED_WATCH)
                }
                else -> {
                    android.util.Log.w(TAG, "Comando desconocido: $command")
                }
            }

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error parseando mensaje de sesion", e)
        }
    }

    // ─── Sesion ───

    private fun startSession(sessionId: String, sportType: String, userId: String) {
        if (_isSessionActive.value) {
            android.util.Log.w(TAG, "Sesion ya activa, ignorando START")
            return
        }

        android.util.Log.i(TAG, "=== INICIANDO SESION: $sessionId ===")
        _currentSessionId = sessionId
        _currentUserId = userId
        _sportType.value = sportType
        _isSessionActive.value = true
        _elapsedSeconds.value = 0
        _blockCount.value = 0
        _heartRate.value = 0.0
        _stopRequestedFromMobile = false

        startForeground(FOREGROUND_NOTIFICATION_ID, buildNotification("Starting...", sportType, 0, 0))

        // Scope de sesion (separado del scope del servicio)
        sessionJob = SupervisorJob()
        sessionScope = CoroutineScope(Dispatchers.Default + sessionJob!!)

        // MessageClient: escuchar STOPPED del reloj
        messageClient?.addListener(this)
        android.util.Log.i(TAG, "MessageClient listener registrado para STOPPED")

        // Timer de sesion
        sessionScope?.launch {
            while (_isSessionActive.value) {
                delay(1000)
                _elapsedSeconds.value += 1
                updateNotification()
            }
        }

        // Detector de desconexion
        sessionScope?.launch {
            while (_isSessionActive.value) {
                delay(1000)
                val timeSinceLastSample = System.currentTimeMillis() - _lastSampleTime
                val wasConnected = _isConnected.value
                _isConnected.value = timeSinceLastSample < DISCONNECT_TIMEOUT_MS

                if (wasConnected && !_isConnected.value) {
                    android.util.Log.w(TAG, "Desconexion detectada: ${timeSinceLastSample}ms sin muestras")
                }
            }
        }

        // Recepcion de FC del reloj
        receiveWearDataUseCase.observeHeartRate()
            .onEach { sample ->
                _lastSampleTime = System.currentTimeMillis()
                _isConnected.value = true
                _heartRate.value = sample.bpm

                synchronized(_buffer) {
                    if (_buffer.size >= BUFFER_CAPACITY) _buffer.removeFirst()
                    _buffer.addLast(sample)
                }

                checkBlockClosure(sessionId, sportType, userId)
            }
            .catch { e ->
                android.util.Log.e(TAG, "Error en flow de FC", e)
                _isConnected.value = false
            }
            .launchIn(sessionScope!!)
    }

    private fun handleStopFromMobile() {
        if (!_isSessionActive.value) {
            android.util.Log.w(TAG, "handleStopFromMobile pero no hay sesion activa")
            return
        }

        android.util.Log.i(TAG, "STOP solicitado desde mobile, esperando confirmacion del reloj...")
        _stopRequestedFromMobile = true

        // Timeout: si el reloj no responde en 10s, forzamos stop por desconexion
        sessionScope?.launch {
            delay(10000L)
            if (_isSessionActive.value && _stopRequestedFromMobile) {
                android.util.Log.w(TAG, "Timeout esperando STOPPED del reloj, forzando stop por desconexion")
                stopSessionInternal(StopReason.DISCONNECTED)
            }
        }
    }

    private fun stopSessionInternal(reason: StopReason) {
        if (!_isSessionActive.value) return

        val sessionId = _currentSessionId ?: return
        android.util.Log.i(TAG, "=== DETENIENDO SESION: $reason ===")

        _isSessionActive.value = false
        _stopRequestedFromMobile = false

        // Cancelar scope de sesion (el servicio sigue vivo)
        sessionScope?.cancel()
        sessionJob?.cancel()
        sessionScope = null
        sessionJob = null

        // Desregistrar MessageClient
        messageClient?.removeListener(this)
        android.util.Log.i(TAG, "MessageClient listener desregistrado")

        // Procesar bloque final
        val totalSamples: Int
        synchronized(_buffer) {
            totalSamples = _buffer.size
            if (_buffer.isNotEmpty()) {
                val samples = _buffer.toList()
                val durationMs = samples.last().timestamp - samples.first().timestamp
                _buffer.clear()

                android.util.Log.d(TAG,
                    "Final block: $totalSamples samples, " +
                            "${durationMs / 1000}s")
                            
                val sportTypeStr = _sportType.value
                val sportTypeEnum = try { SportType.valueOf(sportTypeStr) } catch(e: Exception) { SportType.RUNNING }
                
                serviceScope.launch(Dispatchers.IO) {
                    val blockId = UUID.randomUUID().toString()
                    val entity = LocalBlockEntity(
                        blockId = blockId,
                        sessionId = sessionId,
                        userId = _currentUserId,
                        timestampStart = samples.first().timestamp,
                        timestampEnd = samples.last().timestamp,
                        durationSeconds = (durationMs / 1000).toInt(),
                        avgBpm = samples.map { it.bpm }.average(),
                        maxBpm = samples.map { it.bpm }.maxOrNull() ?: 0.0,
                        minBpm = samples.map { it.bpm }.minOrNull() ?: 0.0,
                        sampleCount = samples.size,
                        sportType = sportTypeEnum,
                        xpCalculated = null, // TODO: Calcular XP usando CalculateBlockXpUseCase (Ver Apendice S5)
                        status = BlockStatus.PENDING // TODO: Actualizar estadisticas usando AccumulateStatsUseCase (Ver Apendice S10)
                    )
                    blockDao.insert(entity)
                    android.util.Log.i(TAG, "Final block persisted: $blockId")
                }
            }
        }

        // Notificar al ViewModel via SharedFlow
        val event = SessionStoppedEvent(
            sessionId = sessionId,
            reason = reason,
            totalBlocks = _blockCount.value,
            totalSamples = totalSamples,
            elapsedSeconds = _elapsedSeconds.value
        )
        serviceScope.launch {
            _sessionStopped.emit(event)
            android.util.Log.i(TAG, "SessionStoppedEvent emitido: $event")
        }

        // Detener foreground pero mantener servicio vivo para re-binding
        stopForeground(STOP_FOREGROUND_REMOVE)

        android.util.Log.i(TAG, "ExerciseSyncService: sesion detenida, servicio sigue vivo para re-binding")
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

                sessionScope?.launch(Dispatchers.IO) {
                    android.util.Log.d(TAG,
                        "BLOCK CLOSED: ${samplesForBlock.size} samples, " +
                                "${durationMs / 1000}s duration, " +
                                "avgBPM: ${samplesForBlock.map { it.bpm }.average()}")

                    _blockCount.value += 1

                    val blockId = UUID.randomUUID().toString()
                    val sportTypeEnum = try { SportType.valueOf(sportType) } catch(e: Exception) { SportType.RUNNING }
                    
                    val entity = LocalBlockEntity(
                        blockId = blockId,
                        sessionId = sessionId,
                        userId = userId,
                        timestampStart = firstSample.timestamp,
                        timestampEnd = lastSample.timestamp,
                        durationSeconds = (durationMs / 1000).toInt(),
                        avgBpm = samplesForBlock.map { it.bpm }.average(),
                        maxBpm = samplesForBlock.map { it.bpm }.maxOrNull() ?: 0.0,
                        minBpm = samplesForBlock.map { it.bpm }.minOrNull() ?: 0.0,
                        sampleCount = samplesForBlock.size,
                        sportType = sportTypeEnum,
                        xpCalculated = null, // TODO: Calcular XP usando CalculateBlockXpUseCase (Ver Apendice S5)
                        status = BlockStatus.PENDING // TODO: Actualizar estadisticas usando AccumulateStatsUseCase (Ver Apendice S10)
                    )
                    
                    blockDao.insert(entity)
                    android.util.Log.i(TAG, "Block persisted in Room: $blockId")
                }
            }
        }
    }

    private fun updateNotification() {
        val notification = buildNotification(
            "FC: ${_heartRate.value.toInt()} bpm · ${_elapsedSeconds.value / 60}:${
                String.format(Locale.getDefault(), "%02d", _elapsedSeconds.value % 60)
            }",
            _sportType.value,
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
        messageClient?.removeListener(this)
        serviceScope.cancel()
        super.onDestroy()
        android.util.Log.i(TAG, "ExerciseSyncService onDestroy")
    }
}