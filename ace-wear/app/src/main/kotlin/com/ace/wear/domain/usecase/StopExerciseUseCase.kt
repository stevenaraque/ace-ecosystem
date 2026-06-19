// ace-wear/app/src/main/kotlin/com/ace/wear/domain/usecase/StopExerciseUseCase.kt

package com.ace.wear.domain.usecase

import com.ace.wear.data.repository.WearHealthRepository
import javax.inject.Inject

/**
 * Caso de uso para detener el monitoreo de ejercicio.
 *
 * Responsabilidades:
 * 1. Detener sesion activa (sensor + notificar al movil)
 * 2. Limpiar recursos al cerrar la app
 *
 * El reloj no decide; solo reacciona a ordenes del ViewModel.
 */
class StopExerciseUseCase @Inject constructor(
    private val wearHealthRepository: WearHealthRepository
) {
    /**
     * Detiene la sesion de ejercicio activa.
     * - Detiene el sensor de FC
     * - Notifica al movil que el reloj detuvo
     *
     * @param sessionId ID de la sesion a detener
     */
    operator fun invoke(sessionId: String) {
        wearHealthRepository.stopSession(sessionId)
    }

    /**
     * Libera todos los recursos del repositorio de salud.
     * Llama al cerrar la app (onDestroy de MainActivity).
     */
    fun dispose() {
        wearHealthRepository.dispose()
    }
}