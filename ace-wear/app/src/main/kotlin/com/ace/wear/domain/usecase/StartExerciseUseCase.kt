// ace-wear/app/src/main/kotlin/com/ace/wear/domain/usecase/StartExerciseUseCase.kt

package com.ace.wear.domain.usecase

import com.ace.wear.data.repository.WearHealthRepository
import javax.inject.Inject

/**
 * Caso de uso para iniciar el monitoreo de ejercicio.
 *
 * Responsabilidades:
 * 1. Inicializar el repositorio de salud (al arrancar la app)
 * 2. Iniciar sesion de ejercicio (sensor de FC)
 *
 * El reloj no decide; solo reacciona a ordenes del ViewModel.
 */
class StartExerciseUseCase @Inject constructor(
    private val wearHealthRepository: WearHealthRepository
) {
    /**
     * Inicializa el repositorio de salud.
     * Escucha muestras de FC del HealthServicesManager para enviar al movil.
     * Debe llamarse al arrancar la app (WearApplication o MainActivity).
     */
    operator fun invoke() {
        wearHealthRepository.initialize()
    }

    /**
     * Inicia la sesion de ejercicio en el reloj.
     * Activa el sensor de FC.
     *
     * @param sessionId ID de la sesion a iniciar
     */
    fun startSession(sessionId: String) {
        wearHealthRepository.startSession(sessionId)
    }
}