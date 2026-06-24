// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/WearSessionState.kt
package com.ace.wear.presentation

/**
 * Estado de la pantalla del reloj.
 */
enum class WearScreenState {
    SPLASH,   // Anillos animados al abrir la app (~2s)
    IDLE,     // Sin sesión activa, esperando START del móvil
    ACTIVE    // Sesión de ejercicio en curso
}

/**
 * Estado de la sesión de ejercicio en el reloj con diagnóstico de conexión.
 */
data class WearSessionState(
    /** FC actual en bpm. Null si no hay sesión activa o no hay datos aún. */
    val bpm: Double? = null,

    /** Tiempo transcurrido de la sesión en segundos. 0 si no hay sesión. */
    val elapsedSeconds: Long = 0L,

    /** True si hay una sesión activa (recibió START del móvil). */
    val isSessionActive: Boolean = false,

    /** Estado actual de la pantalla. */
    val screenState: WearScreenState = WearScreenState.SPLASH,

    /** True si el reloj está conectado al móvil (listener de MessageClient activo). */
    val isConnected: Boolean = false,

    /** Cantidad de nodos conectados (para diagnóstico). */
    val nodeCount: Int = 0,

    /** Último error de conexión (para diagnóstico). */
    val lastError: String? = null,

    /** Logs de diagnóstico (últimos 20 mensajes). */
    val diagLogs: List<String> = emptyList(),

    /** True si el permiso BODY_SENSORS ya fue concedido. */
    val hasSensorPermission: Boolean = false,

    /** True si el usuario negó el permiso BODY_SENSORS. */
    val permissionDenied: Boolean = false,

    /** True si estamos en modo simulación de FC (para testing sin sensor). */
    val isSimulationMode: Boolean = false,

    /** Contador de muestras enviadas al móvil (para diagnóstico). */
    val samplesSent: Int = 0
)