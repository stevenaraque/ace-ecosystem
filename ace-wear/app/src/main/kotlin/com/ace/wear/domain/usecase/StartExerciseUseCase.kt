// ace-wear/app/src/main/kotlin/com/ace/wear/domain/usecase/StartExerciseUseCase.kt

package com.ace.wear.domain.usecase

import com.ace.wear.data.repository.WearHealthRepository
import javax.inject.Inject

/**
 * Caso de uso para iniciar el monitoreo de ejercicio.
 *
 * Delega a WearHealthRepository que:
 * 1. Escucha comandos START/STOP del movil
 * 2. Activa HealthServicesManager al recibir START
 * 3. Envia muestras de FC al movil por DataClient
 *
 * El reloj no decide; solo reacciona a comandos del movil.
 */
class StartExerciseUseCase @Inject constructor(
    private val wearHealthRepository: WearHealthRepository
) {
    /**
     * Inicializa el repositorio de salud.
     * Debe llamarse al arrancar la app (WearApplication o MainActivity).
     */
    operator fun invoke() {
        wearHealthRepository.initialize()
    }
}