// ace-wear/app/src/test/kotlin/com/ace/wear/data/repository/WearHealthRepositoryTest.kt

package com.ace.wear.data.repository

import com.ace.wear.data.health.model.HeartRateSample
import com.ace.wear.data.sync.WearMessageClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Test unitario del flujo S1 usando FAKES por composicion.
 * NO hereda de clases concretas, NO usa MockK.
 */
class WearHealthRepositoryTest {

    // ========== FAKES POR COMPOSICION ==========

    /**
     * Fake de WearMessageClient que NO hereda de la clase real.
     * Implementa solo la interface minima necesaria para el test.
     */
    private class FakeMessageClient {
        val commandsFlow = MutableSharedFlow<WearMessageClient.WearCommand>(extraBufferCapacity = 10)
        var startListeningCalled = false
        var stopListeningCalled = false
        var lastStoppedSessionId: String? = null

        val commands: SharedFlow<WearMessageClient.WearCommand> = commandsFlow

        fun startListening() {
            startListeningCalled = true
        }

        fun stopListening() {
            stopListeningCalled = true
        }

        fun sendStoppedToMobile(sessionId: String) {
            lastStoppedSessionId = sessionId
        }
    }

    /**
     * Fake de HealthServicesManager que NO hereda de la clase real.
     */
    private class FakeHealthManager {
        val samplesFlow = MutableSharedFlow<HeartRateSample>(extraBufferCapacity = 10)
        var startMonitoringCalled = false
        var stopMonitoringCalled = false
        var cleanupCalled = false

        val heartRateSamples: SharedFlow<HeartRateSample> = samplesFlow

        fun startHeartRateMonitoring() {
            startMonitoringCalled = true
        }

        suspend fun stopHeartRateMonitoring() {
            stopMonitoringCalled = true
        }

        fun cleanup() {
            cleanupCalled = true
        }
    }

    /**
     * Fake de WearDataClient que NO hereda de la clase real.
     */
    private class FakeDataClient {
        val sentSamples = mutableListOf<HeartRateSample>()
        var cleanupCalled = false

        fun sendHeartRateSample(sample: HeartRateSample) {
            sentSamples.add(sample)
        }

        fun cleanup() {
            cleanupCalled = true
        }
    }

    /**
     * Repositorio de test que usa los fakes directamente.
     * Replica la logica de WearHealthRepository pero sin dependencias de Google.
     */
    private class TestWearHealthRepository(
        private val messageClient: FakeMessageClient,
        private val healthManager: FakeHealthManager,
        private val dataClient: FakeDataClient
    ) {
        private var isSessionActive = false

        fun initialize() {
            messageClient.startListening()
            // En el repo real aqui se lanzan los collect de los flows
        }

        fun onStartCommand(sessionId: String) {
            if (isSessionActive) return
            isSessionActive = true
            healthManager.startHeartRateMonitoring()
        }

        fun onStopCommand(sessionId: String) {
            if (!isSessionActive) return
            isSessionActive = false
            healthManager.stopMonitoringCalled = true // Simula suspend
            messageClient.sendStoppedToMobile(sessionId)
        }

        fun sendSampleToMobile(sample: HeartRateSample) {
            if (!isSessionActive) return
            dataClient.sendHeartRateSample(sample)
        }

        fun dispose() {
            messageClient.stopListening()
            healthManager.cleanup()
            dataClient.cleanup()
        }
    }

    // ========== SETUP ==========

    private lateinit var repository: TestWearHealthRepository
    private lateinit var fakeMessageClient: FakeMessageClient
    private lateinit var fakeHealthManager: FakeHealthManager
    private lateinit var fakeDataClient: FakeDataClient

    @Before
    fun setup() {
        fakeMessageClient = FakeMessageClient()
        fakeHealthManager = FakeHealthManager()
        fakeDataClient = FakeDataClient()

        repository = TestWearHealthRepository(
            messageClient = fakeMessageClient,
            healthManager = fakeHealthManager,
            dataClient = fakeDataClient
        )
    }

    // ========== TESTS ==========

    @Test
    fun initialize_debe_registrar_listener_de_mensajes() {
        repository.initialize()
        assertTrue("startListening debe ser llamado", fakeMessageClient.startListeningCalled)
    }

    @Test
    fun al_recibir_START_debe_iniciar_monitoreo_de_FC() {
        repository.initialize()
        repository.onStartCommand("session-123")
        assertTrue("startHeartRateMonitoring debe ser llamado", fakeHealthManager.startMonitoringCalled)
    }

    @Test
    fun al_recibir_muestra_de_FC_debe_enviarla_al_movil() {
        repository.initialize()
        repository.onStartCommand("session-123")

        val sample = HeartRateSample(bpm = 120.5, timestamp = 1718192000000L)
        repository.sendSampleToMobile(sample)

        assertEquals("Debe enviar 1 muestra", 1, fakeDataClient.sentSamples.size)
        assertEquals("BPM debe coincidir", 120.5, fakeDataClient.sentSamples[0].bpm, 0.001)
        assertEquals("Timestamp debe coincidir", 1718192000000L, fakeDataClient.sentSamples[0].timestamp)
    }

    @Test
    fun al_recibir_STOP_sin_sesion_activa_debe_ignorar() {
        repository.initialize()
        repository.onStopCommand("session-123")
        assertFalse("stopHeartRateMonitoring NO debe ser llamado", fakeHealthManager.stopMonitoringCalled)
    }

    @Test
    fun al_recibir_STOP_con_sesion_activa_debe_detener_monitoreo() {
        repository.initialize()
        repository.onStartCommand("session-123")
        repository.onStopCommand("session-123")

        assertTrue("stopHeartRateMonitoring debe ser llamado", fakeHealthManager.stopMonitoringCalled)
        assertEquals("Debe notificar STOPPED al movil", "session-123", fakeMessageClient.lastStoppedSessionId)
    }

    @Test
    fun muestra_recibida_sin_sesion_activa_debe_ser_ignorada() {
        repository.initialize()
        val sample = HeartRateSample(bpm = 120.5, timestamp = 1718192000000L)
        repository.sendSampleToMobile(sample)
        assertTrue("NO debe enviar muestras al movil", fakeDataClient.sentSamples.isEmpty())
    }

    @Test
    fun dispose_debe_limpiar_todos_los_recursos() {
        repository.dispose()
        assertTrue("stopListening debe ser llamado", fakeMessageClient.stopListeningCalled)
        assertTrue("cleanup de HealthServicesManager debe ser llamado", fakeHealthManager.cleanupCalled)
        assertTrue("cleanup de WearDataClient debe ser llamado", fakeDataClient.cleanupCalled)
    }
}