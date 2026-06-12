// ace-wear/app/src/test/kotlin/com/ace/wear/data/repository/WearHealthRepositoryTest.kt

package com.ace.wear.data.repository

import com.ace.wear.data.health.HealthServicesManager
import com.ace.wear.data.health.model.HeartRateSample
import com.ace.wear.data.sync.WearDataClient
import com.ace.wear.data.sync.WearMessageClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Test unitario del flujo S1 usando FAKES manuales.
 */
class WearHealthRepositoryTest {

    // ========== FAKES ==========

    private class FakeWearMessageClient : WearMessageClient(
        messageClient = org.mockito.kotlin.mock()
    ) {
        val commandsFlow = MutableSharedFlow<WearMessageClient.WearCommand>()
        var startListeningCalled = false
        var stopListeningCalled = false
        var lastStoppedSessionId: String? = null

        override val commands: SharedFlow<WearMessageClient.WearCommand> = commandsFlow

        override fun startListening() {
            startListeningCalled = true
        }

        override fun stopListening() {
            stopListeningCalled = true
        }

        override fun sendStoppedToMobile(sessionId: String) {
            lastStoppedSessionId = sessionId
        }
    }

    private class FakeHealthServicesManager : HealthServicesManager(
        healthServicesClient = org.mockito.kotlin.mock()
    ) {
        val samplesFlow = MutableSharedFlow<HeartRateSample>()
        var startMonitoringCalled = false
        var stopMonitoringCalled = false
        var cleanupCalled = false

        override val heartRateSamples: SharedFlow<HeartRateSample> = samplesFlow
        override val availability: SharedFlow<Any> = MutableSharedFlow()

        override fun startHeartRateMonitoring() {
            startMonitoringCalled = true
        }

        override suspend fun stopHeartRateMonitoring() {
            stopMonitoringCalled = true
        }

        override fun cleanup() {
            cleanupCalled = true
        }
    }

    private class FakeWearDataClient : WearDataClient(
        dataClient = org.mockito.kotlin.mock()
    ) {
        val sentSamples = mutableListOf<HeartRateSample>()
        var cleanupCalled = false

        override fun sendHeartRateSample(sample: HeartRateSample) {
            sentSamples.add(sample)
        }

        override fun cleanup() {
            cleanupCalled = true
        }
    }

    // ========== TEST ==========

    private lateinit var repository: WearHealthRepository
    private lateinit var fakeMessageClient: FakeWearMessageClient
    private lateinit var fakeHealthManager: FakeHealthServicesManager
    private lateinit var fakeDataClient: FakeWearDataClient

    @Before
    fun setup() {
        fakeMessageClient = FakeWearMessageClient()
        fakeHealthManager = FakeHealthServicesManager()
        fakeDataClient = FakeWearDataClient()

        repository = WearHealthRepository(
            wearMessageClient = fakeMessageClient,
            healthServicesManager = fakeHealthManager,
            wearDataClient = fakeDataClient
        )
    }

    @Test
    fun initialize_debe_registrar_listener_de_mensajes() {
        repository.initialize()
        assertTrue("startListening debe ser llamado", fakeMessageClient.startListeningCalled)
    }

    @Test
    fun al_recibir_START_debe_iniciar_monitoreo_de_FC() = runBlocking {
        repository.initialize()
        fakeMessageClient.commandsFlow.emit(WearMessageClient.WearCommand.Start("session-123"))
        kotlinx.coroutines.delay(100)
        assertTrue("startHeartRateMonitoring debe ser llamado", fakeHealthManager.startMonitoringCalled)
    }

    @Test
    fun al_recibir_muestra_de_FC_debe_enviarla_al_movil() = runBlocking {
        repository.initialize()
        fakeMessageClient.commandsFlow.emit(WearMessageClient.WearCommand.Start("session-123"))
        kotlinx.coroutines.delay(100)

        val sample = HeartRateSample(bpm = 120.5, timestamp = 1718192000000L)
        fakeHealthManager.samplesFlow.emit(sample)
        kotlinx.coroutines.delay(100)

        assertEquals("Debe enviar 1 muestra", 1, fakeDataClient.sentSamples.size)
        assertEquals("BPM debe coincidir", 120.5, fakeDataClient.sentSamples[0].bpm, 0.001)
        assertEquals("Timestamp debe coincidir", 1718192000000L, fakeDataClient.sentSamples[0].timestamp)
    }

    @Test
    fun al_recibir_STOP_sin_sesion_activa_debe_ignorar() = runBlocking {
        repository.initialize()
        fakeMessageClient.commandsFlow.emit(WearMessageClient.WearCommand.Stop("session-123"))
        kotlinx.coroutines.delay(100)
        assertFalse("stopHeartRateMonitoring NO debe ser llamado", fakeHealthManager.stopMonitoringCalled)
    }

    @Test
    fun al_recibir_STOP_con_sesion_activa_debe_detener_monitoreo() = runBlocking {
        repository.initialize()
        fakeMessageClient.commandsFlow.emit(WearMessageClient.WearCommand.Start("session-123"))
        kotlinx.coroutines.delay(100)

        fakeMessageClient.commandsFlow.emit(WearMessageClient.WearCommand.Stop("session-123"))
        kotlinx.coroutines.delay(100)

        assertTrue("stopHeartRateMonitoring debe ser llamado", fakeHealthManager.stopMonitoringCalled)
        assertEquals("Debe notificar STOPPED al movil", "session-123", fakeMessageClient.lastStoppedSessionId)
    }

    @Test
    fun muestra_recibida_sin_sesion_activa_debe_ser_ignorada() = runBlocking {
        repository.initialize()
        val sample = HeartRateSample(bpm = 120.5, timestamp = 1718192000000L)
        fakeHealthManager.samplesFlow.emit(sample)
        kotlinx.coroutines.delay(100)
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