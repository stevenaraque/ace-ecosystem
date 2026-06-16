// ace-wear/app/src/main/kotlin/com/ace/wear/presentation/WearSessionState.kt

package com.ace.wear.presentation

/**
 * Estado de la sesion de ejercicio en el reloj.
 *
 * Representa lo que se muestra en la UI:
 * - FC en vivo (del HealthServicesManager)
 * - Tiempo transcurrido desde START
 * - Si hay sesion activa (para mostrar/ocultar boton DETENER)
 * - Si esta conectado al movil (para ConnectionStatusChip)
 */
data class WearSessionState(
    /** FC actual en bpm. Null si no hay sesion activa o no hay datos aun. */
    val bpm: Double? = null,

    /** Tiempo transcurrido de la sesion en segundos. 0 si no hay sesion. */
    val elapsedSeconds: Long = 0L,

    /** True si hay una sesion activa (recibio START del movil). */
    val isSessionActive: Boolean = false,

    /** True si el reloj esta conectado al movil (listener de MessageClient activo). */
    val isConnected: Boolean = false
)