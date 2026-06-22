// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/WearSessionState.kt
package com.ace.wear.presentation

/**
 * Estado de la sesion de ejercicio en el reloj con diagnostico de conexion.
 */
data class WearSessionState(
    /** FC actual en bpm. Null si no hay sesion activa o no hay datos aun. */
    val bpm: Double? = null,

    /** Tiempo transcurrido de la sesion en segundos. 0 si no hay sesion. */
    val elapsedSeconds: Long = 0L,

    /** True si hay una sesion activa (recibio START del movil). */
    val isSessionActive: Boolean = false,

    /** True si el reloj esta conectado al movil (listener de MessageClient activo). */
    val isConnected: Boolean = false,

    /** Cantidad de nodos conectados (para diagnostico). */
    val nodeCount: Int = 0,

    /** Ultimo error de conexion (para diagnostico). */
    val lastError: String? = null,

    /** Logs de diagnostico (ultimos 20 mensajes). */
    val diagLogs: List<String> = emptyList(),

    /** True si el permiso BODY_SENSORS ya fue concedido. */
    val hasSensorPermission: Boolean = false,

    /** True si el usuario nego el permiso BODY_SENSORS. */
    val permissionDenied: Boolean = false,

    /** True si estamos en modo simulacion de FC (para testing sin sensor). */
    val isSimulationMode: Boolean = false,

    /** Contador de muestras enviadas al movil (para diagnostico). */
    val samplesSent: Int = 0
)